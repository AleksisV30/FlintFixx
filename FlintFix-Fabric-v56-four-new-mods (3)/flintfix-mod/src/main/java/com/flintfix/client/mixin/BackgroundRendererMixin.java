package com.flintfix.client.mixin;

import com.flintfix.client.FlintFixSky;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
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

@Mixin(BackgroundRenderer.class)
public abstract class BackgroundRendererMixin {
    //? if >=1.21.2 {
    /*/^* Pulls the horizon fog toward the custom sky so terrain fades into it. The returned color also clears the frame. ^/
    @Inject(method = "getFogColor", at = @At("RETURN"))
    private static void flintfix$tintFogForSky(Camera camera, float tickDelta, ClientWorld world,
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
    @Shadow private static float red;
    @Shadow private static float green;
    @Shadow private static float blue;

    /** Pulls the horizon fog toward the custom sky so terrain fades into it. */
    @Inject(method = "render", at = @At("TAIL"))
    private static void flintfix$tintFogForSky(Camera camera, float tickDelta, ClientWorld world,
                                               int viewDistance, float skyDarkness, CallbackInfo ci) {
        float[] sky = FlintFixSky.fogColor(camera, world, tickDelta);
        if (sky == null) return;
        float amount = sky[3];
        red += (sky[0] - red) * amount;
        green += (sky[1] - green) * amount;
        blue += (sky[2] - blue) * amount;
        RenderSystem.clearColor(red, green, blue, 0.0f);
    }
    //?}
}
