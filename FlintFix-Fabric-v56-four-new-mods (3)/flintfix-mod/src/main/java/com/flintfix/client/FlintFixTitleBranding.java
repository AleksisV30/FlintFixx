package com.flintfix.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/** FlintFix logo block for the title screen: glowing flint, wordmark and tagline. */
public final class FlintFixTitleBranding {
    private static final ItemStack FLINT = new ItemStack(Items.FLINT);

    private FlintFixTitleBranding() {}

    public static void render(DrawContext context, int x, int y, int width, int height) {
        int centerX = x + width / 2;
        float t = (System.currentTimeMillis() % 3_600_000L) / 1000.0f;
        float pulse = 0.5f + 0.5f * (float) Math.sin(t * 1.4f);

        // Flint icon with a breathing glow behind it.
        FlintFixTitleBackground.glow(context, centerX, y + 18, 84 + pulse * 10, 0x8A6BFF, 0.42f + pulse * 0.12f);
        context.getMatrices().push();
        context.getMatrices().translate(centerX - 16, y + 2 + (float) Math.sin(t * 1.1f) * 1.5f, 0);
        context.getMatrices().scale(2.0f, 2.0f, 1.0f);
        context.drawItem(FLINT, 0, 0);
        context.getMatrices().pop();

        String title = "FlintFix";
        int titleSize = 24;
        int titleY = y + 40;
        int titleW = FlintFixFont.width(title, titleSize, true);
        FlintFixTitleBackground.glow(context, centerX, titleY + 11, titleW * 1.8f, 0x7B5CFF, 0.22f);
        FlintFixFont.drawCenteredExact(context, title, centerX + 1, titleY + 2, titleSize, 0x88000000, true);
        FlintFixFont.drawCenteredExact(context, title, centerX, titleY, titleSize, 0xFFF8F6FF, true);

        // Letter-spaced subtitle flanked by two short rules.
        String sub = "CLIENT";
        int spacing = 4;
        int subW = 0;
        for (char ch : sub.toCharArray()) subW += FlintFixFont.width(String.valueOf(ch), 7, true) + spacing;
        subW -= spacing;
        int cx = centerX - subW / 2;
        int subY = Math.min(y + height - 8, titleY + 28);
        for (char ch : sub.toCharArray()) {
            String letter = String.valueOf(ch);
            FlintFixFont.drawExact(context, letter, cx, subY, 7, 0xFFB9A8FF, true);
            cx += FlintFixFont.width(letter, 7, true) + spacing;
        }
        context.fill(centerX - subW / 2 - 18, subY + 3, centerX - subW / 2 - 6, subY + 4, 0x66B9A8FF);
        context.fill(centerX + subW / 2 + 6, subY + 3, centerX + subW / 2 + 18, subY + 4, 0x66B9A8FF);
    }
}
