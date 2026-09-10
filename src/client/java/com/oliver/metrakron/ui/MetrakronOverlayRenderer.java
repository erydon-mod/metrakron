package com.oliver.metrakron.ui;

import com.oliver.metrakron.MetrakronClient;
import com.oliver.metrakron.overlay.BronzeFrameStyle;
import com.oliver.metrakron.overlay.AppearanceSettings;
import com.oliver.metrakron.overlay.CountdownBar;
import com.oliver.metrakron.overlay.OverlayClock;
import com.oliver.metrakron.overlay.OverlayLayout;
import com.oliver.metrakron.timing.LoadSnapshot;
//? if >=26.1 {
/*import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.client.MinecraftClient;
//? if >=1.21.6 {
/*import net.minecraft.client.gl.RenderPipelines;
*///?} else if >=1.21.4 {
/*import net.minecraft.client.render.RenderLayer;
*///?}
//? if >=1.20
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
//?}

import java.io.IOException;
import java.io.InputStream;

public final class MetrakronOverlayRenderer {
    private static final String NERIUM_RESOURCE = "/assets/metrakron/textures/gui/nerium_panel.png";
    //? if >=26.1 {
    /*private static final Identifier NERIUM_TEXTURE = Identifier.fromNamespaceAndPath(
    *///?} else if >=1.21 {
    /*private static final Identifier NERIUM_TEXTURE = Identifier.of(
    *///?} else {
    private static final Identifier NERIUM_TEXTURE = new Identifier(
    //?}
            MetrakronClient.MOD_ID,
            "dynamic/nerium_panel"
    );
    private static final int PANEL_SHADE = 0x9A000000;
    private static final int TEXT = 0xFFF7F0E4;
    private static final int TIMER = 0xFFFFC65A;
    private static final int DETAIL = 0xFFD7C8B2;
    private static final int MAX_INITIALIZATION_ATTEMPTS = 5;
    private static final long INITIALIZATION_RETRY_BASE_NANOS = 1_000_000_000L;
    private static boolean neriumTextureReady;
    private static boolean neriumFailureLogged;
    private static int initializationAttempts;
    private static long nextInitializationAttemptNanos;
    private static AppearanceSettings loadedAppearance;
    private static int stoneWidth = 1024, stoneHeight = 1024;

    private MetrakronOverlayRenderer() {
    }

    //? if >=26.1 {
    /*public static void draw(GuiGraphicsExtractor context, LoadSnapshot snapshot) {
    *///?} else if >=1.20 {
    public static void draw(DrawContext context, LoadSnapshot snapshot) {
    //?} else {
    /*public static void draw(LegacyDrawContext context, LoadSnapshot snapshot) {
    *///?}
        if (!snapshot.active()) {
            return;
        }

        ensureInitialized();

        //? if >=26.1 {
        /*int screenWidth = context.guiWidth();
        int screenHeight = context.guiHeight();
        *///?} else {
        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();
        //?}
        if (screenWidth < 96 || screenHeight < OverlayLayout.PANEL_HEIGHT + OverlayLayout.MARGIN * 2) {
            return;
        }

        int width = Math.min(OverlayLayout.PANEL_WIDTH, screenWidth - OverlayLayout.MARGIN * 2);
        int x = OverlayLayout.MARGIN;
        int y = OverlayLayout.MARGIN;
        drawAt(context, snapshot, x, y, width);
    }

    //? if >=26.1 {
    /*public static void drawAt(GuiGraphicsExtractor context, LoadSnapshot snapshot, int x, int y, int width) {
    *///?} else if >=1.20 {
    public static void drawAt(DrawContext context, LoadSnapshot snapshot, int x, int y, int width) {
    //?} else {
    /*public static void drawAt(LegacyDrawContext context, LoadSnapshot snapshot, int x, int y, int width) {
    *///?}
        ensureInitialized();
        AppearanceSettings style = AppearanceSettings.current();
        drawRecessedPanel(context, x, y, width, OverlayLayout.PANEL_HEIGHT);

        if (!BitmapCinzelFont.isReady()) {
            return;
        }

        int centerX = x + width / 2;
        int textWidth = width - 20;
        BitmapCinzelFont.drawCentered(
                context,
                OverlayCopy.heading(snapshot),
                centerX,
                y + 9,
                textWidth,
                0.22F,
                0.40F,
                style.textColor(),
                true
        );

        BitmapCinzelFont.drawCentered(
                context,
                OverlayClock.displayedTime(snapshot.elapsedMillis(), snapshot.averageMillis()),
                centerX,
                y + 27,
                textWidth,
                0.48F,
                0.0F,
                style.accentColor(),
                true
        );

        BitmapCinzelFont.drawCentered(
                context,
                OverlayCopy.detail(snapshot),
                centerX,
                y + 62,
                textWidth,
                0.14F,
                0.20F,
                style.textColor(),
                true
        );
        CountdownBar.draw(context::fill, x + 12, y + 56, width - 24, snapshot.elapsedMillis(), snapshot.averageMillis(), style);
    }

    //? if >=26.1 {
    /*private static void drawRecessedPanel(GuiGraphicsExtractor context, int x, int y, int width, int height) {
    *///?} else if >=1.20 {
    private static void drawRecessedPanel(DrawContext context, int x, int y, int width, int height) {
    //?} else {
    /*private static void drawRecessedPanel(LegacyDrawContext context, int x, int y, int width, int height) {
    *///?}
        AppearanceSettings style = AppearanceSettings.current();
        BronzeFrameStyle.draw((l, t, r, b, color) -> context.fill(l, t, r, b, style.metalColor(color)), x, y, width, height);
        int contentInset = BronzeFrameStyle.CONTENT_INSET;
        if (neriumTextureReady) {
            drawTexture(
                    context,
                    NERIUM_TEXTURE,
                    x + contentInset,
                    y + contentInset,
                    width - contentInset * 2,
                    height - contentInset * 2,
                    style.stone() == AppearanceSettings.Stone.NERIUM ? 192.0F : 0.0F,
                    style.stone() == AppearanceSettings.Stone.NERIUM ? 336.0F : 0.0F,
                    style.stone() == AppearanceSettings.Stone.NERIUM ? 640 : stoneWidth,
                    style.stone() == AppearanceSettings.Stone.NERIUM ? 256 : stoneHeight,
                    stoneWidth,
                    stoneHeight
            );
        } else {
            context.fill(
                    x + contentInset,
                    y + contentInset,
                    x + width - contentInset,
                    y + height - contentInset,
                    style.darkText() ? 0xFFF8F6F0 : 0xFF080807
            );
        }
    }

    //? if >=26.1 {
    /*private static boolean initializeNeriumTexture(Minecraft client) {
    *///?} else {
    private static boolean initializeNeriumTexture(MinecraftClient client) {
    //?}
        NativeImage image = null;
        //? if >=26.1 {
        /*DynamicTexture texture = null;
        *///?} else {
        NativeImageBackedTexture texture = null;
        //?}
        try (InputStream stream = MetrakronOverlayRenderer.class.getResourceAsStream(AppearanceSettings.current().stoneResource())) {
            if (stream == null) {
                throw new IOException("Missing bundled resource " + NERIUM_RESOURCE);
            }
            image = NativeImage.read(stream);
            stoneWidth = image.getWidth();
            stoneHeight = image.getHeight();
            //? if >=26.1 {
            /*texture = createTexture("ERYDON Metrakron nerium panel", image);
            *///?} else {
            texture = createTexture("ERYDON Metrakron nerium panel", image);
            //?}
            image = null;
            //? if <1.21.5
            texture.setFilter(true, false);
            //? if >=26.1 {
            /*client.getTextureManager().register(NERIUM_TEXTURE, texture);
            *///?} else {
            client.getTextureManager().registerTexture(NERIUM_TEXTURE, texture);
            //?}
            neriumTextureReady = true;
            texture = null;
            return true;
        } catch (IOException | RuntimeException exception) {
            if (!neriumFailureLogged) {
                neriumFailureLogged = true;
                MetrakronClient.LOGGER.error("Unable to load the early nerium texture", exception);
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

    private static void ensureInitialized() {
        AppearanceSettings selected = AppearanceSettings.current();
        if (!selected.equals(loadedAppearance)) {
            loadedAppearance = selected;
            neriumTextureReady = false;
            initializationAttempts = 0;
            nextInitializationAttemptNanos = 0;
        }
        if (BitmapCinzelFont.isReady() && neriumTextureReady) {
            return;
        }
        long nowNanos = System.nanoTime();
        if (initializationAttempts >= MAX_INITIALIZATION_ATTEMPTS
                || nowNanos < nextInitializationAttemptNanos) {
            return;
        }
        initializationAttempts++;
        long retryDelay = INITIALIZATION_RETRY_BASE_NANOS
                << Math.min(initializationAttempts - 1, 3);
        nextInitializationAttemptNanos = nowNanos + retryDelay;

        //? if >=26.1 {
        /*Minecraft client = Minecraft.getInstance();
        *///?} else {
        MinecraftClient client = MinecraftClient.getInstance();
        //?}
        BitmapCinzelFont.initialize(client);
        if (!neriumTextureReady) {
            initializeNeriumTexture(client);
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

    //? if >=26.1 {
    /*private static void drawTexture(
            GuiGraphicsExtractor context,
    *///?} else if >=1.20 {
    private static void drawTexture(
            DrawContext context,
    //?} else {
    /*private static void drawTexture(
            LegacyDrawContext context,
    *///?}
            Identifier texture,
            int x,
            int y,
            int width,
            int height,
            float u,
            float v,
            int regionWidth,
            int regionHeight,
            int textureWidth,
            int textureHeight
    ) {
        //? if >=26.1 {
        /*context.blit(
                RenderPipelines.GUI_TEXTURED,
                texture,
                x,
                y,
                u,
                v,
                width,
                height,
                regionWidth,
                regionHeight,
                textureWidth,
                textureHeight
        );
        *///?} else if >=1.21.6 {
        /*context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                texture,
                x,
                y,
                u,
                v,
                width,
                height,
                regionWidth,
                regionHeight,
                textureWidth,
                textureHeight
        );
        *///?} else if >=1.21.4 {
        /*context.drawTexture(
                RenderLayer::getGuiTextured,
                texture,
                x,
                y,
                u,
                v,
                width,
                height,
                regionWidth,
                regionHeight,
                textureWidth,
                textureHeight
        );
        *///?} else {
        context.drawTexture(
                texture,
                x,
                y,
                width,
                height,
                u,
                v,
                regionWidth,
                regionHeight,
                textureWidth,
                textureHeight
        );
        //?}
    }
}
