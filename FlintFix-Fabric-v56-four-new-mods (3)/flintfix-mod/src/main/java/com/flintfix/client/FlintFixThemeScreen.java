package com.flintfix.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Small palette picker opened from the FlintFix sidebar. */
public final class FlintFixThemeScreen extends FlintFixScreen {
    private final Screen parent;
    private final long openedAt = System.currentTimeMillis();
    private int x, y, w, h, listTop;

    public FlintFixThemeScreen(Screen parent) {
        super(Component.literal("FlintFix Themes"));
        this.parent = parent;
    }

    private void layout() {
        w = Math.min(294, Math.max(1, width - 20));
        h = Math.min(212, Math.max(1, height - 20));
        x = (width - w) / 2;
        y = (height - h) / 2;
        listTop = y + 44;
    }

    @Override protected void init() { layout(); }

    @Override
    public void render(GuiGraphics c, int mouseX, int mouseY, float delta) {
        layout();
        float intro = FlintFixUi.openProgress(openedAt);
        blurBehind(delta);
        FlintFixUi.backdrop(c, width, height, intro);
        FlintFixUi.pushPanelIntro(c, x, y, w, h, intro);
        FlintFixUi.panelFrame(c, x, y, w, h);
        FlintFixUi.header(c, x + 12, y + 10, w - 24, "themes", "Client theme",
            "Choose a palette for FlintFix screens");
        FlintFixUi.hairline(c, x + 12, y + 37, w - 24);

        FlintFixTheme[] themes = FlintFixTheme.values();
        for (int i = 0; i < themes.length; i++) {
            FlintFixTheme theme = themes[i];
            int rowY = listTop + i * 27;
            int rowX = x + 12;
            int rowW = w - 24;
            boolean selected = theme == FlintFixUi.activeTheme();
            boolean hover = FlintFixUi.inside(mouseX, mouseY, rowX, rowY, rowW, 23);
            float hoverT = FlintFixUi.hoverProgress("theme-row:" + theme.id(), hover);
            float selectedT = FlintFixUi.hoverProgress("theme-selected:" + theme.id(), selected);
            int fill = FlintFixUi.blendColors(FlintFixUi.card(), FlintFixUi.raised(), Math.max(hoverT * 0.6f, selectedT));
            int edge = FlintFixUi.blendColors(FlintFixUi.border(), FlintFixUi.accent(), selectedT);
            FlintFixUi.surface(c, rowX, rowY, rowW, 23, fill, edge);
            renderSwatch(c, theme, rowX + 6, rowY + 4);
            FlintFixFont.drawExact(c, theme.label(), rowX + 50, rowY + 4, 7, FlintFixUi.text(), true);
            FlintFixUi.drawTrimmedExact(c, theme.description(), rowX + 50, rowY + 13, rowW - 74, 6,
                FlintFixUi.muted(), false);
            if (selectedT > 0.01f) {
                int checkX = rowX + rowW - 19;
                FlintFixUi.roundedRaw(c, checkX, rowY + 5, 13, 13, 3, FlintFixUi.opacity(FlintFixUi.accent(), selectedT));
                FlintFixIcons.drawExact(c, "check", checkX + 2, rowY + 7, 9,
                    FlintFixUi.opacity(FlintFixUi.onAccent(), selectedT));
            }
        }

        FlintFixUi.compactButton(c, x + w - 58, y + h - 22, 46, 14, "BACK",
            FlintFixUi.inside(mouseX, mouseY, x + w - 58, y + h - 22, 46, 14), false);
        FlintFixUi.finishPanelIntro(c, x, y, w, h, intro);
    }

    /** A tiny window drawn in the theme's own colors. */
    private static void renderSwatch(GuiGraphics c, FlintFixTheme theme, int sx, int sy) {
        int sw = 38;
        int sh = 15;
        FlintFixUi.roundedRaw(c, sx, sy, sw, sh, 2, theme.border());
        FlintFixUi.roundedRaw(c, sx + 1, sy + 1, sw - 2, sh - 2, 2, theme.background());
        c.fill(sx + 1, sy + 1, sx + 11, sy + sh - 1, theme.panel());
        c.fill(sx + 3, sy + 4, sx + 9, sy + 5, theme.muted());
        c.fill(sx + 3, sy + 7, sx + 8, sy + 8, theme.muted());
        FlintFixUi.roundedRaw(c, sx + 14, sy + 3, 10, 9, 1, theme.card());
        FlintFixUi.roundedRaw(c, sx + 26, sy + 3, 10, 9, 1, theme.card());
        c.fill(sx + 16, sy + 5, sx + 20, sy + 7, theme.accent());
        c.fill(sx + 28, sy + 5, sx + 32, sy + 7, theme.raised());
        c.fill(sx + 16, sy + 9, sx + 22, sy + 10, theme.text());
        c.fill(sx + 28, sy + 9, sx + 33, sy + 10, theme.text());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        layout();
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        for (int i = 0; i < FlintFixTheme.values().length; i++) {
            int rowY = listTop + i * 27;
            if (FlintFixUi.inside(mouseX, mouseY, x + 12, rowY, w - 24, 23)) {
                FlintFixClient.CONFIG.theme = FlintFixTheme.values()[i].id();
                FlintFixClient.CONFIG.save();
                FlintFixUi.applyTheme();
                return true;
            }
        }
        if (FlintFixUi.inside(mouseX, mouseY, x + w - 58, y + h - 22, 46, 14)) {
            closeToParent();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            closeToParent();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void closeToParent() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override public boolean isPauseScreen() { return false; }
}
