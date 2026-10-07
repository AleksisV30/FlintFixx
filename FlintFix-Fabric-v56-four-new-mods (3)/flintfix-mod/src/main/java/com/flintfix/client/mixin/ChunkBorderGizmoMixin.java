package com.flintfix.client.mixin;

import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
//? if >=1.21.11 {
/*import com.flintfix.client.FlintFixClient;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
*///?}

/** 1.21.11 draws chunk borders as gizmos; emit FlintFix's next to vanilla's debug gizmos. Empty before that. */
@Mixin(LevelRenderer.class)
public abstract class ChunkBorderGizmoMixin {
    //? if >=1.21.11 {
    /*@Inject(method = "renderLevel", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/debug/DebugRenderer;emitGizmos(Lnet/minecraft/client/renderer/culling/Frustum;DDDF)V",
        shift = At.Shift.AFTER))
    private void flintfix$emitChunkBorders(CallbackInfo ci) {
        FlintFixClient.emitChunkBorders();
    }
    *///?}
}
