package com.flintfix.client.mixin;

import net.minecraft.client.renderer.texture.AbstractTexture;
import org.spongepowered.asm.mixin.Mixin;
//? if >=1.21.11 {
/*import com.mojang.blaze3d.textures.GpuSampler;
import org.spongepowered.asm.mixin.gen.Accessor;
*///?}

/** 1.21.11 replaced AbstractTexture.setFilter with a sampler field. Empty before that. */
@Mixin(AbstractTexture.class)
public interface AbstractTextureAccessor {
    //? if >=1.21.11 {
    /*@Accessor("sampler")
    void flintfix$setSampler(GpuSampler sampler);
    *///?}
}
