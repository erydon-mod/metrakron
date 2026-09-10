package com.oliver.metrakron.ui;

import com.google.gson.Gson;
import com.oliver.metrakron.MetrakronClient;
import com.oliver.metrakron.overlay.AppearanceSettings;
//? if >=26.1 {
/*import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
*///?} else {
//? if >=1.21.6 {
/*import net.minecraft.client.gl.RenderPipelines;
*///?} else if >=1.21.4 {
/*import net.minecraft.client.render.RenderLayer;
*///?} else {
import com.mojang.blaze3d.systems.RenderSystem;
//?}
import net.minecraft.client.MinecraftClient;
//? if >=1.20
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
//?}

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * A tiny Cinzel renderer that is independent of Minecraft's resource reload.
 *
 * <p>The Mojang splash begins before Minecraft's normal font atlas is ready,
 * so Metrakron loads its own pre-baked atlas directly from the mod JAR.</p>
 */
final class BitmapCinzelFont {
    private static final String ATLAS_PNG = "/assets/metrakron/textures/gui/cinzel_atlas.png";
    private static final String ATLAS_JSON = "/assets/metrakron/textures/gui/cinzel_atlas.json";
    //? if >=26.1 {
    /*private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
    *///?} else if >=1.21 {
    /*private static final Identifier TEXTURE = Identifier.of(
    *///?} else {
    private static final Identifier TEXTURE = new Identifier(
    //?}
            MetrakronClient.MOD_ID,
            "dynamic/cinzel_atlas"
    );

    private static Atlas atlas;
    private static boolean ready;
    private static boolean failureLogged;
    private static String loadedFont;

    private BitmapCinzelFont() {
    }

    //? if >=26.1 {
    /*static boolean initialize(Minecraft client) {
    *///?} else {
    static boolean initialize(MinecraftClient client) {
    //?}
        String selectedFont = AppearanceSettings.current().fontId();
        if (ready && selectedFont.equals(loadedFont)) {
            return true;
        }
        ready = false;
        loadedFont = selectedFont;

        NativeImage image = null;
        //? if >=26.1 {
        /*DynamicTexture texture = null;
        *///?} else {
        NativeImageBackedTexture texture = null;
        //?}
        try (
                InputStream metadataStream = requiredResource(ATLAS_JSON.replace("cinzel", selectedFont));
                InputStreamReader metadataReader = new InputStreamReader(metadataStream, StandardCharsets.UTF_8);
                InputStream imageStream = requiredResource(ATLAS_PNG.replace("cinzel", selectedFont))
        ) {
            Atlas loadedAtlas = new Gson().fromJson(metadataReader, Atlas.class);
            image = NativeImage.read(imageStream);
            if (loadedAtlas == null
                    || loadedAtlas.glyphs == null
                    || loadedAtlas.textureWidth != image.getWidth()
                    || loadedAtlas.textureHeight != image.getHeight()) {
                throw new IOException("Cinzel atlas metadata does not match its texture");
            }

            //? if >=26.1 {
            /*texture = createTexture(image);
            *///?} else {
            texture = createTexture(image);
            //?}
            image = null;
            //? if <1.21.5
            texture.setFilter(true, false);
            //? if >=26.1 {
            /*client.getTextureManager().register(TEXTURE, texture);
            *///?} else {
            client.getTextureManager().registerTexture(TEXTURE, texture);
            //?}
            atlas = loadedAtlas;
            ready = true;
            texture = null;
            return true;
        } catch (IOException | RuntimeException exception) {
            if (!failureLogged) {
                failureLogged = true;
                MetrakronClient.LOGGER.error("Unable to load the early Cinzel font atlas", exception);
            }
            return false;
        } finally {
            if (texture != null) {
                texture.close();
            } else if (image != null) {
                image.close();
            }
        }
    }

    static boolean isReady() {
        return ready;
    }

    static void drawCentered(
            //? if >=26.1 {
            /*GuiGraphicsExtractor context,
            *///?} else if >=1.20 {
            DrawContext context,
            //?} else {
            /*LegacyDrawContext context,
            *///?}
            String value,
            int centerX,
            int topY,
            int maximumWidth,
            float preferredScale,
            float tracking,
            int color,
            boolean shadow
    ) {
        if (!ready || value == null || value.isEmpty()) {
            return;
        }

        float unscaledWidth = measure(value, 1.0F, tracking / preferredScale);
        float scale = preferredScale;
        if (unscaledWidth * scale > maximumWidth) {
            scale = maximumWidth / unscaledWidth;
        }

        float width = measure(value, scale, tracking);
        float startX = centerX - width / 2.0F;
        if (shadow) {
            // Translate after glyph rounding so every version retains a tight subpixel shadow.
            //? if >=26.1 {
            /*var pose = context.pose();
            pose.pushMatrix();
            pose.translate(0.25F, 0.25F);
            *///?} else if >=1.21.6 {
            /*var pose = context.getMatrices();
            pose.pushMatrix();
            pose.translate(0.25F, 0.25F);
            *///?} else {
            var pose = context.getMatrices();
            pose.push();
            pose.translate(0.25F, 0.25F, 0);
            //?}
            try {
                draw(context, value, startX, topY, scale, tracking, AppearanceSettings.current().shadowColor());
            } finally {
                //? if >=1.21.6 {
                /*pose.popMatrix();
                *///?} else {
                pose.pop();
                //?}
            }
        }
        draw(context, value, startX, topY, scale, tracking, color);
    }

    private static void draw(
            //? if >=26.1 {
            /*GuiGraphicsExtractor context,
            *///?} else if >=1.20 {
            DrawContext context,
            //?} else {
            /*LegacyDrawContext context,
            *///?}
            String value,
            float startX,
            float topY,
            float scale,
            float tracking,
            int color
    ) {
        float baseline = topY + atlas.ascent * scale;
        float cursor = startX;

        //? if <1.21.4 {
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        float alpha = ((color >>> 24) & 0xFF) / 255.0F;
        RenderSystem.setShaderColor(red, green, blue, alpha);
        //?}

        int[] codepoints = value.codePoints().toArray();
        for (int index = 0; index < codepoints.length; index++) {
            Glyph glyph = glyph(codepoints[index]);
            if (glyph.width > 0 && glyph.height > 0) {
                int x = Math.round(cursor + glyph.offsetX * scale);
                int y = Math.round(baseline + glyph.offsetY * scale);
                int width = Math.max(1, Math.round(glyph.width * scale));
                int height = Math.max(1, Math.round(glyph.height * scale));
                //? if >=26.1 {
                /*context.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        x,
                        y,
                        (float) glyph.x,
                        (float) glyph.y,
                        width,
                        height,
                        glyph.width,
                        glyph.height,
                        atlas.textureWidth,
                        atlas.textureHeight,
                        color
                );
                *///?} else if >=1.21.6 {
                /*context.drawTexture(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        x,
                        y,
                        (float) glyph.x,
                        (float) glyph.y,
                        width,
                        height,
                        glyph.width,
                        glyph.height,
                        atlas.textureWidth,
                        atlas.textureHeight,
                        color
                );
                *///?} else if >=1.21.4 {
                /*context.drawTexture(
                        RenderLayer::getGuiTextured,
                        TEXTURE,
                        x,
                        y,
                        (float) glyph.x,
                        (float) glyph.y,
                        width,
                        height,
                        glyph.width,
                        glyph.height,
                        atlas.textureWidth,
                        atlas.textureHeight,
                        color
                );
                *///?} else {
                context.drawTexture(
                        TEXTURE,
                        x,
                        y,
                        width,
                        height,
                        (float) glyph.x,
                        (float) glyph.y,
                        glyph.width,
                        glyph.height,
                        atlas.textureWidth,
                        atlas.textureHeight
                );
                //?}
            }
            cursor += glyph.advance * scale;
            if (index + 1 < codepoints.length) {
                cursor += tracking;
            }
        }

        //? if <1.21.4
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    //? if >=26.1 {
    /*private static DynamicTexture createTexture(NativeImage image) {
        return new DynamicTexture(() -> "ERYDON Metrakron Cinzel atlas", image);
    }
    *///?} else {
    private static NativeImageBackedTexture createTexture(NativeImage image) {
        //? if >=1.21.5 {
        /*return new NativeImageBackedTexture(() -> "ERYDON Metrakron Cinzel atlas", image);
        *///?} else {
        return new NativeImageBackedTexture(image);
        //?}
    }
    //?}

    private static float measure(String value, float scale, float tracking) {
        int[] codepoints = value.codePoints().toArray();
        float width = 0.0F;
        for (int index = 0; index < codepoints.length; index++) {
            width += glyph(codepoints[index]).advance * scale;
            if (index + 1 < codepoints.length) {
                width += tracking;
            }
        }
        return width;
    }

    private static Glyph glyph(int codepoint) {
        Glyph glyph = atlas.glyphs.get(Integer.toString(codepoint));
        if (glyph != null) {
            return glyph;
        }
        Glyph fallback = atlas.glyphs.get(Integer.toString('?'));
        if (fallback == null) {
            throw new IllegalStateException("Cinzel atlas has no fallback glyph");
        }
        return fallback;
    }

    private static InputStream requiredResource(String path) throws IOException {
        InputStream stream = BitmapCinzelFont.class.getResourceAsStream(path);
        if (stream == null) {
            throw new IOException("Missing bundled resource " + path);
        }
        return stream;
    }

    private static final class Atlas {
        int textureWidth;
        int textureHeight;
        int ascent;
        Map<String, Glyph> glyphs;
    }

    private static final class Glyph {
        int width;
        int height;
        int offsetX;
        int offsetY;
        float advance;
        int x;
        int y;
    }
}
