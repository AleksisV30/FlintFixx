package com.flintfix.client;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** Draws a lightweight wireframe around nearby living entities and dropped items. */
public final class FlintFixHitboxes {
    private static final double RANGE = 64.0;

    private FlintFixHitboxes() {}

    public static void render(WorldRenderContext context) {
        if (context.world() == null || context.camera() == null || context.matrixStack() == null || context.consumers() == null) return;
        Vec3d camera = context.camera().getPos();
        Box search = new Box(camera, camera).expand(RANGE);
        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider consumers = context.consumers();
        matrices.push();
        matrices.translate(-camera.x, -camera.y, -camera.z);
        MatrixStack.Entry entry = matrices.peek();
        VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());
        Entity localPlayer = MinecraftClient.getInstance().player;
        for (Entity entity : context.world().getOtherEntities(null, search)) {
            if (entity == localPlayer) continue;
            int color = entity instanceof PlayerEntity ? 0xFF72D6FF
                : entity instanceof ItemEntity ? 0xFFFFD27A
                : entity instanceof MobEntity ? 0xFF9AE68A : 0xFFB6A0FF;
            drawBox(lines, entry, entity.getBoundingBox().expand(0.002), color);
            if (entity instanceof PlayerEntity || entity instanceof MobEntity) {
                Vec3d eye = entity.getEyePos();
                Vec3d lookEnd = eye.add(entity.getRotationVec(1.0f).multiply(0.85));
                line(lines, entry, eye, lookEnd, 0xFFFF3B3B);
            }
        }
        matrices.pop();
    }

    private static void drawBox(VertexConsumer lines, MatrixStack.Entry matrix, Box box, int color) {
        double x0 = box.minX, y0 = box.minY, z0 = box.minZ;
        double x1 = box.maxX, y1 = box.maxY, z1 = box.maxZ;
        Vec3d[] p = {
            new Vec3d(x0,y0,z0), new Vec3d(x1,y0,z0), new Vec3d(x1,y0,z1), new Vec3d(x0,y0,z1),
            new Vec3d(x0,y1,z0), new Vec3d(x1,y1,z0), new Vec3d(x1,y1,z1), new Vec3d(x0,y1,z1)
        };
        int[][] edges = {{0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4},{0,4},{1,5},{2,6},{3,7}};
        for (int[] edge : edges) line(lines, matrix, p[edge[0]], p[edge[1]], color);
    }

    private static void line(VertexConsumer consumer, MatrixStack.Entry matrix, Vec3d start, Vec3d end, int color) {
        Vec3d normal = end.subtract(start).normalize();
        int r = color >> 16 & 255, g = color >> 8 & 255, b = color & 255;
        consumer.vertex(matrix.getPositionMatrix(), (float)start.x, (float)start.y, (float)start.z)
            .color(r,g,b,230).normal(matrix,(float)normal.x,(float)normal.y,(float)normal.z);
        consumer.vertex(matrix.getPositionMatrix(), (float)end.x, (float)end.y, (float)end.z)
            .color(r,g,b,230).normal(matrix,(float)normal.x,(float)normal.y,(float)normal.z);
    }
}
