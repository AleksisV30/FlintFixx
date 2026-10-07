package com.flintfix.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Compact live-rendered FPS settings screen in the same FlintFix visual style. */
public final class FlintFixFpsSettingsScreen extends FlintFixScreen {
    private final Screen parent;

    private int x, y, w, h;
    private int leftX, leftW, rightX, rightW;
    private int listTop, listBottom;
    private int scroll, maxScroll;
    private boolean draggingOpacity;
    private boolean draggingScale;
    private final long openedAt = System.currentTimeMillis();

    private static final int MAX_W = 266;
    private static final int MAX_H = 162;
    private static final int MIN_W = 248;
    private static final int MIN_H = 148;
    private static final int CONTENT_H = 150;

    public FlintFixFpsSettingsScreen(Screen parent) {
        super(Component.literal("FPS Display Settings"));
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
    public void render(GuiGraphics c, int mouseX, int mouseY, float delta) {
        layout();
        float intro = FlintFixUi.openProgress(openedAt);
        blurBehind(c, delta);
        FlintFixUi.backdrop(c, width, height, intro);
        FlintFixUi.pushPanelIntro(c, x, y, w, h, intro);
        FlintFixUi.panelFrame(c, x, y, w, h);

        FlintFixUi.iconButton(c, "fps-back", x + 10, y + 9, 16, "back",
            FlintFixUi.inside(mouseX, mouseY, x + 10, y + 9, 16, 16));
        FlintFixFont.drawExact(c, FlintFixFont.trim("FPS Display", w - 44, 9, true), x + 32, y + 9, 9,
            FlintFixUi.text(), true);
        FlintFixUi.drawTrimmedExact(c, "Shows your live frame rate in the HUD", x + 32, y + 21, w - 44, 6,
            FlintFixUi.muted(), false);
        FlintFixUi.hairline(c, x + 12, y + 35, w - 24);

        c.enableScissor(leftX - 1, listTop, leftX + leftW + 1, listBottom);
        int sy = listTop - scroll;
        optionRow(c, sy, "Enabled", FlintFixClient.CONFIG.fpsEnabled, mouseX, mouseY);
        optionRow(c, sy + 29, "Text Shadow", FlintFixClient.CONFIG.fpsTextShadow, mouseX, mouseY);
        optionRow(c, sy + 58, "Background", FlintFixClient.CONFIG.fpsBackground, mouseX, mouseY);
        sliderRow(c, sy + 91, "Opacity", FlintFixClient.CONFIG.fpsBackgroundOpacity, 0.0f, 1.0f,
            Math.round(FlintFixClient.CONFIG.fpsBackgroundOpacity * 100) + "%", draggingOpacity);
        sliderRow(c, sy + 121, "HUD Scale", FlintFixClient.CONFIG.fpsScale, 0.25f, 2.0f,
            Math.round(FlintFixClient.CONFIG.fpsScale * 100) + "%", draggingScale);
        c.disableScissor();

        renderPreview(c);
        renderScrollbar(c);

        int footerY = y + h - 24;
        FlintFixUi.compactButton(c, leftX, footerY, 55, 16, "HUD EDIT", FlintFixUi.inside(mouseX, mouseY, leftX, footerY, 55, 16), true);
        FlintFixUi.compactButton(c, leftX + 61, footerY, 42, 16, "RESET", FlintFixUi.inside(mouseX, mouseY, leftX + 61, footerY, 42, 16), false);
        FlintFixUi.finishPanelIntro(c, x, y, w, h, intro);
    }

    private void optionRow(GuiGraphics c, int rowY, String label, boolean enabled, int mouseX, int mouseY) {
        boolean hover = mouseY >= listTop && mouseY <= listBottom
            && FlintFixUi.inside(mouseX, mouseY, leftX, rowY, leftW, 24);
        float t = FlintFixUi.hoverProgress("fps-row:" + label, hover);
        FlintFixUi.surface(c, leftX, rowY, leftW, 24,
            FlintFixUi.blendColors(FlintFixUi.card(), FlintFixUi.raised(), t * 0.6f), FlintFixUi.border());
        FlintFixFont.drawExact(c, label, leftX + 8, FlintFixFont.centeredY(rowY, 24, 7), 7, FlintFixUi.text(), true);
        FlintFixUi.switchToggle(c, "fps:" + label, leftX + leftW - 28, rowY + 7, enabled);
    }

    private void sliderRow(GuiGraphics c, int rowY, String label, float value, float min, float max,
                           String valueText, boolean dragging) {
        FlintFixUi.surface(c, leftX, rowY, leftW, 26, FlintFixUi.card(), FlintFixUi.border());
        FlintFixFont.drawExact(c, label, leftX + 8, rowY + 5, 7, FlintFixUi.text(), true);
        FlintFixFont.drawExact(c, valueText, leftX + leftW - 8 - FlintFixFont.width(valueText, 7, true), rowY + 5, 7,
            FlintFixUi.accentBright(), true);
        FlintFixUi.slider(c, leftX + 8, rowY + 17, leftW - 16, (value - min) / (max - min), dragging);
    }

    private void renderPreview(GuiGraphics c) {
        FlintFixUi.surface(c, rightX, listTop, rightW, 68, FlintFixUi.panel(), FlintFixUi.border());
        FlintFixUi.sectionLabel(c, "PREVIEW", rightX + 7, listTop + 7);
        boolean active = FlintFixClient.CONFIG.fpsEnabled;
        FlintFixUi.badge(c, rightX + rightW - 5, listTop + 5, active ? "ON" : "OFF", active);

        int previewX = rightX + 4;
        int previewY = listTop + 20;
        int previewW = Math.max(1, rightW - 8);
        int previewH = 43;
        c.enableScissor(previewX, previewY, previewX + previewW, previewY + previewH);
        FlintFixHudPreview.render(c, Minecraft.getInstance(), FlintFixHudPreview.Widget.FPS,
            previewX, previewY, previewW, previewH);
        c.disableScissor();
    }

    private void renderScrollbar(GuiGraphics c) {
        FlintFixUi.scrollbar(c, leftX + leftW + 3, listTop, listBottom - listTop, scroll, maxScroll);
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
            onClose();
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
            if (minecraft != null) minecraft.setScreen(new FlintFixHudEditorScreen(this));
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
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        FlintFixClient.CONFIG.save();
        if (minecraft != null) {
            minecraft.setScreen(parent instanceof FlintFixSettingsScreen ? parent : new FlintFixSettingsScreen(null));
        }
    }
}
