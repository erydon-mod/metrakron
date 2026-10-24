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
import java.util.HashMap;
import java.util.Properties;

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
    //? if >=26.1 {
    /*private static final Identifier TIMER_TEXTURE = Identifier.fromNamespaceAndPath(
    *///?} else if >=1.21 {
    /*private static final Identifier TIMER_TEXTURE = Identifier.of(
    *///?} else {
    private static final Identifier TIMER_TEXTURE = new Identifier(
    //?}
            MetrakronClient.MOD_ID, "dynamic/cinzel_timer_atlas");

    private static Atlas atlas;
    private static Atlas timerAtlas;
    private static String timerFont;
    private static boolean ready;
    private static boolean failureLogged;
    private static String loadedFont;
    private static BitmapMenuLayout menuLayout;
    private static String menuFont;

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
            // Dynamic textures bypass .mcmeta, so filtering is set explicitly.
            //? if <1.21.9
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

    //? if >=26.1 {
    /*private static boolean initializeTimer(Minecraft client) {
    *///?} else {
    private static boolean initializeTimer(MinecraftClient client) {
    //?}
        String selectedFont = AppearanceSettings.current().fontId();
        if (selectedFont.equals(timerFont)) return timerAtlas != null;
        timerFont = selectedFont;
        timerAtlas = null;
        NativeImage image = null;
        //? if >=26.1 {
        /*DynamicTexture texture = null;
        *///?} else {
        NativeImageBackedTexture texture = null;
        //?}
        String base = "/assets/metrakron/textures/gui/" + selectedFont + "_timer_atlas";
        try (InputStream metadata = requiredResource(base + ".properties");
             InputStream png = requiredResource(base + ".png")) {
            Properties properties = new Properties();
            properties.load(metadata);
            Atlas loaded = new Atlas();
            loaded.textureWidth = Integer.parseInt(properties.getProperty("textureWidth"));
            loaded.textureHeight = Integer.parseInt(properties.getProperty("textureHeight"));
            loaded.fontSize = Integer.parseInt(properties.getProperty("fontSize"));
            loaded.ascent = Integer.parseInt(properties.getProperty("ascent"));
            loaded.glyphs = new HashMap<>();
            for (String key : properties.stringPropertyNames()) {
                if (!key.startsWith("glyph.")) continue;
                String[] values = properties.getProperty(key).split(",");
                if (values.length != 7) throw new IOException("Invalid timer glyph metrics");
                Glyph glyph = new Glyph();
                glyph.x = Integer.parseInt(values[0]); glyph.y = Integer.parseInt(values[1]);
                glyph.width = Integer.parseInt(values[2]); glyph.height = Integer.parseInt(values[3]);
                glyph.offsetX = Integer.parseInt(values[4]); glyph.offsetY = Integer.parseInt(values[5]);
                glyph.advance = Float.parseFloat(values[6]);
                loaded.glyphs.put(key.substring("glyph.".length()), glyph);
            }
            image = NativeImage.read(png);
            if (loaded.fontSize <= 0 || loaded.glyphs.size() != 12
                    || loaded.textureWidth != image.getWidth() || loaded.textureHeight != image.getHeight()) {
                throw new IOException("Timer atlas metrics do not match its texture");
            }
            texture = createTexture(image);
            image = null;
            //? if <1.21.9
            texture.setFilter(true, false);
            //? if >=26.1 {
            /*client.getTextureManager().register(TIMER_TEXTURE, texture);
            *///?} else {
            client.getTextureManager().registerTexture(TIMER_TEXTURE, texture);
            //?}
            texture = null;
            timerAtlas = loaded;
            return true;
        } catch (IOException | RuntimeException exception) {
            MetrakronClient.LOGGER.warn("Unable to load high-resolution {} timer; retaining the standard font", selectedFont, exception);
            return false;
        } finally {
            if (texture != null) texture.close();
            else if (image != null) image.close();
        }
    }

    /** Centers visible menu lettering and scales a complete baseline together. */
    static void drawMenuLabel(
            //? if >=26.1 {
            /*GuiGraphicsExtractor context,
            *///?} else if >=1.20 {
            DrawContext context,
            //?} else {
            /*LegacyDrawContext context,
            *///?}
            String value, int centerX, int centerY, int maximumWidth, int maximumHeight,
            float preferredScale, int color
    ) {
        if (!ready || value == null || value.isEmpty()) return;
        if (!loadedFont.equals(menuFont)) {
            menuFont = loadedFont;
            menuLayout = null;
            try (InputStream metadata = requiredResource(ATLAS_JSON.replace("cinzel", loadedFont));
                 InputStream png = requiredResource(ATLAS_PNG.replace("cinzel", loadedFont))) {
                menuLayout = BitmapMenuLayout.load(metadata, png);
            } catch (IOException | RuntimeException exception) {
                MetrakronClient.LOGGER.warn("Unable to measure menu font ink bounds", exception);
            }
        }
        if (menuLayout == null) {
            drawCentered(context, value, centerX, centerY - 5, maximumWidth, preferredScale, 0, color, false);
            return;
        }
        var placement = menuLayout.place(value, centerX, centerY, maximumWidth, maximumHeight, preferredScale);
        // Glyph positions and sizes stay at atlas resolution. One matrix transform scales
        // the whole baseline, avoiding a different rounding error for each letter.
        //? if >=26.1 {
        /*var pose = context.pose();
        pose.pushMatrix();
        pose.translate(placement.originX(), placement.baselineY());
        pose.scale(placement.scale(), placement.scale());
        *///?} else if >=1.21.6 {
        /*var pose = context.getMatrices();
        pose.pushMatrix();
        pose.translate(placement.originX(), placement.baselineY());
        pose.scale(placement.scale(), placement.scale());
        *///?} else {
        var pose = context.getMatrices();
        pose.push();
        pose.translate(placement.originX(), placement.baselineY(), 0);
        pose.scale(placement.scale(), placement.scale(), 1);
        //?}
        try {
            draw(context, value, 0, -atlas.ascent, 1, 0, color);
        } finally {
            //? if >=1.21.6 {
            /*pose.popMatrix();
            *///?} else {
            pose.pop();
            //?}
        }
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

        drawCentered(context, TEXTURE, atlas, value, centerX, topY, maximumWidth,
                preferredScale, tracking, color, shadow);
    }

    /** Uses the existing 160px timer artwork at the same logical size as the 40px font. */
    static void drawTimerCentered(
            //? if >=26.1 {
            /*GuiGraphicsExtractor context,
            *///?} else if >=1.20 {
            DrawContext context,
            //?} else {
            /*LegacyDrawContext context,
            *///?}
            String value, int centerX, int topY, int maximumWidth, float preferredScale,
            float tracking, int color, boolean shadow
    ) {
        if (!ready || value == null || value.isEmpty()) return;
        //? if >=26.1 {
        /*boolean highResolution = initializeTimer(Minecraft.getInstance());
        *///?} else {
        boolean highResolution = initializeTimer(MinecraftClient.getInstance());
        //?}
        if (!highResolution) {
            drawCentered(context, value, centerX, topY, maximumWidth, preferredScale, tracking, color, shadow);
            return;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!timerAtlas.glyphs.containsKey(Integer.toString(value.charAt(i)))) {
                drawCentered(context, value, centerX, topY, maximumWidth, preferredScale, tracking, color, shadow);
                return;
            }
        }
        float scale = preferredScale * atlas.fontSize / timerAtlas.fontSize;
        drawCentered(context, TIMER_TEXTURE, timerAtlas, value, centerX, topY, maximumWidth,
                scale, tracking, color, shadow);
    }

    private static void drawCentered(
            //? if >=26.1 {
            /*GuiGraphicsExtractor context,
            *///?} else if >=1.20 {
            DrawContext context,
            //?} else {
            /*LegacyDrawContext context,
            *///?}
            Identifier texture, Atlas selectedAtlas, String value, int centerX, int topY,
            int maximumWidth, float preferredScale, float tracking, int color, boolean shadow
    ) {

        float unscaledWidth = measure(selectedAtlas, value, 1.0F, tracking / preferredScale);
        float scale = preferredScale;
        if (unscaledWidth * scale > maximumWidth) {
            scale = maximumWidth / unscaledWidth;
        }

        float width = measure(selectedAtlas, value, scale, tracking);
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
                draw(context, texture, selectedAtlas, value, startX, topY, scale, tracking,
                        AppearanceSettings.current().shadowColor());
            } finally {
                //? if >=1.21.6 {
                /*pose.popMatrix();
                *///?} else {
                pose.pop();
                //?}
            }
        }
        draw(context, texture, selectedAtlas, value, startX, topY, scale, tracking, color);
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
        draw(context, TEXTURE, atlas, value, startX, topY, scale, tracking, color);
    }

    private static void draw(
            //? if >=26.1 {
            /*GuiGraphicsExtractor context,
            *///?} else if >=1.20 {
            DrawContext context,
            //?} else {
            /*LegacyDrawContext context,
            *///?}
            Identifier texture, Atlas selectedAtlas, String value, float startX, float topY,
            float scale, float tracking, int color
    ) {
        float baseline = topY + selectedAtlas.ascent * scale;
        float cursor = 0;
        // Scale one baseline rather than rounding every glyph to a logical GUI
        // pixel first. At a large GUI scale those rounding errors become visible.
        //? if >=26.1 {
        /*var pose = context.pose();
        pose.pushMatrix();
        pose.translate(startX, baseline);
        pose.scale(scale, scale);
        *///?} else if >=1.21.6 {
        /*var pose = context.getMatrices();
        pose.pushMatrix();
        pose.translate(startX, baseline);
        pose.scale(scale, scale);
        *///?} else {
        var pose = context.getMatrices();
        pose.push();
        pose.translate(startX, baseline, 0);
        pose.scale(scale, scale, 1);
        //?}

        //? if <1.21.4 {
        // GUI fills (including the preview bar) can disable blending. Restore it
        // for every font pass so antialiased glyph edges retain their alpha.
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        float alpha = ((color >>> 24) & 0xFF) / 255.0F;
        RenderSystem.setShaderColor(red, green, blue, alpha);
        //?}

        try {
            int[] codepoints = value.codePoints().toArray();
            for (int index = 0; index < codepoints.length; index++) {
                Glyph glyph = glyph(selectedAtlas, codepoints[index]);
                if (glyph.width > 0 && glyph.height > 0) {
                    int x = Math.round(cursor + glyph.offsetX);
                    int y = glyph.offsetY;
                    int width = glyph.width;
                    int height = glyph.height;
                //? if >=26.1 {
                /*context.blit(
                        RenderPipelines.GUI_TEXTURED,
                        texture,
                        x,
                        y,
                        (float) glyph.x,
                        (float) glyph.y,
                        width,
                        height,
                        glyph.width,
                        glyph.height,
                        selectedAtlas.textureWidth,
                        selectedAtlas.textureHeight,
                        color
                );
                *///?} else if >=1.21.6 {
                /*context.drawTexture(
                        RenderPipelines.GUI_TEXTURED,
                        texture,
                        x,
                        y,
                        (float) glyph.x,
                        (float) glyph.y,
                        width,
                        height,
                        glyph.width,
                        glyph.height,
                        selectedAtlas.textureWidth,
                        selectedAtlas.textureHeight,
                        color
                );
                *///?} else if >=1.21.4 {
                /*context.drawTexture(
                        RenderLayer::getGuiTextured,
                        texture,
                        x,
                        y,
                        (float) glyph.x,
                        (float) glyph.y,
                        width,
                        height,
                        glyph.width,
                        glyph.height,
                        selectedAtlas.textureWidth,
                        selectedAtlas.textureHeight,
                        color
                );
                *///?} else {
                context.drawTexture(
                        texture,
                        x,
                        y,
                        width,
                        height,
                        (float) glyph.x,
                        (float) glyph.y,
                        glyph.width,
                        glyph.height,
                        selectedAtlas.textureWidth,
                        selectedAtlas.textureHeight
                );
                //?}
                }
                cursor += glyph.advance;
                if (index + 1 < codepoints.length) {
                    cursor += tracking / scale;
                }
            }
        } finally {
            //? if <1.21.4
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            //? if >=1.21.6 {
            /*pose.popMatrix();
            *///?} else {
            pose.pop();
            //?}
        }
    }

    //? if >=26.1 {
    /*private static DynamicTexture createTexture(NativeImage image) {
        return new DynamicTexture(() -> "ERYDON Metrakron Cinzel atlas", image) {
            @Override public com.mojang.blaze3d.textures.GpuSampler getSampler() {
                return com.mojang.blaze3d.systems.RenderSystem.getSamplerCache()
                        .getClampToEdge(com.mojang.blaze3d.textures.FilterMode.LINEAR);
            }
        };
    }
    *///?} else {
    private static NativeImageBackedTexture createTexture(NativeImage image) {
        //? if >=1.21.9 {
        /*return new NativeImageBackedTexture(() -> "ERYDON Metrakron Cinzel atlas", image) {
            @Override public net.minecraft.client.gl.GpuSampler getSampler() {
                return com.mojang.blaze3d.systems.RenderSystem.getSamplerCache()
                        .get(com.mojang.blaze3d.textures.FilterMode.LINEAR);
            }
        };
        *///?} else if >=1.21.5 {
        /*return new NativeImageBackedTexture(() -> "ERYDON Metrakron Cinzel atlas", image);
        *///?} else {
        return new NativeImageBackedTexture(image);
        //?}
    }
    //?}

    private static float measure(Atlas selectedAtlas, String value, float scale, float tracking) {
        int[] codepoints = value.codePoints().toArray();
        float width = 0.0F;
        for (int index = 0; index < codepoints.length; index++) {
            width += glyph(selectedAtlas, codepoints[index]).advance * scale;
            if (index + 1 < codepoints.length) {
                width += tracking;
            }
        }
        return width;
    }

    private static Glyph glyph(Atlas selectedAtlas, int codepoint) {
        Glyph glyph = selectedAtlas.glyphs.get(Integer.toString(codepoint));
        if (glyph != null) {
            return glyph;
        }
        Glyph fallback = selectedAtlas.glyphs.get(Integer.toString('?'));
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
        int fontSize;
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
