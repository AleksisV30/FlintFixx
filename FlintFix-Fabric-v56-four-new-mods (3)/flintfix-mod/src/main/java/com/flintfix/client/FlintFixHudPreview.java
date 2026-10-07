package com.flintfix.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Renders settings previews through the same routines used by the live HUD. */
public final class FlintFixHudPreview {
    public enum Widget { FPS, CPS, COORDINATES, PING, KEYSTROKES, ARMOR, CHUNKS }

    private static final float FIT_SCALE = 0.72f;

    private FlintFixHudPreview() {}

    public static void render(GuiGraphics context, Minecraft client, Widget widget,
                              int x, int y, int width, int height) {
        if (context == null || client == null || width <= 0 || height <= 0) return;
        if (widget == Widget.CHUNKS) {
            renderChunks(context, x, y, width, height);
            return;
        }

        float oldX = getX(widget);
        float oldY = getY(widget);
        setPosition(widget, 0.5f, 0.5f);
        context.pose().pushPose();
        try {
            context.pose().translate(x + width / 2.0f, y + height / 2.0f, 0);
            context.pose().scale(FIT_SCALE, FIT_SCALE, 1.0f);
            context.pose().translate(-client.getWindow().getGuiScaledWidth() / 2.0f,
                -client.getWindow().getGuiScaledHeight() / 2.0f, 0);
            switch (widget) {
                case FPS -> FlintFixClient.renderFpsHud(context, client, false, false, true);
                case CPS -> FlintFixClient.renderCpsHud(context, client, false, false, true);
                case COORDINATES -> FlintFixClient.renderCoordinatesHud(context, client, false, false, true);
                case PING -> FlintFixClient.renderPingHud(context, client, false, false, true);
                case KEYSTROKES -> FlintFixClient.renderKeystrokesHud(context, client, false, false, true);
                case ARMOR -> FlintFixClient.renderArmorHud(context, client, false, false, true);
                case CHUNKS -> { }
            }
        } finally {
            context.pose().popPose();
            setPosition(widget, oldX, oldY);
        }
    }

    private static void renderChunks(GuiGraphics context, int x, int y, int width, int height) {
        int cell = Math.max(5, Math.min(10, Math.min((width - 8) / 5, (height - 12) / 5)));
        int gridW = cell * 5;
        int gridH = cell * 5;
        int left = x + (width - gridW) / 2;
        int top = y + Math.max(2, (height - gridH - 8) / 2);

        for (int line = 0; line <= 5; line++) {
            int px = left + line * cell;
            int pz = top + line * cell;
            int color = line == 2 || line == 3 ? 0xFFEFCE68 : 0xFF4BBFC0;
            context.fill(px, top, px + 1, top + gridH, color);
            context.fill(left, pz, left + gridW, pz + 1, color);
        }

        String label = "16 BLOCK GRID";
        FlintFixUi.drawTrimmed(context, label, x + 3, top + gridH + 3, width - 6, 5,
            FlintFixClient.MUTED, false);
    }

    private static float getX(Widget widget) {
        return switch (widget) {
            case FPS -> FlintFixClient.CONFIG.fpsX;
            case CPS -> FlintFixClient.CONFIG.cpsX;
            case COORDINATES -> FlintFixClient.CONFIG.coordinatesX;
            case PING -> FlintFixClient.CONFIG.pingX;
            case KEYSTROKES -> FlintFixClient.CONFIG.keystrokesX;
            case ARMOR -> FlintFixClient.CONFIG.armorX;
            case CHUNKS -> 0.0f;
        };
    }

    private static float getY(Widget widget) {
        return switch (widget) {
            case FPS -> FlintFixClient.CONFIG.fpsY;
            case CPS -> FlintFixClient.CONFIG.cpsY;
            case COORDINATES -> FlintFixClient.CONFIG.coordinatesY;
            case PING -> FlintFixClient.CONFIG.pingY;
            case KEYSTROKES -> FlintFixClient.CONFIG.keystrokesY;
            case ARMOR -> FlintFixClient.CONFIG.armorY;
            case CHUNKS -> 0.0f;
        };
    }

    private static void setPosition(Widget widget, float x, float y) {
        switch (widget) {
            case FPS -> { FlintFixClient.CONFIG.fpsX = x; FlintFixClient.CONFIG.fpsY = y; }
            case CPS -> { FlintFixClient.CONFIG.cpsX = x; FlintFixClient.CONFIG.cpsY = y; }
            case COORDINATES -> { FlintFixClient.CONFIG.coordinatesX = x; FlintFixClient.CONFIG.coordinatesY = y; }
            case PING -> { FlintFixClient.CONFIG.pingX = x; FlintFixClient.CONFIG.pingY = y; }
            case KEYSTROKES -> { FlintFixClient.CONFIG.keystrokesX = x; FlintFixClient.CONFIG.keystrokesY = y; }
            case ARMOR -> { FlintFixClient.CONFIG.armorX = x; FlintFixClient.CONFIG.armorY = y; }
            case CHUNKS -> { }
        }
    }
}
