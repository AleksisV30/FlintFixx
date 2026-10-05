package com.flintfix.client.mixin;

import com.flintfix.client.FlintFixClient;
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
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
    @Unique private boolean flintfix$inspectPushed;

    @Shadow
    protected abstract void renderArmHoldingItem(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                                 int light, float equipProgress, float swingProgress, Arm arm);

    /**
     * Draws the arm behind held items (Show Hand), and drives the inspect
     * animation for the main hand.
     */
    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"))
    private void flintfix$beginHand(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand,
                                    float swingProgress, ItemStack item, float equipProgress,
                                    MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                    CallbackInfo ci) {
        flintfix$inspectPushed = false;
        if (item.isEmpty()) return;
        Arm arm = hand == Hand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        boolean showHand = FlintFixClient.CONFIG.showHandEnabled;

        if (hand == Hand.MAIN_HAND && FlintFixInspect.isActive()) {
            int side = player.getMainArm() == Arm.RIGHT ? 1 : -1;
            matrices.push();
            flintfix$inspectPushed = true;
            FlintFixInspect.applyHand(matrices, side);
            // With Show Hand off, the arm slides in from below the screen and back out again.
            float visible = FlintFixInspect.armVisibility(showHand);
            if (visible > 0.01f && !player.isInvisible()) {
                float hidden = 1.0f - visible;
                matrices.push();
                matrices.translate(0.35f * side * hidden, -0.75f * hidden, 0.0f);
                renderArmHoldingItem(matrices, vertexConsumers, light, 0.0F, 0.0F, arm);
                matrices.pop();
            }
            return;
        }

        boolean usingThisHand = player.isUsingItem() && player.getActiveHand() == hand;
        if (showHand && !usingThisHand && !item.isOf(Items.FILLED_MAP) && !player.isInvisible()) {
            matrices.push();
            renderArmHoldingItem(matrices, vertexConsumers, light, equipProgress, swingProgress, arm);
            matrices.pop();
        }
    }

    /** Twirls just the item, right before it is drawn. */
    @Inject(method = "renderFirstPersonItem", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"))
    private void flintfix$twirlItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand,
                                    float swingProgress, ItemStack item, float equipProgress,
                                    MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                    CallbackInfo ci) {
        if (!flintfix$inspectPushed) return;
        FlintFixInspect.applyItem(matrices, player.getMainArm() == Arm.RIGHT ? 1 : -1);
    }

    @Inject(method = "renderFirstPersonItem", at = @At("RETURN"))
    private void flintfix$endHand(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand,
                                  float swingProgress, ItemStack item, float equipProgress,
                                  MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                  CallbackInfo ci) {
        if (flintfix$inspectPushed) {
            matrices.pop();
            flintfix$inspectPushed = false;
        }
    }
}
