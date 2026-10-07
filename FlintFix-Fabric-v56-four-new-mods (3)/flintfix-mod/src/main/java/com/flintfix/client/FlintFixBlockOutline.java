package com.flintfix.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;

/**
 * Replaces the thin black block outline with one in any color and thickness,
 * optionally cycling through the rainbow and with a translucent fill.
 */
public final class FlintFixBlockOutline {
    private FlintFixBlockOutline() {}

    /** Fabric BEFORE_BLOCK_OUTLINE callback: returns false after drawing, which skips vanilla's outline. */
    public static boolean render(WorldRenderContext context, HitResult hit) {
        FlintFixConfig config = FlintFixClient.CONFIG;
        if (config == null || !config.blockOutlineEnabled) return true;
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) return true;
        Minecraft client = Minecraft.getInstance();
        ClientLevel world = client.level;
        if (world == null || context.matrixStack() == null || context.consumers() == null) return true;
        BlockPos pos = blockHit.getBlockPos();
        BlockState state = world.getBlockState(pos);
        if (state.isAir() || !world.getWorldBorder().isWithinBounds(pos)) return true;
        VoxelShape shape = state.getShape(world, pos, CollisionContext.of(client.getCameraEntity()));
        if (shape.isEmpty()) return true;

        Vec3 camera = context.camera().getPosition();
        PoseStack matrices = context.matrixStack();
        matrices.pushPose();
        matrices.translate(-camera.x, -camera.y, -camera.z);
        Matrix4f matrix = matrices.last().pose();
        VertexConsumer quads = context.consumers().getBuffer(RenderType.debugQuads());
        int color = config.blockOutlineRainbow ? FlintFixWorldDraw.rainbow(0.0) : config.blockOutlineColor;
        double distance = Math.max(1.0, Vec3.atCenterOf(pos).distanceTo(camera));
        // Constant on-screen thickness: wider lines farther away.
        double width = config.blockOutlineWidth * 0.0011 * distance;

        if (config.blockOutlineFill) {
            double inflate = 0.002;
            shape.forAllBoxes((x1, y1, z1, x2, y2, z2) -> fillBox(quads, matrix, camera,
                pos.getX() + x1 - inflate, pos.getY() + y1 - inflate, pos.getZ() + z1 - inflate,
                pos.getX() + x2 + inflate, pos.getY() + y2 + inflate, pos.getZ() + z2 + inflate,
                color, config.blockOutlineFillOpacity));
        }
        shape.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
            Vec3 a = pullToward(new Vec3(pos.getX() + x1, pos.getY() + y1, pos.getZ() + z1), camera);
            Vec3 b = pullToward(new Vec3(pos.getX() + x2, pos.getY() + y2, pos.getZ() + z2), camera);
            FlintFixWorldDraw.line(quads, matrix, camera, a, b, width, color, 0.95f);
        });
        matrices.popPose();
        return false;
    }

    /** Nudges a point slightly toward the camera so the outline never z-fights with the block. */
    private static Vec3 pullToward(Vec3 point, Vec3 camera) {
        Vec3 toCamera = camera.subtract(point);
        double length = toCamera.length();
        return length < 1.0e-6 ? point : point.add(toCamera.scale(0.004 / length));
    }

    private static void fillBox(VertexConsumer quads, Matrix4f matrix, Vec3 camera,
                                double x1, double y1, double z1, double x2, double y2, double z2,
                                int color, float alpha) {
        Vec3[] c = {
            new Vec3(x1, y1, z1), new Vec3(x2, y1, z1), new Vec3(x2, y1, z2), new Vec3(x1, y1, z2),
            new Vec3(x1, y2, z1), new Vec3(x2, y2, z1), new Vec3(x2, y2, z2), new Vec3(x1, y2, z2)
        };
        int[][] faces = {{0, 1, 2, 3}, {4, 7, 6, 5}, {0, 4, 5, 1}, {2, 6, 7, 3}, {0, 3, 7, 4}, {1, 5, 6, 2}};
        // Only faces turned toward the camera (bottom, top, north, south, west, east), so the fill doesn't double up.
        boolean[] visible = {camera.y < y1, camera.y > y2, camera.z < z1, camera.z > z2, camera.x < x1, camera.x > x2};
        for (int i = 0; i < faces.length; i++) {
            if (!visible[i]) continue;
            int[] f = faces[i];
            FlintFixWorldDraw.quad(quads, matrix, camera, c[f[0]], c[f[1]], c[f[2]], c[f[3]], color, alpha);
        }
    }
}
