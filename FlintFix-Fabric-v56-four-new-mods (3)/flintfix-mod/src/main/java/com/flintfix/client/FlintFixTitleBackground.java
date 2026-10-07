package com.flintfix.client;

import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Animated night backdrop for the title screen: drifting color glows and twinkling stars. */
public final class FlintFixTitleBackground {
    private static final ResourceLocation GLOW = FlintFixCompat.id("flintfix", "textures/gui/glow.png");
    private static final int STAR_COUNT = 150;
    private static final float[] STARS = buildStars();
    private static boolean glowFiltered;

    private FlintFixTitleBackground() {}

    public static void render(GuiGraphics c, int w, int h) {
        float t = (System.currentTimeMillis() % 3_600_000L) / 1000.0f;
        c.fillGradient(0, 0, w, h, 0xFF070A17, 0xFF0E1130);

        glow(c, w * 0.24f + (float) Math.sin(t * 0.07f) * w * 0.08f,
            h * 0.34f + (float) Math.cos(t * 0.05f) * h * 0.08f, w * 0.80f, 0x7B5CFF, 0.30f);
        glow(c, w * 0.80f + (float) Math.cos(t * 0.06f) * w * 0.07f,
            h * 0.62f + (float) Math.sin(t * 0.08f) * h * 0.07f, w * 0.72f, 0x2EC5CE, 0.20f);
        glow(c, w * 0.55f + (float) Math.sin(t * 0.05f + 2.0f) * w * 0.10f,
            h * 0.08f + (float) Math.cos(t * 0.04f + 1.0f) * h * 0.05f, w * 0.55f, 0x3A6BFF, 0.20f);
        glow(c, w * 0.5f, h * 1.18f, w * 1.5f, 0x5C4DFF, 0.28f);

        for (int i = 0; i < STAR_COUNT; i++) {
            int o = i * 4;
            float speed = 1.5f + STARS[o + 3] * 3.0f;
            float y = (STARS[o + 1] * h - t * speed) % h;
            if (y < 0) y += h;
            int x = Math.round(STARS[o] * w);
            int size = STARS[o + 3] > 0.85f ? 2 : 1;
            float twinkle = 0.35f + 0.65f * (0.5f + 0.5f * (float) Math.sin(t * (0.8f + STARS[o + 3] * 2.5f) + STARS[o + 2]));
            int alpha = Math.round(twinkle * (size == 2 ? 230 : 170));
            int sy = Math.round(y);
            c.fill(x, sy, x + size, sy + size, (alpha << 24) | 0xE6ECFF);
        }

        c.fillGradient(0, 0, w, h / 4, 0x70000000, 0x00000000);
        c.fillGradient(0, h - h / 3, w, h, 0x00000000, 0x90000000);
    }

    /** Soft additive glow centered on (cx, cy). */
    public static void glow(GuiGraphics c, float cx, float cy, float size, int rgb, float alpha) {
        if (!glowFiltered) {
            Minecraft.getInstance().getTextureManager().getTexture(GLOW).setFilter(true, false);
            glowFiltered = true;
        }
        int s = Math.max(1, Math.round(size));
        int a = Math.round(Math.max(0.0f, Math.min(1.0f, alpha)) * 255.0f);
        FlintFixCompat.drawAdditiveTexture(c, GLOW, Math.round(cx - s / 2.0f), Math.round(cy - s / 2.0f), s, s,
            (a << 24) | (rgb & 0x00FFFFFF));
    }

    private static float[] buildStars() {
        Random random = new Random(4021L);
        float[] data = new float[STAR_COUNT * 4];
        for (int i = 0; i < data.length; i += 4) {
            data[i] = random.nextFloat();
            data[i + 1] = random.nextFloat();
            data[i + 2] = random.nextFloat() * 6.283f;
            data[i + 3] = random.nextFloat();
        }
        return data;
    }
}
