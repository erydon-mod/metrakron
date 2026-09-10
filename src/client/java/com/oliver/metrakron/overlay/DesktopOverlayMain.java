package com.oliver.metrakron.overlay;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Small child-process renderer for supported desktop environments.
 *
 * <p>This class deliberately has no Minecraft, Fabric, or native-library
 * dependencies so its event loop remains independent when Minecraft's window
 * thread stops responding.</p>
 */
public final class DesktopOverlayMain {
    private static final long POLL_MILLIS = 100L;
    private static final long STATUS_INTERVAL_NANOS = 1_000_000_000L;

    private DesktopOverlayMain() {
    }

    public static void main(String[] rawArguments) {
        Arguments arguments = null;
        try {
            arguments = Arguments.parse(rawArguments);
            run(arguments);
        } catch (Throwable exception) {
            if (arguments != null) {
                writeFailureStatus(arguments, exception);
            }
            exception.printStackTrace();
        }
    }

    private static void run(Arguments arguments) throws Exception {
        DesktopOverlayPlatform.Capability capability = DesktopOverlayPlatform.current();
        if (!capability.supported()) {
            throw new IllegalStateException(capability.reason());
        }
        if (GraphicsEnvironment.isHeadless()) {
            throw new IllegalStateException("A graphical desktop session is required");
        }

        ProcessHandle parent = ProcessHandle.of(arguments.parentPid)
                .orElseThrow(() -> new IllegalStateException("Minecraft parent process is unavailable"));
        OverlayApplication application = OverlayApplication.create();
        long lastRevision = Long.MIN_VALUE;
        long lastReportedRevision = Long.MIN_VALUE;
        long lastReportedPaintedRevision = Long.MIN_VALUE;
        boolean lastReportedVisible = false;
        long nextStatusNanos = 0L;

        writeStatus(arguments, application.status(System.nanoTime()), "ready");
        try {
            while (parent.isAlive()) {
                try {
                    Path appearanceFile = arguments.stateFile.getParent().resolve("appearance.properties");
                    try { AppearanceSettings.preview(AppearanceSettings.read(appearanceFile)); }
                    catch (IOException | IllegalArgumentException ignored) { /* Retain the last good appearance. */ }
                    if (Files.isRegularFile(arguments.stateFile)) {
                        OverlayState state = OverlayStateFile.read(arguments.stateFile);
                        if (state.sessionId().equals(arguments.sessionId)
                                && state.parentPid() == arguments.parentPid
                                && state.revision() != lastRevision) {
                            application.apply(state, System.nanoTime());
                            lastRevision = state.revision();
                        }
                    }
                } catch (IOException | RuntimeException ignored) {
                    // Preserve the last complete state if a file replacement is briefly in flight.
                }

                application.repaintIfVisible();
                long nowNanos = System.nanoTime();
                HelperStatus helperStatus = application.status(nowNanos);
                boolean presentationChanged = helperStatus.revision != lastReportedRevision
                        || helperStatus.paintedRevision != lastReportedPaintedRevision
                        || helperStatus.visible != lastReportedVisible;
                if (presentationChanged || nowNanos >= nextStatusNanos) {
                    writeStatus(arguments, helperStatus, "running");
                    lastReportedRevision = helperStatus.revision;
                    lastReportedPaintedRevision = helperStatus.paintedRevision;
                    lastReportedVisible = helperStatus.visible;
                    nextStatusNanos = nowNanos + STATUS_INTERVAL_NANOS;
                }
                Thread.sleep(POLL_MILLIS);
            }
        } finally {
            application.close();
            writeStatus(arguments, application.status(System.nanoTime()), "stopped");
        }
    }

    private static void writeStatus(
            Arguments arguments,
            HelperStatus helperStatus,
            String processState
    ) {
        Properties properties = new Properties();
        properties.setProperty("processState", processState);
        properties.setProperty("sessionId", arguments.sessionId);
        properties.setProperty("helperPid", Long.toString(ProcessHandle.current().pid()));
        properties.setProperty("parentPid", Long.toString(arguments.parentPid));
        properties.setProperty("revision", Long.toString(helperStatus.revision));
        properties.setProperty("paintedRevision", Long.toString(helperStatus.paintedRevision));
        properties.setProperty("active", Boolean.toString(helperStatus.active));
        properties.setProperty("visible", Boolean.toString(helperStatus.visible));
        properties.setProperty("sequence", Long.toString(helperStatus.sequence));
        properties.setProperty("framesRendered", Long.toString(helperStatus.framesRendered));
        properties.setProperty("heading", helperStatus.heading);
        properties.setProperty("displayedTime", helperStatus.displayedTime);
        properties.setProperty("detail", helperStatus.detail);
        properties.setProperty("updatedAtEpochMillis", Long.toString(System.currentTimeMillis()));
        try {
            OverlayStateFile.writeProperties(
                    arguments.statusFile,
                    properties,
                    "ERYDON Metrakron window overlay status"
            );
        } catch (IOException ignored) {
            // Status is diagnostic only; it must never affect the timer window.
        }
    }

    private static void writeFailureStatus(Arguments arguments, Throwable exception) {
        Properties properties = new Properties();
        properties.setProperty("processState", "error");
        properties.setProperty("sessionId", arguments.sessionId);
        properties.setProperty("helperPid", Long.toString(ProcessHandle.current().pid()));
        properties.setProperty("parentPid", Long.toString(arguments.parentPid));
        properties.setProperty("error", exception.getClass().getSimpleName());
        properties.setProperty("message", String.valueOf(exception.getMessage()));
        properties.setProperty("updatedAtEpochMillis", Long.toString(System.currentTimeMillis()));
        try {
            OverlayStateFile.writeProperties(
                    arguments.statusFile,
                    properties,
                    "ERYDON Metrakron window overlay failure"
            );
        } catch (IOException ignored) {
            // The redirected helper log remains available if status cannot be written.
        }
    }

    private static final class OverlayApplication {
        private final JWindow window;
        private final OverlayPanel panel;
        private volatile boolean visible;

        private OverlayApplication() {
            panel = new OverlayPanel();
            if (!Toolkit.getDefaultToolkit().isAlwaysOnTopSupported()) {
                throw new IllegalStateException("Always-on-top windows are not supported by this desktop");
            }

            window = new JWindow();
            try {
                window.setType(Window.Type.UTILITY);
            } catch (UnsupportedOperationException ignored) {
                // UTILITY avoids task-switcher clutter where supported; it is not required to draw.
            }
            window.setAlwaysOnTop(true);
            window.setFocusableWindowState(false);
            window.setAutoRequestFocus(false);
            if (supportsPerPixelTransparency()) {
                window.setBackground(new Color(0, 0, 0, 0));
            } else {
                window.setBackground(new Color(0x080807));
            }
            window.setContentPane(panel);
        }

        private static boolean supportsPerPixelTransparency() {
            GraphicsDevice device = GraphicsEnvironment
                    .getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice();
            return device.isWindowTranslucencySupported(
                    GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT
            );
        }

        private static OverlayApplication create() throws Exception {
            OverlayApplication[] result = new OverlayApplication[1];
            SwingUtilities.invokeAndWait(() -> result[0] = new OverlayApplication());
            return result[0];
        }

        private void apply(OverlayState state, long nowNanos) throws Exception {
            SwingUtilities.invokeAndWait(() -> {
                panel.apply(state, nowNanos);
                if (state.active() && state.width() > 0 && state.height() > 0) {
                    window.setBounds(state.x(), state.y(), state.width(), state.height());
                    if (!window.isVisible()) {
                        window.setVisible(true);
                    }
                    visible = true;
                } else {
                    window.setVisible(false);
                    visible = false;
                }
            });
        }

        private void repaintIfVisible() {
            if (visible) {
                SwingUtilities.invokeLater(panel::repaint);
            }
        }

        private HelperStatus status(long nowNanos) {
            PanelStatus status = panel.status(nowNanos);
            return new HelperStatus(
                    status.revision,
                    status.paintedRevision,
                    status.active,
                    visible,
                    status.sequence,
                    status.framesRendered,
                    status.heading,
                    status.displayedTime,
                    status.detail
            );
        }

        private void close() throws Exception {
            SwingUtilities.invokeAndWait(() -> {
                window.setVisible(false);
                window.dispose();
                visible = false;
            });
        }
    }

    private static final class OverlayPanel extends JComponent {
        private static final String NERIUM_RESOURCE = "/assets/metrakron/textures/gui/nerium_panel.png";
        private static final String CINZEL_RESOURCE = "/assets/metrakron/font/cinzel.ttf";

        private static final Color TEXT = new Color(0xF7F0E4);
        private static final Color TIMER = new Color(0xFFC65A);
        private static final Color DETAIL = new Color(0xD7C8B2);
        private static final Color LINK = new Color(0xE2A12A);
        private static final Color LINK_HOVER = new Color(0xFFD474);

        private AppearanceSettings appearance;
        private BufferedImage nerium;
        private Font cinzel;
        private BitmapCinzelTimer bitmapTimer;

        private void refreshAppearance() {
            AppearanceSettings selected = AppearanceSettings.current();
            if (selected.equals(appearance)) return;
            appearance = selected;
            nerium = loadImage(selected.stoneResource());
            cinzel = loadCinzel();
            bitmapTimer = BitmapCinzelTimer.load(selected.fontId(), new Color(selected.accentColor(), true),
                    new Color(selected.shadowColor(), true));
        }

        private OverlayState state;
        private long activeSequence = Long.MIN_VALUE;
        private long anchorNanos;
        private long anchoredElapsedMillis;
        private long framesRendered;
        private long paintedRevision;
        private int linkLeft;
        private int linkRight;
        private boolean linkHovered;

        private OverlayPanel() {
            setOpaque(false);
            MouseAdapter linkMouse = new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent event) {
                    updateLinkHover(event.getX(), event.getY());
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    setLinkHovered(false);
                }

                @Override
                public void mouseClicked(MouseEvent event) {
                    if (event.getClickCount() == 1 && DesktopOverlayLink.isActivation(
                            event.getButton(),
                            event.getX(),
                            event.getY(),
                            getWidth(),
                            getHeight(),
                            linkLeft,
                            linkRight
                    )) {
                        DesktopOverlayLink.openInBrowser();
                    }
                }
            };
            addMouseListener(linkMouse);
            addMouseMotionListener(linkMouse);
        }

        private synchronized void apply(OverlayState newState, long nowNanos) {
            if (newState.active()) {
                if (newState.sequence() == activeSequence) {
                    anchoredElapsedMillis = Math.max(
                            elapsedMillis(nowNanos),
                            newState.elapsedMillis()
                    );
                } else {
                    activeSequence = newState.sequence();
                    anchoredElapsedMillis = Math.max(0L, newState.elapsedMillis());
                }
                anchorNanos = nowNanos;
            } else {
                activeSequence = Long.MIN_VALUE;
                anchoredElapsedMillis = 0L;
                anchorNanos = nowNanos;
            }
            state = newState;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            refreshAppearance();
            RenderText renderText;
            long renderRevision;
            long barElapsed;
            Long barAverage;
            synchronized (this) {
                if (state == null || !state.active()) {
                    return;
                }
                long elapsedMillis = elapsedMillis(System.nanoTime());
                barElapsed = elapsedMillis;
                barAverage = state.averageMillis();
                renderText = new RenderText(
                        OverlayClock.isRelearning(elapsedMillis, state.averageMillis())
                                ? state.relearningHeading()
                                : state.heading(),
                        OverlayClock.displayedTime(elapsedMillis, state.averageMillis()),
                        state.detail()
                );
                renderRevision = state.revision();
            }

            Graphics2D context = (Graphics2D) graphics.create();
            try {
                context.scale(
                        getWidth() / (double) OverlayLayout.PANEL_WIDTH,
                        getHeight() / (double) DesktopOverlayLink.PANEL_HEIGHT
                );
                context.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON
                );
                context.setRenderingHint(
                        RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON
                );
                context.setRenderingHint(
                        RenderingHints.KEY_FRACTIONALMETRICS,
                        RenderingHints.VALUE_FRACTIONALMETRICS_ON
                );
                context.setRenderingHint(
                        RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR
                );
                drawPanel(context);
                drawCentered(context, renderText.heading, 18, 10.5F, 6.0F, new Color(appearance.textColor(), true), true);
                if (bitmapTimer == null) {
                    drawCentered(context, renderText.timer, 51, 25.0F, 15.0F,
                            new Color(appearance.accentColor(), true), true);
                } else {
                    bitmapTimer.drawCentered(
                            context,
                            renderText.timer,
                            OverlayLayout.PANEL_WIDTH / 2,
                            25,
                            OverlayLayout.PANEL_WIDTH - 20,
                            0.16F,
                            0.0F
                    );
                }
                drawCentered(
                        context,
                        renderText.detail,
                        DesktopOverlayLink.DETAIL_BASELINE,
                        7.5F,
                        4.5F,
                        new Color(appearance.textColor(), true),
                        true
                );
                CountdownBar.draw((l, t, r, b, color) -> {
                    context.setColor(new Color(color, true));
                    context.fillRect(l, t, r - l, b - t);
                }, 12, 57, OverlayLayout.PANEL_WIDTH - 24, barElapsed, barAverage, appearance);
                drawLink(context);
            } finally {
                context.dispose();
            }
            synchronized (this) {
                framesRendered++;
                paintedRevision = Math.max(paintedRevision, renderRevision);
            }
        }

        private void drawPanel(Graphics2D context) {
            int width = OverlayLayout.PANEL_WIDTH;
            int height = DesktopOverlayLink.PANEL_HEIGHT;

            BronzeFrameStyle.draw((left, top, right, bottom, argb) -> {
                context.setColor(new Color(appearance.metalColor(argb), true));
                context.fillRect(left, top, right - left, bottom - top);
            }, 0, 0, width, height);

            if (nerium != null) {
                int contentInset = BronzeFrameStyle.CONTENT_INSET;
                context.drawImage(
                        nerium,
                        contentInset,
                        contentInset,
                        width - contentInset,
                        height - contentInset,
                        appearance.stone() == AppearanceSettings.Stone.NERIUM ? 192 : 0,
                        appearance.stone() == AppearanceSettings.Stone.NERIUM ? 336 : 0,
                        appearance.stone() == AppearanceSettings.Stone.NERIUM ? 832 : nerium.getWidth(),
                        appearance.stone() == AppearanceSettings.Stone.NERIUM ? 592 : nerium.getHeight(),
                        null
                );
            } else {
                context.setColor(new Color(appearance.darkText() ? 0xF8F6F0 : 0x080807));
                context.fillRect(
                        BronzeFrameStyle.CONTENT_INSET,
                        BronzeFrameStyle.CONTENT_INSET,
                        width - BronzeFrameStyle.CONTENT_INSET * 2,
                        height - BronzeFrameStyle.CONTENT_INSET * 2
                );
            }
        }

        private void drawLink(Graphics2D context) {
            Font font = fitFont(
                    context,
                    DesktopOverlayLink.LABEL,
                    6.5F,
                    5.0F,
                    OverlayLayout.PANEL_WIDTH - 20
            );
            context.setFont(font);
            FontMetrics metrics = context.getFontMetrics(font);
            int textWidth = metrics.stringWidth(DesktopOverlayLink.LABEL);
            int x = (OverlayLayout.PANEL_WIDTH - textWidth) / 2;
            linkLeft = x;
            linkRight = x + textWidth;

            context.setColor(new Color(appearance.shadowColor(), true));
            context.drawString(
                    DesktopOverlayLink.LABEL,
                    x + 1,
                    DesktopOverlayLink.LINK_BASELINE + 1
            );
            context.setColor(new Color(linkHovered ? appearance.textColor() : appearance.accentColor(), true));
            context.drawString(DesktopOverlayLink.LABEL, x, DesktopOverlayLink.LINK_BASELINE);
            Stroke previousStroke = context.getStroke();
            context.setStroke(new BasicStroke(0.25F));
            context.drawLine(
                    x,
                    DesktopOverlayLink.LINK_BASELINE + 2,
                    x + textWidth,
                    DesktopOverlayLink.LINK_BASELINE + 2
            );
            context.setStroke(previousStroke);
        }

        private void updateLinkHover(int x, int y) {
            setLinkHovered(DesktopOverlayLink.isPointerOver(
                    x,
                    y,
                    getWidth(),
                    getHeight(),
                    linkLeft,
                    linkRight
            ));
        }

        private void setLinkHovered(boolean hovered) {
            if (linkHovered == hovered) {
                return;
            }
            linkHovered = hovered;
            setCursor(Cursor.getPredefinedCursor(
                    hovered ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR
            ));
            repaint();
        }

        private void drawCentered(
                Graphics2D context,
                String value,
                int baseline,
                float preferredSize,
                float minimumSize,
                Color color,
                boolean shadow
        ) {
            Font font = fitFont(
                    context,
                    value,
                    preferredSize,
                    minimumSize,
                    OverlayLayout.PANEL_WIDTH - 20
            );
            context.setFont(font);
            FontMetrics metrics = context.getFontMetrics(font);
            int x = (OverlayLayout.PANEL_WIDTH - metrics.stringWidth(value)) / 2;
            if (shadow) {
                context.setColor(new Color(appearance.shadowColor(), true));
                context.drawString(value, x + 1, baseline + 1);
            }
            context.setColor(color);
            context.drawString(value, x, baseline);
        }

        private Font fitFont(
                Graphics2D context,
                String value,
                float preferredSize,
                float minimumSize,
                int maximumWidth
        ) {
            float size = preferredSize;
            Font font = cinzel.deriveFont(Font.PLAIN, size);
            while (size > minimumSize
                    && context.getFontMetrics(font).stringWidth(value) > maximumWidth) {
                size = Math.max(minimumSize, size - 0.25F);
                font = cinzel.deriveFont(Font.PLAIN, size);
            }
            return font;
        }

        private synchronized PanelStatus status(long nowNanos) {
            if (state == null) {
                return new PanelStatus(0L, paintedRevision, false, 0L, framesRendered, "", "", "");
            }
            long elapsedMillis = elapsedMillis(nowNanos);
            String heading = state.active()
                    ? (OverlayClock.isRelearning(elapsedMillis, state.averageMillis())
                            ? state.relearningHeading()
                            : state.heading())
                    : "";
            String displayedTime = state.active()
                    ? OverlayClock.displayedTime(elapsedMillis, state.averageMillis())
                    : "";
            return new PanelStatus(
                    state.revision(),
                    paintedRevision,
                    state.active(),
                    state.sequence(),
                    framesRendered,
                    heading,
                    displayedTime,
                    state.detail()
            );
        }

        private long elapsedMillis(long nowNanos) {
            return OverlayClock.elapsedMillis(anchoredElapsedMillis, anchorNanos, nowNanos);
        }

        private static BufferedImage loadImage(String resource) {
            try (InputStream input = DesktopOverlayMain.class.getResourceAsStream(resource)) {
                return input == null ? null : ImageIO.read(input);
            } catch (IOException exception) {
                return null;
            }
        }

        private static Font loadCinzel() {
            try (InputStream input = DesktopOverlayMain.class.getResourceAsStream(CINZEL_RESOURCE.replace("cinzel", AppearanceSettings.current().fontId()))) {
                if (input != null) {
                    return Font.createFont(Font.TRUETYPE_FONT, input);
                }
            } catch (Exception ignored) {
                // A logical serif font keeps the helper usable if the bundled font is unavailable.
            }
            return new Font(Font.SERIF, Font.PLAIN, 12);
        }
    }

    private record Arguments(
            Path stateFile,
            Path statusFile,
            long parentPid,
            String sessionId
    ) {
        private static Arguments parse(String[] rawArguments) {
            if (rawArguments.length % 2 != 0) {
                throw new IllegalArgumentException("Overlay arguments must be key/value pairs");
            }
            Map<String, String> values = new HashMap<>();
            for (int index = 0; index < rawArguments.length; index += 2) {
                values.put(rawArguments[index], rawArguments[index + 1]);
            }
            return new Arguments(
                    Path.of(required(values, "--state")),
                    Path.of(required(values, "--status")),
                    Long.parseLong(required(values, "--parent")),
                    required(values, "--session")
            );
        }

        private static String required(Map<String, String> values, String key) {
            String value = values.get(key);
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Missing overlay argument " + key);
            }
            return value;
        }
    }

    private record RenderText(String heading, String timer, String detail) {
    }

    private record PanelStatus(
            long revision,
            long paintedRevision,
            boolean active,
            long sequence,
            long framesRendered,
            String heading,
            String displayedTime,
            String detail
    ) {
    }

    private record HelperStatus(
            long revision,
            long paintedRevision,
            boolean active,
            boolean visible,
            long sequence,
            long framesRendered,
            String heading,
            String displayedTime,
            String detail
    ) {
    }
}
