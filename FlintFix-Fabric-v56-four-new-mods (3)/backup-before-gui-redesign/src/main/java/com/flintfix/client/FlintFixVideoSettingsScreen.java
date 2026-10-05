package com.flintfix.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.ParticlesMode;
import net.minecraft.text.Text;
import org.lwjgl.opengl.GL11;

import java.util.Locale;

/** FlintFix-styled video controls with transparent hardware-based recommendations. */
public final class FlintFixVideoSettingsScreen extends Screen {
    private static final int PERFORMANCE_TAB = 0;
    private static final int VISUAL_TAB = 1;
    private static final int ROW_H = 34;
    private static final int ROW_GAP = 5;
    private static final long TAB_ANIMATION_MS = 190L;
    private static final long CLOSE_ANIMATION_MS = 180L;

    private final Screen parent;
    private int panelX, panelY, panelW, panelH;
    private int leftX, leftY, leftW;
    private int rightX, rightY, rightW;
    private int selectedTab = PERFORMANCE_TAB;
    private int cpuThreads;
    private double javaMemoryGb;
    private String renderer = "Checking graphics renderer...";
    private String tier = "Balanced";
    private String tierDescription = "A balanced starting point for this system.";
    private String status = "Recommendations are estimates; shaders and modpacks affect FPS.";
    private long openedAt = System.currentTimeMillis();
    private long tabChangedAt = openedAt;
    private long statusChangedAt = openedAt;
    private long lastChangedAt;
    private long applyPulseAt;
    private long lastActionAt;
    private long closingAt;
    private String lastChangedLabel = "";
    private String lastActionButton = "";
    private boolean closing;
    private boolean closeFinished;

    public FlintFixVideoSettingsScreen(Screen parent) {
        super(Text.literal("FlintFix Video Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        layout();
        tabChangedAt = System.currentTimeMillis();
        scanHardware();
    }

    private void layout() {
        panelW = Math.min(700, Math.max(320, width - 24));
        panelH = Math.min(390, Math.max(300, height - 24));
        panelW = Math.min(panelW, width - 12);
        panelH = Math.min(panelH, height - 12);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;

        int innerX = panelX + 16;
        int innerW = panelW - 32;
        int gap = 12;
        leftW = Math.max(104, Math.round((innerW - gap) * 0.36f));
        rightW = Math.max(150, innerW - leftW - gap);
        leftX = innerX;
        rightX = leftX + leftW + gap;
        leftY = panelY + 48;
        rightY = leftY;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        layout();
        long now = System.currentTimeMillis();
        float intro = closing
            ? 1.0f - ease(clamp01((now - closingAt) / (float) CLOSE_ANIMATION_MS))
            : FlintFixUi.openProgress(openedAt);

        // Paint an opaque backdrop first so no vanilla panorama/blur can show
        // through this screen. Keep geometry on integer pixels: scaling the
        // whole canvas during the intro makes both glyphs and edges look soft.
        context.fill(0, 0, width, height, 0xFF0A0A10);
        int panelFill = FlintFixUi.blendColors(FlintFixUi.BG, FlintFixUi.PANEL, 0.36f);
        FlintFixUi.outlinedBox(context, panelX, panelY, panelW, panelH, 5,
            FlintFixUi.opacity(FlintFixUi.BORDER, intro), FlintFixUi.opacity(panelFill, intro));
        renderHeader(context, mouseX, mouseY, intro);
        float hardwareIn = ease(clamp01((now - openedAt - 35L) / 190.0f));
        float controlsIn = ease(clamp01((now - openedAt - 75L) / 210.0f));
        renderHardwareCard(context, mouseX, mouseY, intro * hardwareIn);
        renderSettingsCard(context, mouseX, mouseY, intro * controlsIn);
        renderFooter(context, mouseX, mouseY, intro);
        // All animation here is alpha/color based; no transformed framebuffer
        // or superclass background pass is drawn over the crisp controls.

        if (closing && now - closingAt >= CLOSE_ANIMATION_MS) finishClose();
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static float ease(float value) {
        float t = clamp01(value);
        return t * t * (3.0f - 2.0f * t);
    }

    private void setStatus(String value) {
        status = value;
        statusChangedAt = System.currentTimeMillis();
    }

    private void renderHeader(DrawContext c, int mouseX, int mouseY, float alpha) {
        FlintFixFont.drawCrisp(c, "VIDEO SETTINGS", panelX + 17, panelY + 13, 13,
            FlintFixUi.opacity(FlintFixThemeText(), alpha), true);
        FlintFixFont.drawCrisp(c, "A cleaner control panel, tuned to your PC", panelX + 18, panelY + 29, 7,
            FlintFixUi.opacity(FlintFixUi.activeTheme().muted(), alpha), false);

        int closeX = panelX + panelW - 34;
        int closeY = panelY + 11;
        boolean hover = inside(mouseX, mouseY, closeX, closeY, 21, 19);
        float hoverT = FlintFixUi.hoverProgress("video-close", hover);
        int closeFill = FlintFixUi.blendColors(FlintFixUi.CARD, FlintFixUi.CARD_HOVER, hoverT);
        int closeBorder = FlintFixUi.blendColors(FlintFixUi.BORDER,
            FlintFixUi.activeTheme().accentBright(), hoverT);
        FlintFixUi.outlinedBox(c, closeX, closeY, 21, 19, 3,
            FlintFixUi.opacity(closeBorder, alpha), FlintFixUi.opacity(closeFill, alpha));
        FlintFixFont.drawCenteredCrisp(c, "X", closeX + 10, closeY + 5, 8,
            FlintFixUi.opacity(FlintFixUi.activeTheme().text(), alpha), true);
        FlintFixUi.divider(c, panelX + 16, panelY + 41, panelW - 32);
    }

    private int FlintFixThemeText() {
        return FlintFixUi.activeTheme().text();
    }

    private void renderHardwareCard(DrawContext c, int mouseX, int mouseY, float alpha) {
        int cardH = leftCardHeight();
        FlintFixUi.outlinedBox(c, leftX, leftY, leftW, cardH, 4,
            FlintFixUi.opacity(FlintFixUi.BORDER, alpha), FlintFixUi.opacity(FlintFixUi.CARD, alpha));
        FlintFixFont.drawCrisp(c, "PC CHECK", leftX + 10, leftY + 10, 8,
            FlintFixUi.opacity(FlintFixUi.activeTheme().accentBright(), alpha), true);
        FlintFixFont.drawCrisp(c, "ESTIMATED PROFILE", leftX + 10, leftY + 25, 6,
            FlintFixUi.opacity(FlintFixUi.activeTheme().muted(), alpha), true);

        int badgeY = leftY + 39;
        FlintFixUi.outlinedBox(c, leftX + 9, badgeY, leftW - 18, 22, 3,
            FlintFixUi.BORDER, FlintFixUi.PANEL);
        FlintFixUi.rounded(c, leftX + 14, badgeY + 8, 5, 5, 2, FlintFixUi.ACCENT);
        FlintFixFont.drawTrimmedCrisp(c, tier.toUpperCase(Locale.ROOT), leftX + 24, badgeY + 8,
            leftW - 42, 6, FlintFixUi.activeTheme().text(), true);

        int infoY = badgeY + 34;
        drawHardwareLine(c, "CPU THREADS", cpuThreads <= 0 ? "—" : Integer.toString(cpuThreads), leftX + 10, infoY, leftW - 20, alpha);
        drawHardwareLine(c, "JAVA HEAP CAP", javaMemoryGb <= 0 ? "—" : String.format(Locale.ROOT, "%.1f GB", javaMemoryGb), leftX + 10, infoY + 28, leftW - 20, alpha);
        FlintFixFont.drawCrisp(c, "GRAPHICS RENDERER", leftX + 10, infoY + 56, 5,
            FlintFixUi.opacity(FlintFixUi.activeTheme().muted(), alpha), true);
        FlintFixFont.drawTrimmedCrisp(c, renderer, leftX + 10, infoY + 67, leftW - 20, 6,
            FlintFixUi.opacity(FlintFixUi.activeTheme().text(), alpha), false);

        int descriptionY = Math.min(leftY + cardH - 24, infoY + 85);
        FlintFixFont.drawTrimmedCrisp(c, tierDescription, leftX + 10, descriptionY,
            leftW - 20, 6, FlintFixUi.opacity(FlintFixUi.activeTheme().muted(), alpha), false);

        int scanY = leftY + cardH + 8;
        drawButton(c, leftX, scanY, leftW, 22, "SCAN AGAIN", inside(mouseX, mouseY, leftX, scanY, leftW, 22), false, alpha);
    }

    private void drawHardwareLine(DrawContext c, String label, String value, int x, int y, int w, float alpha) {
        FlintFixFont.drawCrisp(c, label, x, y, 5, FlintFixUi.opacity(FlintFixUi.activeTheme().muted(), alpha), true);
        FlintFixFont.drawTrimmedCrisp(c, value, x, y + 10, w, 8,
            FlintFixUi.opacity(FlintFixUi.activeTheme().text(), alpha), true);
    }

    private void renderSettingsCard(DrawContext c, int mouseX, int mouseY, float alpha) {
        int cardH = rightCardHeight();
        FlintFixUi.outlinedBox(c, rightX, rightY, rightW, cardH, 4,
            FlintFixUi.opacity(FlintFixUi.BORDER, alpha), FlintFixUi.opacity(FlintFixUi.CARD, alpha));

        int tabY = rightY + 8;
        int tabGap = 5;
        int tabW = Math.max(54, (rightW - 26 - tabGap) / 2);
        drawTab(c, rightX + 9, tabY, tabW, "PERFORMANCE", PERFORMANCE_TAB, mouseX, mouseY, alpha);
        drawTab(c, rightX + 9 + tabW + tabGap, tabY, tabW, "VISUALS", VISUAL_TAB, mouseX, mouseY, alpha);

        int rowTop = rightY + 38;
        Setting[] settings = selectedTab == PERFORMANCE_TAB ? performanceSettings() : visualSettings();
        int actualRowH = Math.min(ROW_H, Math.max(27, (cardH - 49 - (settings.length - 1) * 4) / settings.length));
        for (int i = 0; i < settings.length; i++) {
            int rowY = rowTop + i * (actualRowH + ROW_GAP);
            float stagger = ease(clamp01((System.currentTimeMillis() - tabChangedAt - i * 24L) / (float) TAB_ANIMATION_MS));
            renderSettingRow(c, settings[i], i, rowY, actualRowH, mouseX, mouseY, alpha * stagger);
        }
    }

    private void drawTab(DrawContext c, int x, int y, int w, String label, int tab, int mouseX, int mouseY, float alpha) {
        boolean selected = selectedTab == tab;
        boolean hover = inside(mouseX, mouseY, x, y, w, 20);
        float hoverT = FlintFixUi.hoverProgress("video-tab-" + tab, hover);
        int base = selected ? FlintFixUi.CARD_HOVER : FlintFixUi.BG;
        int fill = FlintFixUi.blendColors(base, FlintFixUi.PANEL, hoverT * 0.8f);
        int border = FlintFixUi.blendColors(FlintFixUi.BORDER,
            FlintFixUi.activeTheme().accent(), selected ? 0.85f : hoverT * 0.65f);
        FlintFixUi.outlinedBox(c, x, y, w, 20, 3,
            FlintFixUi.opacity(border, alpha), FlintFixUi.opacity(fill, alpha));
        if (selected) {
            int indicatorW = Math.max(12, Math.round((w - 16) * ease(clamp01((System.currentTimeMillis() - tabChangedAt) / 150.0f))));
            FlintFixUi.rounded(c, x + (w - indicatorW) / 2, y + 17, indicatorW, 2, 1,
                FlintFixUi.opacity(FlintFixUi.activeTheme().accentBright(), alpha));
        }
        FlintFixFont.drawCenteredCrisp(c, label, x + w / 2, y + 6, 6,
            FlintFixUi.opacity(selected ? FlintFixUi.activeTheme().text() : FlintFixUi.activeTheme().muted(), alpha), selected);
    }

    private void renderSettingRow(DrawContext c, Setting setting, int index, int y, int h,
                                  int mouseX, int mouseY, float alpha) {
        boolean hover = inside(mouseX, mouseY, rightX + 8, y, rightW - 16, h);
        float hoverT = FlintFixUi.hoverProgress("video-row-" + setting.label, hover);
        long now = System.currentTimeMillis();
        float changedPulse = setting.label.equals(lastChangedLabel)
            ? 1.0f - clamp01((now - lastChangedAt) / 460.0f) : 0.0f;
        float appliedPulse = applyPulseAt == 0L ? 0.0f : 1.0f - clamp01((now - applyPulseAt) / 600.0f);
        float pulse = Math.max(changedPulse, appliedPulse * 0.6f);
        int rowFill = FlintFixUi.blendColors(FlintFixUi.PANEL, FlintFixUi.CARD_HOVER, hoverT * 0.85f);
        rowFill = FlintFixUi.blendColors(rowFill, FlintFixUi.ACCENT_SOFT, pulse * 0.55f);
        int rowBorder = FlintFixUi.blendColors(FlintFixUi.BORDER,
            FlintFixUi.activeTheme().accentBright(), Math.max(hoverT * 0.65f, pulse * 0.8f));
        FlintFixUi.outlinedBox(c, rightX + 8, y, rightW - 16, h, 3,
            FlintFixUi.opacity(rowBorder, alpha), FlintFixUi.opacity(rowFill, alpha));
        if (pulse > 0.02f) {
            FlintFixUi.rounded(c, rightX + 9, y + 4, 2, h - 8, 1,
                FlintFixUi.opacity(FlintFixUi.activeTheme().accentBright(), alpha * pulse));
        }
        FlintFixFont.drawCrisp(c, setting.label, rightX + 15, y + 5, 7,
            FlintFixUi.opacity(FlintFixUi.activeTheme().text(), alpha), true);

        String current = setting.current.get();
        String suggested = setting.suggested.get();
        String detail = "Now  " + current + "    Suggested  " + suggested;
        FlintFixFont.drawTrimmedCrisp(c, detail, rightX + 15, y + h - 11,
            rightW - 54, 5, FlintFixUi.opacity(FlintFixUi.activeTheme().muted(), alpha), false);
        FlintFixFont.drawCenteredCrisp(c, "<", rightX + rightW - 36, y + h / 2 - 3, 8,
            FlintFixUi.opacity(FlintFixUi.activeTheme().muted(), alpha), true);
        FlintFixFont.drawCenteredCrisp(c, ">", rightX + rightW - 17, y + h / 2 - 3, 8,
            FlintFixUi.opacity(FlintFixUi.activeTheme().muted(), alpha), true);
    }

    private void renderFooter(DrawContext c, int mouseX, int mouseY, float alpha) {
        int footerY = panelY + panelH - 39;
        c.fill(panelX + 16, footerY - 8, panelX + panelW - 16, footerY - 7,
            FlintFixUi.opacity(FlintFixUi.BORDER, alpha));
        float statusAlpha = ease(clamp01((System.currentTimeMillis() - statusChangedAt) / 170.0f));
        FlintFixFont.drawTrimmedCrisp(c, status, panelX + 88, footerY + 8, Math.max(24, panelW - 270), 6,
            FlintFixUi.opacity(FlintFixUi.activeTheme().muted(), alpha * statusAlpha), false);
        int applyW = 150;
        int applyX = panelX + panelW - applyW - 16;
        drawButton(c, applyX, footerY, applyW, 25, "APPLY RECOMMENDATION",
            inside(mouseX, mouseY, applyX, footerY, applyW, 25), true, alpha);
        int backX = panelX + 16;
        drawButton(c, backX, footerY, 60, 25, "BACK",
            inside(mouseX, mouseY, backX, footerY, 60, 25), false, alpha);
    }

    private void drawButton(DrawContext c, int x, int y, int w, int h, String label,
                            boolean hover, boolean primary, float alpha) {
        float hoverT = FlintFixUi.hoverProgress("video-button-" + label, hover);
        int baseFill = primary ? FlintFixUi.ACCENT_SOFT : FlintFixUi.PANEL;
        int hoverFill = primary ? FlintFixUi.ACCENT : FlintFixUi.CARD_HOVER;
        float pressPulse = label.equals(lastActionButton)
            ? 1.0f - clamp01((System.currentTimeMillis() - lastActionAt) / 420.0f) : 0.0f;
        int fill = FlintFixUi.blendColors(baseFill, hoverFill, Math.max(hoverT, pressPulse * 0.5f));
        int edge = primary ? FlintFixUi.activeTheme().accent()
            : FlintFixUi.blendColors(FlintFixUi.BORDER, FlintFixUi.activeTheme().accentBright(), hoverT);
        FlintFixUi.outlinedBox(c, x, y, w, h, 3,
            FlintFixUi.opacity(edge, alpha), FlintFixUi.opacity(fill, alpha));
        if (hoverT > 0.02f || pressPulse > 0.02f) {
            FlintFixUi.rounded(c, x + 5, y + h - 3, w - 10, 1, 0,
                FlintFixUi.opacity(FlintFixUi.activeTheme().accentBright(), alpha * Math.max(hoverT, pressPulse)));
        }
        FlintFixFont.drawCenteredCrisp(c, label, x + w / 2, y + (h - 7) / 2, 7,
            FlintFixUi.opacity(FlintFixUi.activeTheme().text(), alpha), true);
    }

    private Setting[] performanceSettings() {
        return new Setting[] {
            new Setting("RENDER DISTANCE", () -> options().getViewDistance().getValue() + " chunks",
                () -> recommendation().renderDistance + " chunks", this::cycleRenderDistance),
            new Setting("SIMULATION DISTANCE", () -> options().getSimulationDistance().getValue() + " chunks",
                () -> recommendation().simulationDistance + " chunks", this::cycleSimulationDistance),
            new Setting("GRAPHICS", () -> title(options().getGraphicsMode().getValue().name()),
                () -> "Fast", this::cycleGraphics),
            new Setting("PARTICLES", () -> title(options().getParticles().getValue().name()),
                () -> title(recommendation().particles.name()), this::cycleParticles)
        };
    }

    private Setting[] visualSettings() {
        return new Setting[] {
            new Setting("CLOUDS", () -> title(options().getCloudRenderMode().getValue().name()),
                () -> title(recommendation().clouds.name()), this::cycleClouds),
            new Setting("AMBIENT OCCLUSION", () -> onOff(options().getAo().getValue()),
                () -> onOff(recommendation().ambientOcclusion), this::cycleAo),
            new Setting("ENTITY DISTANCE", () -> Math.round(options().getEntityDistanceScaling().getValue() * 100) + "%",
                () -> Math.round(recommendation().entityDistance * 100) + "%", this::cycleEntityDistance),
            new Setting("MAX FRAME RATE", () -> fpsLabel(options().getMaxFps().getValue()),
                () -> fpsLabel(recommendation().maxFps), this::cycleMaxFps),
            new Setting("VERTICAL SYNC", () -> onOff(options().getEnableVsync().getValue()),
                () -> onOff(recommendation().verticalSync), this::cycleVsync)
        };
    }

    private void scanHardware() {
        cpuThreads = Runtime.getRuntime().availableProcessors();
        javaMemoryGb = Runtime.getRuntime().maxMemory() / (1024.0 * 1024.0 * 1024.0);
        try {
            String activeRenderer = GL11.glGetString(GL11.GL_RENDERER);
            renderer = activeRenderer == null || activeRenderer.isBlank() ? "Unknown renderer" : activeRenderer.trim();
        } catch (RuntimeException | LinkageError ignored) {
            renderer = "Renderer unavailable";
        }

        String lower = renderer.toLowerCase(Locale.ROOT);
        boolean integrated = (lower.contains("intel") && !lower.contains("arc"))
            || lower.contains("llvmpipe") || lower.contains("swiftshader") || lower.contains("gallium");
        boolean dedicated = lower.contains("nvidia") || lower.contains("geforce")
            || lower.contains("radeon") || lower.contains("arc") || lower.contains("apple m");
        if (cpuThreads <= 4 || javaMemoryGb < 3.25 || integrated) {
            tier = "Performance";
            tierDescription = "Prioritizes a steadier frame rate on limited resources.";
        } else if (cpuThreads >= 8 && javaMemoryGb >= 6.0 && dedicated) {
            tier = "High headroom";
            tierDescription = "Keeps good detail while easing the heaviest settings.";
        } else {
            tier = "Balanced";
            tierDescription = "Balances view distance, clarity and frame rate.";
        }
        setStatus("Estimated from CPU threads, Java memory cap and renderer; no benchmark run.");
    }

    private Recommendation recommendation() {
        return switch (tier) {
            case "Performance" -> new Recommendation(8, 5, ParticlesMode.MINIMAL, CloudRenderMode.OFF, false, 0.5, 120, false);
            case "High headroom" -> new Recommendation(16, 10, ParticlesMode.DECREASED, CloudRenderMode.FAST, true, 0.9, 240, false);
            default -> new Recommendation(12, 8, ParticlesMode.DECREASED, CloudRenderMode.FAST, false, 0.75, 180, false);
        };
    }

    private void applyRecommendation() {
        GameOptions options = options();
        Recommendation r = recommendation();
        options.getViewDistance().setValue(r.renderDistance);
        options.getSimulationDistance().setValue(r.simulationDistance);
        options.getGraphicsMode().setValue(GraphicsMode.FAST);
        options.getParticles().setValue(r.particles);
        options.getCloudRenderMode().setValue(r.clouds);
        options.getAo().setValue(r.ambientOcclusion);
        options.getEntityDistanceScaling().setValue(r.entityDistance);
        options.getMaxFps().setValue(r.maxFps);
        options.getEnableVsync().setValue(r.verticalSync);
        options.write();
        setStatus("Recommended settings applied. Adjust any control above whenever you like.");
        applyPulseAt = System.currentTimeMillis();
        lastActionButton = "APPLY RECOMMENDATION";
        lastActionAt = applyPulseAt;
    }

    private void cycleRenderDistance(boolean forward) {
        int next = stepped(options().getViewDistance().getValue(), 2, 32, forward ? 2 : -2);
        options().getViewDistance().setValue(next);
    }

    private void cycleSimulationDistance(boolean forward) {
        int next = stepped(options().getSimulationDistance().getValue(), 5, 32, forward ? 1 : -1);
        options().getSimulationDistance().setValue(next);
    }

    private void cycleGraphics(boolean forward) {
        // Keep Fabulous behind Minecraft's native confirmation flow; this page
        // intentionally cycles only the safe Fast/Fancy choices.
        GraphicsMode[] values = {GraphicsMode.FAST, GraphicsMode.FANCY};
        options().getGraphicsMode().setValue(next(values, options().getGraphicsMode().getValue(), forward));
    }

    private void cycleParticles(boolean forward) {
        ParticlesMode[] values = {ParticlesMode.MINIMAL, ParticlesMode.DECREASED, ParticlesMode.ALL};
        options().getParticles().setValue(next(values, options().getParticles().getValue(), forward));
    }

    private void cycleClouds(boolean forward) {
        CloudRenderMode[] values = {CloudRenderMode.OFF, CloudRenderMode.FAST, CloudRenderMode.FANCY};
        options().getCloudRenderMode().setValue(next(values, options().getCloudRenderMode().getValue(), forward));
    }

    private void cycleAo(boolean ignored) {
        options().getAo().setValue(!options().getAo().getValue());
    }

    private void cycleEntityDistance(boolean forward) {
        double[] values = {0.5, 0.6, 0.7, 0.8, 0.9, 1.0, 1.25, 1.5};
        options().getEntityDistanceScaling().setValue(next(values, options().getEntityDistanceScaling().getValue(), forward));
    }

    private void cycleMaxFps(boolean forward) {
        int[] values = {30, 60, 90, 120, 144, 165, 180, 240, 260};
        options().getMaxFps().setValue(next(values, options().getMaxFps().getValue(), forward));
    }

    private void cycleVsync(boolean ignored) {
        options().getEnableVsync().setValue(!options().getEnableVsync().getValue());
    }

    private int leftCardHeight() {
        return Math.max(150, panelH - 124);
    }

    private int rightCardHeight() {
        return Math.max(106, panelH - 104);
    }

    private static int stepped(int value, int min, int max, int delta) {
        return Math.max(min, Math.min(max, value + delta));
    }

    private static <T> T next(T[] values, T current, boolean forward) {
        int index = 0;
        for (int i = 0; i < values.length; i++) if (values[i].equals(current)) { index = i; break; }
        return values[Math.floorMod(index + (forward ? 1 : -1), values.length)];
    }

    private static int next(int[] values, int current, boolean forward) {
        int index = 0;
        for (int i = 0; i < values.length; i++) if (values[i] == current) { index = i; break; }
        return values[Math.floorMod(index + (forward ? 1 : -1), values.length)];
    }

    private static double next(double[] values, double current, boolean forward) {
        int index = 0;
        for (int i = 0; i < values.length; i++) if (Math.abs(values[i] - current) < 0.001) { index = i; break; }
        return values[Math.floorMod(index + (forward ? 1 : -1), values.length)];
    }

    private void selectTab(int tab) {
        if (selectedTab == tab) return;
        selectedTab = tab;
        tabChangedAt = System.currentTimeMillis();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (closing) return true;
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        layout();
        if (inside(mouseX, mouseY, panelX + panelW - 34, panelY + 11, 21, 19)) {
            close();
            return true;
        }

        int scanY = leftY + leftCardHeight() + 8;
        if (inside(mouseX, mouseY, leftX, scanY, leftW, 22)) {
            scanHardware();
            lastActionButton = "SCAN AGAIN";
            lastActionAt = System.currentTimeMillis();
            return true;
        }

        int footerY = panelY + panelH - 39;
        if (inside(mouseX, mouseY, panelX + 16, footerY, 60, 25)) {
            close();
            return true;
        }
        int applyW = 150;
        int applyX = panelX + panelW - applyW - 16;
        if (inside(mouseX, mouseY, applyX, footerY, applyW, 25)) {
            applyRecommendation();
            return true;
        }

        int tabY = rightY + 8;
        int tabGap = 5;
        int tabW = Math.max(54, (rightW - 26 - tabGap) / 2);
        if (inside(mouseX, mouseY, rightX + 9, tabY, tabW, 20)) {
            selectTab(PERFORMANCE_TAB);
            return true;
        }
        if (inside(mouseX, mouseY, rightX + 9 + tabW + tabGap, tabY, tabW, 20)) {
            selectTab(VISUAL_TAB);
            return true;
        }

        Setting[] settings = selectedTab == PERFORMANCE_TAB ? performanceSettings() : visualSettings();
        int actualRowH = Math.min(ROW_H, Math.max(27, (rightCardHeight() - 49 - (settings.length - 1) * 4) / settings.length));
        int rowTop = rightY + 38;
        int rowX = rightX + 8;
        for (int i = 0; i < settings.length; i++) {
            int rowY = rowTop + i * (actualRowH + ROW_GAP);
            if (inside(mouseX, mouseY, rowX, rowY, rightW - 16, actualRowH)) {
                boolean forward = mouseX >= rightX + rightW - 29;
                settings[i].change.accept(forward);
                lastChangedLabel = settings[i].label;
                lastChangedAt = System.currentTimeMillis();
                setStatus("Setting updated. Your choice is saved when you leave this page.");
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void close() {
        if (client == null || closing || closeFinished) return;
        closing = true;
        closingAt = System.currentTimeMillis();
    }

    private void finishClose() {
        if (client == null || closeFinished) return;
        closeFinished = true;
        client.options.write();
        client.setScreen(parent);
    }

    private GameOptions options() {
        return MinecraftClient.getInstance().options;
    }

    private static String title(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.isEmpty() ? lower : Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static String fpsLabel(int fps) {
        return fps >= 260 ? "Unlimited" : fps + " FPS";
    }

    private static String onOff(boolean value) {
        return value ? "On" : "Off";
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private record Recommendation(int renderDistance, int simulationDistance, ParticlesMode particles,
                                  CloudRenderMode clouds, boolean ambientOcclusion,
                                  double entityDistance, int maxFps, boolean verticalSync) {}

    private record Setting(String label, Value current, Value suggested, Change change) {}
    @FunctionalInterface private interface Value { String get(); }
    @FunctionalInterface private interface Change { void accept(boolean forward); }
}
