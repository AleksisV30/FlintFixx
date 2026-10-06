package com.flintfix.client.mixin;

import com.flintfix.client.FlintFixClient;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Low Totem: shrinks the totem-of-undying pop animation around the center of the screen. */
@Mixin(GameRenderer.class)
public abstract class GameRendererTotemMixin {
    @Unique private boolean flintfix$totemScaled;

    @Inject(method = "renderFloatingItem", at = @At("HEAD"), require = 0)
    private void flintfix$shrinkTotem(CallbackInfo ci, @Local(argsOnly = true) DrawContext context) {
        flintfix$totemScaled = false;
        if (FlintFixClient.CONFIG == null || !FlintFixClient.CONFIG.lowOverlaysEnabled || !FlintFixClient.CONFIG.lowTotem) return;
        float scale = FlintFixClient.CONFIG.totemSize;
        float cx = context.getScaledWindowWidth() / 2.0f;
        float cy = context.getScaledWindowHeight() / 2.0f;
        context.getMatrices().push();
        context.getMatrices().translate(cx, cy, 0.0f);
        context.getMatrices().scale(scale, scale, 1.0f);
        context.getMatrices().translate(-cx, -cy, 0.0f);
        flintfix$totemScaled = true;
    }

    @Inject(method = "renderFloatingItem", at = @At("RETURN"), require = 0)
    private void flintfix$restoreTotem(CallbackInfo ci, @Local(argsOnly = true) DrawContext context) {
        if (!flintfix$totemScaled) return;
        context.getMatrices().pop();
        flintfix$totemScaled = false;
    }
}
