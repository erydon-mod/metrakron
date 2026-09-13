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
    private static final Identifier LOGO = id("textures/gui/portorium_logo.png");
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
        int frame = LogoAnimation.frame(System.nanoTime());
        boolean wide = headerHeight > 0;
        Identifier texture = wide ? LOGO : EMBLEM;
        int w = wide ? 182 : 14;
        int h = wide ? 54 : 14;
        int x = wide ? centerX - w / 2 : centerX - 106;
        int y = top + 1;
        float u = wide ? (frame % 8) * 256 : 0;
        float v = wide ? (frame / 8) * 76 : 0;
        int sw = wide ? 256 : 512;
        int sh = wide ? 76 : 512;
        int tw = wide ? 2048 : 512;
        int th = wide ? 1672 : 512;
        PortoriumSurfaces.texture(context, texture, x, y, w, h, u, v, sw, sh, tw, th);
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
                Math.max(1, width * 76 / 256), (frame % 8) * 256, (frame / 8) * 76,
                256, 76, 2048, 1672);
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
            //? if <1.21.5
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
        return new DynamicTexture(() -> name, image);
    }
    *///?} else {
    private static NativeImageBackedTexture createTexture(String name, NativeImage image) {
        //? if >=1.21.5 {
        /*return new NativeImageBackedTexture(() -> name, image);
        *///?} else {
        return new NativeImageBackedTexture(image);
        //?}
    }
    //?}

}
