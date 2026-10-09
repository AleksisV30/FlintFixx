package com.flintfix.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Helpers for drawing flat, translucent geometry in the world with RenderLayer.getDebugQuads(). */
final class FlintFixWorldDraw {
    private FlintFixWorldDraw() {}

    /** One quad, wound to face the viewer so it survives back-face culling. */
    static void quad(VertexConsumer consumer, Matrix4f matrix, Vec3 viewer,
                     Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color, float alpha) {
        Vec3 normal = b.subtract(a).cross(c.subtract(a));
        if (normal.dot(viewer.subtract(a)) < 0.0) {
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
    static void line(VertexConsumer consumer, Matrix4f matrix, Vec3 camera, Vec3 a, Vec3 b, double width,
                     int color, float alpha) {
        Vec3 along = b.subtract(a);
        if (along.lengthSqr() < 1.0e-12) return;
        Vec3 dir = along.normalize();
        Vec3 mid = a.add(b).scale(0.5);
        Vec3 across = dir.cross(mid.subtract(camera));
        if (across.lengthSqr() < 1.0e-12) return;
        Vec3 side = across.normalize().scale(width / 2.0);
        Vec3 start = a.subtract(dir.scale(width / 2.0));
        Vec3 end = b.add(dir.scale(width / 2.0));
        quad(consumer, matrix, camera, start.subtract(side), start.add(side), end.add(side), end.subtract(side), color, alpha);
    }

    static void vertex(VertexConsumer consumer, Matrix4f matrix, Vec3 p, int color, float alpha) {
        int a = Math.round(Math.max(0.0f, Math.min(1.0f, alpha)) * ((color >>> 24) & 0xFF));
        FlintFixCompat.colorVertex(consumer, matrix, (float) p.x, (float) p.y, (float) p.z,
            (color >>> 16) & 0xFF, (color >>> 8) & 0xFF, color & 0xFF, a);
    }

    /** Fully saturated hue that cycles slowly over time, for rainbow effects. */
    static int rainbow(double phase) {
        float hue = (float) ((System.currentTimeMillis() % 6000L) / 6000.0 + phase) % 1.0f;
        int rgb = java.awt.Color.HSBtoRGB(hue, 0.75f, 1.0f);
        return 0xFF000000 | (rgb & 0x00FFFFFF);
    }
}
