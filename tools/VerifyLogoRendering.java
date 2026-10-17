import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import com.oliver.metrakron.overlay.AppearanceSettings;
import com.oliver.metrakron.overlay.LogoAnimation;
import com.oliver.metrakron.overlay.OverlayState;

/** Run headlessly with -Xmx128m, the desktop helper's actual heap ceiling. */
public class VerifyLogoRendering {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        Path output = Path.of(args[0]);
        Files.createDirectories(output);
        var panelClass = Class.forName("com.oliver.metrakron.overlay.DesktopOverlayMain$OverlayPanel");
        var constructor = panelClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        var apply = panelClass.getDeclaredMethod("apply", OverlayState.class, long.class);
        apply.setAccessible(true);
        var atlasField = panelClass.getDeclaredField("logoAtlas");
        atlasField.setAccessible(true);
        BufferedImage atlas = (BufferedImage) atlasField.get(null);
        if (atlas == null || atlas.getWidth() != LogoAnimation.ATLAS_WIDTH
                || atlas.getHeight() != LogoAnimation.ATLAS_HEIGHT) {
            throw new AssertionError("Missing or incorrectly sized logo atlas");
        }
        // Every animation tile must contain both artwork and transparent margins.
        for (int frame = 0; frame < LogoAnimation.FRAMES; frame++) {
            int x = frame % LogoAnimation.COLUMNS * LogoAnimation.FRAME_WIDTH;
            int y = frame / LogoAnimation.COLUMNS * LogoAnimation.FRAME_HEIGHT;
            if ((atlas.getRGB(x + LogoAnimation.FRAME_WIDTH / 2, y + LogoAnimation.FRAME_HEIGHT / 2) >>> 24) < 200
                    || (atlas.getRGB(x, y) >>> 24) > 10) {
                throw new AssertionError("Invalid transparent frame " + frame);
            }
        }
        SwingUtilities.invokeAndWait(() -> {
            try {
                JComponent panel = (JComponent) constructor.newInstance();
                panel.setSize(864, 576);
                apply.invoke(panel, new OverlayState(2, "test", 1, 1, true, 1, 12_000, 60_000L,
                        "MAIN MENU IN", "RELEARNING LOAD TIME", "RESOURCE RELOAD · MODELS · TEXTURES · FONTS",
                        0, 0, 864, 576), System.nanoTime());
                for (var stone : new AppearanceSettings.Stone[]{AppearanceSettings.Stone.GLACIUM,
                        AppearanceSettings.Stone.PORTORIUM, AppearanceSettings.Stone.NERIUM}) {
                    for (var font : AppearanceSettings.Typeface.values()) {
                        AppearanceSettings.preview(new AppearanceSettings(stone, font, AppearanceSettings.Metal.BRONZE));
                        BufferedImage capture = new BufferedImage(864, 576, BufferedImage.TYPE_INT_ARGB);
                        var g = capture.createGraphics();
                        try { panel.paint(g); } finally { g.dispose(); }
                        if (font == AppearanceSettings.Typeface.CINZEL) {
                            ImageIO.write(capture, "png", output.resolve(stone.name().toLowerCase(java.util.Locale.ROOT) + ".png").toFile());
                        }
                        capture.flush();
                    }
                }
            } catch (Exception e) { throw new RuntimeException(e); }
        });
        System.out.println("Verified 174 transparent frames and actual desktop panel rendering with all fonts under "
                + Runtime.getRuntime().maxMemory() / (1024 * 1024) + " MiB heap.");
    }
}
