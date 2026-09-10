package com.oliver.metrakron.overlay;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/** Draws the large timer from a FreeType-baked Cinzel atlas. */
final class BitmapCinzelTimer {
    private static final String ATLAS_RESOURCE =
            "/assets/metrakron/textures/gui/cinzel_timer_atlas.png";
    private static final String METRICS_RESOURCE =
            "/assets/metrakron/textures/gui/cinzel_timer_atlas.properties";

    private final BufferedImage foregroundAtlas;
    private final BufferedImage shadowAtlas;
    private final int ascent;
    private final Map<Integer, Glyph> glyphs;

    private BitmapCinzelTimer(
            BufferedImage foregroundAtlas,
            BufferedImage shadowAtlas,
            int ascent,
            Map<Integer, Glyph> glyphs
    ) {
        this.foregroundAtlas = foregroundAtlas;
        this.shadowAtlas = shadowAtlas;
        this.ascent = ascent;
        this.glyphs = glyphs;
    }

    static BitmapCinzelTimer load(Color foreground, Color shadow) {
        return load("cinzel", foreground, shadow);
    }

    static BitmapCinzelTimer load(String fontId, Color foreground, Color shadow) {
        try (
                InputStream imageInput = requiredResource(ATLAS_RESOURCE.replace("cinzel", fontId));
                InputStream metricsInput = requiredResource(METRICS_RESOURCE.replace("cinzel", fontId))
        ) {
            BufferedImage sourceAtlas = ImageIO.read(imageInput);
            if (sourceAtlas == null) {
                throw new IOException("Unable to decode " + ATLAS_RESOURCE);
            }

            Properties properties = new Properties();
            properties.load(metricsInput);
            int textureWidth = Integer.parseInt(properties.getProperty("textureWidth"));
            int textureHeight = Integer.parseInt(properties.getProperty("textureHeight"));
            if (textureWidth != sourceAtlas.getWidth() || textureHeight != sourceAtlas.getHeight()) {
                throw new IOException("Cinzel timer metrics do not match the atlas texture");
            }

            Map<Integer, Glyph> glyphs = new HashMap<>();
            for (String key : properties.stringPropertyNames()) {
                if (!key.startsWith("glyph.")) {
                    continue;
                }
                int codepoint = Integer.parseInt(key.substring("glyph.".length()));
                glyphs.put(codepoint, Glyph.parse(properties.getProperty(key)));
            }
            if (glyphs.isEmpty()) {
                throw new IOException("Cinzel timer atlas contains no glyph metrics");
            }

            return new BitmapCinzelTimer(
                    tint(sourceAtlas, foreground),
                    tint(sourceAtlas, shadow),
                    Integer.parseInt(properties.getProperty("ascent")),
                    Map.copyOf(glyphs)
            );
        } catch (IOException | RuntimeException exception) {
            return null;
        }
    }

    void drawCentered(
            Graphics2D context,
            String value,
            int centerX,
            int topY,
            int maximumWidth,
            float preferredScale,
            float tracking
    ) {
        if (value == null || value.isEmpty()) {
            return;
        }

        float unscaledWidth = measure(value, 1.0F, tracking / preferredScale);
        float scale = preferredScale;
        if (unscaledWidth * scale > maximumWidth) {
            scale = maximumWidth / unscaledWidth;
        }

        float width = measure(value, scale, tracking);
        float startX = centerX - width / 2.0F;
        Graphics2D shadowContext = (Graphics2D) context.create();
        try {
            shadowContext.translate(0.25, 0.25);
            draw(shadowContext, shadowAtlas, value, startX, topY, scale, tracking);
        } finally { shadowContext.dispose(); }
        draw(context, foregroundAtlas, value, startX, topY, scale, tracking);
    }

    private void draw(
            Graphics2D context,
            BufferedImage atlas,
            String value,
            float startX,
            int topY,
            float scale,
            float tracking
    ) {
        float baseline = topY + ascent * scale;
        float cursor = startX;
        int[] codepoints = value.codePoints().toArray();
        for (int index = 0; index < codepoints.length; index++) {
            Glyph glyph = glyphs.get(codepoints[index]);
            if (glyph == null) {
                continue;
            }
            int x = Math.round(cursor + glyph.offsetX * scale);
            int y = Math.round(baseline + glyph.offsetY * scale);
            int width = Math.max(1, Math.round(glyph.width * scale));
            int height = Math.max(1, Math.round(glyph.height * scale));
            context.drawImage(
                    atlas,
                    x,
                    y,
                    x + width,
                    y + height,
                    glyph.x,
                    glyph.y,
                    glyph.x + glyph.width,
                    glyph.y + glyph.height,
                    null
            );
            cursor += glyph.advance * scale;
            if (index + 1 < codepoints.length) {
                cursor += tracking;
            }
        }
    }

    private float measure(String value, float scale, float tracking) {
        int[] codepoints = value.codePoints().toArray();
        float width = 0.0F;
        for (int index = 0; index < codepoints.length; index++) {
            Glyph glyph = glyphs.get(codepoints[index]);
            if (glyph != null) {
                width += glyph.advance * scale;
            }
            if (index + 1 < codepoints.length) {
                width += tracking;
            }
        }
        return width;
    }

    private static BufferedImage tint(BufferedImage source, Color color) {
        BufferedImage tinted = new BufferedImage(
                source.getWidth(),
                source.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );
        int rgb = color.getRGB() & 0x00FFFFFF;
        int colorAlpha = color.getAlpha();
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int sourceAlpha = source.getRGB(x, y) >>> 24;
                int alpha = (sourceAlpha * colorAlpha + 127) / 255;
                tinted.setRGB(x, y, (alpha << 24) | rgb);
            }
        }
        return tinted;
    }

    private static InputStream requiredResource(String resource) throws IOException {
        InputStream input = BitmapCinzelTimer.class.getResourceAsStream(resource);
        if (input == null) {
            throw new IOException("Missing bundled resource " + resource);
        }
        return input;
    }

    private record Glyph(
            int x,
            int y,
            int width,
            int height,
            int offsetX,
            int offsetY,
            float advance
    ) {
        private static Glyph parse(String value) {
            String[] parts = value.split(",", -1);
            if (parts.length != 7) {
                throw new IllegalArgumentException("Invalid Cinzel timer glyph metrics");
            }
            return new Glyph(
                    Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3]),
                    Integer.parseInt(parts[4]),
                    Integer.parseInt(parts[5]),
                    Float.parseFloat(parts[6])
            );
        }
    }
}
