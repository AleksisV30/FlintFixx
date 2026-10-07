package com.flintfix.client;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Client-only projectile path preview for the held throwable or ranged weapon:
 * a soft ribbon along the predicted arc with a flowing dash pattern, a pulsing
 * ring on the block face it will land on, and a highlight on any entity in
 * the way.
 */
public final class FlintFixTrajectory {
    private static final int MAX_TICKS = 160;
    private static final double AIR_DRAG = 0.99;
    private static final float RIBBON_WIDTH = 0.028f;

    private FlintFixTrajectory() {}

    public static void render(WorldRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null || client.level == null || context.matrixStack() == null || context.consumers() == null) return;

        ItemStack held = player.getMainHandItem();
        Shot shot = shotFor(held, player);
        if (shot == null) {
            held = player.getOffhandItem();
            shot = shotFor(held, player);
        }
        if (shot == null) return;

        float tickDelta = FlintFixCompat.tickDelta(context);
        Vec3 eye = player.getEyePosition(tickDelta);
        float pitch = player.getViewXRot(tickDelta);
        float yaw = player.getViewYRot(tickDelta);
        Vec3 direction = Vec3.directionFromRotation(pitch + shot.pitchOffset, yaw).normalize();
        Vec3 start = eye.add(0.0, -0.1, 0.0);
        // Thrown projectiles inherit the shooter's motion (vertical only while airborne).
        Vec3 motion = player.getDeltaMovement();
        Vec3 velocity = direction.scale(shot.speed).add(motion.x, player.onGround() ? 0.0 : motion.y, motion.z);

        Path path = simulate(client, player, start, velocity, shot.gravity, shot.size);
        if (path.points.size() < 2) return;

        PoseStack matrices = context.matrixStack();
        MultiBufferSource consumers = context.consumers();
        Vec3 camera = context.camera().getPosition();
        matrices.pushPose();
        matrices.translate(-camera.x, -camera.y, -camera.z);
        Matrix4f matrix = matrices.last().pose();
        float time = (System.currentTimeMillis() % 100_000L) / 1000.0f;

        VertexConsumer quads = consumers.getBuffer(RenderType.debugQuads());
        drawRibbon(quads, matrix, path.points, camera, shot.color, time);
        int markerColor = path.entity != null ? 0xFFFF6B6B : shot.color;
        Vec3 end = path.points.get(path.points.size() - 1);
        if (path.side != null) {
            drawLandingRing(quads, matrix, end, path.side, markerColor, time);
        }

        if (path.entity != null) {
            VertexConsumer lines = consumers.getBuffer(RenderType.lines());
            AABB box = path.entity.getBoundingBox().inflate(0.05);
            FlintFixCompat.drawBoxOutline(matrices, lines, box, 1.0f, 0.42f, 0.42f, 0.9f);
        }
        matrices.popPose();
    }

    private record Path(List<Vec3> points, Direction side, Entity entity) {}

    private static Path simulate(Minecraft client, Player player, Vec3 start, Vec3 initialVelocity,
                                 double gravity, double size) {
        List<Vec3> points = new ArrayList<>(MAX_TICKS + 1);
        Vec3 position = start;
        Vec3 velocity = initialVelocity;
        points.add(position);

        for (int tick = 0; tick < MAX_TICKS; tick++) {
            Vec3 next = position.add(velocity);
            BlockHitResult hit = client.level.clip(new ClipContext(position, next,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            Vec3 segmentEnd = hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : next;

            // Entities in the way, ignoring the shooter for the first few ticks like vanilla.
            AABB sweep = new AABB(position, segmentEnd).inflate(size + 0.3);
            Entity nearest = null;
            Vec3 nearestPos = null;
            double best = Double.MAX_VALUE;
            for (Entity entity : client.level.getEntities(player, sweep,
                candidate -> !candidate.isSpectator() && candidate.isPickable())) {
                Optional<Vec3> contact = entity.getBoundingBox().inflate(size).clip(position, segmentEnd);
                if (contact.isPresent()) {
                    double distance = position.distanceToSqr(contact.get());
                    if (distance < best) {
                        best = distance;
                        nearest = entity;
                        nearestPos = contact.get();
                    }
                }
            }
            if (nearest != null) {
                points.add(nearestPos);
                return new Path(points, null, nearest);
            }
            if (hit.getType() == HitResult.Type.BLOCK) {
                points.add(hit.getLocation());
                return new Path(points, hit.getDirection(), null);
            }

            points.add(next);
            position = next;
            velocity = velocity.scale(AIR_DRAG).add(0.0, -gravity, 0.0);
            if (position.y < FlintFixCompat.minBuildY(client.level) - 16) break;
        }
        return new Path(points, null, null);
    }

    /**
     * Camera-facing ribbon through the points. It fades in near the player so it
     * never covers the crosshair, carries slowly flowing dashes, and fades out
     * toward the far end.
     */
    private static void drawRibbon(VertexConsumer consumer, Matrix4f matrix, List<Vec3> points, Vec3 camera,
                                   int color, float time) {
        int count = points.size();
        double[] distance = new double[count];
        for (int i = 1; i < count; i++) distance[i] = distance[i - 1] + points.get(i).distanceTo(points.get(i - 1));
        double total = Math.max(0.001, distance[count - 1]);

        Vec3[] side = new Vec3[count];
        for (int i = 0; i < count; i++) {
            Vec3 prev = points.get(Math.max(0, i - 1));
            Vec3 next = points.get(Math.min(count - 1, i + 1));
            Vec3 tangent = next.subtract(prev);
            Vec3 view = points.get(i).subtract(camera);
            Vec3 across = tangent.cross(view);
            if (across.lengthSqr() < 1.0e-10) across = new Vec3(0, 1, 0);
            // Keep a constant on-screen thickness by widening with distance.
            double width = RIBBON_WIDTH * Math.max(1.0, Math.min(6.0, view.length() / 6.0));
            side[i] = across.normalize().scale(width);
        }

        for (int i = 1; i < count; i++) {
            float a0 = ribbonAlpha(distance[i - 1], total, time);
            float a1 = ribbonAlpha(distance[i], total, time);
            if (a0 <= 0.01f && a1 <= 0.01f) continue;
            Vec3 p0 = points.get(i - 1);
            Vec3 p1 = points.get(i);
            quad(consumer, matrix, camera,
                p0.subtract(side[i - 1]), p0.add(side[i - 1]), p1.add(side[i]), p1.subtract(side[i]),
                color, a0, a0, a1, a1);
            // Brighter, thinner core for a glowing look.
            Vec3 c0 = side[i - 1].scale(0.35);
            Vec3 c1 = side[i].scale(0.35);
            int core = FlintFixUi.blendColors(color, 0xFFFFFFFF, 0.55f);
            quad(consumer, matrix, camera, p0.subtract(c0), p0.add(c0), p1.add(c1), p1.subtract(c1),
                core, a0, a0, a1, a1);
        }
    }

    private static float ribbonAlpha(double at, double total, float time) {
        float fadeIn = smooth((float) ((at - 0.6) / 2.2));
        float fadeOut = 1.0f - 0.45f * (float) (at / total);
        // Dashes about 0.7 blocks long drift outward along the path.
        float dash = 0.62f + 0.38f * (float) Math.cos((at - time * 2.4) * Math.PI * 2.0 / 0.7);
        return 0.78f * fadeIn * fadeOut * dash;
    }

    /** Pulsing ring and soft fill lying flat on the face that will be hit. */
    private static void drawLandingRing(VertexConsumer consumer, Matrix4f matrix, Vec3 center, Direction face,
                                        int color, float time) {
        Vec3 normal = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
        Vec3 u = Math.abs(normal.y) > 0.5 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 v = normal.cross(u).normalize();
        u = v.cross(normal).normalize();
        Vec3 origin = center.add(normal.scale(0.012));
        float pulse = 0.5f + 0.5f * (float) Math.sin(time * 4.0f);
        double outer = 0.30 + 0.04 * pulse;
        double inner = outer - 0.06;
        int segments = 32;
        Vec3 eye = origin.add(normal);
        for (int s = 0; s < segments; s++) {
            double t0 = s * Math.PI * 2.0 / segments;
            double t1 = (s + 1) * Math.PI * 2.0 / segments;
            Vec3 d0 = u.scale(Math.cos(t0)).add(v.scale(Math.sin(t0)));
            Vec3 d1 = u.scale(Math.cos(t1)).add(v.scale(Math.sin(t1)));
            quad(consumer, matrix, eye,
                origin.add(d0.scale(inner)), origin.add(d0.scale(outer)),
                origin.add(d1.scale(outer)), origin.add(d1.scale(inner)),
                color, 0.9f, 0.9f, 0.9f, 0.9f);
            quad(consumer, matrix, eye,
                origin, origin.add(d0.scale(inner)), origin.add(d1.scale(inner)), origin,
                color, 0.28f + 0.1f * pulse, 0.08f, 0.08f, 0.28f + 0.1f * pulse);
        }
        // Small center dot.
        Vec3 du = u.scale(0.035);
        Vec3 dv = v.scale(0.035);
        quad(consumer, matrix, eye, origin.subtract(du).subtract(dv), origin.add(du).subtract(dv),
            origin.add(du).add(dv), origin.subtract(du).add(dv), 0xFFFFFFFF, 0.95f, 0.95f, 0.95f, 0.95f);
    }

    /** One quad, wound to face the viewer so it survives back-face culling. */
    private static void quad(VertexConsumer consumer, Matrix4f matrix, Vec3 viewer,
                             Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color,
                             float alphaA, float alphaB, float alphaC, float alphaD) {
        Vec3 normal = b.subtract(a).cross(c.subtract(a));
        boolean flip = normal.dot(viewer.subtract(a)) < 0.0;
        if (flip) {
            vertex(consumer, matrix, a, color, alphaA);
            vertex(consumer, matrix, d, color, alphaD);
            vertex(consumer, matrix, c, color, alphaC);
            vertex(consumer, matrix, b, color, alphaB);
        } else {
            vertex(consumer, matrix, a, color, alphaA);
            vertex(consumer, matrix, b, color, alphaB);
            vertex(consumer, matrix, c, color, alphaC);
            vertex(consumer, matrix, d, color, alphaD);
        }
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Vec3 p, int color, float alpha) {
        int a = Math.round(Math.max(0.0f, Math.min(1.0f, alpha)) * 255.0f);
        FlintFixCompat.colorVertex(consumer, matrix, (float) p.x, (float) p.y, (float) p.z,
            (color >>> 16) & 0xFF, (color >>> 8) & 0xFF, color & 0xFF, a);
    }

    private static Shot shotFor(ItemStack stack, Player player) {
        if (stack.is(Items.ENDER_PEARL)) return new Shot(1.5, 0.03, 0.0f, 0.25, 0xFFB77BFF);
        if (stack.is(Items.SNOWBALL) || stack.is(Items.EGG)) return new Shot(1.5, 0.03, 0.0f, 0.25, 0xFFE6F2FF);
        if (stack.is(Items.EXPERIENCE_BOTTLE)) return new Shot(0.7, 0.07, -20.0f, 0.25, 0xFF78E1B8);
        if (stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION)) {
            return new Shot(0.5, 0.05, -20.0f, 0.25, 0xFFFF8FD8);
        }
        if (stack.is(Items.BOW)) {
            int drawTicks = player.isUsingItem() && player.getUseItem().is(Items.BOW)
                ? player.getTicksUsingItem() : 20;
            float pull = Math.min(1.0f, Math.max(0.0f, drawTicks / 20.0f));
            pull = (pull * pull + pull * 2.0f) / 3.0f;
            if (pull < 0.1f) return null;
            return new Shot(3.0 * pull, 0.05, 0.0f, 0.5, 0xFFFFC873);
        }
        if (stack.is(Items.CROSSBOW) && CrossbowItem.isCharged(stack)) {
            return new Shot(3.15, 0.05, 0.0f, 0.5, 0xFFFFC873);
        }
        if (stack.is(Items.TRIDENT) && player.isUsingItem() && player.getUseItem().is(Items.TRIDENT)
            && player.getTicksUsingItem() >= 10) {
            return new Shot(2.5, 0.05, 0.0f, 0.5, 0xFF7FE3FF);
        }
        return null;
    }

    private static float smooth(float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));
        return t * t * (3.0f - 2.0f * t);
    }

    private record Shot(double speed, double gravity, float pitchOffset, double size, int color) {}
}
