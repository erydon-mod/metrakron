package com.oliver.metrakron.ui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BitmapMenuLayoutTest {
    private static final String GUI = "/assets/metrakron/textures/gui/";
    private static final List<String> FONTS = List.of("cinzel", "lato", "spacemono");
    private static final List<String> LANGUAGES = List.of("en_us", "de_de", "es_es");

    private BitmapMenuLayout font(String font) throws Exception {
        try (var json = getClass().getResourceAsStream(GUI + font + "_atlas.json");
             var png = getClass().getResourceAsStream(GUI + font + "_atlas.png")) {
            return BitmapMenuLayout.load(json, png);
        }
    }

    private JsonObject language(String locale) throws Exception {
        try (var input = getClass().getResourceAsStream("/assets/metrakron/lang/" + locale + ".json")) {
            assertNotNull(input);
            return JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private List<String> labels(JsonObject lang) {
        List<String> values = new ArrayList<>();
        for (var entry : lang.entrySet()) {
            if (entry.getKey().startsWith("metrakron.appearance.") && !entry.getValue().getAsString().contains("%s")) {
                values.add(entry.getValue().getAsString());
            }
        }
        for (String key : List.of("stone", "font", "metal", "position")) {
            for (String value : List.copyOf(values)) {
                values.add(lang.get("metrakron.appearance." + key).getAsString().formatted(value));
            }
        }
        return values;
    }

    @Test void actualInkIsCenteredAndEveryTranslatedLabelFitsNarrowControls() throws Exception {
        for (String name : FONTS) {
            var font = font(name);
            for (String locale : LANGUAGES) for (String value : labels(language(locale))) {
                for (int width : new int[]{31, 54, 134, 202}) {
                    var p = font.place(value, 100, 10, width, 12, .20F);
                    var ink = p.ink();
                    assertEquals(100, p.originX() + (ink.left() + ink.right()) * p.scale() / 2, .0001);
                    assertEquals(10, p.baselineY() + (ink.top() + ink.bottom()) * p.scale() / 2, .0001);
                    assertTrue(ink.width() * p.scale() <= width + .0001);
                    assertTrue(ink.height() * p.scale() <= 12.0001);
                }
            }
        }
    }

    @Test void mixedCaseCinzelRetainsItsCommonBaselineWithoutPerLetterPixelRounding() throws Exception {
        var font = font("cinzel");
        var p = font.place("Font: Cinzel", 108, 10, 202, 12, .20F);
        // Cinzel small capitals have natural one-atlas-pixel overshoots. At menu scale
        // these remain .2 pixels instead of becoming the old one-GUI-pixel staircase.
        float min = Float.POSITIVE_INFINITY, max = Float.NEGATIVE_INFINITY;
        for (int codepoint : "FontCinzel".codePoints().toArray()) {
            var g = font.glyph(codepoint);
            float bottom = p.baselineY() + (g.offsetY() + g.inkBottom()) * p.scale();
            min = Math.min(min, bottom); max = Math.max(max, bottom);
        }
        assertTrue(max - min <= .201F);
        assertEquals(font.glyph('?'), font.glyph(0x1F600));
        assertEquals(0, font.bounds("   ").height());
    }

    @Test void writesPreviewFromTheActualAtlasesAtTheSameSharedScaleAsTheMenu() throws Exception {
        int cellWidth = 264, cellHeight = 230;
        BufferedImage preview = new BufferedImage(cellWidth * 3, cellHeight * 3, BufferedImage.TYPE_INT_ARGB);
        var graphics = preview.createGraphics();
        try {
            graphics.setColor(new Color(0x262626)); graphics.fillRect(0, 0, preview.getWidth(), preview.getHeight());
            for (int col = 0; col < FONTS.size(); col++) {
                String name = FONTS.get(col);
                var font = font(name);
                BufferedImage atlas;
                try (var png = getClass().getResourceAsStream(GUI + name + "_atlas.png")) { atlas = ImageIO.read(png); }
                for (int row = 0; row < LANGUAGES.size(); row++) {
                    JsonObject lang = language(LANGUAGES.get(row));
                    int x = col * cellWidth, y = row * cellHeight;
                    draw(graphics, atlas, font, lang.get("metrakron.appearance.title").getAsString(), x + 132, y + 19, 216, .23F);
                    List<String> labels = List.of(
                            lang.get("metrakron.appearance.stone").getAsString().formatted("Glacium"),
                            lang.get("metrakron.appearance.font").getAsString().formatted(lang.get("metrakron.appearance." + name).getAsString()),
                            lang.get("metrakron.appearance.metal").getAsString().formatted(lang.get("metrakron.appearance.bronze").getAsString()),
                            lang.get("metrakron.appearance.position").getAsString().formatted(lang.get("metrakron.appearance.bottom_centre").getAsString()),
                            lang.get("metrakron.appearance.save_failed").getAsString());
                    for (int i = 0; i < labels.size(); i++) {
                        graphics.setColor(new Color(0xC3BBAA)); graphics.drawRect(x + 24, y + 39 + i * 31, 216, 24);
                        draw(graphics, atlas, font, labels.get(i), x + 132, y + 51 + i * 31, 202, .20F);
                    }
                    draw(graphics, atlas, font, lang.get("metrakron.appearance.reset").getAsString(), x + 65, y + 211, 54, .20F);
                    draw(graphics, atlas, font, lang.get("metrakron.appearance.save").getAsString(), x + 132, y + 211, 54, .20F);
                    draw(graphics, atlas, font, lang.get("metrakron.appearance.cancel").getAsString(), x + 199, y + 211, 54, .20F);
                }
            }
        } finally { graphics.dispose(); }
        Path output = Path.of("build/reports/menu-font-alignment.png");
        Files.createDirectories(output.getParent());
        assertTrue(ImageIO.write(preview, "png", output.toFile()));
    }

    private void draw(java.awt.Graphics2D graphics, BufferedImage atlas, BitmapMenuLayout font,
                      String value, int centerX, int centerY, int width, float scale) {
        var p = font.place(value, centerX, centerY, width, 14, scale);
        var g = (java.awt.Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.translate(p.originX(), p.baselineY()); g.scale(p.scale(), p.scale());
            float cursor = 0;
            for (int codepoint : value.codePoints().toArray()) {
                var glyph = font.glyph(codepoint);
                if (glyph.visible()) {
                    int x = Math.round(cursor + glyph.offsetX()), y = glyph.offsetY();
                    g.drawImage(atlas, x, y, x + glyph.width(), y + glyph.height(), glyph.x(), glyph.y(),
                            glyph.x() + glyph.width(), glyph.y() + glyph.height(), null);
                }
                cursor += glyph.advance();
            }
        } finally { g.dispose(); }
    }
}
