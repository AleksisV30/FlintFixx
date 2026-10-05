package com.flintfix.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Client-only free camera. The real player stays at the starting position. */
public final class FlintFixFreecam {
    private static boolean active;
    private static ClientWorld sourceWorld;
    private static Perspective previousPerspective;
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

    public static void requestEnable(MinecraftClient client, net.minecraft.client.gui.screen.Screen parent) {
        if (active || client.player == null || client.world == null) return;
        if (FlintFixClient.CONFIG.freecamRiskAccepted) {
            client.setScreen(null);
            enable(client);
            return;
        }
        client.setScreen(new FlintFixFreecamWarningScreen(parent));
    }

    static void enable(MinecraftClient client) {
        if (active || client.player == null || client.world == null) return;
        Entity player = client.player;
        playerYaw = player.getYaw();
        playerPitch = player.getPitch();
        cameraYaw = playerYaw;
        cameraPitch = playerPitch;
        sourceWorld = client.world;
        previousPerspective = client.options.getPerspective();

        // Start behind the avatar so third-person mode can actually show its body.
        Vec3d forward = Vec3d.fromPolar(cameraPitch, cameraYaw).normalize();
        Vec3d eye = player.getCameraPosVec(1.0f).subtract(forward.multiply(4.0));
        cameraX = previousX = eye.x;
        cameraY = previousY = eye.y;
        cameraZ = previousZ = eye.z;

        active = true;
        client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
    }

    /** Redirect normal mouse look away from the stationary player and into freecam state. */
    public static boolean redirectMouseLook(Entity entity, double cursorDeltaX, double cursorDeltaY) {
        if (!active || entity != MinecraftClient.getInstance().player) return false;
        // These deltas already include Minecraft's sensitivity and smoothing.
        cameraYaw += (float)(cursorDeltaX * 0.15);
        cameraPitch = MathHelper.clamp(cameraPitch + (float)(cursorDeltaY * 0.15), -90.0f, 90.0f);
        return true;
    }

    public static Vec3d renderPosition(float tickDelta) {
        return new Vec3d(
            MathHelper.lerp(tickDelta, previousX, cameraX),
            MathHelper.lerp(tickDelta, previousY, cameraY),
            MathHelper.lerp(tickDelta, previousZ, cameraZ)
        );
    }

    public static float cameraYaw() {
        return cameraYaw;
    }

    public static float cameraPitch() {
        return cameraPitch;
    }

    public static void disable(MinecraftClient client) {
        if (!active) return;
        if (client.player != null) {
            client.player.setYaw(playerYaw);
            client.player.setPitch(playerPitch);
        }
        if (previousPerspective != null) client.options.setPerspective(previousPerspective);
        sourceWorld = null;
        previousPerspective = null;
        active = false;
    }

    public static void tick(MinecraftClient client) {
        if (!active) return;
        if (client.player == null || client.world == null || client.world != sourceWorld) {
            disable(client);
            return;
        }

        if (client.options.getPerspective() != Perspective.THIRD_PERSON_BACK) {
            client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        }

        previousX = cameraX;
        previousY = cameraY;
        previousZ = cameraZ;
        if (client.currentScreen != null) return;

        double forwardInput = (client.options.forwardKey.isPressed() ? 1.0 : 0.0)
            - (client.options.backKey.isPressed() ? 1.0 : 0.0);
        double sideInput = (client.options.rightKey.isPressed() ? 1.0 : 0.0)
            - (client.options.leftKey.isPressed() ? 1.0 : 0.0);
        double verticalInput = (client.options.jumpKey.isPressed() ? 1.0 : 0.0)
            - (client.options.sneakKey.isPressed() ? 1.0 : 0.0);

        Vec3d forward = Vec3d.fromPolar(cameraPitch, cameraYaw).normalize();
        Vec3d right = new Vec3d(-forward.z, 0.0, forward.x).normalize();
        Vec3d movement = forward.multiply(forwardInput).add(right.multiply(sideInput))
            .add(0.0, verticalInput, 0.0);
        if (movement.lengthSquared() > 0.0) {
            double speed = FlintFixClient.CONFIG.freecamSpeed
                * (client.options.sprintKey.isPressed() ? 2.0 : 1.0);
            Vec3d step = movement.normalize().multiply(speed);
            cameraX += step.x;
            cameraY += step.y;
            cameraZ += step.z;
        }
    }
}
