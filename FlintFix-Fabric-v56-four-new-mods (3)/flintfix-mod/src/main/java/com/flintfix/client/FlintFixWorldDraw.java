package com.flintfix.client;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** Helpers for drawing flat, translucent geometry in the world with RenderLayer.getDebugQuads(). */
final class FlintFixWorldDraw {
    private FlintFixWorldDraw() {}

    /** One quad, wound to face the viewer so it survives back-face culling. */
    static void quad(VertexConsumer consumer, Matrix4f matrix, Vec3d viewer,
                     Vec3d a, Vec3d b, Vec3d c, Vec3d d, int color, float alpha) {
        Vec3d normal = b.subtract(a).crossProduct(c.subtract(a));
        if (normal.dotProduct(viewer.subtract(a)) < 0.0) {
            vertex(consumer, matrix, a, color, alpha);
            vertex(consumer, matrix, d, color, alpha);
            vertex(consumer, matrix, c, color, alpha);
            vertex(consumer, matrix, b, color, alpha);
        } else {
            vertex(consumer, matrix, a, color, alpha);
            vertex(consumer, matrix, b, color, alpha);
            vertex(consumer, matrix, c, color, alpha);
            vertex(consumer, matrix, d, color, alpha);
        }
    }

    /** A camera-facing strip from a to b, width wide, extended past both ends to close corners. */
    static void line(VertexConsumer consumer, Matrix4f matrix, Vec3d camera, Vec3d a, Vec3d b, double width,
                     int color, float alpha) {
        Vec3d along = b.subtract(a);
        if (along.lengthSquared() < 1.0e-12) return;
        Vec3d dir = along.normalize();
        Vec3d mid = a.add(b).multiply(0.5);
        Vec3d across = dir.crossProduct(mid.subtract(camera));
        if (across.lengthSquared() < 1.0e-12) return;
        Vec3d side = across.normalize().multiply(width / 2.0);
        Vec3d start = a.subtract(dir.multiply(width / 2.0));
        Vec3d end = b.add(dir.multiply(width / 2.0));
        quad(consumer, matrix, camera, start.subtract(side), start.add(side), end.add(side), end.subtract(side), color, alpha);
    }

    static void vertex(VertexConsumer consumer, Matrix4f matrix, Vec3d p, int color, float alpha) {
        int a = Math.round(Math.max(0.0f, Math.min(1.0f, alpha)) * ((color >>> 24) & 0xFF));
        consumer.vertex(matrix, (float) p.x, (float) p.y, (float) p.z)
            .color((color >>> 16) & 0xFF, (color >>> 8) & 0xFF, color & 0xFF, a);
    }

    /** Fully saturated hue that cycles slowly over time, for rainbow effects. */
    static int rainbow(double phase) {
        float hue = (float) ((System.currentTimeMillis() % 6000L) / 6000.0 + phase) % 1.0f;
        int rgb = java.awt.Color.HSBtoRGB(hue, 0.75f, 1.0f);
        return 0xFF000000 | (rgb & 0x00FFFFFF);
    }
}
