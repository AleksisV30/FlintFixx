package com.flintfix.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.flintfix.client.mixin.ScreenInvoker;
import org.lwjgl.opengl.GL11;

import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
//? if >=1.21.2 {
/*import net.minecraft.server.level.ParticleStatus;
*///?} else {
import net.minecraft.client.ParticleStatus;
//?}
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** FlintFix-styled video controls with transparent hardware-based recommendations. */
public final class FlintFixVideoSettingsScreen extends FlintFixScreen {
    private static final int PERFORMANCE_TAB = 0;
    private static final int VISUAL_TAB = 1;
    private static final long TAB_ANIMATION_MS = 220L;
    private static final int STEP_BUTTON = 13;
    private static final int VALUE_PILL_W = 62;
    private static final String[] PRESET_NAMES = {"Performance", "Balanced", "Quality"};

    private final Screen parent;
    private int panelX, panelY, panelW, panelH;
    private int leftX, leftW, rightX, rightW;
    private int bodyTop, bodyBottom, footerY;
    private int selectedTab = PERFORMANCE_TAB;
    private int cpuThreads;
    private double javaMemoryGb;
    private String renderer = "Checking graphics renderer...";
    private String tier = "Balanced";
    private String tierDescription = "A balanced starting point for this system.";
    private String status = "Recommendations are estimates; shaders and modpacks affect FPS.";
    private final long openedAt = System.currentTimeMillis();
    private long tabChangedAt = openedAt;
    private long lastChangedAt;
    private String lastChangedLabel = "";
    private int draggingSlider = -1;

    public FlintFixVideoSettingsScreen(Screen parent) {
        super(Component.literal("FlintFix Video Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        layout();
        scanHardware();
    }

    private void layout() {
        panelW = Math.min(540, Math.max(300, width - 16));
        panelH = Math.min(360, Math.max(220, height - 12));
        panelW = Math.min(panelW, width - 4);
        panelH = Math.min(panelH, height - 4);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;

        int innerX = panelX + 14;
        int innerW = panelW - 28;
        leftW = Math.max(118, Math.min(178, Math.round(innerW * 0.34f)));
        leftX = innerX;
        rightX = leftX + leftW + 10;
        rightW = panelX + panelW - 14 - rightX;
        bodyTop = panelY + 50;
        footerY = panelY + panelH - 26;
        bodyBottom = footerY - 10;
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

    @Override
    public void render(GuiGraphics c, int mouseX, int mouseY, float delta) {
        layout();
        float intro = FlintFixUi.openProgress(openedAt);
        if (minecraft != null && minecraft.level != null) {
            blurBehind(c, delta);
            FlintFixUi.backdrop(c, width, height, intro);
        } else {
            FlintFixTitleBackground.render(c, width, height);
        }

        FlintFixUi.pushPanelIntro(c, panelX, panelY, panelW, panelH, intro);
        FlintFixUi.panelFrame(c, panelX, panelY, panelW, panelH);
        FlintFixUi.header(c, panelX + 14, panelY + 11, panelW - 28 - 22, "video", "Video Settings",
            "Tuned to your PC  ·  saved when you leave this page");
        int closeX = closeX();
        FlintFixUi.iconButton(c, "video-close", closeX, panelY + 13, 15, "close",
            FlintFixUi.inside(mouseX, mouseY, closeX, panelY + 13, 15, 15));
        FlintFixUi.hairline(c, panelX + 14, panelY + 42, panelW - 28);

        renderPcCheck(c, mouseX, mouseY);
        renderPresets(c, mouseX, mouseY);
        renderSettings(c, mouseX, mouseY);
        renderFooter(c, mouseX, mouseY);
        FlintFixUi.finishPanelIntro(c, panelX, panelY, panelW, panelH, intro);
    }

    private int closeX() { return panelX + panelW - 14 - 15; }

    private int presetBlockHeight() { return 12 + 3 * 16 + 2 * 4; }

    private int pcCardHeight() { return Math.max(60, bodyBottom - bodyTop - presetBlockHeight() - 8); }

    private void renderPcCheck(GuiGraphics c, int mouseX, int mouseY) {
        int cardH = pcCardHeight();
        FlintFixUi.surface(c, leftX, bodyTop, leftW, cardH, FlintFixUi.panel(), FlintFixUi.border());
        FlintFixUi.sectionLabel(c, "PC CHECK", leftX + 8, bodyTop + 8);
        int rescanX = leftX + leftW - 8 - 42;
        FlintFixUi.actionButton(c, rescanX, bodyTop + 5, 42, 13, "RESCAN",
            FlintFixUi.inside(mouseX, mouseY, rescanX, bodyTop + 5, 42, 13), FlintFixUi.ButtonStyle.SECONDARY);

        int tierY = bodyTop + 25;
        FlintFixUi.roundedRaw(c, leftX + 8, tierY + 2, 6, 6, 3, tierColor());
        FlintFixUi.drawTrimmedExact(c, tier, leftX + 19, tierY, leftW - 27, 9, FlintFixUi.text(), true);
        int y = tierY + 13;
        if (cardH >= 140) {
            String[] lines = wrap(tierDescription, leftW - 16, 6, 2);
            for (String line : lines) {
                FlintFixFont.drawExact(c, line, leftX + 8, y, 6, FlintFixUi.muted(), false);
                y += 9;
            }
        }
        FlintFixUi.hairline(c, leftX + 8, y + 2, leftW - 16);
        y += 8;
        int statStep = Math.max(16, Math.min(22, (bodyTop + cardH - 6 - y) / 3));
        drawStat(c, "CPU THREADS", cpuThreads <= 0 ? "—" : Integer.toString(cpuThreads), y);
        drawStat(c, "JAVA MEMORY", javaMemoryGb <= 0 ? "—" : String.format(Locale.ROOT, "%.1f GB", javaMemoryGb), y + statStep);
        drawStat(c, "GRAPHICS", renderer, y + statStep * 2);
    }

    private void drawStat(GuiGraphics c, String label, String value, int y) {
        if (y + 16 > bodyTop + pcCardHeight()) return;
        FlintFixFont.drawExact(c, label, leftX + 8, y, 6, FlintFixUi.subtle(), true);
        FlintFixUi.drawTrimmedExact(c, value, leftX + 8, y + 8, leftW - 16, 7, FlintFixUi.text(), true);
    }

    private int tierColor() {
        return switch (tier) {
            case "Performance" -> 0xFFE2BE72;
            case "High headroom" -> 0xFF81C995;
            default -> FlintFixUi.accentBright();
        };
    }

    private void renderPresets(GuiGraphics c, int mouseX, int mouseY) {
        int y = bodyTop + pcCardHeight() + 8;
        FlintFixUi.sectionLabel(c, "QUICK PRESETS", leftX + 2, y);
        String recommended = recommendedPresetName();
        for (int i = 0; i < PRESET_NAMES.length; i++) {
            int by = presetButtonY(i);
            boolean hover = FlintFixUi.inside(mouseX, mouseY, leftX, by, leftW, 16);
            FlintFixUi.actionButton(c, leftX, by, leftW, 16, "", hover, FlintFixUi.ButtonStyle.SECONDARY);
            FlintFixFont.drawExact(c, PRESET_NAMES[i], leftX + 8, FlintFixFont.centeredY(by, 16, 7), 7,
                FlintFixUi.text(), true);
            if (PRESET_NAMES[i].equals(recommended)) {
                FlintFixUi.badge(c, leftX + leftW - 4, by + 3, "BEST FIT", true);
            }
        }
    }

    private int presetButtonY(int index) {
        return bodyTop + pcCardHeight() + 8 + 12 + index * 20;
    }

    private void renderSettings(GuiGraphics c, int mouseX, int mouseY) {
        FlintFixUi.surface(c, rightX, bodyTop, rightW, bodyBottom - bodyTop, FlintFixUi.panel(), FlintFixUi.border());
        renderTabs(c, mouseX, mouseY);

        Setting[] settings = currentSettings();
        int rowTop = rowTop();
        int step = rowStep(settings.length);
        float slide = ease(clamp01((System.currentTimeMillis() - tabChangedAt) / (float) TAB_ANIMATION_MS));
        FlintFixCompat.pushGui(c);
        FlintFixCompat.translateGui(c, (1.0f - slide) * 8.0f, 0.0f);
        for (int i = 0; i < settings.length; i++) {
            renderSettingRow(c, settings[i], rowTop + i * step, step - 4, mouseX, mouseY);
        }
        FlintFixCompat.popGui(c);
        if (slide < 1.0f) {
            // Fade the incoming rows in by covering them with the card color.
            c.fill(rightX + 1, rowTop - 2, rightX + rightW - 1, bodyBottom - 1,
                FlintFixUi.opacity(FlintFixUi.panel(), 1.0f - slide));
        }
    }

    private int tabsX() { return rightX + 8; }
    private int tabsY() { return bodyTop + 8; }
    private int tabsW() { return rightW - 16; }
    private int rowTop() { return bodyTop + 34; }

    private int rowStep(int count) {
        int available = bodyBottom - 6 - rowTop();
        return Math.max(20, Math.min(34, available / Math.max(1, count)));
    }

    private void renderTabs(GuiGraphics c, int mouseX, int mouseY) {
        int tx = tabsX(), ty = tabsY(), tw = tabsW();
        int segW = tw / 2;
        FlintFixUi.surface(c, tx, ty, tw, 18, FlintFixUi.bg(), FlintFixUi.border());
        float slideT = FlintFixUi.hoverProgress("video-tab-indicator", selectedTab == VISUAL_TAB);
        int indicatorX = tx + 2 + Math.round((segW - 2) * slideT);
        FlintFixUi.roundedRaw(c, indicatorX, ty + 2, segW - 2, 14, 2, FlintFixUi.accent());
        String[] labels = {"PERFORMANCE", "VISUALS"};
        for (int i = 0; i < 2; i++) {
            int segX = tx + i * segW;
            boolean selected = selectedTab == i;
            boolean hover = !selected && FlintFixUi.inside(mouseX, mouseY, segX, ty, segW, 18);
            int ink = selected ? FlintFixUi.onAccent() : (hover ? FlintFixUi.text() : FlintFixUi.muted());
            FlintFixFont.drawCenteredExact(c, labels[i], segX + segW / 2, FlintFixFont.centeredY(ty, 18, 6), 6, ink, true);
        }
    }

    private void renderSettingRow(GuiGraphics c, Setting setting, int y, int h, int mouseX, int mouseY) {
        int rowX = rightX + 8;
        int rowW = rightW - 16;
        boolean hover = FlintFixUi.inside(mouseX, mouseY, rowX, y, rowW, h);
        float hoverT = FlintFixUi.hoverProgress("video-row-" + setting.label, hover);
        float pulse = setting.label.equals(lastChangedLabel)
            ? 1.0f - clamp01((System.currentTimeMillis() - lastChangedAt) / 520.0f) : 0.0f;
        int fill = FlintFixUi.blendColors(FlintFixUi.card(), FlintFixUi.raised(), hoverT * 0.6f);
        fill = FlintFixUi.blendColors(fill, FlintFixUi.accent(), pulse * 0.18f);
        int edge = FlintFixUi.blendColors(FlintFixUi.border(), FlintFixUi.accent(), pulse);
        FlintFixUi.surface(c, rowX, y, rowW, h, fill, edge);

        String current = setting.current.get();
        if (setting.slider != null) {
            renderSliderRow(c, setting, rowX, y, rowW, h, current, mouseX, mouseY);
            return;
        }
        String suggested = setting.suggested == null ? null : setting.suggested.get();
        int controlW = setting.toggle ? 20 : STEP_BUTTON * 2 + VALUE_PILL_W + 6;
        int labelW = rowW - 16 - controlW - 6;
        boolean showHint = h >= 26;
        int labelY = showHint ? y + 6 : FlintFixFont.centeredY(y, h, 7);
        FlintFixUi.drawTrimmedExact(c, setting.label, rowX + 8, labelY, labelW, 7, FlintFixUi.text(), true);
        if (showHint) {
            int hintY = y + h - 11;
            if (suggested == null) {
                FlintFixUi.drawTrimmedExact(c, setting.note, rowX + 8, hintY, labelW, 6, FlintFixUi.subtle(), false);
            } else if (suggested.equals(current)) {
                FlintFixIcons.drawExact(c, "check", rowX + 8, hintY - 1, 7, FlintFixUi.accentBright());
                FlintFixUi.drawTrimmedExact(c, "Matches suggestion", rowX + 18, hintY, labelW - 10, 6,
                    FlintFixUi.muted(), false);
            } else {
                FlintFixUi.drawTrimmedExact(c, "Suggested  " + suggested, rowX + 8, hintY, labelW, 6,
                    FlintFixUi.subtle(), false);
            }
        }

        int right = rowX + rowW - 8;
        if (setting.toggle) {
            FlintFixUi.switchToggle(c, "video-" + setting.label, right - 20, y + (h - 11) / 2, "On".equals(current));
            return;
        }
        int buttonY = y + (h - STEP_BUTTON) / 2;
        int nextX = right - STEP_BUTTON;
        int pillX = nextX - 3 - VALUE_PILL_W;
        int backX = pillX - 3 - STEP_BUTTON;
        FlintFixUi.iconButton(c, "video-back-" + setting.label, backX, buttonY, STEP_BUTTON, "back",
            FlintFixUi.inside(mouseX, mouseY, backX, buttonY, STEP_BUTTON, STEP_BUTTON));
        FlintFixUi.iconButton(c, "video-next-" + setting.label, nextX, buttonY, STEP_BUTTON, "next",
            FlintFixUi.inside(mouseX, mouseY, nextX, buttonY, STEP_BUTTON, STEP_BUTTON));
        FlintFixUi.surface(c, pillX, buttonY, VALUE_PILL_W, STEP_BUTTON, FlintFixUi.bg(),
            FlintFixUi.blendColors(FlintFixUi.border(), FlintFixUi.accent(), pulse));
        String shown = FlintFixFont.trim(current, VALUE_PILL_W - 6, 7, true);
        FlintFixFont.drawCenteredExact(c, shown, pillX + VALUE_PILL_W / 2,
            FlintFixFont.centeredY(buttonY, STEP_BUTTON, 7), 7, FlintFixUi.accentBright(), true);
    }

    /** Label and value on top, full-width slider underneath; compact rows put the slider on the right. */
    private void renderSliderRow(GuiGraphics c, Setting setting, int rowX, int y, int rowW, int h, String current,
                                 int mouseX, int mouseY) {
        Slider slider = setting.slider;
        int[] track = sliderTrack(y, h);
        Setting[] visible = currentSettings();
        boolean dragging = draggingSlider >= 0 && draggingSlider < visible.length
            && visible[draggingSlider].label.equals(setting.label);
        if (h >= 26) {
            FlintFixUi.drawTrimmedExact(c, setting.label, rowX + 8, y + 5, rowW / 2, 7, FlintFixUi.text(), true);
            int valueW = FlintFixFont.width(current, 7, true);
            FlintFixFont.drawExact(c, current, rowX + rowW - 8 - valueW, y + 5, 7, FlintFixUi.accentBright(), true);
        } else {
            int valueW = FlintFixFont.width(current, 6, true);
            FlintFixUi.drawTrimmedExact(c, setting.label, rowX + 8, FlintFixFont.centeredY(y, h, 7),
                track[0] - rowX - 14 - valueW, 7, FlintFixUi.text(), true);
            FlintFixFont.drawExact(c, current, track[0] - 6 - valueW, FlintFixFont.centeredY(y, h, 6), 6,
                FlintFixUi.accentBright(), true);
        }
        if (slider.suggested != null) {
            float tick = (float) ((slider.suggested.getAsDouble() - slider.min) / (slider.max - slider.min));
            int tickX = track[0] + Math.round(track[2] * Math.max(0.0f, Math.min(1.0f, tick)));
            c.fill(tickX, track[1] - 3, tickX + 1, track[1] + 6, FlintFixUi.opacity(FlintFixUi.accentBright(), 0.55f));
        }
        float t = (float) ((slider.get.getAsDouble() - slider.min) / (slider.max - slider.min));
        boolean hover = FlintFixUi.inside(mouseX, mouseY, track[0] - 4, track[1] - 5, track[2] + 8, 13);
        FlintFixUi.slider(c, track[0], track[1], track[2], t, dragging || hover);
    }

    /** x, y and width of a slider track inside a settings row. */
    private int[] sliderTrack(int y, int h) {
        int rowX = rightX + 8;
        int rowW = rightW - 16;
        if (h >= 26) return new int[] {rowX + 10, y + h - 9, rowW - 20};
        int trackX = rowX + rowW / 2 + 6;
        return new int[] {trackX, y + h / 2 - 1, rowX + rowW - 10 - trackX};
    }

    private void setSliderFromMouse(Setting setting, double mouseX, int rowY, int rowH) {
        int[] track = sliderTrack(rowY, rowH);
        Slider slider = setting.slider;
        double t = Math.max(0.0, Math.min(1.0, (mouseX - track[0]) / Math.max(1.0, track[2])));
        double raw = slider.min + t * (slider.max - slider.min);
        double snapped = slider.min + Math.round((raw - slider.min) / slider.step) * slider.step;
        slider.set.accept(Math.max(slider.min, Math.min(slider.max, snapped)));
        lastChangedLabel = setting.label;
        lastChangedAt = System.currentTimeMillis();
        setStatus(setting.label + " set to " + setting.current.get() + ".");
    }

    private void renderFooter(GuiGraphics c, int mouseX, int mouseY) {
        int applyW = 122;
        int applyX = panelX + panelW - 14 - applyW;
        int backX = applyX - 6 - 52;
        FlintFixUi.drawTrimmedExact(c, status, panelX + 14, FlintFixFont.centeredY(footerY, 18, 6),
            backX - 10 - (panelX + 14), 6, FlintFixUi.subtle(), false);
        FlintFixUi.actionButton(c, backX, footerY, 52, 18, "BACK",
            FlintFixUi.inside(mouseX, mouseY, backX, footerY, 52, 18), FlintFixUi.ButtonStyle.SECONDARY);
        FlintFixUi.actionButton(c, applyX, footerY, applyW, 18, "APPLY RECOMMENDED",
            FlintFixUi.inside(mouseX, mouseY, applyX, footerY, applyW, 18), FlintFixUi.ButtonStyle.PRIMARY);
    }

    // ------------------------------------------------------------------
    // Settings
    // ------------------------------------------------------------------

    private Setting[] currentSettings() {
        return selectedTab == PERFORMANCE_TAB ? performanceSettings() : visualSettings();
    }

    private Setting[] performanceSettings() {
        return new Setting[] {
            slider("Render distance", () -> options().renderDistance().get() + " chunks",
                new Slider(2, 32, 1, () -> options().renderDistance().get(),
                    v -> options().renderDistance().set((int) v), () -> recommendation().renderDistance)),
            slider("Simulation distance", () -> options().simulationDistance().get() + " chunks",
                new Slider(5, 32, 1, () -> options().simulationDistance().get(),
                    v -> options().simulationDistance().set((int) v), () -> recommendation().simulationDistance)),
            new Setting("Graphics", () -> title(options().graphicsMode().get().name()),
                () -> title(recommendation().graphics.name()), this::cycleGraphics, false, null, null),
            slider("Max frame rate", () -> fpsLabel(options().framerateLimit().get()),
                new Slider(10, 260, 10, () -> options().framerateLimit().get(),
                    v -> options().framerateLimit().set((int) v), () -> recommendation().maxFps)),
            new Setting("Vertical sync", () -> onOff(options().enableVsync().get()),
                () -> onOff(recommendation().verticalSync), this::cycleVsync, true, null, null)
        };
    }

    private Setting[] visualSettings() {
        return new Setting[] {
            new Setting("Particles", () -> title(options().particles().get().name()),
                () -> title(recommendation().particles.name()), this::cycleParticles, false, null, null),
            new Setting("Clouds", () -> title(options().cloudStatus().get().name()),
                () -> title(recommendation().clouds.name()), this::cycleClouds, false, null, null),
            new Setting("Ambient occlusion", () -> onOff(options().ambientOcclusion().get()),
                () -> onOff(recommendation().ambientOcclusion), this::cycleAo, true, null, null),
            slider("Entity distance", () -> Math.round(options().entityDistanceScaling().get() * 100) + "%",
                new Slider(0.5, 5.0, 0.25, () -> options().entityDistanceScaling().get(),
                    v -> options().entityDistanceScaling().set(v), () -> recommendation().entityDistance)),
            slider("Field of view", () -> Integer.toString(options().fov().get()),
                new Slider(30, 110, 1, () -> options().fov().get(),
                    v -> options().fov().set((int) v), null)),
            slider("Brightness", this::brightnessLabel,
                new Slider(0.0, 1.0, 0.05, () -> options().gamma().get(),
                    v -> options().gamma().set(v), null)),
            new Setting("Custom sky", this::skyLabel, null, this::cycleSky, false, "FlintFix sky presets", null)
        };
    }

    private Setting slider(String label, Value current, Slider slider) {
        return new Setting(label, current, null, forward -> {
            double next = slider.get.getAsDouble() + (forward ? slider.step : -slider.step);
            slider.set.accept(Math.max(slider.min, Math.min(slider.max, next)));
        }, false, null, slider);
    }

    private String brightnessLabel() {
        double gamma = options().gamma().get();
        if (gamma <= 0.001) return "Moody";
        if (gamma >= 0.999) return "Bright";
        return Math.round(gamma * 100) + "%";
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
            case "Performance" -> preset("Performance");
            case "High headroom" -> new Recommendation(16, 10, ParticleStatus.DECREASED, CloudStatus.FAST,
                true, 1.0, 240, false, GraphicsStatus.FAST);
            default -> preset("Balanced");
        };
    }

    private static Recommendation preset(String name) {
        return switch (name) {
            case "Performance" -> new Recommendation(8, 5, ParticleStatus.MINIMAL, CloudStatus.OFF,
                false, 0.5, 120, false, GraphicsStatus.FAST);
            case "Quality" -> new Recommendation(16, 10, ParticleStatus.ALL, CloudStatus.FANCY,
                true, 1.0, 240, false, GraphicsStatus.FANCY);
            default -> new Recommendation(12, 8, ParticleStatus.DECREASED, CloudStatus.FAST,
                false, 0.75, 180, false, GraphicsStatus.FAST);
        };
    }

    private String recommendedPresetName() {
        return switch (tier) {
            case "Performance" -> "Performance";
            case "High headroom" -> "Quality";
            default -> "Balanced";
        };
    }

    private void apply(Recommendation r, String message) {
        Options options = options();
        options.renderDistance().set(r.renderDistance);
        options.simulationDistance().set(r.simulationDistance);
        options.graphicsMode().set(r.graphics);
        options.particles().set(r.particles);
        options.cloudStatus().set(r.clouds);
        options.ambientOcclusion().set(r.ambientOcclusion);
        options.entityDistanceScaling().set(r.entityDistance);
        options.framerateLimit().set(r.maxFps);
        options.enableVsync().set(r.verticalSync);
        options.save();
        setStatus(message);
        lastChangedLabel = "";
        lastChangedAt = System.currentTimeMillis();
    }

    private void setStatus(String value) {
        status = value;
    }

    private void cycleRenderDistance(boolean forward) {
        options().renderDistance().set(stepped(options().renderDistance().get(), 2, 32, forward ? 2 : -2));
    }

    private void cycleSimulationDistance(boolean forward) {
        options().simulationDistance().set(stepped(options().simulationDistance().get(), 5, 32, forward ? 1 : -1));
    }

    private void cycleGraphics(boolean forward) {
        // Keep Fabulous behind Minecraft's native confirmation flow; this page
        // intentionally cycles only the safe Fast/Fancy choices.
        GraphicsStatus[] values = {GraphicsStatus.FAST, GraphicsStatus.FANCY};
        options().graphicsMode().set(next(values, options().graphicsMode().get(), forward));
    }

    private void cycleParticles(boolean forward) {
        ParticleStatus[] values = {ParticleStatus.MINIMAL, ParticleStatus.DECREASED, ParticleStatus.ALL};
        options().particles().set(next(values, options().particles().get(), forward));
    }

    private void cycleClouds(boolean forward) {
        CloudStatus[] values = {CloudStatus.OFF, CloudStatus.FAST, CloudStatus.FANCY};
        options().cloudStatus().set(next(values, options().cloudStatus().get(), forward));
    }

    private void cycleAo(boolean ignored) {
        options().ambientOcclusion().set(!options().ambientOcclusion().get());
    }

    private void cycleEntityDistance(boolean forward) {
        double[] values = {0.5, 0.6, 0.7, 0.8, 0.9, 1.0, 1.25, 1.5};
        options().entityDistanceScaling().set(next(values, options().entityDistanceScaling().get(), forward));
    }

    private void cycleMaxFps(boolean forward) {
        int[] values = {30, 60, 90, 120, 144, 165, 180, 240, 260};
        options().framerateLimit().set(next(values, options().framerateLimit().get(), forward));
    }

    private void cycleVsync(boolean ignored) {
        options().enableVsync().set(!options().enableVsync().get());
    }

    private String skyLabel() {
        return FlintFixClient.CONFIG.skyEnabled ? FlintFixSky.preset().label() : "Off";
    }

    /** Off, then each preset in order. */
    private void cycleSky(boolean forward) {
        FlintFixSky.Preset[] presets = FlintFixSky.Preset.values();
        int index = FlintFixClient.CONFIG.skyEnabled ? FlintFixSky.preset().ordinal() + 1 : 0;
        index = Math.floorMod(index + (forward ? 1 : -1), presets.length + 1);
        FlintFixClient.CONFIG.skyEnabled = index > 0;
        if (index > 0) FlintFixClient.CONFIG.skyPreset = presets[index - 1].name();
        FlintFixClient.CONFIG.save();
    }

    private void changeSetting(Setting setting, boolean forward) {
        setting.change.accept(forward);
        lastChangedLabel = setting.label;
        lastChangedAt = System.currentTimeMillis();
        setStatus(setting.label + " set to " + setting.current.get() + ".");
    }

    private void selectTab(int tab) {
        if (selectedTab == tab) return;
        selectedTab = tab;
        draggingSlider = -1;
        tabChangedAt = System.currentTimeMillis();
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        layout();
        if (FlintFixUi.inside(mouseX, mouseY, closeX(), panelY + 13, 15, 15)) {
            onClose();
            return true;
        }
        int rescanX = leftX + leftW - 8 - 42;
        if (FlintFixUi.inside(mouseX, mouseY, rescanX, bodyTop + 5, 42, 13)) {
            scanHardware();
            return true;
        }
        for (int i = 0; i < PRESET_NAMES.length; i++) {
            if (FlintFixUi.inside(mouseX, mouseY, leftX, presetButtonY(i), leftW, 16)) {
                apply(preset(PRESET_NAMES[i]), PRESET_NAMES[i] + " preset applied.");
                return true;
            }
        }

        int applyW = 122;
        int applyX = panelX + panelW - 14 - applyW;
        int backX = applyX - 6 - 52;
        if (FlintFixUi.inside(mouseX, mouseY, backX, footerY, 52, 18)) {
            onClose();
            return true;
        }
        if (FlintFixUi.inside(mouseX, mouseY, applyX, footerY, applyW, 18)) {
            apply(recommendation(), "Recommended settings applied. Adjust anything above whenever you like.");
            return true;
        }

        int segW = tabsW() / 2;
        if (FlintFixUi.inside(mouseX, mouseY, tabsX(), tabsY(), segW, 18)) {
            selectTab(PERFORMANCE_TAB);
            return true;
        }
        if (FlintFixUi.inside(mouseX, mouseY, tabsX() + segW, tabsY(), segW, 18)) {
            selectTab(VISUAL_TAB);
            return true;
        }

        Setting[] settings = currentSettings();
        int index = rowAt(mouseX, mouseY, settings.length);
        if (index >= 0) {
            Setting setting = settings[index];
            if (setting.slider != null) {
                draggingSlider = index;
                int step = rowStep(settings.length);
                setSliderFromMouse(setting, mouseX, rowTop() + index * step, step - 4);
            } else if (setting.toggle) {
                changeSetting(setting, true);
            } else {
                int right = rightX + rightW - 16;
                int pillX = right - STEP_BUTTON - 3 - VALUE_PILL_W;
                boolean back = mouseX < pillX - 1 && mouseX >= pillX - 3 - STEP_BUTTON;
                changeSetting(setting, !back);
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && draggingSlider >= 0) {
            layout();
            Setting[] settings = currentSettings();
            if (draggingSlider < settings.length && settings[draggingSlider].slider != null) {
                int step = rowStep(settings.length);
                setSliderFromMouse(settings[draggingSlider], mouseX, rowTop() + draggingSlider * step, step - 4);
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingSlider >= 0) {
            draggingSlider = -1;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        layout();
        Setting[] settings = currentSettings();
        int index = rowAt(mouseX, mouseY, settings.length);
        if (index >= 0 && verticalAmount != 0) {
            changeSetting(settings[index], verticalAmount > 0);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private int rowAt(double mouseX, double mouseY, int count) {
        int step = rowStep(count);
        for (int i = 0; i < count; i++) {
            if (FlintFixUi.inside(mouseX, mouseY, rightX + 8, rowTop() + i * step, rightW - 16, step - 4)) return i;
        }
        return -1;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_TAB) {
            selectTab(selectedTab == PERFORMANCE_TAB ? VISUAL_TAB : PERFORMANCE_TAB);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (minecraft == null) return;
        minecraft.options.save();
        minecraft.setScreen(parent);
        // Since 1.21 a screen keeps its widgets when it is shown again, so the
        // vanilla Video Settings page would still display (and later save back)
        // the values from before this page changed them. Rebuild it.
        if (parent != null) ((ScreenInvoker) parent).flintfix$clearAndInit();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Options options() {
        return Minecraft.getInstance().options;
    }

    /** Greedy word wrap into at most maxLines lines; the last line is trimmed. */
    private static String[] wrap(String text, int maxWidth, int size, int maxLines) {
        String[] words = text.split(" ");
        java.util.List<String> lines = new java.util.ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (FlintFixFont.width(candidate, size, false) <= maxWidth || line.isEmpty()) {
                line.setLength(0);
                line.append(candidate);
            } else {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }
        if (!line.isEmpty()) lines.add(line.toString());
        if (lines.size() > maxLines) {
            String rest = String.join(" ", lines.subList(maxLines - 1, lines.size()));
            lines = new java.util.ArrayList<>(lines.subList(0, maxLines - 1));
            lines.add(rest);
        }
        for (int i = 0; i < lines.size(); i++) lines.set(i, FlintFixFont.trim(lines.get(i), maxWidth, size, false));
        return lines.toArray(new String[0]);
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static float ease(float value) {
        float t = clamp01(value);
        return t * t * (3.0f - 2.0f * t);
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

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record Recommendation(int renderDistance, int simulationDistance, ParticleStatus particles,
                                  CloudStatus clouds, boolean ambientOcclusion,
                                  double entityDistance, int maxFps, boolean verticalSync,
                                  GraphicsStatus graphics) {}

    /** A row on the settings card. suggested may be null, in which case note is shown instead. */
    private record Setting(String label, Value current, Value suggested, Change change,
                           boolean toggle, String note, Slider slider) {}

    /** Range, step and accessors for a slider row; suggested may be null. */
    private record Slider(double min, double max, double step, DoubleSupplier get, DoubleConsumer set,
                          DoubleSupplier suggested) {}
    @FunctionalInterface private interface Value { String get(); }
    @FunctionalInterface private interface Change { void accept(boolean forward); }
}
