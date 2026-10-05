package com.flintfix.client;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.List;

/** Client-only projectile path preview for the currently held throwable or ranged weapon. */
public final class FlintFixTrajectory {
    private static final int MAX_TICKS = 120;
    private static final double AIR_DRAG = 0.99;

    private FlintFixTrajectory() {}

    public static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        if (player == null || client.world == null || context.matrixStack() == null || context.consumers() == null) return;

        ItemStack held = player.getMainHandStack();
        Shot shot = shotFor(held, player);
        if (shot == null) {
            held = player.getOffHandStack();
            shot = shotFor(held, player);
        }
        if (shot == null) return;

        Vec3d direction = Vec3d.fromPolar(player.getPitch() + shot.pitchOffset, player.getYaw()).normalize();
        Vec3d start = player.getEyePos().add(direction.multiply(0.16));
        if (shot.pitchOffset != 0.0f) start = start.add(0.0, -0.10, 0.0);

        Vec3d velocity = direction.multiply(shot.speed).add(player.getVelocity());
        List<Vec3d> points = simulate(client, player, start, velocity, shot.gravity);
        if (points.size() < 2) return;

        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider consumers = context.consumers();
        Vec3d camera = context.camera().getPos();
        matrices.push();
        matrices.translate(-camera.x, -camera.y, -camera.z);
        MatrixStack.Entry entry = matrices.peek();
        VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());

        for (int i = 1; i < points.size(); i++) {
            float progress = i / (float) points.size();
            int alpha = Math.round(220.0f - 60.0f * progress);
            drawLine(lines, entry, points.get(i - 1), points.get(i), shot.color, alpha);
        }

        Vec3d target = points.get(points.size() - 1);
        int marker = 0xFFFFE5A8;
        double radius = 0.13;
        drawLine(lines, entry, target.add(-radius, 0, 0), target.add(radius, 0, 0), marker, 255);
        drawLine(lines, entry, target.add(0, -radius, 0), target.add(0, radius, 0), marker, 255);
        drawLine(lines, entry, target.add(0, 0, -radius), target.add(0, 0, radius), marker, 255);
        matrices.pop();
    }

    private static List<Vec3d> simulate(MinecraftClient client, PlayerEntity player,
                                        Vec3d start, Vec3d initialVelocity, double gravity) {
        List<Vec3d> points = new ArrayList<>(MAX_TICKS + 1);
        Vec3d position = start;
        Vec3d velocity = initialVelocity;
        points.add(position);

        for (int tick = 0; tick < MAX_TICKS; tick++) {
            Vec3d next = position.add(velocity);
            BlockHitResult hit = client.world.raycast(new RaycastContext(position, next,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
            if (hit.getType() == HitResult.Type.BLOCK) {
                points.add(hit.getPos());
                break;
            }

            points.add(next);
            position = next;
            velocity = velocity.multiply(AIR_DRAG).add(0.0, -gravity, 0.0);
        }
        return points;
    }

    private static Shot shotFor(ItemStack stack, PlayerEntity player) {
        if (stack.isOf(Items.ENDER_PEARL)) {
            return new Shot(1.5, 0.03, 0.0f, 0xFFB77BFF);
        }
        if (stack.isOf(Items.EXPERIENCE_BOTTLE)) {
            return new Shot(0.7, 0.07, -20.0f, 0xFF78E1B8);
        }
        if (stack.isOf(Items.BOW)) {
            int drawTicks = player.isUsingItem() && player.getActiveItem().isOf(Items.BOW)
                ? player.getItemUseTime() : 20;
            float pull = Math.min(1.0f, Math.max(0.0f, drawTicks / 20.0f));
            pull = (pull * pull + pull * 2.0f) / 3.0f;
            if (pull < 0.1f) return null;
            return new Shot(3.0 * pull, 0.05, 0.0f, 0xFFFFC873);
        }
        if (stack.isOf(Items.CROSSBOW) && CrossbowItem.isCharged(stack)) {
            return new Shot(3.15, 0.05, 0.0f, 0xFFFFC873);
        }
        return null;
    }

    private static void drawLine(VertexConsumer consumer, MatrixStack.Entry matrix,
                                 Vec3d start, Vec3d end, int color, int alpha) {
        Vec3d normal = end.subtract(start).normalize();
        int red = (color >>> 16) & 0xFF;
        int green = (color >>> 8) & 0xFF;
        int blue = color & 0xFF;
        consumer.vertex(matrix.getPositionMatrix(), (float) start.x, (float) start.y, (float) start.z)
            .color(red, green, blue, alpha)
            .normal(matrix, (float) normal.x, (float) normal.y, (float) normal.z);
        consumer.vertex(matrix.getPositionMatrix(), (float) end.x, (float) end.y, (float) end.z)
            .color(red, green, blue, alpha)
            .normal(matrix, (float) normal.x, (float) normal.y, (float) normal.z);
    }

    private record Shot(double speed, double gravity, float pitchOffset, int color) {}
}
