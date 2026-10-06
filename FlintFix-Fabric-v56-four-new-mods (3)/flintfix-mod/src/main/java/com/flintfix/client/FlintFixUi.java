package com.flintfix.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.function.ToIntFunction;

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
        // Saturated pastel colors made for dark panels (greens, golds) wash out on
        // white; deepen them so they keep their meaning and stay readable.
        if (saturation >= 55 && light >= 150) return blendColors(color, 0xFF000000, 0.35f);
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
        if (activeTheme == FlintFixTheme.LIGHT) return hovered ? ACCENT : 0xFFB4BCC8;
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
        roundedRaw(c, x, y, w, h, r, themeSurface(color));
    }

    /** Rounded rectangle in the exact color given, for colors already taken from the active theme. */
    public static void roundedRaw(DrawContext c, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
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
        actionButton(c, x, y, w, h, label, hover, primary ? ButtonStyle.PRIMARY : ButtonStyle.SECONDARY);
    }

    // ---------------------------------------------------------------------
    // Theme tokens. Each follows the animated transition between palettes,
    // so components drawn with them recolor smoothly when the theme changes.
    // ---------------------------------------------------------------------

    private static int token(ToIntFunction<FlintFixTheme> part) {
        return blendColors(part.applyAsInt(previousTheme), part.applyAsInt(activeTheme), themeProgress());
    }

    public static int bg() { return token(FlintFixTheme::background); }
    public static int panel() { return token(FlintFixTheme::panel); }
    public static int card() { return token(FlintFixTheme::card); }
    public static int raised() { return token(FlintFixTheme::raised); }
    public static int border() { return token(FlintFixTheme::border); }
    public static int accent() { return token(FlintFixTheme::accent); }
    public static int accentBright() { return token(FlintFixTheme::accentBright); }
    public static int text() { return token(FlintFixTheme::text); }
    public static int muted() { return token(FlintFixTheme::muted); }

    /** Quieter than muted: placeholders, hints and footnotes. */
    public static int subtle() { return blendColors(muted(), card(), 0.38f); }

    /** Readable ink for text and icons placed on an accent fill. */
    public static int onAccent() { return luminance(accent()) > 140 ? bg() : 0xFFFFFFFF; }

    public static int danger() { return activeTheme == FlintFixTheme.LIGHT ? 0xFFB63A3F : 0xFFE5767C; }

    private static int luminance(int color) {
        return (((color >>> 16) & 0xFF) * 30 + ((color >>> 8) & 0xFF) * 59 + (color & 0xFF) * 11) / 100;
    }

    // ---------------------------------------------------------------------
    // Shared components. All of them draw with theme tokens directly.
    // ---------------------------------------------------------------------

    public enum ButtonStyle { PRIMARY, SECONDARY, DANGER }

    /** Bordered rounded surface: a 1px edge around a fill. */
    public static void surface(DrawContext c, int x, int y, int w, int h, int fill, int edge) {
        if (w <= 0 || h <= 0) return;
        roundedRaw(c, x, y, w, h, 3, edge);
        if (w > 2 && h > 2) roundedRaw(c, x + 1, y + 1, w - 2, h - 2, 2, fill);
    }

    /** Soft drop shadow that fades out around a panel. */
    public static void shadow(DrawContext c, int x, int y, int w, int h, float strength) {
        int[] spread = {12, 9, 6, 4, 2};
        int[] alpha = {8, 14, 22, 32, 48};
        for (int i = 0; i < spread.length; i++) {
            int s = spread[i];
            int a = Math.round(alpha[i] * Math.max(0.0f, Math.min(1.0f, strength)));
            roundedRaw(c, x - s, y - s + 2, w + s * 2, h + s * 2, 3, a << 24);
        }
    }

    /** Standard window: shadow, edge, background and a faint top highlight. */
    public static void panelFrame(DrawContext c, int x, int y, int w, int h) {
        boolean light = activeTheme == FlintFixTheme.LIGHT;
        shadow(c, x, y, w, h, light ? 0.6f : 1.0f);
        surface(c, x, y, w, h, bg(), border());
        if (!light) c.fill(x + 3, y + 1, x + w - 3, y + 2, 0x0CFFFFFF);
    }

    /** Dims the game behind a FlintFix screen. */
    public static void backdrop(DrawContext c, int width, int height, float amount) {
        c.fill(0, 0, width, height, opacity(0x70000000, amount));
    }

    /** Screen header: accent icon tile, title and an optional subtitle. */
    public static void header(DrawContext c, int x, int y, int maxWidth, String icon, String title, String subtitle) {
        surface(c, x, y, 22, 22, raised(), blendColors(border(), accent(), 0.35f));
        FlintFixIcons.drawExact(c, icon, x + 5, y + 5, 12, accentBright());
        int textW = Math.max(0, maxWidth - 29);
        FlintFixFont.drawExact(c, FlintFixFont.trim(title, textW, 9, true), x + 29, y + 1, 9, text(), true);
        if (subtitle != null && !subtitle.isEmpty()) {
            FlintFixFont.drawExact(c, FlintFixFont.trim(subtitle, textW, 6, false), x + 29, y + 13, 6, muted(), false);
        }
    }

    public static void sectionLabel(DrawContext c, String label, int x, int y) {
        FlintFixFont.drawExact(c, label, x, y, 6, subtle(), true);
    }

    /** Square button holding a single icon, e.g. close or back. */
    public static void iconButton(DrawContext c, String key, int x, int y, int size, String icon, boolean hover) {
        float t = hoverProgress("icon-button:" + key, hover);
        surface(c, x, y, size, size, blendColors(card(), raised(), t), blendColors(border(), accentBright(), t * 0.7f));
        int glyph = Math.max(6, size - 6);
        FlintFixIcons.drawExact(c, icon, x + (size - glyph) / 2, y + (size - glyph) / 2, glyph,
            blendColors(muted(), text(), t));
    }

    public static void actionButton(DrawContext c, int x, int y, int w, int h, String label, boolean hover,
                                    ButtonStyle style) {
        float t = hoverProgress("button:" + x + ":" + y, hover);
        int fill;
        int edge;
        int ink;
        switch (style) {
            case PRIMARY -> {
                fill = blendColors(accent(), accentBright(), t);
                edge = fill;
                ink = onAccent();
            }
            case DANGER -> {
                fill = blendColors(card(), blendColors(card(), danger(), 0.22f), t);
                edge = blendColors(blendColors(border(), danger(), 0.35f), danger(), t);
                ink = danger();
            }
            default -> {
                fill = blendColors(card(), raised(), t);
                edge = blendColors(border(), accentBright(), t * 0.7f);
                ink = text();
            }
        }
        surface(c, x, y, w, h, fill, edge);
        int size = h >= 18 ? 7 : 6;
        String shown = FlintFixFont.trim(label, w - 6, size, true);
        FlintFixFont.drawCenteredExact(c, shown, x + w / 2, FlintFixFont.centeredY(y, h, size), size, ink, true);
    }

    /** Wide button with a leading icon, used for navigation entries. */
    public static void navButton(DrawContext c, int x, int y, int w, int h, String icon, String label, boolean hover) {
        float t = hoverProgress("nav:" + label, hover);
        surface(c, x, y, w, h, blendColors(card(), raised(), t), blendColors(border(), accentBright(), t * 0.6f));
        int glyph = Math.min(9, h - 6);
        FlintFixIcons.drawExact(c, icon, x + 5, y + (h - glyph) / 2, glyph, blendColors(muted(), accentBright(), t));
        FlintFixFont.drawExact(c, FlintFixFont.trim(label, w - glyph - 19, 6, true), x + glyph + 10,
            FlintFixFont.centeredY(y, h, 6), 6, text(), true);
        FlintFixIcons.drawExact(c, "next", x + w - 10, y + (h - 6) / 2, 6, opacity(muted(), 0.5f + 0.5f * t));
    }

    /** Animated on/off switch, 20x11. The key keeps its animation independent of others. */
    public static void switchToggle(DrawContext c, String key, int x, int y, boolean on) {
        float t = hoverProgress("switch:" + key, on);
        roundedRaw(c, x, y, 20, 11, 3, blendColors(raised(), accent(), t));
        int knobX = Math.round(x + 2 + 9 * t);
        roundedRaw(c, knobX, y + 2, 7, 7, 3, blendColors(muted(), onAccent(), t));
    }

    /** Horizontal slider; t is the 0..1 position of the knob. */
    public static void slider(DrawContext c, int x, int y, int w, float t, boolean active) {
        t = Math.max(0.0f, Math.min(1.0f, t));
        roundedRaw(c, x, y, w, 3, 1, raised());
        int fillW = Math.round(w * t);
        if (fillW > 0) roundedRaw(c, x, y, fillW, 3, 1, accent());
        int knobX = x + fillW;
        roundedRaw(c, knobX - 4, y - 4, 9, 11, 3, bg());
        roundedRaw(c, knobX - 3, y - 3, 7, 9, 3, active ? accentBright() : text());
    }

    /** Single-line search input with a leading icon and a blinking caret. */
    public static void searchField(DrawContext c, int x, int y, int w, int h, String value, String placeholder,
                                   boolean focused) {
        surface(c, x, y, w, h, focused ? panel() : bg(), focused ? accent() : border());
        int glyph = Math.min(7, h - 6);
        FlintFixIcons.drawExact(c, "search", x + 5, y + (h - glyph) / 2, glyph, focused ? text() : muted());
        int textX = x + glyph + 9;
        int maxW = Math.max(0, x + w - 5 - textX);
        int textY = FlintFixFont.centeredY(y, h, 6);
        boolean empty = value == null || value.isEmpty();
        FlintFixFont.drawExact(c, FlintFixFont.trim(empty ? placeholder : value, maxW, 6, false),
            textX, textY, 6, empty ? subtle() : text(), false);
        if (focused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int caretX = textX + Math.min(maxW, empty ? 0 : FlintFixFont.width(value, 6, false));
            c.fill(caretX, textY, caretX + 1, textY + 6, text());
        }
    }

    /** Small status pill ending at rightX. Returns its width. */
    public static int badge(DrawContext c, int rightX, int y, String label, boolean on) {
        int w = FlintFixFont.width(label, 6, true) + 8;
        int x = rightX - w;
        roundedRaw(c, x, y, w, 10, 3, on ? blendColors(card(), accent(), 0.28f) : raised());
        FlintFixFont.drawCenteredExact(c, label, x + w / 2, FlintFixFont.centeredY(y, 10, 6), 6,
            on ? accentBright() : muted(), true);
        return w;
    }

    /** Selection/hover highlight for list rows, with an accent marker when selected. */
    public static void selectableRow(DrawContext c, String key, int x, int y, int w, int h,
                                     boolean selected, boolean hover) {
        float t = hoverProgress("row:" + key, hover || selected);
        if (t > 0.01f) {
            int fill = selected ? raised() : blendColors(card(), raised(), 0.5f);
            roundedRaw(c, x, y, w, h, 3, opacity(fill, t));
        }
        if (selected) roundedRaw(c, x + 1, y + 3, 2, h - 6, 1, accent());
    }

    public static void scrollbar(DrawContext c, int x, int top, int trackH, int scroll, int maxScroll) {
        if (maxScroll <= 0 || trackH <= 0) return;
        int thumbH = Math.min(trackH, Math.max(14, trackH * trackH / (trackH + maxScroll)));
        int thumbY = top + (trackH - thumbH) * scroll / maxScroll;
        roundedRaw(c, x, top, 2, trackH, 1, opacity(raised(), 0.7f));
        roundedRaw(c, x, thumbY, 2, thumbH, 1, muted());
    }

    public static void hairline(DrawContext c, int x, int y, int w) {
        c.fill(x, y, x + w, y + 1, opacity(border(), 0.7f));
    }

    public static void drawTrimmed(DrawContext c, String text, int x, int y, int maxWidth, int size, int color, boolean bold) {
        FlintFixFont.draw(c, FlintFixFont.trim(text, maxWidth, size, bold), x, y, size, color, bold);
    }

    public static void drawTrimmedExact(DrawContext c, String text, int x, int y, int maxWidth, int size, int color, boolean bold) {
        FlintFixFont.drawExact(c, FlintFixFont.trim(text, maxWidth, size, bold), x, y, size, color, bold);
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
