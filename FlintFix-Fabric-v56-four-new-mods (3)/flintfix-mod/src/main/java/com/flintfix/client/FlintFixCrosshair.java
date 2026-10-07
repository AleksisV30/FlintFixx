package com.flintfix.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Custom crosshair: cross, dot, circle, cross with dot, or X, with adjustable
 * size, gap, thickness and color, an optional dark outline, a hit marker and
 * the attack cooldown bar.
 */
public final class FlintFixCrosshair {
    public static final String[] STYLES = {"Cross", "Dot", "Circle", "Cross + dot", "X"};
    private static final long HIT_MARKER_MS = 260L;
    private static long lastHitAt;

    private FlintFixCrosshair() {}

    public static void onHit() {
        lastHitAt = System.currentTimeMillis();
    }

    /** Draws the crosshair. Returns true when vanilla's crosshair should be skipped. */
    public static boolean render(GuiGraphics context) {
        Minecraft client = Minecraft.getInstance();
        FlintFixConfig config = FlintFixClient.CONFIG;
        if (config == null || !config.crosshairEnabled || client.player == null) return false;
        // Leave the debug-screen axes and spectator handling to vanilla.
        if (FlintFixCompat.debugHudVisible(client) || client.player.isSpectator()) return false;
        if (!client.options.getCameraType().isFirstPerson()) return true;

        int cx = context.guiWidth() / 2;
        int cy = context.guiHeight() / 2;
        draw(context, cx, cy, config);

        long sinceHit = System.currentTimeMillis() - lastHitAt;
        if (config.crosshairHitMarker && sinceHit < HIT_MARKER_MS) {
            float alpha = 1.0f - sinceHit / (float) HIT_MARKER_MS;
            int color = FlintFixUi.opacity(0xFFFFFFFF, alpha);
            int inner = 4 + Math.round(config.crosshairGap);
            for (int i = inner; i < inner + 4; i++) {
                context.fill(cx + i, cy + i, cx + i + 1, cy + i + 1, color);
                context.fill(cx - i, cy + i, cx - i + 1, cy + i + 1, color);
                context.fill(cx + i, cy - i, cx + i + 1, cy - i + 1, color);
                context.fill(cx - i, cy - i, cx - i + 1, cy - i + 1, color);
            }
        }

        if (client.options.attackIndicator().get() == AttackIndicatorStatus.CROSSHAIR) {
            float progress = client.player.getAttackStrengthScale(0.0f);
            if (progress < 1.0f) {
                int barW = 16;
                int barY = cy + Math.round(config.crosshairGap + config.crosshairSize) + 5;
                context.fill(cx - barW / 2, barY, cx + barW / 2, barY + 2, 0x80000000);
                context.fill(cx - barW / 2, barY, cx - barW / 2 + Math.round(barW * progress), barY + 2,
                    config.crosshairColor);
            }
        }
        return true;
    }

    /** Draws the configured shape centered on (cx, cy); also used for settings previews. */
    public static void draw(GuiGraphics context, int cx, int cy, FlintFixConfig config) {
        int size = Math.round(config.crosshairSize);
        int gap = Math.round(config.crosshairGap);
        int t = Math.max(1, Math.round(config.crosshairThickness));
        int half = t / 2;
        List<int[]> rects = new ArrayList<>();
        switch (config.crosshairStyle) {
            case 1 -> {
                int d = Math.max(2, t + 1);
                rects.add(new int[] {cx - d / 2, cy - d / 2, cx - d / 2 + d, cy - d / 2 + d});
            }
            case 2 -> {
                float radius = gap + size / 2.0f + 1.0f;
                int steps = Math.max(24, Math.round(radius * 8));
                for (int i = 0; i < steps; i++) {
                    double angle = i * Math.PI * 2.0 / steps;
                    int px = cx + (int) Math.round(Math.cos(angle) * radius);
                    int py = cy + (int) Math.round(Math.sin(angle) * radius);
                    rects.add(new int[] {px - half, py - half, px - half + t, py - half + t});
                }
            }
            case 4 -> {
                for (int i = gap + 1; i <= gap + size; i++) {
                    rects.add(new int[] {cx + i - half, cy + i - half, cx + i - half + t, cy + i - half + t});
                    rects.add(new int[] {cx - i - half, cy + i - half, cx - i - half + t, cy + i - half + t});
                    rects.add(new int[] {cx + i - half, cy - i - half, cx + i - half + t, cy - i - half + t});
                    rects.add(new int[] {cx - i - half, cy - i - half, cx - i - half + t, cy - i - half + t});
                }
            }
            default -> {
                rects.add(new int[] {cx - gap - size, cy - half, cx - gap, cy - half + t});
                rects.add(new int[] {cx + 1 + gap, cy - half, cx + 1 + gap + size, cy - half + t});
                rects.add(new int[] {cx - half, cy - gap - size, cx - half + t, cy - gap});
                rects.add(new int[] {cx - half, cy + 1 + gap, cx - half + t, cy + 1 + gap + size});
                if (config.crosshairStyle == 3) {
                    rects.add(new int[] {cx - half, cy - half, cx - half + t, cy - half + t});
                }
            }
        }
        if (config.crosshairOutline) {
            for (int[] r : rects) context.fill(r[0] - 1, r[1] - 1, r[2] + 1, r[3] + 1, 0x99000000);
        }
        for (int[] r : rects) context.fill(r[0], r[1], r[2], r[3], config.crosshairColor);
    }
}
