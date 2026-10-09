package com.flintfix.client.mixin;

import com.flintfix.client.FlintFixClient;
import net.minecraft.client.OptionInstance;
//? if >=26.1 {
/*import net.minecraft.client.renderer.LightmapRenderStateExtractor;
*///?} else {
import net.minecraft.client.renderer.LightTexture;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

//? if >=26.1 {
/*@Mixin(LightmapRenderStateExtractor.class)
*///?} else {
@Mixin(LightTexture.class)
//?}
public abstract class LightmapTextureManagerMixin {
    /**
     * Fullbright: feed the lightmap a very high gamma. The lightmap clamps the
     * result, so every block renders fully lit. The saved Brightness option
     * is never changed.
     */
    //? if >=26.1 {
    /*@Redirect(method = "extract",
    *///?} else {
    @Redirect(method = "updateLightTexture",
    //?}
        slice = @Slice(from = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Options;gamma()Lnet/minecraft/client/OptionInstance;")),
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;",
            ordinal = 0))
    private Object flintfix$fullbrightGamma(OptionInstance<?> option) {
        if (FlintFixClient.CONFIG != null && FlintFixClient.CONFIG.fullbrightEnabled) return 16.0;
        return option.get();
    }
}
