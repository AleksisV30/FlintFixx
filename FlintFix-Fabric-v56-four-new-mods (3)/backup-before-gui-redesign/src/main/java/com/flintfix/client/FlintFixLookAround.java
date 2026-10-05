package com.flintfix.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

/** Temporarily turns only the rendered third-person camera, leaving player facing unchanged. */
public final class FlintFixLookAround {
    private static boolean active;
    private static float yaw;
    private static float pitch;
    private static Perspective previousPerspective = Perspective.FIRST_PERSON;

    private FlintFixLookAround() {}

    public static boolean isActive() { return active; }
    public static float yaw() { return yaw; }
    public static float pitch() { return pitch; }

    public static void begin(MinecraftClient client) {
        if (active || client.player == null) return;
        active = true;
        yaw = client.player.getYaw();
        pitch = client.player.getPitch();
        previousPerspective = client.options.getPerspective();
        client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
    }

    public static void end(MinecraftClient client) {
        if (!active) return;
        active = false;
        if (client != null && client.options != null) client.options.setPerspective(previousPerspective);
    }

    public static boolean redirectMouseLook(Entity entity, double dx, double dy) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!active || client.player == null || entity != client.player) return false;
        yaw += (float) dx * 0.15f;
        pitch = MathHelper.clamp(pitch + (float) dy * 0.15f, -90.0f, 90.0f);
        return true;
    }
}
