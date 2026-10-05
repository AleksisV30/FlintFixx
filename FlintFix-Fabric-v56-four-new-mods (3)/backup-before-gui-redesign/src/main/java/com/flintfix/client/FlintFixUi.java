package com.flintfix.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public final class FlintFixUi {
    public static int BG = 0xFF10151D;
    public static int PANEL = 0xFF171E28;
    public static int CARD = 0xFF1E2024;
    public static int CARD_HOVER = 0xFF303239;
    public static int BORDER = 0xFF454545;
    public static int BORDER_SOFT = 0x80363636;
    public static int ACCENT_MUTED = 0xFF808080;
    public static int ACCENT_BRIGHT = 0xFFD4D4D4;
    public static int ACCENT = 0xFFB0B0B0;
    public static int ACCENT_SOFT = 0xFF444444;
    private static FlintFixTheme activeTheme = FlintFixTheme.GRAPHITE;
    private static FlintFixTheme previousTheme = FlintFixTheme.GRAPHITE;
    private static long themeTransitionStartedAt;
    private static boolean themeInitialized;
    private static final int THEME_TRANSITION_MS = 300;
    private static final Map<String, HoverMotion> HOVER_MOTIONS = new HashMap<>();

    private static final Identifier CORNER_TL = Identifier.of("flintfix", "textures/gui/corner_tl.png");
    private static final Identifier CORNER_TR = Identifier.of("flintfix", "textures/gui/corner_tr.png");
    private static final Identifier CORNER_BL = Identifier.of("flintfix", "textures/gui/corner_bl.png");
    private static final Identifier CORNER_BR = Identifier.of("flintfix", "textures/gui/corner_br.png");
    private static final int CORNER_TEX = 64;
    private static boolean cornerFilteringApplied;

    private FlintFixUi() {}

    public static FlintFixTheme activeTheme() {
        return activeTheme;
    }

    public static void applyTheme() {
        FlintFixTheme nextTheme = FlintFixClient.CONFIG == null
            ? FlintFixTheme.GRAPHITE : FlintFixTheme.fromId(FlintFixClient.CONFIG.theme);
        if (!themeInitialized) {
            activeTheme = nextTheme;
            previousTheme = nextTheme;
            themeInitialized = true;
        } else if (nextTheme != activeTheme) {
            previousTheme = activeTheme;
            activeTheme = nextTheme;
            themeTransitionStartedAt = System.currentTimeMillis();
        }
        BG = activeTheme.background();
        PANEL = activeTheme.panel();
        CARD = activeTheme.card();
        CARD_HOVER = activeTheme.raised();
        BORDER = activeTheme.border();
        BORDER_SOFT = (0x80 << 24) | (activeTheme.border() & 0x00FFFFFF);
        ACCENT_MUTED = activeTheme.muted();
        ACCENT_BRIGHT = activeTheme.accentBright();
        ACCENT = activeTheme.accent();
        ACCENT_SOFT = activeTheme.raised();
    }

    /** Recolors low-chroma panel fills while keeping state colors intact. */
    private static int themeSurface(int color) {
        int start = surfaceForTheme(color, previousTheme);
        int end = surfaceForTheme(color, activeTheme);
        return blendColors(start, end, themeProgress());
    }

    private static int surfaceForTheme(int color, FlintFixTheme theme) {
        if (theme == FlintFixTheme.GRAPHITE) return color;
        int alpha = (color >>> 24) & 0xFF;
        int red = (color >>> 16) & 0xFF;
        int green = (color >>> 8) & 0xFF;
        int blue = color & 0xFF;
        int max = Math.max(red, Math.max(green, blue));
        int min = Math.min(red, Math.min(green, blue));
        if (max - min > 48) return color;
        int light = (red * 30 + green * 59 + blue * 11) / 100;
        int mapped;
        if (light < 24) mapped = theme.background();
        else if (light < 42) mapped = theme.panel();
        else if (light < 58) mapped = theme.card();
        else if (light < 78) mapped = theme.raised();
        else if (light < 120) mapped = theme.border();
        else return color;
        return (alpha << 24) | (mapped & 0x00FFFFFF);
    }

    public static int themedText(int color) {
        return blendColors(textForTheme(color, previousTheme), textForTheme(color, activeTheme), themeProgress());
    }

    private static int textForTheme(int color, FlintFixTheme theme) {
        if (theme != FlintFixTheme.LIGHT) return color;
        int alpha = (color >>> 24) & 0xFF;
        int red = (color >>> 16) & 0xFF;
        int green = (color >>> 8) & 0xFF;
        int blue = color & 0xFF;
        int light = (red * 30 + green * 59 + blue * 11) / 100;
        int saturation = Math.max(red, Math.max(green, blue)) - Math.min(red, Math.min(green, blue));
        if (light >= 190) return (alpha << 24) | (theme.text() & 0x00FFFFFF);
        if (saturation < 55 && light >= 105) return (alpha << 24) | (theme.muted() & 0x00FFFFFF);
        return color;
    }

    private static float themeProgress() {
        if (themeTransitionStartedAt == 0L) return 1.0f;
        float t = Math.min(1.0f, (System.currentTimeMillis() - themeTransitionStartedAt) / (float) THEME_TRANSITION_MS);
        return t * t * (3.0f - 2.0f * t);
    }

    public static int blendColors(int from, int to, float amount) {
        float t = Math.max(0.0f, Math.min(1.0f, amount));
        int a = Math.round(((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * t);
        int r = Math.round(((from >>> 16) & 0xFF) + (((to >>> 16) & 0xFF) - ((from >>> 16) & 0xFF)) * t);
        int g = Math.round(((from >>> 8) & 0xFF) + (((to >>> 8) & 0xFF) - ((from >>> 8) & 0xFF)) * t);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** Smooth hover value shared by small rows and controls. */
    public static float hoverProgress(String key, boolean hovered) {
        long now = System.currentTimeMillis();
        HoverMotion motion = HOVER_MOTIONS.computeIfAbsent(key, ignored -> new HoverMotion(now));
        long elapsed = Math.max(0L, Math.min(80L, now - motion.updatedAt));
        motion.updatedAt = now;
        float target = hovered ? 1.0f : 0.0f;
        float step = Math.min(1.0f, elapsed / (hovered ? 105.0f : 85.0f));
        motion.value += (target - motion.value) * step;
        if (Math.abs(target - motion.value) < 0.01f) motion.value = target;
        return motion.value;
    }

    public static int interactiveBorder(boolean hovered) {
        if (activeTheme == FlintFixTheme.LIGHT) return hovered ? 0xFF151A20 : 0xFF616B76;
        return hovered ? ACCENT_BRIGHT : BORDER;
    }

    public static float openProgress(long openedAt) {
        float t = Math.min(1.0f, Math.max(0.0f, (System.currentTimeMillis() - openedAt) / 220.0f));
        return 1.0f - (float) Math.pow(1.0f - t, 3.0);
    }

    public static int opacity(int color, float amount) {
        int original = (color >>> 24) & 0xFF;
        int adjusted = Math.round(original * Math.max(0.0f, Math.min(1.0f, amount)));
        return (color & 0x00FFFFFF) | (adjusted << 24);
    }

    public static void pushPanelIntro(DrawContext c, int x, int y, int w, int h, float progress) {
        float scale = 0.975f + 0.025f * progress;
        float cx = x + w / 2.0f;
        float cy = y + h / 2.0f;
        c.getMatrices().push();
        c.getMatrices().translate(cx, cy + (1.0f - progress) * 4.0f, 0);
        c.getMatrices().scale(scale, scale, 1.0f);
        c.getMatrices().translate(-cx, -cy, 0);
    }

    public static void finishPanelIntro(DrawContext c, int x, int y, int w, int h, float progress) {
        c.fill(x, y, x + w, y + h, opacity(0xFF000000, (1.0f - progress) * 0.28f));
        c.getMatrices().pop();
    }

    public static void outlinedBox(DrawContext c, int x, int y, int w, int h, int radius,
                                   int borderColor, int fillColor) {
        if (w <= 0 || h <= 0) return;
        c.fill(x, y, x + w, y + h, borderColor);
        if (w > 2 && h > 2) rounded(c, x + 1, y + 1, w - 2, h - 2, Math.max(0, radius - 1), fillColor);
    }

    /** Anti-aliased rounded rectangle using filtered corner masks. */
    public static void rounded(DrawContext c, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
        color = themeSurface(color);
        // FlintFix uses a crisp, near-square style across all Minecraft screens.
        r = Math.max(0, Math.min(Math.min(r, 3), Math.min(w, h) / 2));
        if (r <= 1) {
            c.fill(x, y, x + w, y + h, color);
            return;
        }

        ensureCornerFiltering();
        // Keep the translucent shape from darkening where fill strips overlap:
        // top, middle, and bottom regions meet edge-to-edge and are each painted once.
        c.fill(x + r, y, x + w - r, y + r, color);
        c.fill(x, y + r, x + w, y + h - r, color);
        c.fill(x + r, y + h - r, x + w - r, y + h, color);

        tint(color);
        try {
            c.drawTexture(CORNER_TL, x, y, r, r, 0, 0, CORNER_TEX, CORNER_TEX, CORNER_TEX, CORNER_TEX);
            c.drawTexture(CORNER_TR, x + w - r, y, r, r, 0, 0, CORNER_TEX, CORNER_TEX, CORNER_TEX, CORNER_TEX);
            c.drawTexture(CORNER_BL, x, y + h - r, r, r, 0, 0, CORNER_TEX, CORNER_TEX, CORNER_TEX, CORNER_TEX);
            c.drawTexture(CORNER_BR, x + w - r, y + h - r, r, r, 0, 0, CORNER_TEX, CORNER_TEX, CORNER_TEX, CORNER_TEX);
        } finally {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }
    }

    public static void glass(DrawContext c, int x, int y, int w, int h, int r, int fill) {
        rounded(c, x - 1, y - 1, w + 2, h + 2, r + 1, BORDER_SOFT);
        rounded(c, x, y, w, h, r, fill);
    }

    /** Soft outer edge that fades into the game behind the panel. */
    public static void fadeOutline(DrawContext c, int x, int y, int w, int h, int r, int color) {
        // Use several low-opacity rings so the edge dissolves gradually instead
        // of reading as a few hard, stacked outlines.
        int[] spread = {12, 10, 8, 6, 5, 4, 3, 2, 1};
        int[] opacity = {3, 5, 8, 12, 18, 25, 35, 48, 64};
        int strength = (color >>> 24) & 0xFF;
        for (int i = 0; i < spread.length; i++) {
            int edge = spread[i];
            int layerAlpha = opacity[i] * strength / 255;
            int fill = (layerAlpha << 24) | (color & 0x00FFFFFF);
            rounded(c, x - edge, y - edge, w + edge * 2, h + edge * 2, r + edge, fill);
        }
    }

    public static void glowPanel(DrawContext c, int x, int y, int w, int h, int r, int fill) {
        rounded(c, x - 6, y - 6, w + 12, h + 12, r + 6, 0x11000000);
        rounded(c, x - 4, y - 4, w + 8, h + 8, r + 4, 0x181D1D1D);
        rounded(c, x - 2, y - 2, w + 4, h + 4, r + 2, 0x55444444);
        rounded(c, x, y, w, h, r, fill);
    }

    public static void divider(DrawContext c, int x, int y, int w) {
        int rgb = activeTheme == FlintFixTheme.GRAPHITE
            ? 0x00484848 : (activeTheme.divider() & 0x00FFFFFF);
        c.fill(x, y, x + w, y + 1, 0x55000000 | rgb);
    }

    public static void button(DrawContext c, int x, int y, int w, int h, String label, boolean hover, boolean primary) {
        int fill = primary ? (hover ? 0xFF696969 : 0xFF4D4D4D) : (hover ? 0xFF2D2D2D : 0xFF1A1A1A);
        if (primary && hover) rounded(c, x - 2, y - 2, w + 4, h + 4, 8, 0x34B0B0B0);
        glass(c, x, y, w, h, 7, fill);
        FlintFixFont.drawCentered(c, label, x + w / 2, y + (h - 13) / 2 - 1, 13, 0xFFFFFFFF, true);
    }

    public static void toggle(DrawContext c, int x, int y, boolean enabled) {
        rounded(c, x, y, 38, 18, 9, enabled ? 0xFF666666 : 0xFF383838);
        if (enabled) rounded(c, x + 1, y + 1, 36, 16, 8, 0xFF828282);
        int knobX = enabled ? x + 22 : x + 3;
        rounded(c, knobX, y + 3, 13, 12, 6, 0xFFF7F4F9);
    }

    public static void iconBox(DrawContext c, int x, int y, String glyph, boolean active) {
        rounded(c, x, y, 28, 28, 8, active ? 0x68444B55 : 0x39232932);
        FlintFixFont.drawCentered(c, glyph, x + 14, y + 6, 14, active ? 0xFFE0E4EA : 0xFF9AA6B6, true);
    }

    /** Small reference-style toggle for compact FlintFix panels. */
    public static void compactToggle(DrawContext c, int x, int y, boolean enabled) {
        rounded(c, x, y, 28, 14, 7, enabled ? 0xFF666666 : 0xFF383838);
        if (enabled) rounded(c, x + 1, y + 1, 26, 12, 6, 0xFF828282);
        int knobX = enabled ? x + 16 : x + 2;
        rounded(c, knobX, y + 2, 10, 10, 5, 0xFFF8F5FA);
    }

    public static void microToggle(DrawContext c, int x, int y, boolean enabled, boolean available) {
        int track = !available ? 0xFF2B3038 : (enabled ? 0xFF42644A : 0xFF654641);
        rounded(c, x, y, 18, 10, 5, track);
        int knobX = enabled && available ? x + 10 : x + 2;
        rounded(c, knobX, y + 2, 6, 6, 3, available ? 0xFFF5F1F7 : 0xFF746E79);
    }

    /** Compact settings action using a static Lucide sliders icon. */
    public static void moduleSettingsButton(DrawContext c, int x, int y, boolean hover) {
        rounded(c, x - 1, y - 1, 8, 8, 3, hover ? 0xFF747474 : 0xFF4D4D4D);
        rounded(c, x, y, 6, 6, 3, hover ? 0xFF5C5C5C : 0xFF363636);
        FlintFixIcons.draw(c, "settings", x, y, 6, hover ? 0xFFFFFFFF : 0xFFD0D5DC);
    }

    /** Compact module icon tile. */
    public static void compactIcon(DrawContext c, int x, int y, String glyph, boolean active) {
        rounded(c, x, y, 20, 18, 6, active ? 0x6C484848 : 0x3225222A);
        FlintFixFont.drawCentered(c, glyph, x + 10, y + 5, 8, active ? 0xFFE0E4EA : 0xFFA29AA6, true);
    }

    /** Compact button used by the smaller v15 layouts. */
    public static void compactButton(DrawContext c, int x, int y, int w, int h, String label, boolean hover, boolean primary) {
        float hoverT = hoverProgress("button:" + x + ":" + y, hover);
        int base = primary ? 0xFF4D4D4D : 0xFF1A1A1A;
        int lit = primary ? 0xFF696969 : 0xFF2D2D2D;
        int fill = blendColors(base, lit, hoverT);
        if (primary && hoverT > 0.01f) rounded(c, x - 1, y - 1, w + 2, h + 2, 3,
            (Math.round(26 * hoverT) << 24) | (ACCENT & 0x00FFFFFF));
        int edge = interactiveBorder(hoverT > 0.25f);
        outlinedBox(c, x, y, w, h, 3, edge, fill);
        int textSize = 5;
        int textY = y + Math.max(0, Math.round((h - textSize) / 2.0f));
        FlintFixFont.drawCentered(c, label, x + w / 2, textY, textSize, 0xFFFFFFFF, true);
    }

    public static void drawTrimmed(DrawContext c, String text, int x, int y, int maxWidth, int size, int color, boolean bold) {
        FlintFixFont.draw(c, FlintFixFont.trim(text, maxWidth, size, bold), x, y, size, color, bold);
    }

    public static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static void ensureCornerFiltering() {
        if (cornerFilteringApplied) return;
        var textures = MinecraftClient.getInstance().getTextureManager();
        textures.getTexture(CORNER_TL).setFilter(true, false);
        textures.getTexture(CORNER_TR).setFilter(true, false);
        textures.getTexture(CORNER_BL).setFilter(true, false);
        textures.getTexture(CORNER_BR).setFilter(true, false);
        cornerFilteringApplied = true;
    }

    private static void tint(int color) {
        float a = ((color >>> 24) & 0xFF) / 255.0f;
        float r = ((color >>> 16) & 0xFF) / 255.0f;
        float g = ((color >>> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(r, g, b, a);
    }

    private static final class HoverMotion {
        float value;
        long updatedAt;

        HoverMotion(long updatedAt) {
            this.updatedAt = updatedAt;
        }
    }
}
