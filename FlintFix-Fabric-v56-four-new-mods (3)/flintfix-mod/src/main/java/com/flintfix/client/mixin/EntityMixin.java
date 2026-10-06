package com.flintfix.client.mixin;

import com.flintfix.client.FlintFixFreecam;
import com.flintfix.client.FlintFixLookAround;
import com.flintfix.client.FlintFixTeammates;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Sends the game's mouse-look update to the detached camera while freecam is active. */
@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "changeLookDirection(DD)V", at = @At("HEAD"), cancellable = true)
    private void flintfix$routeFreecamLook(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        if (FlintFixFreecam.redirectMouseLook((Entity)(Object)this, cursorDeltaX, cursorDeltaY)) {
            ci.cancel();
            return;
        }
        if (FlintFixLookAround.redirectMouseLook((Entity)(Object)this, cursorDeltaX, cursorDeltaY)) {
            ci.cancel();
        }
    }

    /** Team Glow color for friends and teammates. */
    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true, require = 0)
    private void flintfix$teammateGlowColor(CallbackInfoReturnable<Integer> cir) {
        if (FlintFixTeammates.shouldGlow((Entity) (Object) this)) cir.setReturnValue(FlintFixTeammates.glowColor());
    }
}
