package com.flintfix.client.mixin;

import com.flintfix.client.FlintFixClient;
import com.flintfix.client.FlintFixHand;
import com.flintfix.client.FlintFixInspect;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
//? if >=1.21.9 {
/*import net.minecraft.client.renderer.SubmitNodeCollector;
*///?}
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class HeldItemRendererMixin {
    @Unique private boolean flintfix$pushed;
    @Unique private boolean flintfix$inspecting;
    @Unique private boolean flintfix$drawArm;

    /**
     * Show Hand lifts the view model slightly so the hand around the grip is on
     * screen, and Item Inspect moves the whole hand for its animation.
     */
    @Inject(method = "renderArmWithItem", at = @At("HEAD"))
    private void flintfix$beginHand(AbstractClientPlayer player, float tickDelta, float pitch, InteractionHand hand,
                                    float swingProgress, ItemStack item, float equipProgress,
                                    //? if >=1.21.9 {
                                    /*PoseStack matrices, SubmitNodeCollector vertexConsumers, int light,
                                    *///?} else {
                                    PoseStack matrices, MultiBufferSource vertexConsumers, int light,
                                    //?}
                                    CallbackInfo ci) {
        flintfix$pushed = false;
        flintfix$inspecting = false;
        flintfix$drawArm = false;
        if (item.isEmpty() || item.is(Items.FILLED_MAP)) return;
        boolean inspecting = hand == InteractionHand.MAIN_HAND && FlintFixInspect.isActive();
        boolean showHand = FlintFixClient.CONFIG.showHandEnabled;
        boolean lowShield = item.is(Items.SHIELD) && FlintFixClient.CONFIG.lowOverlaysEnabled
            && FlintFixClient.CONFIG.lowShield;
        if (!inspecting && !showHand && !lowShield) return;

        matrices.pushPose();
        flintfix$pushed = true;
        flintfix$drawArm = inspecting || showHand;
        // Low Shield: lower the shield so it blocks less of the view.
        if (lowShield) matrices.translate(0.0f, -FlintFixClient.CONFIG.lowShieldAmount, 0.0f);
        if (showHand) FlintFixHand.applyViewLift(matrices);
        if (inspecting) {
            flintfix$inspecting = true;
            FlintFixInspect.applyHand(matrices, player.getMainArm() == HumanoidArm.RIGHT ? 1 : -1);
        }
    }

    /**
     * Right before the item is drawn the matrix sits in the item's hand frame:
     * draw the arm there so its fist closes around the item's grip and follows
     * every swing, then apply the inspect flip to the item alone.
     */
    //? if >=1.21.9 {
    /*@Inject(method = "renderArmWithItem", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"))
    *///?} else if >=1.21.5 {
    /*@Inject(method = "renderArmWithItem", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
    *///?} else {
    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
    //?}
    private void flintfix$holdItem(AbstractClientPlayer player, float tickDelta, float pitch, InteractionHand hand,
                                   float swingProgress, ItemStack item, float equipProgress,
                                   //? if >=1.21.9 {
                                   /*PoseStack matrices, SubmitNodeCollector vertexConsumers, int light,
                                   *///?} else {
                                   PoseStack matrices, MultiBufferSource vertexConsumers, int light,
                                   //?}
                                   CallbackInfo ci) {
        if (!flintfix$drawArm || player.isInvisible()) {
            if (flintfix$inspecting) FlintFixInspect.applyItem(matrices, player, item, player.getMainArm() == HumanoidArm.RIGHT);
            return;
        }
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        float visible = flintfix$inspecting
            ? FlintFixInspect.armVisibility(FlintFixClient.CONFIG.showHandEnabled)
            : 1.0f;
        if (visible > 0.01f) {
            FlintFixHand.renderGrippingArm(player, item, arm, visible, matrices, vertexConsumers, light);
        }
        if (flintfix$inspecting) FlintFixInspect.applyItem(matrices, player, item, player.getMainArm() == HumanoidArm.RIGHT);
    }

    @Inject(method = "renderArmWithItem", at = @At("RETURN"))
    private void flintfix$endHand(AbstractClientPlayer player, float tickDelta, float pitch, InteractionHand hand,
                                  float swingProgress, ItemStack item, float equipProgress,
                                  //? if >=1.21.9 {
                                  /*PoseStack matrices, SubmitNodeCollector vertexConsumers, int light,
                                  *///?} else {
                                  PoseStack matrices, MultiBufferSource vertexConsumers, int light,
                                  //?}
                                  CallbackInfo ci) {
        if (flintfix$pushed) {
            matrices.popPose();
            flintfix$pushed = false;
            flintfix$inspecting = false;
            flintfix$drawArm = false;
        }
    }
}
