package com.flintfix.client.mixin;

import com.flintfix.client.FlintFixClient;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Low Totem: shrinks the totem-of-undying pop animation around the center of the screen. */
@Mixin(GameRenderer.class)
public abstract class GameRendererTotemMixin {
    //? if >=1.21 {
    @Unique private boolean flintfix$totemScaled;

    @Inject(method = "renderItemActivationAnimation", at = @At("HEAD"), require = 0)
    private void flintfix$shrinkTotem(CallbackInfo ci, @Local(argsOnly = true) GuiGraphics context) {
        flintfix$totemScaled = false;
        if (FlintFixClient.CONFIG == null || !FlintFixClient.CONFIG.lowOverlaysEnabled || !FlintFixClient.CONFIG.lowTotem) return;
        float scale = FlintFixClient.CONFIG.totemSize;
        float cx = context.guiWidth() / 2.0f;
        float cy = context.guiHeight() / 2.0f;
        context.pose().pushPose();
        context.pose().translate(cx, cy, 0.0f);
        context.pose().scale(scale, scale, 1.0f);
        context.pose().translate(-cx, -cy, 0.0f);
        flintfix$totemScaled = true;
    }

    @Inject(method = "renderItemActivationAnimation", at = @At("RETURN"), require = 0)
    private void flintfix$restoreTotem(CallbackInfo ci, @Local(argsOnly = true) GuiGraphics context) {
        if (!flintfix$totemScaled) return;
        context.pose().popPose();
        flintfix$totemScaled = false;
    }
    //?} else {
    /*/^* Before 1.21 the item is drawn on a fresh MatrixStack; vanilla pops it at the end, which also undoes this. ^/
    @Inject(method = "renderItemActivationAnimation", require = 0, at = @At(value = "INVOKE",
        target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER, ordinal = 0))
    private void flintfix$shrinkTotem(int scaledWidth, int scaledHeight, float tickDelta, CallbackInfo ci,
                                      @Local PoseStack matrices) {
        if (FlintFixClient.CONFIG == null || !FlintFixClient.CONFIG.lowOverlaysEnabled || !FlintFixClient.CONFIG.lowTotem) return;
        float scale = FlintFixClient.CONFIG.totemSize;
        float cx = scaledWidth / 2.0f;
        float cy = scaledHeight / 2.0f;
        matrices.translate(cx, cy, 0.0f);
        matrices.scale(scale, scale, 1.0f);
        matrices.translate(-cx, -cy, 0.0f);
    }
    *///?}
}
