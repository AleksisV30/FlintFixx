package com.flintfix.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Picks the custom sky preset and chooses which sky effects are drawn. */
public final class FlintFixSkyScreen extends FlintFixScreen {
    private static final String[] EFFECTS = {
        "Stars", "Milky Way", "Shooting stars", "Aurora", "Nebula clouds",
        "Sun & moon glow", "Sun rays", "Moon phases", "Horizon glow", "Matching fog"
    };

    private final Screen parent;
    private final long openedAt = System.currentTimeMillis();
    private int x, y, w, h;
    private int toggleY, timeY, columnsTop, columnsBottom;
    private int leftX, leftW, rightX, rightW;
    private int presetStep, presetH, effectStep, effectH;

    public FlintFixSkyScreen(Screen parent) {
        super(Component.literal("Custom Sky"));
        this.parent = parent;
    }

    private void layout() {
        w = Math.min(420, Math.max(1, width - 16));
        h = Math.min(320, Math.max(1, height - 12));
        x = (width - w) / 2;
        y = (height - h) / 2;
        toggleY = y + 44;
        timeY = toggleY + 26;
        columnsTop = timeY + 26;
        columnsBottom = y + h - 28;
        leftX = x + 12;
        leftW = Math.round((w - 34) * 0.56f);
        rightX = leftX + leftW + 10;
        rightW = x + w - 12 - rightX;
        int presets = FlintFixSky.Preset.values().length;
        presetStep = Math.max(14, Math.min(28, (columnsBottom - columnsTop - 12) / presets));
        presetH = presetStep - 3;
        effectStep = Math.max(12, Math.min(22, (columnsBottom - columnsTop - 12) / EFFECTS.length));
        effectH = effectStep - 2;
    }

    @Override protected void init() { layout(); }

    @Override
    public void render(GuiGraphics c, int mouseX, int mouseY, float delta) {
        layout();
        float intro = FlintFixUi.openProgress(openedAt);
        if (minecraft != null && minecraft.level != null) blurBehind(c, delta);
        FlintFixUi.backdrop(c, width, height, intro);
        FlintFixUi.pushPanelIntro(c, x, y, w, h, intro);
        FlintFixUi.panelFrame(c, x, y, w, h);
        FlintFixUi.header(c, x + 12, y + 10, w - 24, "sky", "Custom Sky",
            "Overworld only  ·  vanilla sky returns underwater");
        FlintFixUi.hairline(c, x + 12, y + 37, w - 24);

        boolean enabled = FlintFixClient.CONFIG.skyEnabled;
        boolean toggleHover = FlintFixUi.inside(mouseX, mouseY, x + 12, toggleY, w - 24, 22);
        float toggleT = FlintFixUi.hoverProgress("sky-toggle", toggleHover);
        FlintFixUi.surface(c, x + 12, toggleY, w - 24, 22,
            FlintFixUi.blendColors(FlintFixUi.card(), FlintFixUi.raised(), toggleT * 0.6f), FlintFixUi.border());
        FlintFixFont.drawExact(c, "Use custom sky", x + 20, FlintFixFont.centeredY(toggleY, 22, 7), 7, FlintFixUi.text(), true);
        FlintFixUi.switchToggle(c, "sky-enabled", x + w - 12 - 28, toggleY + 6, enabled);

        renderTimeModes(c, mouseX, mouseY);
        renderPresets(c, mouseX, mouseY);
        renderEffects(c, mouseX, mouseY);

        String hint = !enabled ? "Picking a preset turns the sky on"
            : (FlintFixClient.CONFIG.skyTimeMode == 0 ? "Stars, aurora and meteors show at night  ·  try Sky time: Night"
            : "Sky time only changes the sky, not the world's light");
        FlintFixUi.drawTrimmedExact(c, hint,
            x + 12, FlintFixFont.centeredY(y + h - 22, 14, 6), w - 84, 6, FlintFixUi.subtle(), false);
        FlintFixUi.compactButton(c, x + w - 58, y + h - 22, 46, 14, "BACK",
            FlintFixUi.inside(mouseX, mouseY, x + w - 58, y + h - 22, 46, 14), false);
        FlintFixUi.finishPanelIntro(c, x, y, w, h, intro);
    }

    /** Segmented control that pins the sky to a time of day for viewing. */
    private void renderTimeModes(GuiGraphics c, int mouseX, int mouseY) {
        FlintFixFont.drawExact(c, "Sky time", x + 14, FlintFixFont.centeredY(timeY, 18, 7), 7, FlintFixUi.text(), true);
        int segX = timeSegX();
        int segW = timeSegW();
        String[] modes = FlintFixSky.TIME_MODES;
        FlintFixUi.surface(c, segX, timeY, segW * modes.length + 4, 18, FlintFixUi.bg(), FlintFixUi.border());
        for (int i = 0; i < modes.length; i++) {
            int bx = segX + 2 + i * segW;
            boolean selected = FlintFixClient.CONFIG.skyTimeMode == i;
            boolean hover = FlintFixUi.inside(mouseX, mouseY, bx, timeY, segW, 18);
            float t = FlintFixUi.hoverProgress("sky-time:" + i, selected);
            if (t > 0.01f) FlintFixUi.roundedRaw(c, bx, timeY + 2, segW, 14, 2, FlintFixUi.opacity(FlintFixUi.accent(), t));
            int ink = selected ? FlintFixUi.onAccent() : (hover ? FlintFixUi.text() : FlintFixUi.muted());
            FlintFixFont.drawCenteredExact(c, FlintFixFont.trim(modes[i], segW - 4, 6, true), bx + segW / 2,
                FlintFixFont.centeredY(timeY, 18, 6), 6, ink, true);
        }
    }

    private int timeSegW() { return Math.max(34, Math.min(64, (w - 24 - 70 - 4) / FlintFixSky.TIME_MODES.length)); }
    private int timeSegX() { return x + w - 12 - (timeSegW() * FlintFixSky.TIME_MODES.length + 4); }

    private void renderPresets(GuiGraphics c, int mouseX, int mouseY) {
        FlintFixUi.sectionLabel(c, "PRESETS", leftX + 2, columnsTop);
        FlintFixSky.Preset current = FlintFixSky.preset();
        FlintFixSky.Preset[] presets = FlintFixSky.Preset.values();
        for (int i = 0; i < presets.length; i++) {
            FlintFixSky.Preset preset = presets[i];
            int rowY = presetY(i);
            boolean selected = preset == current;
            boolean hover = FlintFixUi.inside(mouseX, mouseY, leftX, rowY, leftW, presetH);
            float hoverT = FlintFixUi.hoverProgress("sky-row:" + preset.name(), hover);
            float selectedT = FlintFixUi.hoverProgress("sky-selected:" + preset.name(), selected);
            int fill = FlintFixUi.blendColors(FlintFixUi.card(), FlintFixUi.raised(), Math.max(hoverT * 0.6f, selectedT));
            int edge = FlintFixUi.blendColors(FlintFixUi.border(), FlintFixUi.accent(), selectedT);
            FlintFixUi.surface(c, leftX, rowY, leftW, presetH, fill, edge);
            int swatchW = Math.min(46, presetH * 2 + 6);
            renderSwatch(c, preset, leftX + 3, rowY + 3, swatchW, presetH - 6);
            int textX = leftX + swatchW + 10;
            int textW = leftW - swatchW - 10 - 20;
            if (presetH >= 21) {
                FlintFixUi.drawTrimmedExact(c, preset.label(), textX, rowY + 4, textW, 7, FlintFixUi.text(), true);
                FlintFixUi.drawTrimmedExact(c, preset.description(), textX, rowY + presetH - 10, textW, 6,
                    FlintFixUi.muted(), false);
            } else {
                FlintFixUi.drawTrimmedExact(c, preset.label(), textX, FlintFixFont.centeredY(rowY, presetH, 7), textW, 7,
                    FlintFixUi.text(), true);
            }
            if (selectedT > 0.01f) {
                int size = Math.min(12, presetH - 4);
                int checkX = leftX + leftW - size - 5;
                int checkY = rowY + (presetH - size) / 2;
                FlintFixUi.roundedRaw(c, checkX, checkY, size, size, 3, FlintFixUi.opacity(FlintFixUi.accent(), selectedT));
                FlintFixIcons.drawExact(c, "check", checkX + 2, checkY + 2, size - 4,
                    FlintFixUi.opacity(FlintFixUi.onAccent(), selectedT));
            }
        }
    }

    private void renderEffects(GuiGraphics c, int mouseX, int mouseY) {
        FlintFixUi.sectionLabel(c, "EFFECTS", rightX + 2, columnsTop);
        for (int i = 0; i < EFFECTS.length; i++) {
            int rowY = effectY(i);
            boolean on = effect(i);
            boolean hover = FlintFixUi.inside(mouseX, mouseY, rightX, rowY, rightW, effectH);
            float hoverT = FlintFixUi.hoverProgress("sky-effect:" + i, hover);
            FlintFixUi.surface(c, rightX, rowY, rightW, effectH,
                FlintFixUi.blendColors(FlintFixUi.card(), FlintFixUi.raised(), hoverT * 0.6f), FlintFixUi.border());
            FlintFixUi.drawTrimmedExact(c, EFFECTS[i], rightX + 7, FlintFixFont.centeredY(rowY, effectH, 6),
                rightW - 38, 6, on ? FlintFixUi.text() : FlintFixUi.muted(), on);
            FlintFixUi.switchToggle(c, "sky-effect-switch:" + i, rightX + rightW - 25, rowY + (effectH - 11) / 2, on);
        }
    }

    private int presetY(int index) { return columnsTop + 11 + index * presetStep; }
    private int effectY(int index) { return columnsTop + 11 + index * effectStep; }

    private static boolean effect(int index) {
        FlintFixConfig c = FlintFixClient.CONFIG;
        return switch (index) {
            case 0 -> c.skyStars;
            case 1 -> c.skyMilkyWay;
            case 2 -> c.skyShootingStars;
            case 3 -> c.skyAurora;
            case 4 -> c.skyNebula;
            case 5 -> c.skySunGlow;
            case 6 -> c.skySunRays;
            case 7 -> c.skyMoonPhases;
            case 8 -> c.skyHorizonGlow;
            default -> c.skyFogTint;
        };
    }

    private static void toggleEffect(int index) {
        FlintFixConfig c = FlintFixClient.CONFIG;
        switch (index) {
            case 0 -> c.skyStars = !c.skyStars;
            case 1 -> c.skyMilkyWay = !c.skyMilkyWay;
            case 2 -> c.skyShootingStars = !c.skyShootingStars;
            case 3 -> c.skyAurora = !c.skyAurora;
            case 4 -> c.skyNebula = !c.skyNebula;
            case 5 -> c.skySunGlow = !c.skySunGlow;
            case 6 -> c.skySunRays = !c.skySunRays;
            case 7 -> c.skyMoonPhases = !c.skyMoonPhases;
            case 8 -> c.skyHorizonGlow = !c.skyHorizonGlow;
            default -> c.skyFogTint = !c.skyFogTint;
        }
        c.save();
    }

    /** Day sky on the left half, night sky with stars on the right half. */
    private static void renderSwatch(GuiGraphics c, FlintFixSky.Preset preset, int sx, int sy, int sw, int sh) {
        if (sh < 4) return;
        int half = sw / 2;
        c.fillGradient(sx, sy, sx + half, sy + sh, preset.day().zenith(), preset.day().horizon());
        c.fillGradient(sx + half, sy, sx + sw, sy + sh, preset.night().zenith(), preset.night().horizon());
        c.fill(sx + half - 8, sy + sh / 2 - 2, sx + half - 4, sy + sh / 2 + 2, preset.day().glow());
        int[][] stars = {{4, 3}, {10, 6}, {15, 2}, {19, 8}, {7, 9}};
        for (int[] star : stars) {
            int px = sx + half + Math.min(half - 2, star[0]);
            int py = sy + Math.min(sh - 2, star[1]);
            c.fill(px, py, px + 1, py + 1, 0xDDFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        layout();
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        if (FlintFixUi.inside(mouseX, mouseY, x + 12, toggleY, w - 24, 22)) {
            FlintFixClient.CONFIG.skyEnabled = !FlintFixClient.CONFIG.skyEnabled;
            FlintFixClient.CONFIG.save();
            return true;
        }
        for (int i = 0; i < FlintFixSky.TIME_MODES.length; i++) {
            if (FlintFixUi.inside(mouseX, mouseY, timeSegX() + 2 + i * timeSegW(), timeY, timeSegW(), 18)) {
                FlintFixClient.CONFIG.skyTimeMode = i;
                FlintFixClient.CONFIG.save();
                return true;
            }
        }
        FlintFixSky.Preset[] presets = FlintFixSky.Preset.values();
        for (int i = 0; i < presets.length; i++) {
            if (FlintFixUi.inside(mouseX, mouseY, leftX, presetY(i), leftW, presetH)) {
                FlintFixClient.CONFIG.skyPreset = presets[i].name();
                FlintFixClient.CONFIG.skyEnabled = true;
                FlintFixClient.CONFIG.save();
                return true;
            }
        }
        for (int i = 0; i < EFFECTS.length; i++) {
            if (FlintFixUi.inside(mouseX, mouseY, rightX, effectY(i), rightW, effectH)) {
                toggleEffect(i);
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
