package com.oliver.metrakron.mixin;

import com.oliver.metrakron.MetrakronClient;
//? if >=26.1 {
/*import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
*///?} else {
import net.minecraft.client.gui.screen.world.WorldListWidget;
//?}
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >=26.1 {
/*@Mixin(WorldSelectionList.WorldListEntry.class)
*///?} else {
//? if <1.19 {
/*@Mixin(WorldListWidget.Entry.class)
*///?} else {
@Mixin(WorldListWidget.WorldEntry.class)
//?}
//?}
abstract class WorldEntryMixin {
    //? if >=26.1 {
    /*@Shadow
    public abstract LevelSummary getLevelSummary();

    @Inject(method = "joinWorld", at = @At("HEAD"))
    private void metrakron$startWorldClock(CallbackInfo callbackInfo) {
        LevelSummary level = getLevelSummary();
        MetrakronClient.startWorldClock(
                level.getLevelId(),
                level.getLevelName()
        );
    }
    *///?} else {
    @Shadow
    @Final
    private LevelSummary level;

    @Inject(method = "play", at = @At("HEAD"))
    private void metrakron$startWorldClock(CallbackInfo callbackInfo) {
        MetrakronClient.startWorldClock(
                level.getName(),
                level.getDisplayName()
        );
    }
    //?}
}
