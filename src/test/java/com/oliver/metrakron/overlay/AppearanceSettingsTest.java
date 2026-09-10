package com.oliver.metrakron.overlay;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;

class AppearanceSettingsTest {
    @TempDir Path directory;
    @Test void defaultsAndRoundTrip() throws Exception {
        assertEquals(27, AppearanceSettings.Stone.values().length);
        Path file = directory.resolve("appearance.properties");
        assertEquals(AppearanceSettings.DEFAULT, AppearanceSettings.read(file));
        for (var stone : AppearanceSettings.Stone.values()) for (var font : AppearanceSettings.Typeface.values()) for (var metal : AppearanceSettings.Metal.values()) {
            var settings = new AppearanceSettings(stone, font, metal);
            settings.save(file);
            assertEquals(settings, AppearanceSettings.read(file));
            try (var input = getClass().getResourceAsStream(settings.stoneResource())) { assertNotNull(input); }
        }
        Files.writeString(file, "stone=INVALID\nfont=missing\nmetal=SILVER\n");
        assertEquals(AppearanceSettings.Stone.NERIUM, AppearanceSettings.read(file).stone());
        assertEquals(AppearanceSettings.Metal.SILVER, AppearanceSettings.read(file).metal());
    }
    @Test void countdownUsesOnlyOwnEstimateAndClamps() {
        assertEquals(1, CountdownBar.remaining(0, 1000L));
        assertEquals(.5, CountdownBar.remaining(500, 1000L));
        assertEquals(0, CountdownBar.remaining(1500, 1000L));
        assertEquals(0, CountdownBar.remaining(500, null));
        assertEquals(0, CountdownBar.remaining(500, 0L));
        assertEquals(1, CountdownBar.remaining(-10, 1000L));
    }
    @Test void allTimerDigitsHaveIdenticalAdvances() throws Exception {
        for (String font : new String[]{"cinzel", "lato", "spacemono"}) {
            Properties p = new Properties();
            try (var input = getClass().getResourceAsStream("/assets/metrakron/textures/gui/" + font + "_timer_atlas.properties")) { p.load(input); }
            String advance = p.getProperty("glyph.48").split(",")[6];
            for (int c = 49; c <= 57; c++) assertEquals(advance, p.getProperty("glyph." + c).split(",")[6]);
        }
    }
    @Test void everyStoneHasAnOpposingHighContrastShadow() throws Exception {
        for (var stone : AppearanceSettings.Stone.values()) for (var metal : AppearanceSettings.Metal.values()) {
            var s = new AppearanceSettings(stone, AppearanceSettings.Typeface.CINZEL, metal);
            java.awt.image.BufferedImage image;
            try (var input = getClass().getResourceAsStream(s.stoneResource())) { image = javax.imageio.ImageIO.read(input); }
            assertNotNull(image);
            assertTrue(image.getWidth() >= 676 && image.getHeight() >= 270, stone + " must use a master crop");
            assertTrue((s.shadowColor() >>> 24) >= 230);
            double shadow = luminance(s.shadowColor());
            for (int color : new int[]{s.textColor(), s.accentColor()}) {
                double foreground = luminance(color);
                double ratio = (Math.max(shadow, foreground) + .05) / (Math.min(shadow, foreground) + .05);
                assertTrue(ratio >= 7, s + " text/shadow contrast " + ratio);
            }
        }
    }
    private static double luminance(int rgb) { return .2126 * channel(rgb >> 16 & 255) + .7152 * channel(rgb >> 8 & 255) + .0722 * channel(rgb & 255); }
    private static double channel(int c) { double v = c / 255.0; return v <= .04045 ? v / 12.92 : Math.pow((v + .055) / 1.055, 2.4); }
}
