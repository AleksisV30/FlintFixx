package com.flintfix.client.mixin;

import com.flintfix.client.FlintFixFreecam;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps movement keys from moving the server-controlled player while detached. */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void flintfix$freezePlayerMovement(boolean slowDown, float slowDownFactor, CallbackInfo ci) {
        if (!FlintFixFreecam.isActive()) return;
        Input input = (Input)(Object)this;
        input.movementForward = 0.0f;
        input.movementSideways = 0.0f;
        input.jumping = false;
        input.sneaking = false;
        input.pressingForward = false;
        input.pressingBack = false;
        input.pressingLeft = false;
        input.pressingRight = false;
    }
}
