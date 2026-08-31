//? if <1.20 {
/*package com.oliver.metrakron.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public final class LegacyDrawContext {
    private final MatrixStack matrices;

    public LegacyDrawContext(MatrixStack matrices) {
        this.matrices = matrices;
    }

    public int getScaledWindowWidth() {
        return MinecraftClient.getInstance().getWindow().getScaledWidth();
    }

    public int getScaledWindowHeight() {
        return MinecraftClient.getInstance().getWindow().getScaledHeight();
    }

    public void fill(int x1, int y1, int x2, int y2, int color) {
        DrawableHelper.fill(matrices, x1, y1, x2, y2, color);
    }

    public void drawTexture(
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
        RenderSystem.setShaderTexture(0, texture);
        DrawableHelper.drawTexture(
                matrices,
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
    }
}
*///?}
