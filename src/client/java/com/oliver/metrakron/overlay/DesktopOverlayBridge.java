package com.oliver.metrakron.overlay;

import com.oliver.metrakron.MetrakronClient;
import com.oliver.metrakron.timing.LoadSnapshot;
import com.oliver.metrakron.ui.OverlayCopy;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModOrigin;
//? if >=26.1 {
/*import com.mojang.blaze3d.platform.Window;
*///?} else {
import net.minecraft.client.util.Window;
//?}

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class DesktopOverlayBridge {
    private static final String HELPER_CLASS = "com.oliver.metrakron.overlay.DesktopOverlayMain";
    private static final long STATUS_POLL_NANOS = 100_000_000L;
    private static final long STATUS_MAX_AGE_MILLIS = 2_500L;
    private static final long FRAME_STALL_NANOS = 3_000_000_000L;

    private static boolean initialized;
    private static boolean enabled;
    private static String sessionId;
    private static long parentPid;
    private static long revision;
    private static Path stateFile;
    private static Path statusFile;
    private static Process helperProcess;
    private static DesktopOverlayPlatform.Capability platform;
    private static ViewKey lastView;
    private static Geometry lastGeometry = new Geometry(0, 0, 0, 0);
    private static DesktopOverlayStatus lastHelperStatus;
    private static long healthSequence = Long.MIN_VALUE;
    private static long healthRevision = Long.MIN_VALUE;
    private static long nextStatusPollNanos;
    private static long lastStatusFrames = -1L;
    private static long lastFrameAdvanceNanos;
    private static long pendingHideRevision = Long.MIN_VALUE;

    private DesktopOverlayBridge() {
    }

    public static synchronized void initialize(Path configDirectory) {
        if (initialized) {
            return;
        }
        initialized = true;

        DesktopOverlayPlatform.Capability capability = DesktopOverlayPlatform.current();
        if (!capability.supported()) {
            MetrakronClient.LOGGER.info(
                    "Metrakron is using its in-game loading overlay only: {}",
                    capability.reason()
            );
            return;
        }

        try {
            Files.createDirectories(configDirectory);
            sessionId = UUID.randomUUID().toString();
            parentPid = ProcessHandle.current().pid();
            String runtimeId = parentPid + "-" + sessionId;
            stateFile = configDirectory.resolve("window-overlay-state-" + runtimeId + ".properties");
            statusFile = configDirectory.resolve("window-overlay-status-" + runtimeId + ".properties");
            Path logFile = configDirectory.resolve("window-overlay-" + runtimeId + ".log");
            OverlayStateFile.write(
                    stateFile,
                    OverlayState.inactive(sessionId, parentPid, ++revision, 0, 0, 0, 0)
            );

            DesktopOverlayClasspath.Resolution classpath = helperClasspath(configDirectory);
            List<String> command = helperCommand(capability, statusFile, classpath.classpath());

            ProcessBuilder builder = new ProcessBuilder(command);
            builder.directory(configDirectory.toFile());
            builder.redirectErrorStream(true);
            builder.redirectOutput(ProcessBuilder.Redirect.to(logFile.toFile()));
            helperProcess = builder.start();
            platform = capability;
            enabled = true;
            lastFrameAdvanceNanos = System.nanoTime();

            if (classpath.cached()) {
                MetrakronClient.LOGGER.info(
                        "Prepared a stable desktop-helper copy for the selected universal adapter"
                );
            }

            Runtime.getRuntime().addShutdownHook(new Thread(
                    DesktopOverlayBridge::hideAtShutdown,
                    "Metrakron overlay shutdown"
            ));
            MetrakronClient.LOGGER.info(
                    "Started independent Metrakron desktop overlay process {} on {}",
                    helperProcess.pid(),
                    capability.displayName()
            );
        } catch (IOException | RuntimeException exception) {
            enabled = false;
            MetrakronClient.LOGGER.warn(
                    "Unable to start the independent desktop overlay; the in-game overlay remains available",
                    exception
            );
        }
    }

    public static synchronized boolean publish(LoadSnapshot snapshot, Window window) {
        if (!helperAvailable()) {
            return false;
        }
        if (!snapshot.active()) {
            hide();
            return false;
        }
        pendingHideRevision = Long.MIN_VALUE;

        if (healthSequence != snapshot.sequence()) {
            resetPresentationHealth(snapshot.sequence());
        }

        Geometry geometry = geometry(window, platform);
        ViewKey view = new ViewKey(
                true,
                snapshot.sequence(),
                snapshot.averageMillis(),
                OverlayCopy.baseHeading(snapshot),
                OverlayCopy.relearningHeading(),
                OverlayCopy.detail(snapshot),
                geometry
        );
        if (view.equals(lastView)) {
            return helperOwnsPresentation(snapshot.sequence(), revision);
        }

        OverlayState state = new OverlayState(
                OverlayState.CURRENT_PROTOCOL,
                sessionId,
                parentPid,
                ++revision,
                true,
                snapshot.sequence(),
                snapshot.elapsedMillis(),
                snapshot.averageMillis(),
                view.heading,
                view.relearningHeading,
                view.detail,
                geometry.x,
                geometry.y,
                geometry.width,
                geometry.height
        );
        if (write(state)) {
            lastView = view;
            lastGeometry = geometry;
            return helperOwnsPresentation(snapshot.sequence(), revision);
        }
        disableStaleHelper("its state file could not be updated");
        return false;
    }

    public static synchronized void hide() {
        if (!enabled || pendingHideRevision != Long.MIN_VALUE) {
            return;
        }
        if (lastView != null && !lastView.active) {
            return;
        }
        OverlayState state = OverlayState.inactive(
                sessionId,
                parentPid,
                ++revision,
                lastGeometry.x,
                lastGeometry.y,
                lastGeometry.width,
                lastGeometry.height
        );
        if (write(state)) {
            lastView = ViewKey.inactive(lastGeometry);
            healthSequence = Long.MIN_VALUE;
            healthRevision = Long.MIN_VALUE;
            lastHelperStatus = null;
            pendingHideRevision = state.revision();
            startHideWatchdog(state.revision());
        } else {
            disableStaleHelper("its hide state could not be written");
        }
    }

    private static synchronized void hideAtShutdown() {
        if (!enabled) {
            return;
        }
        try {
            OverlayStateFile.write(
                    stateFile,
                    OverlayState.inactive(
                            sessionId,
                            parentPid,
                            ++revision,
                            lastGeometry.x,
                            lastGeometry.y,
                            lastGeometry.width,
                            lastGeometry.height
                    )
            );
        } catch (IOException ignored) {
            // The child also watches the parent PID and exits after a crash or forced close.
        }
    }

    private static boolean write(OverlayState state) {
        try {
            OverlayStateFile.write(stateFile, state);
            return true;
        } catch (IOException exception) {
            MetrakronClient.LOGGER.warn("Unable to update the independent loading overlay", exception);
            return false;
        }
    }

    private static boolean helperAvailable() {
        if (!enabled) {
            return false;
        }
        if (helperProcess != null && helperProcess.isAlive()) {
            return true;
        }
        enabled = false;
        MetrakronClient.LOGGER.warn(
                "The independent desktop overlay stopped; the in-game overlay remains available"
        );
        return false;
    }

    private static boolean helperOwnsPresentation(long sequence, long expectedRevision) {
        if (!helperAvailable()) {
            return false;
        }

        long nowNanos = System.nanoTime();
        if (healthRevision != expectedRevision) {
            resetRevisionHealth(expectedRevision, nowNanos);
        }
        pollHelperStatus(nowNanos);
        DesktopOverlayStatus status = lastHelperStatus;
        if (status != null && status.acknowledges(
                sessionId,
                parentPid,
                helperProcess.pid(),
                expectedRevision,
                sequence,
                System.currentTimeMillis(),
                STATUS_MAX_AGE_MILLIS
        )) {
            if (nowNanos - lastFrameAdvanceNanos <= FRAME_STALL_NANOS) {
                return true;
            }
            disableStaleHelper("its desktop window stopped painting");
            return false;
        }

        if (nowNanos - lastFrameAdvanceNanos > FRAME_STALL_NANOS) {
            disableStaleHelper("it did not confirm a painted loading window");
        }
        return false;
    }

    private static void pollHelperStatus(long nowNanos) {
        if (statusFile == null || nowNanos < nextStatusPollNanos) {
            return;
        }
        nextStatusPollNanos = nowNanos + STATUS_POLL_NANOS;

        try {
            DesktopOverlayStatus status = DesktopOverlayStatus.read(statusFile);
            if (!status.matchesIdentity(sessionId, parentPid, helperProcess.pid())) {
                return;
            }
            lastHelperStatus = status;
            if (status.sequence() == healthSequence
                    && status.paintedRevision() >= healthRevision
                    && status.framesRendered() > lastStatusFrames) {
                lastStatusFrames = status.framesRendered();
                lastFrameAdvanceNanos = nowNanos;
            }
        } catch (IOException | RuntimeException ignored) {
            // An atomic replacement may briefly make status unavailable; keep the fallback visible.
        }
    }

    private static void resetPresentationHealth(long sequence) {
        healthSequence = sequence;
        healthRevision = Long.MIN_VALUE;
        lastHelperStatus = null;
        nextStatusPollNanos = 0L;
        lastStatusFrames = -1L;
        lastFrameAdvanceNanos = System.nanoTime();
    }

    private static void resetRevisionHealth(long expectedRevision, long nowNanos) {
        healthRevision = expectedRevision;
        lastHelperStatus = null;
        nextStatusPollNanos = 0L;
        lastStatusFrames = -1L;
        lastFrameAdvanceNanos = nowNanos;
    }

    private static void startHideWatchdog(long expectedRevision) {
        String expectedSessionId = sessionId;
        long expectedParentPid = parentPid;
        long expectedHelperPid = helperProcess.pid();
        Path expectedStatusFile = statusFile;
        Process expectedHelperProcess = helperProcess;
        Thread watchdog = new Thread(() -> {
            long deadlineNanos = System.nanoTime() + FRAME_STALL_NANOS;
            while (System.nanoTime() <= deadlineNanos && isPendingHide(expectedRevision)) {
                if (!expectedHelperProcess.isAlive()) {
                    break;
                }
                if (confirmHiddenStatus(
                        expectedStatusFile,
                        expectedSessionId,
                        expectedParentPid,
                        expectedHelperPid,
                        expectedRevision
                )) {
                    return;
                }
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            // A valid acknowledgement may arrive during the watchdog's final sleep.
            if (confirmHiddenStatus(
                    expectedStatusFile,
                    expectedSessionId,
                    expectedParentPid,
                    expectedHelperPid,
                    expectedRevision
            )) {
                return;
            }

            synchronized (DesktopOverlayBridge.class) {
                if (pendingHideRevision == expectedRevision) {
                    pendingHideRevision = Long.MIN_VALUE;
                    disableStaleHelper("it did not confirm that its window was hidden");
                }
            }
        }, "Metrakron overlay hide confirmation");
        watchdog.setDaemon(true);
        watchdog.start();
    }

    private static boolean confirmHiddenStatus(
            Path expectedStatusFile,
            String expectedSessionId,
            long expectedParentPid,
            long expectedHelperPid,
            long expectedRevision
    ) {
        try {
            DesktopOverlayStatus status = DesktopOverlayStatus.read(expectedStatusFile);
            if (!status.acknowledgesHidden(
                    expectedSessionId,
                    expectedParentPid,
                    expectedHelperPid,
                    expectedRevision,
                    System.currentTimeMillis(),
                    STATUS_MAX_AGE_MILLIS
            )) {
                return false;
            }
            acknowledgeHide(expectedRevision, status);
            return true;
        } catch (IOException | RuntimeException ignored) {
            // The helper may be atomically replacing its status file.
            return false;
        }
    }

    private static synchronized boolean isPendingHide(long expectedRevision) {
        return enabled && pendingHideRevision == expectedRevision;
    }

    private static synchronized void acknowledgeHide(
            long expectedRevision,
            DesktopOverlayStatus status
    ) {
        if (pendingHideRevision != expectedRevision) {
            return;
        }
        pendingHideRevision = Long.MIN_VALUE;
        lastHelperStatus = status;
    }

    private static synchronized void disableStaleHelper(String reason) {
        if (!enabled) {
            return;
        }
        enabled = false;
        pendingHideRevision = Long.MIN_VALUE;
        if (helperProcess != null && helperProcess.isAlive()) {
            helperProcess.destroy();
        }
        lastHelperStatus = null;
        MetrakronClient.LOGGER.warn(
                "Metrakron disabled the independent desktop overlay because {}; the in-game fallback remains active",
                reason
        );
    }

    private static List<String> helperCommand(
            DesktopOverlayPlatform.Capability capability,
            Path statusFile,
            String classpath
    ) throws IOException {
        List<String> command = new ArrayList<>();
        command.add(javaExecutable(capability).toString());
        command.add("-Xms16m");
        command.add("-Xmx64m");
        command.add("-XX:+UseSerialGC");
        if (capability.kind() != DesktopOverlayPlatform.Kind.MACOS) {
            command.add("-Dsun.java2d.uiScale=1");
        }
        if (capability.suppressDockIcon()) {
            command.add("-Dapple.awt.UIElement=true");
            command.add("-Dapple.awt.application.name=ERYDON Metrakron");
        }
        command.add("-cp");
        command.add(classpath);
        command.add(HELPER_CLASS);
        command.add("--state");
        command.add(stateFile.toAbsolutePath().toString());
        command.add("--status");
        command.add(statusFile.toAbsolutePath().toString());
        command.add("--parent");
        command.add(Long.toString(parentPid));
        command.add("--session");
        command.add(sessionId);
        return List.copyOf(command);
    }

    private static Geometry geometry(
            Window window,
            DesktopOverlayPlatform.Capability capability
    ) {
        //? if >=26.1 {
        /*double scale = window.getGuiScale();
        if (capability.kind() == DesktopOverlayPlatform.Kind.MACOS && window.getGuiScaledWidth() > 0) {
        *///?} else {
        double scale = window.getScaleFactor();
        if (capability.kind() == DesktopOverlayPlatform.Kind.MACOS && window.getScaledWidth() > 0) {
        //?}
            // GLFW and AWT both express macOS window placement in desktop points. Minecraft's
            // scale factor also includes Retina backing pixels, so use the window-to-GUI ratio.
            //? if >=26.1 {
            /*scale = window.getScreenWidth() / (double) window.getGuiScaledWidth();
            *///?} else {
            scale = window.getWidth() / (double) window.getScaledWidth();
            //?}
        }
        scale = Math.max(0.5D, scale);
        int x = window.getX() + (int) Math.round(OverlayLayout.MARGIN * scale);
        int y = window.getY() + (int) Math.round(OverlayLayout.MARGIN * scale);
        int width = Math.max(1, (int) Math.round(OverlayLayout.PANEL_WIDTH * scale));
        int height = Math.max(1, (int) Math.round(DesktopOverlayLink.PANEL_HEIGHT * scale));
        return new Geometry(x, y, width, height);
    }

    private static Path javaExecutable(DesktopOverlayPlatform.Capability capability) throws IOException {
        Path bin = Path.of(System.getProperty("java.home"), "bin");
        if (capability.kind() == DesktopOverlayPlatform.Kind.WINDOWS) {
            Path javaw = bin.resolve("javaw.exe");
            if (Files.isRegularFile(javaw)) {
                return javaw;
            }
            Path java = bin.resolve("java.exe");
            if (Files.isRegularFile(java)) {
                return java;
            }
        } else {
            Path java = bin.resolve("java");
            if (Files.isRegularFile(java)) {
                return java;
            }
        }
        throw new IOException("No Java executable was found under " + bin);
    }

    private static DesktopOverlayClasspath.Resolution helperClasspath(
            Path configDirectory
    ) throws IOException {
        ModContainer container = FabricLoader.getInstance()
                .getModContainer(MetrakronClient.MOD_ID)
                .orElseThrow(() -> new IOException("Metrakron mod container is unavailable"));

        ModOrigin origin = container.getOrigin();
        if (origin.getKind() == ModOrigin.Kind.PATH) {
            return DesktopOverlayClasspath.direct(origin.getPaths());
        }

        return DesktopOverlayClasspath.selectedCodeSource(
                selectedAdapterCodeSource(),
                configDirectory,
                container.getMetadata().getVersion().getFriendlyString()
        );
    }

    private static Path selectedAdapterCodeSource() throws IOException {
        CodeSource codeSource = DesktopOverlayMain.class
                .getProtectionDomain()
                .getCodeSource();
        if (codeSource == null || codeSource.getLocation() == null) {
            throw new IOException("Metrakron selected adapter has no code-source location");
        }

        URL location = codeSource.getLocation();
        if (!"file".equalsIgnoreCase(location.getProtocol())) {
            throw new IOException(
                    "Metrakron selected adapter has an unsupported code-source protocol: "
                            + location.getProtocol()
            );
        }
        try {
            return Path.of(location.toURI());
        } catch (URISyntaxException | IllegalArgumentException exception) {
            throw new IOException("Metrakron selected adapter code source is invalid", exception);
        }
    }

    private record Geometry(int x, int y, int width, int height) {
    }

    private record ViewKey(
            boolean active,
            long sequence,
            Long averageMillis,
            String heading,
            String relearningHeading,
            String detail,
            Geometry geometry
    ) {
        private static ViewKey inactive(Geometry geometry) {
            return new ViewKey(false, 0L, null, "", "", "", geometry);
        }
    }
}
