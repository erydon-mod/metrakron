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

/** Portorium controls retain native button interaction and focus semantics. */
final class PortoriumSurfaces {
    private static final Identifier STONE = id("portorium_control_background.png");
    private static final Identifier BRONZE = id("portorium_control_frame.png");
    private static final Identifier SILVER = id("portorium_control_frame_silver.png");
    private static final Identifier GLACIUM = id("glacium_panel.png");
    private static boolean earlyFramesReady;
    private static long retryAt;
    private static int attempts;
    private PortoriumSurfaces() { }
    private static Identifier id(String file) {
        //? if >=26.1 {
        /*return Identifier.fromNamespaceAndPath("metrakron", "textures/gui/" + file);
        *///?} else if >=1.21 {
        /*return Identifier.of("metrakron", "textures/gui/" + file);
        *///?} else {
        return new Identifier("metrakron", "textures/gui/" + file);
        //?}
    }
    static void panel(
            //? if >=26.1 {
            /*GuiGraphicsExtractor context,
            *///?} else if >=1.20 {
            DrawContext context,
            //?} else {
            /*LegacyDrawContext context,
            *///?}
            int x, int y, int w, int h, boolean silver) {
        texture(context, GLACIUM, x, y, w, h, 326, 0, 308, 384, 960, 384);
        frame(context, x, y, w, h, silver);
    }
    static void button(
            //? if >=26.1 {
            /*GuiGraphicsExtractor context,
            *///?} else if >=1.20 {
            DrawContext context,
            //?} else {
            /*LegacyDrawContext context,
            *///?}
            int x, int y, int w, boolean highlighted, boolean silver) {
        int sw = Math.min(1024, w * 3);
        int u = Math.floorMod(x * 17 + y * 7, Math.max(1, 1024 - sw));
        int v = Math.floorMod(x * 5 + y * 13, 944);
        texture(context, STONE, x, y, w, 20, u, v, sw, 80, 1024, 1024);
        context.fill(x, y, x + w, y + 20, highlighted ? 0x56000000 : 0x74000000);
        frame(context, x, y, w, 20, silver);
        if (highlighted) {
            context.fill(x + 5, y + 3, x + w - 5, y + 4, 0xFFF4E2B9);
        }
    }
    static void loadingFrame(
            //? if >=26.1 {
            /*GuiGraphicsExtractor context,
            *///?} else if >=1.20 {
            DrawContext context,
            //?} else {
            /*LegacyDrawContext context,
            *///?}
            int x, int y, int w, int h, com.oliver.metrakron.overlay.AppearanceSettings style) {
        long now = System.nanoTime();
        if (!earlyFramesReady && now >= retryAt && attempts < 5) {
            attempts++;
            retryAt = now + 1_000_000_000L;
            earlyFramesReady = PortoriumLogo.loadTexture(BRONZE, "/assets/metrakron/textures/gui/portorium_control_frame.png")
                    && PortoriumLogo.loadTexture(SILVER, "/assets/metrakron/textures/gui/portorium_control_frame_silver.png");
        }
        if (earlyFramesReady) frame(context, x, y, w, h, style.metal() == com.oliver.metrakron.overlay.AppearanceSettings.Metal.SILVER);
        else com.oliver.metrakron.overlay.BronzeFrameStyle.draw(
                (l, t, r, b, color) -> context.fill(l, t, r, b, style.metalColor(color)), x, y, w, h);
    }
    private static void frame(
            //? if >=26.1 {
            /*GuiGraphicsExtractor context,
            *///?} else if >=1.20 {
            DrawContext context,
            //?} else {
            /*LegacyDrawContext context,
            *///?}
            int x, int y, int w, int h, boolean silver) {
        Identifier image = silver ? SILVER : BRONZE;
        for (int row = 0; row < 3; row++) {
            int dy = row == 0 ? y : row == 1 ? y + 4 : y + h - 4;
            int dh = row == 1 ? h - 8 : 4;
            for (int col = 0; col < 3; col++) {
                if (row == 1 && col == 1) continue;
                int dx = col == 0 ? x : col == 1 ? x + 4 : x + w - 4;
                int dw = col == 1 ? w - 8 : 4;
                texture(context, image, dx, dy, dw, dh,
                        col == 0 ? 0 : col == 1 ? 16 : 112,
                        row == 0 ? 0 : row == 1 ? 16 : 64,
                        col == 1 ? 96 : 16, row == 1 ? 48 : 16, 128, 80);
            }
        }
    }
    static void texture(
            //? if >=26.1 {
            /*GuiGraphicsExtractor context,
            *///?} else if >=1.20 {
            DrawContext context,
            //?} else {
            /*LegacyDrawContext context,
            *///?}
            Identifier texture, int x, int y, int w, int h, float u, float v, int sw, int sh, int tw, int th) {
        //? if >=26.1 {
        /*context.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, w, h, sw, sh, tw, th, -1);
        *///?} else if >=1.21.6 {
        /*context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, w, h, sw, sh, tw, th, -1);
        *///?} else if >=1.21.4 {
        /*context.drawTexture(RenderLayer::getGuiTextured, texture, x, y, u, v, w, h, sw, sh, tw, th, -1);
        *///?} else {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        context.drawTexture(texture, x, y, w, h, u, v, sw, sh, tw, th);
        //?}
    }
}
