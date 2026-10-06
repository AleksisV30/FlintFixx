package com.flintfix.client.mixin;

import com.flintfix.client.FlintFixZoom;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies zoom as a reversible field-of-view change, leaving the camera collision-free. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    //? if >=1.21.2 {
    /*@Inject(method = "getFov(Lnet/minecraft/client/render/Camera;FZ)F", at = @At("RETURN"), cancellable = true)
    private void flintfix$applyZoom(net.minecraft.client.render.Camera camera, float tickDelta,
                                     boolean changingFov, CallbackInfoReturnable<Float> cir) {
        if (FlintFixZoom.isActive()) cir.setReturnValue((float) (cir.getReturnValue() / FlintFixZoom.magnification()));
    }
    *///?} else {
    @Inject(method = "getFov(Lnet/minecraft/client/render/Camera;FZ)D", at = @At("RETURN"), cancellable = true)
    private void flintfix$applyZoom(net.minecraft.client.render.Camera camera, float tickDelta,
                                     boolean changingFov, CallbackInfoReturnable<Double> cir) {
        if (FlintFixZoom.isActive()) cir.setReturnValue(cir.getReturnValue() / FlintFixZoom.magnification());
    }
    //?}
}
