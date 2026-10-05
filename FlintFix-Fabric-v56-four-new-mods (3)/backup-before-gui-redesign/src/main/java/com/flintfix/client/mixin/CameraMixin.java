package com.flintfix.client.mixin;

import com.flintfix.client.FlintFixFreecam;
import com.flintfix.client.FlintFixLookAround;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies FlintFix's interpolated freecam transform after vanilla camera setup. */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setPos(double x, double y, double z);
    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Inject(
        method = "update(Lnet/minecraft/world/BlockView;Lnet/minecraft/entity/Entity;ZZF)V",
        at = @At("TAIL")
    )
    private void flintfix$applyCameraModes(BlockView area, Entity focusedEntity,
                                           boolean thirdPerson, boolean inverseView,
                                           float tickDelta, CallbackInfo ci) {
        if (FlintFixFreecam.isActive()) {
            Vec3d position = FlintFixFreecam.renderPosition(tickDelta);
            setPos(position.x, position.y, position.z);
            setRotation(FlintFixFreecam.cameraYaw(), FlintFixFreecam.cameraPitch());
            return;
        }
        if (focusedEntity == null) return;
        Camera camera = (Camera)(Object)this;

        if (FlintFixLookAround.isActive()) {
            float yaw = FlintFixLookAround.yaw();
            float pitch = FlintFixLookAround.pitch();
            Vec3d look = Vec3d.fromPolar(pitch, yaw).normalize();
            Vec3d eye = focusedEntity.getCameraPosVec(tickDelta);
            Vec3d desired = eye.subtract(look.multiply(4.0));
            BlockHitResult hit = area.raycast(new RaycastContext(eye, desired,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, focusedEntity));
            if (hit.getType() == HitResult.Type.BLOCK) desired = hit.getPos().add(look.multiply(0.2));
            setPos(desired.x, desired.y, desired.z);
            setRotation(yaw, pitch);
        }

    }
}
