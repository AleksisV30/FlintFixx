package com.flintfix.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.render.model.json.Transformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.RotationAxis;
import org.joml.Vector3f;

/**
 * Show Hand: draws the player's arm so its fist closes around the held
 * item's grip. The arm is placed in the same matrix frame the item is drawn
 * in, so it follows swings, equip motion, bow drawing and eating exactly.
 */
public final class FlintFixHand {
    /** How far the view model is lifted while Show Hand is on, so the fist sits on screen. */
    private static final float VIEW_LIFT_Y = 0.10f;
    private static final float VIEW_LIFT_Z = 0.02f;

    /**
     * Fist center in the arm model's own space (1/16 block units, divided out):
     * the arm pivot sits at x = -5, y = 2 and the arm hangs 12 pixels down;
     * the fist is about 2.5 pixels above the end.
     */
    private static final float HAND_X = -6.0f / 16.0f;
    private static final float HAND_Y = 9.5f / 16.0f;

    private FlintFixHand() {}

    public static void applyViewLift(MatrixStack matrices) {
        matrices.translate(0.0f, VIEW_LIFT_Y, VIEW_LIFT_Z);
    }

    /**
     * Renders the arm with its fist at the grip of the item that is about to be
     * drawn. The matrix stack must be in the item's hand frame (right before
     * HeldItemRenderer#renderItem). visible slides the arm in from below (0..1).
     */
    public static void renderGrippingArm(AbstractClientPlayerEntity player, ItemStack stack, Arm arm, float visible,
                                         MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        MinecraftClient client = MinecraftClient.getInstance();
        EntityRenderer<?> renderer = client.getEntityRenderDispatcher().getRenderer(player);
        if (!(renderer instanceof PlayerEntityRenderer playerRenderer)) return;

        boolean right = arm == Arm.RIGHT;
        float side = right ? 1.0f : -1.0f;
        Vector3f grip = gripPoint(client, player, stack, right);

        matrices.push();
        float hidden = 1.0f - Math.max(0.0f, Math.min(1.0f, visible));
        if (hidden > 0.0f) matrices.translate(0.35f * side * hidden, -0.75f * hidden, 0.0f);
        matrices.translate(grip.x, grip.y, grip.z);
        // Vanilla's empty-hand arm orientation (HeldItemRenderer#renderArmHoldingItem).
        // Both that pose and the item frame start with the same 45 degree turn,
        // so only the arm's own rotations remain here.
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * 120.0f));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(200.0f));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * -135.0f));
        // Put the fist center on the origin, which is now the grip.
        matrices.translate(-HAND_X * side, -HAND_Y, 0.0f);
        if (right) playerRenderer.renderRightArm(matrices, consumers, light, player);
        else playerRenderer.renderLeftArm(matrices, consumers, light, player);
        matrices.pop();
    }

    /**
     * Grip position in the current item hand frame: runs the item model's own
     * first-person display transform on a point near the bottom of the handle
     * (flat items) or under the middle of the block (3D models).
     */
    private static Vector3f gripPoint(MinecraftClient client, AbstractClientPlayerEntity player, ItemStack stack,
                                      boolean right) {
        return handleAxis(player, stack, right)[0];
    }

    /**
     * The held item's grip point and the unit direction of its handle, both in
     * the item hand frame. Flat items run diagonally from the bottom-left of the
     * texture; 3D models use their vertical center line.
     */
    public static Vector3f[] handleAxis(AbstractClientPlayerEntity player, ItemStack stack, boolean right) {
        MinecraftClient client = MinecraftClient.getInstance();
        BakedModel model = client.getItemRenderer().getModel(stack, player.getWorld(), player, player.getId());
        ModelTransformationMode mode = right
            ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND
            : ModelTransformationMode.FIRST_PERSON_LEFT_HAND;
        Transformation display = model.getTransformation().getTransformation(mode);
        MatrixStack local = new MatrixStack();
        display.apply(!right, local);
        local.translate(-0.5f, -0.5f, -0.5f);
        boolean solid = model.hasDepth();
        Vector3f grip = solid ? new Vector3f(0.5f, 0.12f, 0.5f) : new Vector3f(3.5f / 16.0f, 3.5f / 16.0f, 0.5f);
        Vector3f along = solid ? new Vector3f(0.5f, 1.0f, 0.5f) : new Vector3f(13.0f / 16.0f, 13.0f / 16.0f, 0.5f);
        local.peek().getPositionMatrix().transformPosition(grip);
        local.peek().getPositionMatrix().transformPosition(along);
        Vector3f dir = along.sub(grip, new Vector3f());
        if (dir.lengthSquared() < 1.0e-8f) dir.set(0.0f, 1.0f, 0.0f);
        dir.normalize();
        return new Vector3f[] {grip, dir};
    }
}
