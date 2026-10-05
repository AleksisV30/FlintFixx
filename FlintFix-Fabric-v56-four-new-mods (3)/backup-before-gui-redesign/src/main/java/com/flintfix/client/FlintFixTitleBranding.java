package com.flintfix.client;

import net.minecraft.client.gui.DrawContext;

/** Minimal polished FlintFix title branding for the vanilla title screen. */
public final class FlintFixTitleBranding {
    private FlintFixTitleBranding() {}

    public static void render(DrawContext context, int x, int y, int width, int height) {
        String title = "FlintFix Client";

        int centerX = x + width / 2;
        int titleY = y + Math.max(24, height / 2 - 11);
        int titleSize = 23;

        // Text only: no card, no underline, no background shape.
        // Layered offsets create a clean shadow + subtle purple glow.
        FlintFixFont.drawCentered(context, title, centerX + 2, titleY + 3, titleSize, 0x82000000, true);
        FlintFixFont.drawCentered(context, title, centerX - 1, titleY, titleSize, 0x389C66FF, true);
        FlintFixFont.drawCentered(context, title, centerX + 1, titleY, titleSize, 0x389C66FF, true);
        FlintFixFont.drawCentered(context, title, centerX, titleY - 1, titleSize, 0x2EA96CFF, true);
        FlintFixFont.drawCentered(context, title, centerX, titleY + 1, titleSize, 0x2EA96CFF, true);
        FlintFixFont.drawCentered(context, title, centerX, titleY, titleSize, 0xFFF8F5FF, true);
    }
}
