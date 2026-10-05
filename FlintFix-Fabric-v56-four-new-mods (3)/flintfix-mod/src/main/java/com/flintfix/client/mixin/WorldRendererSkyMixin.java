package com.flintfix.client.mixin;

import com.flintfix.client.FlintFixSky;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererSkyMixin {
    @Inject(method = "renderSky", at = @At("HEAD"), cancellable = true)
    private void flintfix$renderCustomSky(Matrix4f matrix4f, Matrix4f projectionMatrix, float tickDelta,
                                          Camera camera, boolean thickFog, Runnable fogCallback,
                                          CallbackInfo ci) {
        if (!FlintFixSky.shouldRender(camera, thickFog)) return;
        fogCallback.run();
        FlintFixSky.render(matrix4f, tickDelta);
        ci.cancel();
    }
}
