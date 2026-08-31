package com.oliver.metrakron.mixin;

import com.oliver.metrakron.MetrakronClient;
//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
*///?} else {
//? if <1.20 {
/*import net.minecraft.client.util.math.MatrixStack;
*///?} else {
import net.minecraft.client.gui.DrawContext;
//?}
import net.minecraft.client.gui.screen.Screen;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ends startup on the first genuinely usable screen after the loading overlay is gone.
 * This also covers first-run accessibility screens and custom main menus.
 */
@Mixin(Screen.class)
abstract class ReadyScreenMixin {
    //? if >=26.1 {
    /*@Inject(method = "extractRenderState", at = @At("TAIL"))
    private void metrakron$finishStartupClock(
            GuiGraphicsExtractor context,
    *///?} else {
    @Inject(method = "render", at = @At("TAIL"))
    private void metrakron$finishStartupClock(
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
        MetrakronClient.finishStartupIfReady();
    }
}
