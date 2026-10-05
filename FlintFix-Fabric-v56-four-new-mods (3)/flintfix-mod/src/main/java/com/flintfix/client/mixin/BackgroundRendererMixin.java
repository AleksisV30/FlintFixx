package com.flintfix.client.mixin;

import com.flintfix.client.FlintFixSky;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BackgroundRenderer.class)
public abstract class BackgroundRendererMixin {
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
}
