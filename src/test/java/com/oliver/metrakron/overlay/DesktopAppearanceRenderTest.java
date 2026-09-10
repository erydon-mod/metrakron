package com.oliver.metrakron.overlay;

import org.junit.jupiter.api.Test;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class DesktopAppearanceRenderTest {
    @Test void paintsAllAppearancesWithoutOpeningAWindow() throws Exception {
        int columns = 5;
        int cells = AppearanceSettings.Stone.values().length * AppearanceSettings.Typeface.values().length * AppearanceSettings.Metal.values().length;
        BufferedImage sheet = new BufferedImage(432 * columns, ((cells + columns - 1) / columns) * 192, BufferedImage.TYPE_INT_ARGB);
        var panelClass = Class.forName("com.oliver.metrakron.overlay.DesktopOverlayMain$OverlayPanel");
        var constructor = panelClass.getDeclaredConstructor(); constructor.setAccessible(true);
        var apply = panelClass.getDeclaredMethod("apply", OverlayState.class, long.class); apply.setAccessible(true);
        AppearanceSettings previous = AppearanceSettings.current();
        try {
            SwingUtilities.invokeAndWait(() -> {
                int cell = 0;
                try {
                    for (var font : AppearanceSettings.Typeface.values()) for (var metal : AppearanceSettings.Metal.values()) for (var stone : AppearanceSettings.Stone.values()) {
                        AppearanceSettings.preview(new AppearanceSettings(stone, font, metal));
                        JComponent panel = (JComponent) constructor.newInstance();
                        panel.setSize(432, 192);
                        apply.invoke(panel, new OverlayState(2, "test", 1, 1, true, 1, 12_000, 60_000L,
                                "MAIN MENU IN", "RELEARNING LOAD TIME", "RESOURCE RELOAD · MODELS · TEXTURES · FONTS", 0, 0, 432, 192), System.nanoTime());
                        var graphics = sheet.createGraphics();
                        try { graphics.translate(cell % columns * 432, cell / columns * 192); panel.paint(graphics); }
                        finally { graphics.dispose(); }
                        assertNotEquals(0, sheet.getRGB(cell % columns * 432 + 20, cell / columns * 192 + 20));
                        cell++;
                    }
                } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
            });
        } finally { AppearanceSettings.preview(previous); }
        Path output = Path.of("build/reports/appearance-preview.png");
        Files.createDirectories(output.getParent());
        assertTrue(ImageIO.write(sheet, "png", output.toFile()));
        // Compact first-page preview for checking full-size lettering without opening Minecraft.
        assertTrue(ImageIO.write(sheet.getSubimage(0, 0, 2160, 1152), "png",
                output.resolveSibling("appearance-stones-preview.png").toFile()));
    }
}
