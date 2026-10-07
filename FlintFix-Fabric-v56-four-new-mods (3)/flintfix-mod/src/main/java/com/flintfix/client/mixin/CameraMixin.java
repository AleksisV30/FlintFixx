package com.flintfix.client.mixin;

import com.flintfix.client.FlintFixFreecam;
import com.flintfix.client.FlintFixLookAround;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies FlintFix's interpolated freecam transform after vanilla camera setup. */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setPosition(double x, double y, double z);
    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Inject(
        method = "setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V",
        at = @At("TAIL")
    )
    private void flintfix$applyCameraModes(BlockGetter area, Entity focusedEntity,
                                           boolean thirdPerson, boolean inverseView,
                                           float tickDelta, CallbackInfo ci) {
        if (FlintFixFreecam.isActive()) {
            Vec3 position = FlintFixFreecam.renderPosition(tickDelta);
            setPosition(position.x, position.y, position.z);
            setRotation(FlintFixFreecam.cameraYaw(), FlintFixFreecam.cameraPitch());
            return;
        }
        if (focusedEntity == null) return;
        Camera camera = (Camera)(Object)this;

        if (FlintFixLookAround.isActive()) {
            float yaw = FlintFixLookAround.yaw();
            float pitch = FlintFixLookAround.pitch();
            Vec3 look = Vec3.directionFromRotation(pitch, yaw).normalize();
            Vec3 eye = focusedEntity.getEyePosition(tickDelta);
            Vec3 desired = eye.subtract(look.scale(4.0));
            BlockHitResult hit = area.clip(new ClipContext(eye, desired,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, focusedEntity));
            if (hit.getType() == HitResult.Type.BLOCK) desired = hit.getLocation().add(look.scale(0.2));
            setPosition(desired.x, desired.y, desired.z);
            setRotation(yaw, pitch);
        }

    }
}
