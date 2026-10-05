package com.flintfix.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;

/**
 * Press-to-inspect animation for the item in the main hand. The hand and
 * item lift toward the center, turn to show both sides, the item twirls in
 * the fingers, and everything settles back.
 */
public final class FlintFixInspect {
    private static final float DURATION_SECONDS = 2.6f;

    private static boolean active;
    private static long startedAt;
    private static int slot = -1;
    private static Item item;

    private FlintFixInspect() {}

    public static void start(MinecraftClient client) {
        if (!FlintFixClient.CONFIG.itemInspectEnabled || client.player == null) return;
        ItemStack stack = client.player.getMainHandStack();
        if (stack.isEmpty()) return;
        active = true;
        startedAt = System.nanoTime();
        slot = client.player.getInventory().selectedSlot;
        item = stack.getItem();
    }

    /** Ends the animation early when the player switches items or starts using them. */
    public static void tick(MinecraftClient client) {
        if (!active) return;
        if (client.player == null || !FlintFixClient.CONFIG.itemInspectEnabled
            || client.player.getInventory().selectedSlot != slot
            || client.player.getMainHandStack().getItem() != item
            || client.options.attackKey.isPressed() || client.options.useKey.isPressed()
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
    public static void applyHand(MatrixStack matrices, int side) {
        float t = clamp01(progress());
        float envelope = envelope(t);
        float arc = (float) Math.sin(Math.PI * t);

        // Lift toward the center of the screen and slightly closer to the camera.
        matrices.translate(-0.16f * side * envelope, 0.10f * envelope, 0.06f * envelope);

        // Rotate around the wrist so the arm and item turn as one.
        float pivotX = 0.58f * side;
        float pivotY = -0.56f;
        float pivotZ = -0.72f;
        matrices.translate(pivotX, pivotY, pivotZ);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * -35.0f * (float) Math.sin(Math.PI * 2.0 * t) * envelope));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * -28.0f * arc * envelope));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-18.0f * arc * arc * envelope));
        matrices.translate(-pivotX, -pivotY, -pivotZ);
    }

    /** Extra twirl of only the item, in the middle of the animation. */
    public static void applyItem(MatrixStack matrices, int side) {
        float t = clamp01(progress());
        float twirl = easeInOutCubic((t - 0.32f) / 0.36f);
        if (twirl <= 0.0f || twirl >= 1.0f) return;
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * 360.0f * twirl));
    }

    /**
     * How much of the arm is on screen. With Show Hand on, the arm stays fully
     * visible; otherwise it slides in at the start and back off screen at the end.
     */
    public static float armVisibility(boolean showHand) {
        if (showHand) return 1.0f;
        float t = clamp01(progress());
        return smooth(t / 0.16f) * (1.0f - smooth((t - 0.84f) / 0.16f));
    }

    private static float envelope(float t) {
        return smooth(t / 0.18f) * (1.0f - smooth((t - 0.82f) / 0.18f));
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
