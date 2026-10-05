package com.flintfix.client.mixin;

import com.flintfix.client.FlintFixClient;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(LightmapTextureManager.class)
public abstract class LightmapTextureManagerMixin {
    /**
     * Fullbright: feed the lightmap a very high gamma. The lightmap clamps the
     * result, so every block renders fully lit. The saved Brightness option
     * is never changed.
     */
    @Redirect(method = "update",
        slice = @Slice(from = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/option/GameOptions;getGamma()Lnet/minecraft/client/option/SimpleOption;")),
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;",
            ordinal = 0))
    private Object flintfix$fullbrightGamma(SimpleOption<?> option) {
        if (FlintFixClient.CONFIG != null && FlintFixClient.CONFIG.fullbrightEnabled) return 16.0;
        return option.getValue();
    }
}
