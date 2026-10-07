package com.flintfix.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Press-to-inspect "showcase" for the item in the main hand: the hand lifts
 * it into view and turns the face toward the camera, flips it over in the
 * fingers to show the back, tilts the wrist to show the edge, then flips it
 * back while lowering it.
 */
public final class FlintFixInspect {
    private static final float DURATION_SECONDS = 3.4f;

    /**
     * Hand pose keyframes: time, offset x/y/z, then rotation x/y/z in degrees
     * around the wrist. x offsets and y/z rotations are mirrored for left hands.
     */
    private static final float[][] KEYS = {
        {0.00f, 0.00f, 0.00f, 0.00f, 0f, 0f, 0f},
        {0.15f, -0.20f, 0.15f, 0.06f, -12f, -32f, -18f},
        {0.40f, -0.21f, 0.17f, 0.06f, -8f, -44f, -12f},
        {0.47f, -0.20f, 0.22f, 0.06f, -14f, -36f, -16f},
        {0.58f, -0.20f, 0.15f, 0.06f, -10f, -30f, -18f},
        {0.72f, -0.18f, 0.14f, 0.07f, -38f, -12f, -8f},
        {0.82f, -0.16f, 0.13f, 0.06f, -30f, -18f, -10f},
        {1.00f, 0.00f, 0.00f, 0.00f, 0f, 0f, 0f}
    };

    private static boolean active;
    private static long startedAt;
    private static int slot = -1;
    private static Item item;

    private FlintFixInspect() {}

    public static void start(Minecraft client) {
        if (!FlintFixClient.CONFIG.itemInspectEnabled || client.player == null) return;
        ItemStack stack = client.player.getMainHandItem();
        if (stack.isEmpty()) return;
        // Pressing again mid-animation restarts it smoothly from the beginning.
        active = true;
        startedAt = System.nanoTime();
        slot = client.player.getInventory().selected;
        item = stack.getItem();
    }

    /** Ends the animation early when the player switches items or starts using them. */
    public static void tick(Minecraft client) {
        if (!active) return;
        if (client.player == null || !FlintFixClient.CONFIG.itemInspectEnabled
            || client.player.getInventory().selected != slot
            || client.player.getMainHandItem().getItem() != item
            || client.options.keyAttack.isDown() || client.options.keyUse.isDown()
            || progress() >= 1.0f) {
            active = false;
        }
    }

    public static boolean isActive() {
        return active && progress() < 1.0f;
    }

    private static float progress() {
        return (System.nanoTime() - startedAt) / 1.0e9f / DURATION_SECONDS;
    }

    /**
     * Moves the whole hand (arm and item together) around the wrist.
     * side is 1 for a right-handed player and -1 for a left-handed one.
     */
    public static void applyHand(PoseStack matrices, int side) {
        float[] pose = sample(clamp01(progress()));
        matrices.translate(pose[0] * side, pose[1], pose[2]);
        float pivotX = 0.58f * side;
        float pivotY = -0.58f;
        float pivotZ = -0.78f;
        matrices.translate(pivotX, pivotY, pivotZ);
        matrices.mulPose(Axis.YP.rotationDegrees(pose[4] * side));
        matrices.mulPose(Axis.ZP.rotationDegrees(pose[5] * side));
        matrices.mulPose(Axis.XP.rotationDegrees(pose[3]));
        matrices.translate(-pivotX, -pivotY, -pivotZ);
    }

    /**
     * Flips only the item around its own handle, so it turns over inside the
     * fist: once to show the back, and once more on the way down.
     */
    public static void applyItem(PoseStack matrices, AbstractClientPlayer player, ItemStack stack, boolean right) {
        float t = clamp01(progress());
        float angle = 180.0f * easeInOutCubic((t - 0.44f) / 0.16f)
            + 180.0f * easeInOutCubic((t - 0.84f) / 0.14f);
        if (angle <= 0.0f || angle >= 360.0f) return;
        Vector3f[] axis = FlintFixHand.handleAxis(player, stack, right);
        Vector3f grip = axis[0];
        Vector3f dir = axis[1];
        matrices.translate(grip.x, grip.y, grip.z);
        matrices.mulPose(new Quaternionf().rotationAxis((float) Math.toRadians(angle * (right ? 1 : -1)), dir.x, dir.y, dir.z));
        matrices.translate(-grip.x, -grip.y, -grip.z);
    }

    /**
     * How much of the arm is on screen. With Show Hand on, the arm stays fully
     * visible; otherwise it slides in at the start and back off screen at the end.
     */
    public static float armVisibility(boolean showHand) {
        if (showHand) return 1.0f;
        float t = clamp01(progress());
        return smooth(t / 0.14f) * (1.0f - smooth((t - 0.86f) / 0.14f));
    }

    /** Pose (x, y, z, rotX, rotY, rotZ) at time t on a Catmull-Rom spline through the keys. */
    private static float[] sample(float t) {
        int i = 0;
        while (i < KEYS.length - 2 && t > KEYS[i + 1][0]) i++;
        float[] p0 = KEYS[Math.max(0, i - 1)];
        float[] p1 = KEYS[i];
        float[] p2 = KEYS[i + 1];
        float[] p3 = KEYS[Math.min(KEYS.length - 1, i + 2)];
        float u = clamp01((t - p1[0]) / Math.max(1.0e-4f, p2[0] - p1[0]));
        // Ease the very first and last segments so the hand starts and stops softly.
        if (i == 0 || i == KEYS.length - 2) u = smooth(u);
        float u2 = u * u;
        float u3 = u2 * u;
        float[] out = new float[6];
        for (int k = 0; k < 6; k++) {
            float a = p0[k + 1], b = p1[k + 1], c = p2[k + 1], d = p3[k + 1];
            out[k] = 0.5f * (2.0f * b + (c - a) * u + (2.0f * a - 5.0f * b + 4.0f * c - d) * u2
                + (3.0f * b - a - 3.0f * c + d) * u3);
        }
        // Gentle breathing while the item is held up.
        float hold = smooth(t / 0.15f) * (1.0f - smooth((t - 0.82f) / 0.18f));
        out[1] += 0.006f * (float) Math.sin(t * DURATION_SECONDS * 2.6f) * hold;
        return out;
    }

    private static float clamp01(float t) {
        return Math.max(0.0f, Math.min(1.0f, t));
    }

    private static float smooth(float t) {
        t = clamp01(t);
        return t * t * (3.0f - 2.0f * t);
    }

    private static float easeInOutCubic(float t) {
        t = clamp01(t);
        return t < 0.5f ? 4.0f * t * t * t : 1.0f - (float) Math.pow(-2.0f * t + 2.0f, 3.0) / 2.0f;
    }
}
