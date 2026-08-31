package com.oliver.metrakron.mixin;

import com.oliver.metrakron.MetrakronClient;
//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LoadingOverlay;
*///?} else {
//? if <1.20 {
/*import net.minecraft.client.util.math.MatrixStack;
*///?} else {
import net.minecraft.client.gui.DrawContext;
//?}
import net.minecraft.client.gui.screen.SplashOverlay;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >=26.1 {
/*@Mixin(LoadingOverlay.class)
*///?} else {
@Mixin(SplashOverlay.class)
//?}
abstract class SplashOverlayMixin {
    //? if >=26.1 {
    /*@Inject(method = "extractRenderState", at = @At("TAIL"))
    private void metrakron$drawStartupClock(
            GuiGraphicsExtractor context,
    *///?} else {
    @Inject(method = "render", at = @At("TAIL"))
    private void metrakron$drawStartupClock(
            //? if <1.20 {
            /*MatrixStack context,
            *///?} else {
            DrawContext context,
            //?}
    //?}
            int mouseX,
            int mouseY,
            float delta,
            CallbackInfo callbackInfo
    ) {
        MetrakronClient.renderStartupTimer(context);
    }
}
