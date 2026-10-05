package com.flintfix.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Small palette picker opened from the FlintFix sidebar. */
public final class FlintFixThemeScreen extends Screen {
    private final Screen parent;
    private final long openedAt = System.currentTimeMillis();
    private int x, y, w, h, listTop;

    public FlintFixThemeScreen(Screen parent) {
        super(Text.literal("FlintFix Themes"));
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
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        layout();
        float intro = FlintFixUi.openProgress(openedAt);
        c.fill(0, 0, width, height, FlintFixUi.opacity(0xA0000000, intro));
        FlintFixUi.pushPanelIntro(c, x, y, w, h, intro);
        FlintFixUi.fadeOutline(c, x, y, w, h, 6, FlintFixUi.ACCENT);
        FlintFixUi.rounded(c, x, y, w, h, 6, 0xFF10151D);
        FlintFixFont.draw(c, "Client theme", x + 12, y + 9, 10, 0xFFF1F2F4, true);
        FlintFixFont.draw(c, "Choose a palette for FlintFix screens", x + 12, y + 23, 5, 0xFFA4A4A4, false);

        FlintFixTheme[] themes = FlintFixTheme.values();
        for (int i = 0; i < themes.length; i++) {
            FlintFixTheme theme = themes[i];
            int rowY = listTop + i * 27;
            boolean selected = theme == FlintFixUi.activeTheme();
            boolean hover = FlintFixUi.inside(mouseX, mouseY, x + 12, rowY, w - 24, 23);
            float hoverT = FlintFixUi.hoverProgress("theme-row:" + theme.id(), hover || selected);
            int rowFill = FlintFixUi.blendColors(0xFF1B2027, selected ? 0xFF343B45 : 0xFF252A32, hoverT);
            FlintFixUi.outlinedBox(c, x + 12, rowY, w - 24, 23, 3,
                FlintFixUi.interactiveBorder(selected || hoverT > 0.35f), rowFill);
            int swatchX = x + 19;
            int swatchY = rowY + 6;
            c.fill(swatchX, swatchY, swatchX + 8, swatchY + 10, theme.background());
            c.fill(swatchX + 8, swatchY, swatchX + 16, swatchY + 10, theme.panel());
            c.fill(swatchX + 16, swatchY, swatchX + 24, swatchY + 10, theme.card());
            c.fill(swatchX + 24, swatchY, swatchX + 32, swatchY + 10, theme.accent());
            FlintFixFont.draw(c, theme.label(), x + 59, rowY + 4, 6, 0xFFF1F2F4, true);
            FlintFixFont.draw(c, theme.description(), x + 59, rowY + 12, 4, 0xFFA4A4A4, false);
            if (selected) FlintFixFont.draw(c, "SELECTED", x + w - 53, rowY + 8, 4, theme.accentBright(), true);
        }

        FlintFixUi.compactButton(c, x + w - 58, y + h - 22, 46, 14, "BACK",
            FlintFixUi.inside(mouseX, mouseY, x + w - 58, y + h - 22, 46, 14), false);
        FlintFixUi.finishPanelIntro(c, x, y, w, h, intro);
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
        if (client != null) client.setScreen(parent);
    }

    @Override public boolean shouldPause() { return false; }
}
