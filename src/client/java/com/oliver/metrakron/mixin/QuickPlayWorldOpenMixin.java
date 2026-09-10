package com.oliver.metrakron.mixin;

import com.oliver.metrakron.MetrakronClient;
//? if >=26.1 {
/*import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
*///?} else if >=1.19 {
import net.minecraft.server.integrated.IntegratedServerLoader;
//? if <1.20.2
import net.minecraft.client.gui.screen.Screen;
//?} else {
/*import net.minecraft.client.MinecraftClient;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Resolves the actual Quick Play save before blocking world-data work, without restarting its clock. */
//? if >=26.1 {
/*@Mixin(WorldOpenFlows.class)
*///?} else if >=1.19 {
@Mixin(IntegratedServerLoader.class)
//?} else {
/*@Mixin(MinecraftClient.class)
*///?}
abstract class QuickPlayWorldOpenMixin {
    //? if >=26.1 {
    /*@Inject(method = "openWorld(Ljava/lang/String;Ljava/lang/Runnable;)V", at = @At("HEAD"))
    private void metrakron$observeQuickPlayWorld(String folderName, Runnable onCancel, CallbackInfo ci) {
    *///?} else if >=1.20.2 {
    /*@Inject(method = "start(Ljava/lang/String;Ljava/lang/Runnable;)V", at = @At("HEAD"))
    private void metrakron$observeQuickPlayWorld(String folderName, Runnable onCancel, CallbackInfo ci) {
    *///?} else if >=1.19 {
    @Inject(method = "start(Lnet/minecraft/client/gui/screen/Screen;Ljava/lang/String;)V", at = @At("HEAD"))
    private void metrakron$observeQuickPlayWorld(Screen parent, String folderName, CallbackInfo ci) {
    //?} else {
    /*@Inject(method = "startIntegratedServer(Ljava/lang/String;)V", at = @At("HEAD"))
    private void metrakron$observeQuickPlayWorld(String folderName, CallbackInfo ci) {
    *///?}
        MetrakronClient.quickPlayWorldOpening(folderName);
    }
}
