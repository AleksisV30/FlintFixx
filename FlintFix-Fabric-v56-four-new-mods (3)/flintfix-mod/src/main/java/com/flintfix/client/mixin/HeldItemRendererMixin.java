package com.flintfix.client.mixin;

import com.flintfix.client.FlintFixClient;
import com.flintfix.client.FlintFixHand;
import com.flintfix.client.FlintFixInspect;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
    @Unique private boolean flintfix$pushed;
    @Unique private boolean flintfix$inspecting;
    @Unique private boolean flintfix$drawArm;

    /**
     * Show Hand lifts the view model slightly so the hand around the grip is on
     * screen, and Item Inspect moves the whole hand for its animation.
     */
    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"))
    private void flintfix$beginHand(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand,
                                    float swingProgress, ItemStack item, float equipProgress,
                                    MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                    CallbackInfo ci) {
        flintfix$pushed = false;
        flintfix$inspecting = false;
        flintfix$drawArm = false;
        if (item.isEmpty() || item.isOf(Items.FILLED_MAP)) return;
        boolean inspecting = hand == Hand.MAIN_HAND && FlintFixInspect.isActive();
        boolean showHand = FlintFixClient.CONFIG.showHandEnabled;
        boolean lowShield = item.isOf(Items.SHIELD) && FlintFixClient.CONFIG.lowOverlaysEnabled
            && FlintFixClient.CONFIG.lowShield;
        if (!inspecting && !showHand && !lowShield) return;

        matrices.push();
        flintfix$pushed = true;
        flintfix$drawArm = inspecting || showHand;
        // Low Shield: lower the shield so it blocks less of the view.
        if (lowShield) matrices.translate(0.0f, -FlintFixClient.CONFIG.lowShieldAmount, 0.0f);
        if (showHand) FlintFixHand.applyViewLift(matrices);
        if (inspecting) {
            flintfix$inspecting = true;
            FlintFixInspect.applyHand(matrices, player.getMainArm() == Arm.RIGHT ? 1 : -1);
        }
    }

    /**
     * Right before the item is drawn the matrix sits in the item's hand frame:
     * draw the arm there so its fist closes around the item's grip and follows
     * every swing, then apply the inspect flip to the item alone.
     */
    @Inject(method = "renderFirstPersonItem", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"))
    private void flintfix$holdItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand,
                                   float swingProgress, ItemStack item, float equipProgress,
                                   MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                   CallbackInfo ci) {
        if (!flintfix$drawArm || player.isInvisible()) {
            if (flintfix$inspecting) FlintFixInspect.applyItem(matrices, player, item, player.getMainArm() == Arm.RIGHT);
            return;
        }
        Arm arm = hand == Hand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        float visible = flintfix$inspecting
            ? FlintFixInspect.armVisibility(FlintFixClient.CONFIG.showHandEnabled)
            : 1.0f;
        if (visible > 0.01f) {
            FlintFixHand.renderGrippingArm(player, item, arm, visible, matrices, vertexConsumers, light);
        }
        if (flintfix$inspecting) FlintFixInspect.applyItem(matrices, player, item, player.getMainArm() == Arm.RIGHT);
    }

    @Inject(method = "renderFirstPersonItem", at = @At("RETURN"))
    private void flintfix$endHand(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand,
                                  float swingProgress, ItemStack item, float equipProgress,
                                  MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                  CallbackInfo ci) {
        if (flintfix$pushed) {
            matrices.pop();
            flintfix$pushed = false;
            flintfix$inspecting = false;
            flintfix$drawArm = false;
        }
    }
}
