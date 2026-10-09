package com.flintfix.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
//? if <26.2 {
import net.minecraft.client.renderer.MultiBufferSource;
//?}
import net.minecraft.client.renderer.block.model.ItemTransform;
//? if >=1.21.9 {
/*import net.minecraft.client.renderer.SubmitNodeCollector;
*///?} else {
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
//?}
//? if >=1.21.4 {
/*import net.minecraft.client.renderer.item.ItemStackRenderState;
*///?} else {
import net.minecraft.client.resources.model.BakedModel;
//?}
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

/**
 * Show Hand: draws the player's arm so its fist closes around the held
 * item's grip. The arm is placed in the same matrix frame the item is drawn
 * in, so it follows swings, equip motion, bow drawing and eating exactly.
 */
public final class FlintFixHand {

    /**
     * Fist center in the arm model's own space (1/16 block units, divided out):
     * the arm pivot sits at x = -5, y = 2 and the arm hangs 12 pixels down;
     * the fist is about 2.5 pixels above the end.
     */
    private static final float HAND_X = -6.0f / 16.0f;
    private static final float HAND_Y = 9.5f / 16.0f;

    private FlintFixHand() {}

    public static void applyViewLift(PoseStack matrices) {
        FlintFixConfig config = FlintFixClient.CONFIG;
        matrices.translate(0.0f, config.showHandHeight, config.showHandDepth);
    }

    /**
     * Renders the arm with its fist at the grip of the item that is about to be
     * drawn. The matrix stack must be in the item's hand frame (right before
     * HeldItemRenderer#renderItem). visible slides the arm in from below (0..1).
     */
    public static void renderGrippingArm(AbstractClientPlayer player, ItemStack stack, HumanoidArm arm, float visible,
                                         //? if >=1.21.9 {
                                         /*PoseStack matrices, SubmitNodeCollector consumers, int light) {
                                         *///?} else {
                                         PoseStack matrices, MultiBufferSource consumers, int light) {
                                         //?}
        //? if >=26.3 {
        /*// Show Hand is not ported to 26.3 yet (ItemInHandRenderer was replaced).
        *///?} else {
        Minecraft client = Minecraft.getInstance();
        //? if >=1.21.9 {
        /*var playerRenderer = client.getEntityRenderDispatcher().getPlayerRenderer(player);
        *///?} else {
        Object renderer = client.getEntityRenderDispatcher().getRenderer(player);
        if (!(renderer instanceof PlayerRenderer playerRenderer)) return;
        //?}

        boolean right = arm == HumanoidArm.RIGHT;
        float side = right ? 1.0f : -1.0f;
        Vector3f grip = gripPoint(client, player, stack, right);

        matrices.pushPose();
        float hidden = 1.0f - Math.max(0.0f, Math.min(1.0f, visible));
        if (hidden > 0.0f) matrices.translate(0.35f * side * hidden, -0.75f * hidden, 0.0f);
        matrices.translate(grip.x, grip.y, grip.z);
        // Vanilla's empty-hand arm orientation (HeldItemRenderer#renderArmHoldingItem).
        // Both that pose and the item frame start with the same 45 degree turn,
        // so only the arm's own rotations remain here.
        matrices.mulPose(Axis.ZP.rotationDegrees(side * 120.0f));
        matrices.mulPose(Axis.XP.rotationDegrees(200.0f));
        matrices.mulPose(Axis.YP.rotationDegrees(side * -135.0f));
        // Turn the arm around its own length so the right face of the hand shows.
        matrices.mulPose(Axis.YP.rotationDegrees(side * FlintFixClient.CONFIG.showHandTurn * 90.0f));
        // Put the fist center on the origin, which is now the grip.
        matrices.translate(-HAND_X * side, -HAND_Y, 0.0f);
        //? if >=1.21.2 {
        /*//? if >=1.21.9 {
        /^var skin = player.getSkin().body().texturePath();
        ^///?} else {
        var skin = player.getSkin().texture();
        //?}
        if (right) playerRenderer.renderRightHand(matrices, consumers, light, skin,
            player.isModelPartShown(net.minecraft.world.entity.player.PlayerModelPart.RIGHT_SLEEVE));
        else playerRenderer.renderLeftHand(matrices, consumers, light, skin,
            player.isModelPartShown(net.minecraft.world.entity.player.PlayerModelPart.LEFT_SLEEVE));
        *///?} else {
        if (right) playerRenderer.renderRightHand(matrices, consumers, light, player);
        else playerRenderer.renderLeftHand(matrices, consumers, light, player);
        //?}
        matrices.popPose();
        //?}
    }

    /**
     * Grip position in the current item hand frame: runs the item model's own
     * first-person display transform on a point near the bottom of the handle
     * (flat items) or under the middle of the block (3D models).
     */
    private static Vector3f gripPoint(Minecraft client, AbstractClientPlayer player, ItemStack stack,
                                      boolean right) {
        return handleAxis(player, stack, right)[0];
    }

    /**
     * The held item's grip point and the unit direction of its handle, both in
     * the item hand frame. Flat items run diagonally from the bottom-left of the
     * texture; 3D models use their vertical center line.
     */
    public static Vector3f[] handleAxis(AbstractClientPlayer player, ItemStack stack, boolean right) {
        Minecraft client = Minecraft.getInstance();
        ItemDisplayContext mode = right
            ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
            : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        //? if >=1.21.5 {
        /*// 1.21.5 no longer exposes the model's display transform; use vanilla's standard
        // first-person transforms for blocks and for flat (generated/handheld) items.
        ItemStackRenderState state = new ItemStackRenderState();
        client.getItemModelResolver().updateForTopItem(state, stack, mode, player.level(), player, player.getId());
        boolean solid = state.usesBlockLight();
        ItemTransform display = solid
            ? new ItemTransform(new Vector3f(0.0f, 45.0f, 0.0f), new Vector3f(), new Vector3f(0.4f))
            : new ItemTransform(new Vector3f(0.0f, -90.0f, 25.0f), new Vector3f(1.13f, 3.2f, 1.13f).mul(0.0625f),
                new Vector3f(0.68f));
        *///?} else if >=1.21.4 {
        /*ItemStackRenderState state = new ItemStackRenderState();
        client.getItemModelResolver().updateForTopItem(state, stack, mode, !right, player.level(), player, player.getId());
        ItemTransform display = state.transform();
        boolean solid = state.isGui3d();
        *///?} else {
        BakedModel model = client.getItemRenderer().getModel(stack, player.level(), player, player.getId());
        ItemTransform display = model.getTransforms().getTransform(mode);
        boolean solid = model.isGui3d();
        //?}
        PoseStack local = new PoseStack();
        //? if >=1.21.5 {
        /*display.apply(!right, local.last());
        *///?} else {
        display.apply(!right, local);
        //?}
        local.translate(-0.5f, -0.5f, -0.5f);
        Vector3f grip = solid ? new Vector3f(0.5f, 0.12f, 0.5f) : new Vector3f(3.5f / 16.0f, 3.5f / 16.0f, 0.5f);
        Vector3f along = solid ? new Vector3f(0.5f, 1.0f, 0.5f) : new Vector3f(13.0f / 16.0f, 13.0f / 16.0f, 0.5f);
        local.last().pose().transformPosition(grip);
        local.last().pose().transformPosition(along);
        Vector3f dir = along.sub(grip, new Vector3f());
        if (dir.lengthSquared() < 1.0e-8f) dir.set(0.0f, 1.0f, 0.0f);
        dir.normalize();
        return new Vector3f[] {grip, dir};
    }
}
