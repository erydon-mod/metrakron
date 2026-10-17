package com.oliver.metrakron.ui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Menu-only ink metrics; loading-screen text keeps its established layout. */
final class BitmapMenuLayout {
    record Glyph(int x, int y, int width, int height, int offsetX, int offsetY,
                 float advance, int inkLeft, int inkTop, int inkRight, int inkBottom) {
        boolean visible() { return inkRight > inkLeft && inkBottom > inkTop; }
    }

    record Bounds(float left, float top, float right, float bottom) {
        float width() { return right - left; }
        float height() { return bottom - top; }
    }

    record Placement(float originX, float baselineY, float scale, Bounds ink) { }

    private final Map<Integer, Glyph> glyphs;

    private BitmapMenuLayout(Map<Integer, Glyph> glyphs) { this.glyphs = Map.copyOf(glyphs); }

    static BitmapMenuLayout load(InputStream metadata, InputStream png) throws IOException {
        if (metadata == null || png == null) throw new IOException("Missing menu font atlas");
        JsonObject atlas = JsonParser.parseReader(new InputStreamReader(metadata, StandardCharsets.UTF_8)).getAsJsonObject();
        BufferedImage image = ImageIO.read(png);
        if (image == null || image.getWidth() != atlas.get("textureWidth").getAsInt()
                || image.getHeight() != atlas.get("textureHeight").getAsInt()) {
            throw new IOException("Menu font atlas dimensions disagree");
        }
        Map<Integer, Glyph> glyphs = new HashMap<>();
        for (var entry : atlas.getAsJsonObject("glyphs").entrySet()) {
            JsonObject g = entry.getValue().getAsJsonObject();
            int x = g.get("x").getAsInt(), y = g.get("y").getAsInt();
            int width = g.get("width").getAsInt(), height = g.get("height").getAsInt();
            int left = width, top = height, right = 0, bottom = 0;
            for (int py = 0; py < height; py++) for (int px = 0; px < width; px++) {
                if ((image.getRGB(x + px, y + py) >>> 24) != 0) {
                    left = Math.min(left, px); top = Math.min(top, py);
                    right = Math.max(right, px + 1); bottom = Math.max(bottom, py + 1);
                }
            }
            glyphs.put(Integer.parseInt(entry.getKey()), new Glyph(x, y, width, height,
                    g.get("offsetX").getAsInt(), g.get("offsetY").getAsInt(), g.get("advance").getAsFloat(),
                    left, top, right, bottom));
        }
        if (!glyphs.containsKey((int) '?')) throw new IOException("Menu font has no fallback glyph");
        return new BitmapMenuLayout(glyphs);
    }

    Glyph glyph(int codepoint) { return glyphs.getOrDefault(codepoint, glyphs.get((int) '?')); }

    Bounds bounds(String value) {
        float cursor = 0, left = Float.POSITIVE_INFINITY, top = Float.POSITIVE_INFINITY;
        float right = Float.NEGATIVE_INFINITY, bottom = Float.NEGATIVE_INFINITY;
        for (int codepoint : value.codePoints().toArray()) {
            Glyph glyph = glyph(codepoint);
            if (glyph.visible()) {
                // Matches the atlas-sized draw before the whole line is scaled once.
                int x = Math.round(cursor + glyph.offsetX);
                left = Math.min(left, x + glyph.inkLeft); right = Math.max(right, x + glyph.inkRight);
                top = Math.min(top, glyph.offsetY + glyph.inkTop); bottom = Math.max(bottom, glyph.offsetY + glyph.inkBottom);
            }
            cursor += glyph.advance;
        }
        return Float.isFinite(left) ? new Bounds(left, top, right, bottom) : new Bounds(0, 0, 0, 0);
    }

    Placement place(String value, float centerX, float centerY, int maximumWidth, int maximumHeight,
                    float preferredScale) {
        Bounds ink = bounds(value);
        float scale = preferredScale;
        if (ink.width() > 0) scale = Math.min(scale, Math.max(1, maximumWidth) / ink.width());
        if (ink.height() > 0) scale = Math.min(scale, Math.max(1, maximumHeight) / ink.height());
        return new Placement(centerX - (ink.left + ink.right) * scale / 2,
                centerY - (ink.top + ink.bottom) * scale / 2, scale, ink);
    }
}
