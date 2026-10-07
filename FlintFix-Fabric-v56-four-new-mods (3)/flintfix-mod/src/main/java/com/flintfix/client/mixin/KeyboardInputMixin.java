package com.flintfix.client.mixin;

import com.flintfix.client.FlintFixFreecam;
import net.minecraft.client.player.KeyboardInput;
//? if >=1.21.2 {
/*import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;
*///?} else {
import net.minecraft.client.player.Input;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps movement keys from moving the server-controlled player while detached. */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    //? if >=1.21.2 {
    /*private void flintfix$freezePlayerMovement(CallbackInfo ci) {
    *///?} else {
    private void flintfix$freezePlayerMovement(boolean slowDown, float slowDownFactor, CallbackInfo ci) {
    //?}
        if (!FlintFixFreecam.isActive()) return;
        //? if >=1.21.2 {
        /*ClientInput input = (ClientInput) (Object) this;
        *///?} else {
        Input input = (Input) (Object) this;
        //?}
        input.forwardImpulse = 0.0f;
        input.leftImpulse = 0.0f;
        //? if >=1.21.2 {
        /*input.keyPresses = Input.EMPTY;
        *///?} else {
        input.jumping = false;
        input.shiftKeyDown = false;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        //?}
    }
}
