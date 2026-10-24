package com.oliver.metrakron.ui;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class BitmapTimerAtlasTest {
    @Test void highResolutionTimersRetainFontIdentityTabularWidthsAndSmoothInk() throws Exception {
        for (String font : List.of("cinzel", "lato", "spacemono")) {
            String gui = "/assets/metrakron/textures/gui/" + font;
            Properties metrics = new Properties();
            try (var input = getClass().getResourceAsStream(gui + "_timer_atlas.properties")) {
                assertNotNull(input);
                metrics.load(input);
            }
            try (var png = getClass().getResourceAsStream(gui + "_timer_atlas.png");
                 var json = getClass().getResourceAsStream(gui + "_atlas.json");
                 var ttf = getClass().getResourceAsStream("/assets/metrakron/font/" + font + ".ttf")) {
                assertNotNull(png); assertNotNull(json); assertNotNull(ttf);
                var image = ImageIO.read(png);
                var standard = JsonParser.parseReader(new InputStreamReader(json, StandardCharsets.UTF_8)).getAsJsonObject();
                float ratio = standard.get("fontSize").getAsFloat() / Float.parseFloat(metrics.getProperty("fontSize"));
                assertTrue(ratio < 1, "Timer artwork must carry more source detail than ordinary text");
                assertEquals(Integer.parseInt(metrics.getProperty("textureWidth")), image.getWidth());
                assertEquals(Integer.parseInt(metrics.getProperty("textureHeight")), image.getHeight());
                String fontHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(ttf.readAllBytes()));
                assertEquals(fontHash, metrics.getProperty("sourceFontSha256"));
                assertEquals(fontHash, standard.get("sourceFontSha256").getAsString());

                Float digitAdvance = null;
                for (char character : "+:0123456789".toCharArray()) {
                    String key = Integer.toString(character);
                    String[] glyph = metrics.getProperty("glyph." + key).split(",");
                    assertEquals(7, glyph.length, "A timer glyph must provide its complete drawing metrics");
                    int x = Integer.parseInt(glyph[0]), y = Integer.parseInt(glyph[1]);
                    int width = Integer.parseInt(glyph[2]), height = Integer.parseInt(glyph[3]);
                    assertTrue(x >= 0 && y >= 0 && width > 0 && height > 0);
                    assertTrue(x + width <= image.getWidth() && y + height <= image.getHeight());
                    float advance = Float.parseFloat(glyph[6]);
                    assertTrue(advance > 0);
                    if (Character.isDigit(character)) {
                        if (digitAdvance == null) digitAdvance = advance;
                        assertEquals(digitAdvance, advance, 0.001F, "Changing digits must not shift timer slots");
                        float standardAdvance = standard.getAsJsonObject("glyphs").getAsJsonObject(key).get("advance").getAsFloat();
                        assertEquals(standardAdvance, advance * ratio, 1.0F,
                                "After rescaling, digit width must retain the established logical size");
                    }
                    boolean transparent = false, opaque = false, antialiased = false;
                    for (int py = y; py < y + height; py++) for (int px = x; px < x + width; px++) {
                        int alpha = image.getRGB(px, py) >>> 24;
                        transparent |= alpha == 0;
                        opaque |= alpha == 255;
                        antialiased |= alpha > 0 && alpha < 255;
                    }
                    assertTrue(transparent && opaque && antialiased,
                            font + " " + character + " must retain transparent margins and antialiased ink");
                }
            }
        }
    }
}
