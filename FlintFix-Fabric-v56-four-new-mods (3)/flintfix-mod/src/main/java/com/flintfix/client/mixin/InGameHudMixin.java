package com.flintfix.client.mixin;

import com.flintfix.client.FlintFixCrosshair;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Custom crosshair: draws FlintFix's crosshair in place of vanilla's when enabled. */
//? if >=26.2 {
/*@Mixin(net.minecraft.client.gui.Hud.class)
*///?} else {
@Mixin(Gui.class)
//?}
public abstract class InGameHudMixin {
    //? if >=26.1 {
    /*@Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true, require = 0)
    *///?} else {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true, require = 0)
    //?}
    private void flintfix$customCrosshair(CallbackInfo ci, @Local(argsOnly = true) GuiGraphics context) {
        if (FlintFixCrosshair.render(context)) ci.cancel();
    }
}
