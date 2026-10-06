package com.flintfix.client.mixin;

import com.flintfix.client.FlintFixSky;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if >=1.21.2 {
/*import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DimensionEffects;
import net.minecraft.client.render.Fog;
*///?} else if <1.20.5 {
/*import net.minecraft.client.util.math.MatrixStack;
*///?}

@Mixin(WorldRenderer.class)
public abstract class WorldRendererSkyMixin {
    //? if >=1.21.2 {
    /*/^*
     * 1.21.2+ draws the sky in a frame-graph pass; this is that pass's body
     * (Fog, sky type, tick delta, dimension effects). The camera rotation is
     * already on RenderSystem's model-view stack here, so the sky draws with an
     * identity matrix.
     ^/
    @Inject(method = "method_62215", at = @At("HEAD"), cancellable = true)
    private void flintfix$renderCustomSky(Fog fog, DimensionEffects.SkyType skyType, float tickDelta,
                                          DimensionEffects effects, CallbackInfo ci) {
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        if (!FlintFixSky.shouldRender(camera, false)) return;
        RenderSystem.setShaderFog(fog);
        FlintFixSky.render(new Matrix4f(), tickDelta);
        ci.cancel();
    }
    *///?} else if >=1.20.5 {
    @Inject(method = "renderSky(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FLnet/minecraft/client/render/Camera;ZLjava/lang/Runnable;)V",
        at = @At("HEAD"), cancellable = true)
    private void flintfix$renderCustomSky(Matrix4f matrix4f, Matrix4f projectionMatrix, float tickDelta,
                                          Camera camera, boolean thickFog, Runnable fogCallback,
                                          CallbackInfo ci) {
        if (!FlintFixSky.shouldRender(camera, thickFog)) return;
        fogCallback.run();
        FlintFixSky.render(matrix4f, tickDelta);
        ci.cancel();
    }
    //?} else {
    /*@Inject(method = "renderSky(Lnet/minecraft/client/util/math/MatrixStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/render/Camera;ZLjava/lang/Runnable;)V",
        at = @At("HEAD"), cancellable = true)
    private void flintfix$renderCustomSky(MatrixStack matrices, Matrix4f projectionMatrix, float tickDelta,
                                          Camera camera, boolean thickFog, Runnable fogCallback,
                                          CallbackInfo ci) {
        if (!FlintFixSky.shouldRender(camera, thickFog)) return;
        fogCallback.run();
        FlintFixSky.render(matrices.peek().getPositionMatrix(), tickDelta);
        ci.cancel();
    }
    *///?}
}
