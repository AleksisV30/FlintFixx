package com.flintfix.client.mixin;

import com.flintfix.client.FlintFixFreecam;
import net.minecraft.client.player.KeyboardInput;
//? if >=1.21.2 {
/*import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;
*///?} else {
import net.minecraft.client.player.Input;
//?}
//? if >=1.21.5 {
/*import net.minecraft.world.phys.Vec2;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps movement keys from moving the server-controlled player while detached. */
@Mixin(KeyboardInput.class)
//? if >=1.21.5 {
/*public abstract class KeyboardInputMixin extends ClientInput {
*///?} else {
public abstract class KeyboardInputMixin {
//?}
    @Inject(method = "tick", at = @At("TAIL"))
    //? if >=1.21.2 {
    /*private void flintfix$freezePlayerMovement(CallbackInfo ci) {
    *///?} else {
    private void flintfix$freezePlayerMovement(boolean slowDown, float slowDownFactor, CallbackInfo ci) {
    //?}
        if (!FlintFixFreecam.isActive()) return;
        //? if >=1.21.5 {
        /*// 1.21.5 keeps the movement impulses in one protected vector, reachable because this mixin extends ClientInput.
        this.moveVector = Vec2.ZERO;
        this.keyPresses = Input.EMPTY;
        *///?} else if >=1.21.2 {
        /*ClientInput input = (ClientInput) (Object) this;
        input.forwardImpulse = 0.0f;
        input.leftImpulse = 0.0f;
        input.keyPresses = Input.EMPTY;
        *///?} else {
        Input input = (Input) (Object) this;
        input.forwardImpulse = 0.0f;
        input.leftImpulse = 0.0f;
        input.jumping = false;
        input.shiftKeyDown = false;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        //?}
    }
}
