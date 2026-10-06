package com.flintfix.client;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Floating numbers above mobs and players when their health changes: red for
 * damage (bigger hits are larger and deeper red), green for healing. Health is
 * read from what the server already sends to the client.
 */
public final class FlintFixDamageNumbers {
    private static final long LIFETIME_MS = 1100L;
    private static final double RANGE = 40.0;
    private static final int MAX_POPUPS = 48;
    private static final Map<Integer, Float> LAST_HEALTH = new HashMap<>();
    private static final List<Popup> POPUPS = new ArrayList<>();

    private record Popup(Vec3d pos, String text, int color, float size, long born, double driftX, double driftZ) {}

    private FlintFixDamageNumbers() {}

    public static void tick(MinecraftClient client) {
        if (FlintFixClient.CONFIG == null || !FlintFixClient.CONFIG.damageNumbersEnabled
            || client.world == null || client.player == null) {
            LAST_HEALTH.clear();
            POPUPS.clear();
            return;
        }
        Set<Integer> seen = new HashSet<>();
        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof LivingEntity living) || entity == client.player) continue;
            if (entity.squaredDistanceTo(client.player) > RANGE * RANGE) continue;
            seen.add(entity.getId());
            float health = living.getHealth();
            Float previous = LAST_HEALTH.put(entity.getId(), health);
            if (previous == null) continue;
            float change = health - previous;
            if (Math.abs(change) < 0.5f) continue;
            if (change > 0 && !FlintFixClient.CONFIG.damageNumbersHealing) continue;
            spawn(living, change);
        }
        LAST_HEALTH.keySet().retainAll(seen);
    }

    private static void spawn(LivingEntity entity, float change) {
        float amount = Math.abs(change);
        String value = amount == Math.floor(amount)
            ? Integer.toString((int) amount)
            : String.format(Locale.ROOT, "%.1f", amount);
        boolean heal = change > 0;
        int color;
        if (heal) {
            color = 0xFF6FE39A;
        } else if (amount >= 8) {
            color = 0xFFFF3B3B;
        } else if (amount >= 4) {
            color = 0xFFFF6A4D;
        } else {
            color = 0xFFFFB45C;
        }
        float size = 1.0f + Math.min(1.0f, amount / 12.0f) * 0.6f;
        double angle = Math.random() * Math.PI * 2.0;
        Vec3d pos = entity.getPos().add(0.0, entity.getHeight() + 0.25, 0.0);
        POPUPS.add(new Popup(pos, (heal ? "+" : "-") + value, color, size, System.currentTimeMillis(),
            Math.cos(angle) * 0.25, Math.sin(angle) * 0.25));
        while (POPUPS.size() > MAX_POPUPS) POPUPS.remove(0);
    }

    public static void render(WorldRenderContext context) {
        if (POPUPS.isEmpty() || context.matrixStack() == null || context.consumers() == null) return;
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer text = client.textRenderer;
        Vec3d camera = context.camera().getPos();
        MatrixStack matrices = context.matrixStack();
        long now = System.currentTimeMillis();
        Iterator<Popup> iterator = POPUPS.iterator();
        while (iterator.hasNext()) {
            Popup popup = iterator.next();
            float t = (now - popup.born()) / (float) LIFETIME_MS;
            if (t >= 1.0f) {
                iterator.remove();
                continue;
            }
            // Pop up quickly, drift a little sideways, then fade.
            double rise = 0.9 * (1.0 - Math.pow(1.0 - t, 3.0));
            Vec3d at = popup.pos().add(popup.driftX() * t, rise, popup.driftZ() * t);
            float pop = t < 0.12f ? 0.6f + 0.4f * (t / 0.12f) * 1.15f : 1.0f;
            float alpha = t < 0.7f ? 1.0f : 1.0f - (t - 0.7f) / 0.3f;
            int a = Math.round(alpha * 255.0f);
            if (a < 8) continue;
            double distance = at.distanceTo(camera);
            float scale = 0.025f * popup.size() * pop * (float) Math.max(1.0, distance / 8.0);

            matrices.push();
            matrices.translate(at.x - camera.x, at.y - camera.y, at.z - camera.z);
            matrices.multiply(context.camera().getRotation());
            matrices.scale(scale, -scale, scale);
            int color = (a << 24) | (popup.color() & 0x00FFFFFF);
            float x = -text.getWidth(popup.text()) / 2.0f;
            text.draw(popup.text(), x, 0.0f, color, true, matrices.peek().getPositionMatrix(), context.consumers(),
                TextRenderer.TextLayerType.NORMAL, 0, LightmapTextureManager.MAX_LIGHT_COORDINATE);
            matrices.pop();
        }
    }
}
