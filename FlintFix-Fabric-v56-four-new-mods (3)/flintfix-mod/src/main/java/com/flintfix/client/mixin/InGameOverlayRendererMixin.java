package com.flintfix.client.mixin;

import com.flintfix.client.FlintFixClient;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Low Fire: moves the first-person fire overlay down so it covers less of the view. */
@Mixin(ScreenEffectRenderer.class)
public abstract class InGameOverlayRendererMixin {
    @Unique private static boolean flintfix$fireLowered;

    @Inject(method = "renderFire", at = @At("HEAD"), require = 0)
    private static void flintfix$lowerFire(CallbackInfo ci, @Local(argsOnly = true) PoseStack matrices) {
        flintfix$fireLowered = false;
        if (FlintFixClient.CONFIG == null || !FlintFixClient.CONFIG.lowOverlaysEnabled || !FlintFixClient.CONFIG.lowFire) return;
        matrices.pushPose();
        matrices.translate(0.0f, -FlintFixClient.CONFIG.lowFireAmount, 0.0f);
        flintfix$fireLowered = true;
    }

    @Inject(method = "renderFire", at = @At("RETURN"), require = 0)
    private static void flintfix$restoreFire(CallbackInfo ci, @Local(argsOnly = true) PoseStack matrices) {
        if (!flintfix$fireLowered) return;
        matrices.popPose();
        flintfix$fireLowered = false;
    }
}
