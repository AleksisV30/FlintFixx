package com.flintfix.client.mixin;

import com.flintfix.client.FlintFixTeammates;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Team Glow: gives friends and teammates the entity outline (the glowing effect), only on this client. */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Inject(method = "hasOutline", at = @At("RETURN"), cancellable = true, require = 0)
    private void flintfix$teammateOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && FlintFixTeammates.shouldGlow(entity)) cir.setReturnValue(true);
    }
}
