package com.flintfix.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/** Temporarily turns only the rendered third-person camera, leaving player facing unchanged. */
public final class FlintFixLookAround {
    private static boolean active;
    private static float yaw;
    private static float pitch;
    private static CameraType previousPerspective = CameraType.FIRST_PERSON;

    private FlintFixLookAround() {}

    public static boolean isActive() { return active; }
    public static float yaw() { return yaw; }
    public static float pitch() { return pitch; }

    public static void begin(Minecraft client) {
        if (active || client.player == null) return;
        active = true;
        yaw = client.player.getYRot();
        pitch = client.player.getXRot();
        previousPerspective = client.options.getCameraType();
        client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
    }

    public static void end(Minecraft client) {
        if (!active) return;
        active = false;
        if (client != null && client.options != null) client.options.setCameraType(previousPerspective);
    }

    public static boolean redirectMouseLook(Entity entity, double dx, double dy) {
        Minecraft client = Minecraft.getInstance();
        if (!active || client.player == null || entity != client.player) return false;
        yaw += (float) dx * 0.15f;
        pitch = Mth.clamp(pitch + (float) dy * 0.15f, -90.0f, 90.0f);
        return true;
    }
}
