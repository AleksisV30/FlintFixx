package com.flintfix.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Draws a lightweight wireframe around nearby living entities and dropped items. */
public final class FlintFixHitboxes {
    private static final double RANGE = 64.0;

    private FlintFixHitboxes() {}

    public static void render(WorldRenderContext context) {
        if (context.world() == null || context.camera() == null || context.matrixStack() == null || context.consumers() == null) return;
        Vec3 camera = context.camera().getPosition();
        AABB search = new AABB(camera, camera).inflate(RANGE);
        PoseStack matrices = context.matrixStack();
        MultiBufferSource consumers = context.consumers();
        matrices.pushPose();
        matrices.translate(-camera.x, -camera.y, -camera.z);
        PoseStack.Pose entry = matrices.last();
        VertexConsumer lines = consumers.getBuffer(RenderType.lines());
        Entity localPlayer = Minecraft.getInstance().player;
        for (Entity entity : context.world().getEntities(null, search)) {
            if (entity == localPlayer) continue;
            int color = entity instanceof Player ? 0xFF72D6FF
                : entity instanceof ItemEntity ? 0xFFFFD27A
                : entity instanceof Mob ? 0xFF9AE68A : 0xFFB6A0FF;
            drawBox(lines, entry, entity.getBoundingBox().inflate(0.002), color);
            if (entity instanceof Player || entity instanceof Mob) {
                Vec3 eye = entity.getEyePosition();
                Vec3 lookEnd = eye.add(entity.getViewVector(1.0f).scale(0.85));
                line(lines, entry, eye, lookEnd, 0xFFFF3B3B);
            }
        }
        matrices.popPose();
    }

    private static void drawBox(VertexConsumer lines, PoseStack.Pose matrix, AABB box, int color) {
        double x0 = box.minX, y0 = box.minY, z0 = box.minZ;
        double x1 = box.maxX, y1 = box.maxY, z1 = box.maxZ;
        Vec3[] p = {
            new Vec3(x0,y0,z0), new Vec3(x1,y0,z0), new Vec3(x1,y0,z1), new Vec3(x0,y0,z1),
            new Vec3(x0,y1,z0), new Vec3(x1,y1,z0), new Vec3(x1,y1,z1), new Vec3(x0,y1,z1)
        };
        int[][] edges = {{0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4},{0,4},{1,5},{2,6},{3,7}};
        for (int[] edge : edges) line(lines, matrix, p[edge[0]], p[edge[1]], color);
    }

    private static void line(VertexConsumer consumer, PoseStack.Pose matrix, Vec3 start, Vec3 end, int color) {
        Vec3 normal = end.subtract(start).normalize();
        int r = color >> 16 & 255, g = color >> 8 & 255, b = color & 255;
        FlintFixCompat.lineVertex(consumer, matrix, (float) start.x, (float) start.y, (float) start.z,
            r, g, b, 230, (float) normal.x, (float) normal.y, (float) normal.z);
        FlintFixCompat.lineVertex(consumer, matrix, (float) end.x, (float) end.y, (float) end.z,
            r, g, b, 230, (float) normal.x, (float) normal.y, (float) normal.z);
    }
}
