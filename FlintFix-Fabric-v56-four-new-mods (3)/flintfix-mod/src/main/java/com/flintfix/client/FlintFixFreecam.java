package com.flintfix.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Client-only free camera. The real player stays at the starting position. */
public final class FlintFixFreecam {
    private static boolean active;
    private static ClientLevel sourceWorld;
    private static CameraType previousPerspective;
    private static float playerYaw;
    private static float playerPitch;
    private static float cameraYaw;
    private static float cameraPitch;
    private static double cameraX, cameraY, cameraZ;
    private static double previousX, previousY, previousZ;

    private FlintFixFreecam() {}

    public static boolean isActive() {
        return active;
    }

    public static void requestEnable(Minecraft client, net.minecraft.client.gui.screens.Screen parent) {
        if (active || client.player == null || client.level == null) return;
        if (FlintFixClient.CONFIG.freecamRiskAccepted) {
            client.setScreen(null);
            enable(client);
            return;
        }
        client.setScreen(new FlintFixFreecamWarningScreen(parent));
    }

    static void enable(Minecraft client) {
        if (active || client.player == null || client.level == null) return;
        Entity player = client.player;
        playerYaw = player.getYRot();
        playerPitch = player.getXRot();
        cameraYaw = playerYaw;
        cameraPitch = playerPitch;
        sourceWorld = client.level;
        previousPerspective = client.options.getCameraType();

        // Start behind the avatar so third-person mode can actually show its body.
        Vec3 forward = Vec3.directionFromRotation(cameraPitch, cameraYaw).normalize();
        Vec3 eye = player.getEyePosition(1.0f).subtract(forward.scale(4.0));
        cameraX = previousX = eye.x;
        cameraY = previousY = eye.y;
        cameraZ = previousZ = eye.z;

        active = true;
        client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
    }

    /** Redirect normal mouse look away from the stationary player and into freecam state. */
    public static boolean redirectMouseLook(Entity entity, double cursorDeltaX, double cursorDeltaY) {
        if (!active || entity != Minecraft.getInstance().player) return false;
        // These deltas already include Minecraft's sensitivity and smoothing.
        cameraYaw += (float)(cursorDeltaX * 0.15);
        cameraPitch = Mth.clamp(cameraPitch + (float)(cursorDeltaY * 0.15), -90.0f, 90.0f);
        return true;
    }

    public static Vec3 renderPosition(float tickDelta) {
        return new Vec3(
            Mth.lerp(tickDelta, previousX, cameraX),
            Mth.lerp(tickDelta, previousY, cameraY),
            Mth.lerp(tickDelta, previousZ, cameraZ)
        );
    }

    public static float cameraYaw() {
        return cameraYaw;
    }

    public static float cameraPitch() {
        return cameraPitch;
    }

    public static void disable(Minecraft client) {
        if (!active) return;
        if (client.player != null) {
            client.player.setYRot(playerYaw);
            client.player.setXRot(playerPitch);
        }
        if (previousPerspective != null) client.options.setCameraType(previousPerspective);
        sourceWorld = null;
        previousPerspective = null;
        active = false;
    }

    public static void tick(Minecraft client) {
        if (!active) return;
        if (client.player == null || client.level == null || client.level != sourceWorld) {
            disable(client);
            return;
        }

        if (client.options.getCameraType() != CameraType.THIRD_PERSON_BACK) {
            client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }

        previousX = cameraX;
        previousY = cameraY;
        previousZ = cameraZ;
        if (client.screen != null) return;

        double forwardInput = (client.options.keyUp.isDown() ? 1.0 : 0.0)
            - (client.options.keyDown.isDown() ? 1.0 : 0.0);
        double sideInput = (client.options.keyRight.isDown() ? 1.0 : 0.0)
            - (client.options.keyLeft.isDown() ? 1.0 : 0.0);
        double verticalInput = (client.options.keyJump.isDown() ? 1.0 : 0.0)
            - (client.options.keyShift.isDown() ? 1.0 : 0.0);

        Vec3 forward = Vec3.directionFromRotation(cameraPitch, cameraYaw).normalize();
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x).normalize();
        Vec3 movement = forward.scale(forwardInput).add(right.scale(sideInput))
            .add(0.0, verticalInput, 0.0);
        if (movement.lengthSqr() > 0.0) {
            double speed = FlintFixClient.CONFIG.freecamSpeed
                * (client.options.keySprint.isDown() ? 2.0 : 1.0);
            Vec3 step = movement.normalize().scale(speed);
            cameraX += step.x;
            cameraY += step.y;
            cameraZ += step.z;
        }
    }
}
