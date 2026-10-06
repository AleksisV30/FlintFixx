package com.flintfix.client;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
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
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        if (world == null || context.matrixStack() == null || context.consumers() == null) return true;
        BlockPos pos = blockHit.getBlockPos();
        BlockState state = world.getBlockState(pos);
        if (state.isAir() || !world.getWorldBorder().contains(pos)) return true;
        VoxelShape shape = state.getOutlineShape(world, pos, ShapeContext.of(client.getCameraEntity()));
        if (shape.isEmpty()) return true;

        Vec3d camera = context.camera().getPos();
        MatrixStack matrices = context.matrixStack();
        matrices.push();
        matrices.translate(-camera.x, -camera.y, -camera.z);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer quads = context.consumers().getBuffer(RenderLayer.getDebugQuads());
        int color = config.blockOutlineRainbow ? FlintFixWorldDraw.rainbow(0.0) : config.blockOutlineColor;
        double distance = Math.max(1.0, Vec3d.ofCenter(pos).distanceTo(camera));
        // Constant on-screen thickness: wider lines farther away.
        double width = config.blockOutlineWidth * 0.0011 * distance;

        if (config.blockOutlineFill) {
            double inflate = 0.002;
            shape.forEachBox((x1, y1, z1, x2, y2, z2) -> fillBox(quads, matrix, camera,
                pos.getX() + x1 - inflate, pos.getY() + y1 - inflate, pos.getZ() + z1 - inflate,
                pos.getX() + x2 + inflate, pos.getY() + y2 + inflate, pos.getZ() + z2 + inflate,
                color, config.blockOutlineFillOpacity));
        }
        shape.forEachEdge((x1, y1, z1, x2, y2, z2) -> {
            Vec3d a = pullToward(new Vec3d(pos.getX() + x1, pos.getY() + y1, pos.getZ() + z1), camera);
            Vec3d b = pullToward(new Vec3d(pos.getX() + x2, pos.getY() + y2, pos.getZ() + z2), camera);
            FlintFixWorldDraw.line(quads, matrix, camera, a, b, width, color, 0.95f);
        });
        matrices.pop();
        return false;
    }

    /** Nudges a point slightly toward the camera so the outline never z-fights with the block. */
    private static Vec3d pullToward(Vec3d point, Vec3d camera) {
        Vec3d toCamera = camera.subtract(point);
        double length = toCamera.length();
        return length < 1.0e-6 ? point : point.add(toCamera.multiply(0.004 / length));
    }

    private static void fillBox(VertexConsumer quads, Matrix4f matrix, Vec3d camera,
                                double x1, double y1, double z1, double x2, double y2, double z2,
                                int color, float alpha) {
        Vec3d[] c = {
            new Vec3d(x1, y1, z1), new Vec3d(x2, y1, z1), new Vec3d(x2, y1, z2), new Vec3d(x1, y1, z2),
            new Vec3d(x1, y2, z1), new Vec3d(x2, y2, z1), new Vec3d(x2, y2, z2), new Vec3d(x1, y2, z2)
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
