package com.flintfix.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Small-size text helper for the FlintFix screens.
 *
 * Use the bundled rounded sans-serif for FlintFix labels, with Minecraft's
 * built-in font as the resource-level fallback.
 */
public final class FlintFixFont {
    private FlintFixFont() {}

    public static int width(String text, int size, boolean bold) {
        if (text == null || text.isEmpty() || size <= 0) return 0;
        TextRenderer renderer = MinecraftClient.getInstance().textRenderer;
        return Math.round(renderer.getWidth(styled(text, bold)) * scaleFor(renderer, size));
    }

    public static void draw(DrawContext context, String text, int x, int y, int size, int color, boolean bold) {
        drawExact(context, text, x, y, size, FlintFixUi.themedText(color), bold);
    }

    /** Draws with the given color as-is, for colors already taken from the active theme. */
    public static void drawExact(DrawContext context, String text, int x, int y, int size, int color, boolean bold) {
        if (text == null || text.isEmpty() || size <= 0) return;

        TextRenderer renderer = MinecraftClient.getInstance().textRenderer;
        float scale = scaleFor(renderer, size);
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0.0f);
        context.getMatrices().scale(scale, scale, 1.0f);
        // Render once at the intended scale. A sub-pixel duplicate pass
        // softened every regular label and made this screen look out of focus.
        context.drawText(renderer, styled(text, bold), 0, 0, color, false);
        context.getMatrices().pop();
    }

    public static void drawCentered(DrawContext context, String text, int centerX, int y, int size,
                                    int color, boolean bold) {
        draw(context, text, centerX - width(text, size, bold) / 2, y, size, color, bold);
    }

    public static void drawCenteredExact(DrawContext context, String text, int centerX, int y, int size,
                                         int color, boolean bold) {
        drawExact(context, text, centerX - width(text, size, bold) / 2, y, size, color, bold);
    }

    /** Top y that visually centers a line of capitals of the given size inside a box. */
    public static int centeredY(int top, int height, int size) {
        float lineHeight = 9.0f * snapToPixels(Math.max(0.66f, size / 9.0f));
        return top + Math.round(height / 2.0f - lineHeight * 0.445f);
    }

    /** Draws the same bundled UI font without shrinking the glyph atlas below 1:1. */
    public static void drawCrisp(DrawContext context, String text, int x, int y, int size, int color, boolean bold) {
        if (text == null || text.isEmpty() || size <= 0) return;
        color = FlintFixUi.themedText(color);
        TextRenderer renderer = MinecraftClient.getInstance().textRenderer;
        float scale = crispScaleFor(renderer, size);
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0.0f);
        context.getMatrices().scale(scale, scale, 1.0f);
        context.drawText(renderer, styled(text, bold), 0, 0, color, false);
        context.getMatrices().pop();
    }

    public static int widthCrisp(String text, int size, boolean bold) {
        if (text == null || text.isEmpty() || size <= 0) return 0;
        TextRenderer renderer = MinecraftClient.getInstance().textRenderer;
        return Math.round(renderer.getWidth(styled(text, bold)) * crispScaleFor(renderer, size));
    }

    public static void drawCenteredCrisp(DrawContext context, String text, int centerX, int y, int size,
                                         int color, boolean bold) {
        drawCrisp(context, text, centerX - widthCrisp(text, size, bold) / 2, y, size, color, bold);
    }

    public static void drawTrimmedCrisp(DrawContext context, String text, int x, int y, int maxWidth,
                                        int size, int color, boolean bold) {
        drawCrisp(context, trimCrisp(text, maxWidth, size, bold), x, y, size, color, bold);
    }

    public static String trimCrisp(String text, int maxWidth, int size, boolean bold) {
        if (text == null || maxWidth <= 0) return "";
        if (widthCrisp(text, size, bold) <= maxWidth) return text;
        String ellipsis = "...";
        int ellipsisWidth = widthCrisp(ellipsis, size, bold);
        if (ellipsisWidth >= maxWidth) return "";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            String candidate = out.toString() + text.charAt(i);
            if (widthCrisp(candidate, size, bold) + ellipsisWidth > maxWidth) break;
            out.append(text.charAt(i));
        }
        return out + ellipsis;
    }

    public static String trim(String text, int maxWidth, int size, boolean bold) {
        if (text == null || maxWidth <= 0) return "";
        if (width(text, size, bold) <= maxWidth) return text;

        String ellipsis = "...";
        int ellipsisWidth = width(ellipsis, size, bold);
        if (ellipsisWidth >= maxWidth) return "";

        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            String candidate = out.toString() + text.charAt(i);
            if (width(candidate, size, bold) + ellipsisWidth > maxWidth) break;
            out.append(text.charAt(i));
        }
        return out + ellipsis;
    }

    private static MutableText styled(String value, boolean bold) {
        MutableText text = Text.literal(value);
        // Use a real bold face instead of Minecraft synthesizing bold from
        // the regular TTF. Synthetic bold made small UI labels look doubled
        // and fuzzy in the atlas.
        text.setStyle(Style.EMPTY.withFont(Identifier.of("flintfix", bold ? "ui_bold" : "ui")));
        return text;
    }

    private static float crispScaleFor(TextRenderer renderer, int size) {
        return Math.max(1.0f, size / (float) Math.max(1, renderer.fontHeight));
    }

    private static float scaleFor(TextRenderer renderer, int size) {
        // Avoid crushing the smallest labels into 2–4px glyphs. The TTF atlas
        // stays legible at a six-pixel minimum; width() uses the same scale.
        return snapToPixels(Math.max(0.66f, size / (float) Math.max(1, renderer.fontHeight)));
    }

    /**
     * Rounds a text scale so each font unit covers a whole number of real
     * screen pixels. Fractional scales sample the glyph atlas unevenly, which
     * is what made small labels look soft with uneven stroke widths.
     */
    private static float snapToPixels(float scale) {
        double guiScale = MinecraftClient.getInstance().getWindow().getScaleFactor();
        if (guiScale <= 0.0) return scale;
        double physical = scale * guiScale;
        double snapped = Math.max(1.0, Math.round(physical));
        // Never shrink a label by more than a fifth; round up instead.
        if (snapped < physical * 0.8) snapped = Math.ceil(physical);
        return (float) (snapped / guiScale);
    }
}
