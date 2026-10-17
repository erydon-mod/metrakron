package com.oliver.metrakron.ui;

//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.util.Identifier;
//? if >=1.20
import net.minecraft.client.gui.DrawContext;
//? if >=1.21.6 {
/*import net.minecraft.client.gl.RenderPipelines;
*///?} else if >=1.21.4 {
/*import net.minecraft.client.render.RenderLayer;
*///?} else {
import com.mojang.blaze3d.systems.RenderSystem;
//?}
//?}

import com.oliver.metrakron.MetrakronClient;
import com.oliver.metrakron.overlay.LogoAnimation;
import java.io.IOException;
import java.io.InputStream;
//? if >=26.1 {
/*import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
*///?} else {
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
//?}

/** Cached branding for menus and the early loading display. */
final class PortoriumLogo {
    private static final Identifier EMBLEM = id("icon.png");
    private static final Identifier EARLY_LOGO = id("dynamic/portorium_logo");
    private static boolean logoReady;
    private static boolean logoFailureLogged;
    private static int attempts;
    private static long retryAt;

    private PortoriumLogo() { }

    private static Identifier id(String path) {
        //? if >=26.1 {
        /*return Identifier.fromNamespaceAndPath("metrakron", path);
        *///?} else if >=1.21 {
        /*return Identifier.of("metrakron", path);
        *///?} else {
        return new Identifier("metrakron", path);
        //?}
    }

    static boolean loadTexture(Identifier target, String resource) {
        //? if >=26.1 {
        /*return initializeLogoTexture(Minecraft.getInstance(), target, resource);
        *///?} else {
        return initializeLogoTexture(MinecraftClient.getInstance(), target, resource);
        //?}
    }

    static int headerHeight(int screenHeight) {
        return screenHeight >= 296 ? 56 : 0;
    }

    static void draw(
            //? if >=26.1 {
            /*GuiGraphicsExtractor context,
            *///?} else if >=1.20 {
            DrawContext context,
            //?} else {
            /*LegacyDrawContext context,
            *///?}
            int centerX, int top, int headerHeight) {
        if (headerHeight > 0) {
            // Reuse the early-loading texture rather than uploading a second
            // decoded copy of the enlarged animation for the settings menu.
            drawLoading(context, centerX, top + 1, 182);
        } else {
            PortoriumSurfaces.texture(context, EMBLEM, centerX - 106, top + 1,
                    14, 14, 0, 0, 512, 512, 512, 512);
        }
    }
    static void drawLoading(
            //? if >=26.1 {
            /*GuiGraphicsExtractor context,
            *///?} else if >=1.20 {
            DrawContext context,
            //?} else {
            /*LegacyDrawContext context,
            *///?}
            int centerX, int top, int width) {
        long now = System.nanoTime();
        if (!logoReady && now >= retryAt && attempts < 5) {
            attempts++;
            retryAt = now + 1_000_000_000L;
            logoReady = loadTexture(EARLY_LOGO, LogoAnimation.RESOURCE);
        }
        if (!logoReady) return;
        int frame = LogoAnimation.frame(now);
        PortoriumSurfaces.texture(context, EARLY_LOGO, centerX - width / 2, top, width,
                Math.max(1, width * LogoAnimation.FRAME_HEIGHT / LogoAnimation.FRAME_WIDTH),
                (frame % LogoAnimation.COLUMNS) * LogoAnimation.FRAME_WIDTH,
                (frame / LogoAnimation.COLUMNS) * LogoAnimation.FRAME_HEIGHT,
                LogoAnimation.FRAME_WIDTH, LogoAnimation.FRAME_HEIGHT,
                LogoAnimation.ATLAS_WIDTH, LogoAnimation.ATLAS_HEIGHT);
    }

    //? if >=26.1 {
    /*private static boolean initializeLogoTexture(Minecraft client, Identifier target, String resource) {
    *///?} else {
    private static boolean initializeLogoTexture(MinecraftClient client, Identifier target, String resource) {
    //?}
        NativeImage image = null;
        //? if >=26.1 {
        /*DynamicTexture texture = null;
        *///?} else {
        NativeImageBackedTexture texture = null;
        //?}
        try (InputStream stream = PortoriumLogo.class.getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IOException("Missing bundled resource " + resource);
            }
            image = NativeImage.read(stream);
            //? if >=26.1 {
            /*texture = createTexture("ERYDON Metrakron branding atlas", image);
            *///?} else {
            texture = createTexture("ERYDON Metrakron branding atlas", image);
            //?}
            image = null;
            // Dynamic textures bypass .mcmeta; keep smooth filtering on every adapter.
            //? if <1.21.9
            texture.setFilter(true, false);
            //? if >=26.1 {
            /*client.getTextureManager().register(target, texture);
            *///?} else {
            client.getTextureManager().registerTexture(target, texture);
            //?}
            texture = null;
            return true;
        } catch (IOException | RuntimeException exception) {
            if (!logoFailureLogged) {
                logoFailureLogged = true;
                MetrakronClient.LOGGER.error("Unable to load the early branding atlas", exception);
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

    //? if >=26.1 {
    /*private static DynamicTexture createTexture(String name, NativeImage image) {
        return new DynamicTexture(() -> name, image) {
            @Override public com.mojang.blaze3d.textures.GpuSampler getSampler() {
                return com.mojang.blaze3d.systems.RenderSystem.getSamplerCache()
                        .getClampToEdge(com.mojang.blaze3d.textures.FilterMode.LINEAR);
            }
        };
    }
    *///?} else {
    private static NativeImageBackedTexture createTexture(String name, NativeImage image) {
        //? if >=1.21.9 {
        /*return new NativeImageBackedTexture(() -> name, image) {
            @Override public net.minecraft.client.gl.GpuSampler getSampler() {
                return com.mojang.blaze3d.systems.RenderSystem.getSamplerCache()
                        .get(com.mojang.blaze3d.textures.FilterMode.LINEAR);
            }
        };
        *///?} else if >=1.21.5 {
        /*return new NativeImageBackedTexture(() -> name, image);
        *///?} else {
        return new NativeImageBackedTexture(image);
        //?}
    }
    //?}

}
