package com.flintfix.client.mixin;

import com.flintfix.client.FlintFixSky;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
//? if >=1.21.2 {
/*import org.joml.Vector4f;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
*///?} else {
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//?}

@Mixin(FogRenderer.class)
public abstract class BackgroundRendererMixin {
    //? if >=1.21.2 {
    /*/^* Pulls the horizon fog toward the custom sky so terrain fades into it. The returned color also clears the frame. ^/
    @Inject(method = "computeFogColor", at = @At("RETURN"))
    private static void flintfix$tintFogForSky(Camera camera, float tickDelta, ClientLevel world,
                                               int viewDistance, float skyDarkness, CallbackInfoReturnable<Vector4f> cir) {
        float[] sky = FlintFixSky.fogColor(camera, world, tickDelta);
        if (sky == null) return;
        Vector4f color = cir.getReturnValue();
        float amount = sky[3];
        color.x += (sky[0] - color.x) * amount;
        color.y += (sky[1] - color.y) * amount;
        color.z += (sky[2] - color.z) * amount;
    }
    *///?} else {
    @Shadow private static float fogRed;
    @Shadow private static float fogGreen;
    @Shadow private static float fogBlue;

    /** Pulls the horizon fog toward the custom sky so terrain fades into it. */
    @Inject(method = "setupColor", at = @At("TAIL"))
    private static void flintfix$tintFogForSky(Camera camera, float tickDelta, ClientLevel world,
                                               int viewDistance, float skyDarkness, CallbackInfo ci) {
        float[] sky = FlintFixSky.fogColor(camera, world, tickDelta);
        if (sky == null) return;
        float amount = sky[3];
        fogRed += (sky[0] - fogRed) * amount;
        fogGreen += (sky[1] - fogGreen) * amount;
        fogBlue += (sky[2] - fogBlue) * amount;
        RenderSystem.clearColor(fogRed, fogGreen, fogBlue, 0.0f);
    }
    //?}
}
