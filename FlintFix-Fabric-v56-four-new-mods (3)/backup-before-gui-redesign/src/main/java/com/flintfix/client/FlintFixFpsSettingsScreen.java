package com.flintfix.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Compact live-rendered FPS settings screen in the same FlintFix visual style. */
public final class FlintFixFpsSettingsScreen extends Screen {
    private final Screen parent;

    private int x, y, w, h;
    private int leftX, leftW, rightX, rightW;
    private int listTop, listBottom;
    private int scroll, maxScroll;
    private boolean draggingOpacity;
    private boolean draggingScale;

    private static final int MAX_W = 266;
    private static final int MAX_H = 162;
    private static final int MIN_W = 248;
    private static final int MIN_H = 148;
    private static final int CONTENT_H = 150;

    public FlintFixFpsSettingsScreen(Screen parent) {
        super(Text.literal("FPS Display Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        layout();
    }

    private void layout() {
        int margin = 16;
        w = Math.min(MAX_W, Math.max(MIN_W, width - margin * 2));
        h = Math.min(MAX_H, Math.max(MIN_H, height - margin * 2));
        x = (width - w) / 2;
        y = (height - h) / 2;

        leftX = x + 12;
        leftW = Math.max(160, w - 106);
        rightX = leftX + leftW + 8;
        rightW = x + w - 12 - rightX;
        listTop = y + 45;
        listBottom = y + h - 30;

        int viewport = Math.max(1, listBottom - listTop);
        maxScroll = Math.max(0, CONTENT_H - viewport);
        scroll = clamp(scroll, 0, maxScroll);
    }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        layout();
        c.fill(0, 0, width, height, 0x14000000);

        FlintFixUi.fadeOutline(c, x, y, w, h, 12, FlintFixUi.ACCENT);
        FlintFixUi.rounded(c, x, y, w, h, 12, 0xFF10151D);

        FlintFixUi.rounded(c, x + 10, y + 9, 16, 16, 5, 0x3D252D39);
        FlintFixFont.drawCentered(c, "<", x + 18, y + 12, 8, 0xFFD0DCEB, true);
        FlintFixFont.draw(c, "FPS Display Settings", x + 32, y + 9, 11, 0xFFF8F5FA, true);
        FlintFixFont.draw(c, "Shows your live frame rate in the HUD.", x + 32, y + 23, 6, 0xFF8E8793, false);
        FlintFixUi.divider(c, x + 12, y + 35, w - 24);

        c.enableScissor(leftX - 1, listTop, leftX + leftW + 1, listBottom);
        int sy = listTop - scroll;
        optionRow(c, sy, "Enabled", FlintFixClient.CONFIG.fpsEnabled);
        optionRow(c, sy + 29, "Text Shadow", FlintFixClient.CONFIG.fpsTextShadow);
        optionRow(c, sy + 58, "Background", FlintFixClient.CONFIG.fpsBackground);
        sliderRow(c, sy + 91, "Opacity", FlintFixClient.CONFIG.fpsBackgroundOpacity, 0.0f, 1.0f,
            Math.round(FlintFixClient.CONFIG.fpsBackgroundOpacity * 100) + "%");
        sliderRow(c, sy + 121, "HUD Scale", FlintFixClient.CONFIG.fpsScale, 0.25f, 2.0f,
            Math.round(FlintFixClient.CONFIG.fpsScale * 100) + "%");
        c.disableScissor();

        renderPreview(c);
        renderScrollbar(c);

        int footerY = y + h - 24;
        FlintFixUi.compactButton(c, leftX, footerY, 55, 16, "HUD EDIT", FlintFixUi.inside(mouseX, mouseY, leftX, footerY, 55, 16), true);
        FlintFixUi.compactButton(c, leftX + 61, footerY, 42, 16, "RESET", FlintFixUi.inside(mouseX, mouseY, leftX + 61, footerY, 42, 16), false);
    }

    private void optionRow(DrawContext c, int rowY, String label, boolean enabled) {
        FlintFixUi.rounded(c, leftX, rowY, leftW, 24, 6, 0xFF1A222D);
        FlintFixFont.draw(c, label, leftX + 8, rowY + 7, 6, 0xFFF2EEF4, true);
        FlintFixUi.compactToggle(c, leftX + leftW - 34, rowY + 5, enabled);
    }

    private void sliderRow(DrawContext c, int rowY, String label, float value, float min, float max, String valueText) {
        FlintFixUi.rounded(c, leftX, rowY, leftW, 26, 6, 0xFF1A222D);
        FlintFixFont.draw(c, label, leftX + 8, rowY + 4, 6, 0xFFF2EEF4, true);
        FlintFixFont.draw(c, valueText, leftX + leftW - 30, rowY + 4, 6, FlintFixUi.ACCENT_BRIGHT, true);

        int sx = leftX + 8;
        int sw = leftW - 16;
        int barY = rowY + 17;
        float t = (value - min) / (max - min);
        int knobX = sx + Math.round(t * sw);
        FlintFixUi.rounded(c, sx, barY, sw, 3, 2, 0xFF302D37);
        FlintFixUi.rounded(c, sx, barY, Math.max(3, knobX - sx), 3, 2, FlintFixUi.ACCENT);
        FlintFixUi.rounded(c, knobX - 3, barY - 3, 6, 9, 4, 0xFFF7F4F9);
    }

    private void renderPreview(DrawContext c) {
        FlintFixUi.rounded(c, rightX, listTop, rightW, 68, 7, 0xFF1A222D);
        FlintFixFont.draw(c, "PREVIEW", rightX + 7, listTop + 7, 6, 0xFFF4F1F6, true);
        FlintFixUi.rounded(c, rightX + rightW - 25, listTop + 6, 18, 10, 4,
            FlintFixClient.CONFIG.fpsEnabled ? 0xFF345541 : 0xFF583B38);
        FlintFixFont.drawCentered(c, FlintFixClient.CONFIG.fpsEnabled ? "ON" : "OFF",
            rightX + rightW - 16, listTop + 8, 4,
            FlintFixClient.CONFIG.fpsEnabled ? 0xFFD4F0D8 : 0xFFF0D0CA, true);

        int previewX = rightX + 4;
        int previewY = listTop + 20;
        int previewW = Math.max(1, rightW - 8);
        int previewH = 43;
        c.enableScissor(previewX, previewY, previewX + previewW, previewY + previewH);
        FlintFixHudPreview.render(c, MinecraftClient.getInstance(), FlintFixHudPreview.Widget.FPS,
            previewX, previewY, previewW, previewH);
        c.disableScissor();
    }

    private void renderScrollbar(DrawContext c) {
        if (maxScroll <= 0) return;
        int trackH = listBottom - listTop;
        int thumbH = Math.max(16, trackH * trackH / (trackH + maxScroll));
        int thumbY = listTop + (trackH - thumbH) * scroll / maxScroll;
        int sx = leftX + leftW - 2;
        FlintFixUi.rounded(c, sx, listTop, 2, trackH, 1, 0x2E312E38);
        FlintFixUi.rounded(c, sx, thumbY, 2, thumbH, 1, FlintFixUi.ACCENT);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        layout();
        if (mouseX >= leftX - 4 && mouseX <= leftX + leftW + 4 && mouseY >= listTop && mouseY <= listBottom) {
            scroll = clamp(scroll - (int) Math.round(verticalAmount * 22.0), 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        layout();
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        if (FlintFixUi.inside(mouseX, mouseY, x + 10, y + 9, 16, 16)) {
            close();
            return true;
        }

        int sy = listTop - scroll;
        if (mouseY >= listTop && mouseY <= listBottom) {
            if (FlintFixUi.inside(mouseX, mouseY, leftX, sy, leftW, 24)) {
                FlintFixClient.CONFIG.fpsEnabled = !FlintFixClient.CONFIG.fpsEnabled;
                FlintFixClient.CONFIG.save();
                return true;
            }
            if (FlintFixUi.inside(mouseX, mouseY, leftX, sy + 29, leftW, 24)) {
                FlintFixClient.CONFIG.fpsTextShadow = !FlintFixClient.CONFIG.fpsTextShadow;
                FlintFixClient.CONFIG.save();
                return true;
            }
            if (FlintFixUi.inside(mouseX, mouseY, leftX, sy + 58, leftW, 24)) {
                FlintFixClient.CONFIG.fpsBackground = !FlintFixClient.CONFIG.fpsBackground;
                FlintFixClient.CONFIG.save();
                return true;
            }
            if (sliderHit(mouseX, mouseY, sy + 91)) {
                draggingOpacity = true;
                setSlider(mouseX, true);
                return true;
            }
            if (sliderHit(mouseX, mouseY, sy + 121)) {
                draggingScale = true;
                setSlider(mouseX, false);
                return true;
            }
        }

        int footerY = y + h - 24;
        if (FlintFixUi.inside(mouseX, mouseY, leftX, footerY, 55, 16)) {
            if (client != null) client.setScreen(new FlintFixHudEditorScreen(this));
            return true;
        }
        if (FlintFixUi.inside(mouseX, mouseY, leftX + 61, footerY, 42, 16)) {
            FlintFixClient.CONFIG.resetFps();
            FlintFixClient.CONFIG.save();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && (draggingOpacity || draggingScale)) {
            setSlider(mouseX, draggingOpacity);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && (draggingOpacity || draggingScale)) {
            draggingOpacity = false;
            draggingScale = false;
            FlintFixClient.CONFIG.save();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean sliderHit(double mouseX, double mouseY, int rowY) {
        return mouseX >= leftX && mouseX <= leftX + leftW && mouseY >= rowY + 10 && mouseY <= rowY + 26;
    }

    private void setSlider(double mouseX, boolean opacity) {
        int sx = leftX + 8;
        int sw = Math.max(1, leftW - 16);
        float t = (float) Math.max(0.0, Math.min(1.0, (mouseX - sx) / (double) sw));
        if (opacity) FlintFixClient.CONFIG.fpsBackgroundOpacity = t;
        else FlintFixClient.CONFIG.fpsScale = 0.25f + t * 1.75f;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        FlintFixClient.CONFIG.save();
        if (client != null) {
            client.setScreen(parent instanceof FlintFixSettingsScreen ? parent : new FlintFixSettingsScreen(null));
        }
    }
}
