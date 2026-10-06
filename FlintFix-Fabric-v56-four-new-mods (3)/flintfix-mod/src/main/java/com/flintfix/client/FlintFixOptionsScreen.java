package com.flintfix.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * Settings page built from a list of options (switches, sliders, choices,
 * color swatches and info notes), so each module only describes its settings.
 * Changes save immediately.
 */
public final class FlintFixOptionsScreen extends FlintFixScreen {
    public sealed interface Option permits Toggle, Slider, Choice, ColorPick, Info, Action {}

    public record Toggle(String label, BooleanSupplier get, Consumer<Boolean> set) implements Option {}

    public record Slider(String label, float min, float max, float step, DoubleSupplier get, Consumer<Float> set,
                         Function<Float, String> format) implements Option {}

    public record Choice(String label, String[] values, IntSupplier get, IntConsumer set) implements Option {}

    public record ColorPick(String label, IntSupplier get, IntConsumer set) implements Option {}

    public record Info(String title, String text) implements Option {}

    /** A row with a button on the right, e.g. to open another screen. */
    public record Action(String label, String button, Runnable run) implements Option {}

    /** Swatches offered by every color option. */
    public static final int[] SWATCHES = {
        0xFFFFFFFF, 0xFF000000, 0xFFFF5C5C, 0xFFFFA14A, 0xFFFFE15C, 0xFF6FDC9A,
        0xFF5CFFB0, 0xFF5CC8FF, 0xFF6F86FF, 0xFFB57BFF, 0xFFFF7AD9
    };

    private static final int MAX_W = 320;
    private static final int MAX_H = 224;
    private static final int ROW_GAP = 5;

    private final Screen parent;
    private final String icon;
    private final String heading;
    private final String description;
    private final List<Option> options;
    private final Runnable reset;
    private final boolean hudModule;
    private final long openedAt = System.currentTimeMillis();
    private int x, y, w, h, listTop, listBottom, rowX, rowW;
    private int scroll, maxScroll;
    private Slider dragging;

    public FlintFixOptionsScreen(Screen parent, String icon, String heading, String description,
                                 List<Option> options, Runnable reset, boolean hudModule) {
        super(Text.literal(heading));
        this.parent = parent;
        this.icon = icon;
        this.heading = heading;
        this.description = description;
        this.options = options;
        this.reset = reset;
        this.hudModule = hudModule;
    }

    private void layout() {
        w = Math.min(MAX_W, Math.max(1, width - 24));
        h = Math.min(MAX_H, Math.max(1, height - 24));
        x = (width - w) / 2;
        y = (height - h) / 2;
        rowX = x + 14;
        rowW = w - 28 - 6;
        listTop = y + 50;
        listBottom = y + h - 32;
        int content = 0;
        for (Option option : options) content += rowHeight(option) + ROW_GAP;
        maxScroll = Math.max(0, content - ROW_GAP - (listBottom - listTop));
        scroll = Math.max(0, Math.min(maxScroll, scroll));
    }

    private static int rowHeight(Option option) {
        // instanceof chains instead of a pattern switch so the code also compiles for Java 17 (Minecraft 1.20.1-1.20.4).
        if (option instanceof Slider || option instanceof Info) return 30;
        if (option instanceof ColorPick) return 32;
        return 22;
    }

    @Override
    protected void init() {
        layout();
    }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        layout();
        float intro = FlintFixUi.openProgress(openedAt);
        if (client != null && client.world != null) blurBehind(delta);
        FlintFixUi.backdrop(c, width, height, intro);
        FlintFixUi.pushPanelIntro(c, x, y, w, h, intro);
        FlintFixUi.panelFrame(c, x, y, w, h);
        FlintFixUi.header(c, x + 14, y + 11, w - 28 - 22, icon, heading, description);
        FlintFixUi.iconButton(c, "options-close", closeX(), y + 13, 15, "close",
            FlintFixUi.inside(mouseX, mouseY, closeX(), y + 13, 15, 15));
        FlintFixUi.hairline(c, x + 14, y + 42, w - 28);

        c.enableScissor(rowX - 1, listTop, rowX + rowW + 1, listBottom);
        int rowY = listTop - scroll;
        for (Option option : options) {
            int rh = rowHeight(option);
            if (rowY + rh >= listTop && rowY <= listBottom) renderRow(c, option, rowY, mouseX, mouseY);
            rowY += rh + ROW_GAP;
        }
        c.disableScissor();
        FlintFixUi.scrollbar(c, rowX + rowW + 3, listTop, listBottom - listTop, scroll, maxScroll);

        int fy = y + h - 26;
        if (hudModule) {
            FlintFixUi.actionButton(c, x + 14, fy, 66, 17, "HUD EDIT",
                FlintFixUi.inside(mouseX, mouseY, x + 14, fy, 66, 17), FlintFixUi.ButtonStyle.PRIMARY);
        }
        FlintFixUi.actionButton(c, x + w - 126, fy, 52, 17, "RESET",
            FlintFixUi.inside(mouseX, mouseY, x + w - 126, fy, 52, 17), FlintFixUi.ButtonStyle.SECONDARY);
        FlintFixUi.actionButton(c, x + w - 68, fy, 54, 17, "BACK",
            FlintFixUi.inside(mouseX, mouseY, x + w - 68, fy, 54, 17), FlintFixUi.ButtonStyle.SECONDARY);
        FlintFixUi.finishPanelIntro(c, x, y, w, h, intro);
    }

    private int closeX() {
        return x + w - 14 - 15;
    }

    private void renderRow(DrawContext c, Option option, int rowY, int mouseX, int mouseY) {
        boolean hover = mouseY >= listTop && mouseY <= listBottom
            && FlintFixUi.inside(mouseX, mouseY, rowX, rowY, rowW, rowHeight(option));
        {
            if (option instanceof Toggle t) {
                float ht = FlintFixUi.hoverProgress("opt-row:" + heading + t.label(), hover);
                FlintFixUi.surface(c, rowX, rowY, rowW, 22,
                    FlintFixUi.blendColors(FlintFixUi.card(), FlintFixUi.raised(), ht * 0.6f), FlintFixUi.border());
                FlintFixUi.drawTrimmedExact(c, t.label(), rowX + 8, FlintFixFont.centeredY(rowY, 22, 7), rowW - 42, 7,
                    FlintFixUi.text(), true);
                FlintFixUi.switchToggle(c, "opt:" + heading + t.label(), rowX + rowW - 28, rowY + 6, t.get().getAsBoolean());
            } else if (option instanceof Slider s) {
                FlintFixUi.surface(c, rowX, rowY, rowW, 30, FlintFixUi.card(), FlintFixUi.border());
                float value = (float) s.get().getAsDouble();
                String text = s.format().apply(value);
                FlintFixUi.drawTrimmedExact(c, s.label(), rowX + 8, rowY + 6, rowW - 60, 7, FlintFixUi.text(), true);
                FlintFixFont.drawExact(c, text, rowX + rowW - 8 - FlintFixFont.width(text, 7, true), rowY + 6, 7,
                    FlintFixUi.accentBright(), true);
                FlintFixUi.slider(c, rowX + 8, rowY + 20, rowW - 16, (value - s.min()) / (s.max() - s.min()),
                    dragging == s || hover);
            } else if (option instanceof Choice ch) {
                float ht = FlintFixUi.hoverProgress("opt-row:" + heading + ch.label(), hover);
                FlintFixUi.surface(c, rowX, rowY, rowW, 22,
                    FlintFixUi.blendColors(FlintFixUi.card(), FlintFixUi.raised(), ht * 0.6f), FlintFixUi.border());
                int pillW = 78;
                int pillX = rowX + rowW - 8 - pillW;
                FlintFixUi.drawTrimmedExact(c, ch.label(), rowX + 8, FlintFixFont.centeredY(rowY, 22, 7),
                    pillX - rowX - 14, 7, FlintFixUi.text(), true);
                FlintFixUi.surface(c, pillX, rowY + 4, pillW, 14, FlintFixUi.bg(), FlintFixUi.border());
                FlintFixIcons.drawExact(c, "back", pillX + 3, rowY + 7, 8, FlintFixUi.muted());
                FlintFixIcons.drawExact(c, "next", pillX + pillW - 11, rowY + 7, 8, FlintFixUi.muted());
                String value = ch.values()[Math.max(0, Math.min(ch.values().length - 1, ch.get().getAsInt()))];
                FlintFixFont.drawCenteredExact(c, FlintFixFont.trim(value, pillW - 24, 6, true), pillX + pillW / 2,
                    FlintFixFont.centeredY(rowY + 4, 14, 6), 6, FlintFixUi.accentBright(), true);
            } else if (option instanceof ColorPick cp) {
                FlintFixUi.surface(c, rowX, rowY, rowW, 32, FlintFixUi.card(), FlintFixUi.border());
                FlintFixUi.drawTrimmedExact(c, cp.label(), rowX + 8, rowY + 5, rowW - 16, 7, FlintFixUi.text(), true);
                int current = cp.get().getAsInt() | 0xFF000000;
                for (int i = 0; i < SWATCHES.length; i++) {
                    int sx = swatchX(i);
                    int sy = rowY + 17;
                    boolean selected = (SWATCHES[i] | 0xFF000000) == current;
                    if (selected) FlintFixUi.roundedRaw(c, sx - 2, sy - 2, 13, 13, 3, FlintFixUi.accentBright());
                    FlintFixUi.roundedRaw(c, sx - 1, sy - 1, 11, 11, 2, FlintFixUi.border());
                    FlintFixUi.roundedRaw(c, sx, sy, 9, 9, 2, SWATCHES[i]);
                }
            } else if (option instanceof Action action) {
                FlintFixUi.surface(c, rowX, rowY, rowW, 22, FlintFixUi.card(), FlintFixUi.border());
                int buttonW = Math.max(54, FlintFixFont.width(action.button(), 6, true) + 16);
                int buttonX = rowX + rowW - 4 - buttonW;
                FlintFixUi.drawTrimmedExact(c, action.label(), rowX + 8, FlintFixFont.centeredY(rowY, 22, 7),
                    buttonX - rowX - 14, 7, FlintFixUi.text(), true);
                FlintFixUi.actionButton(c, buttonX, rowY + 3, buttonW, 16, action.button(), hover,
                    FlintFixUi.ButtonStyle.PRIMARY);
            } else if (option instanceof Info info) {
                FlintFixUi.surface(c, rowX, rowY, rowW, 30, FlintFixUi.panel(), FlintFixUi.border());
                FlintFixUi.drawTrimmedExact(c, info.title(), rowX + 8, rowY + 6, rowW - 16, 7, FlintFixUi.text(), true);
                FlintFixUi.drawTrimmedExact(c, info.text(), rowX + 8, rowY + 17, rowW - 16, 6, FlintFixUi.muted(), false);
            }
        }
    }

    private int swatchX(int index) {
        return rowX + 8 + index * 15;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        layout();
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        if (FlintFixUi.inside(mouseX, mouseY, closeX(), y + 13, 15, 15)) {
            close();
            return true;
        }
        int fy = y + h - 26;
        if (hudModule && FlintFixUi.inside(mouseX, mouseY, x + 14, fy, 66, 17)) {
            save();
            if (client != null) client.setScreen(new FlintFixHudEditorScreen(this));
            return true;
        }
        if (FlintFixUi.inside(mouseX, mouseY, x + w - 126, fy, 52, 17)) {
            if (reset != null) reset.run();
            save();
            return true;
        }
        if (FlintFixUi.inside(mouseX, mouseY, x + w - 68, fy, 54, 17)) {
            close();
            return true;
        }
        if (mouseY < listTop || mouseY > listBottom) return super.mouseClicked(mouseX, mouseY, button);

        int rowY = listTop - scroll;
        for (Option option : options) {
            int rh = rowHeight(option);
            if (FlintFixUi.inside(mouseX, mouseY, rowX, rowY, rowW, rh)) {
                click(option, mouseX, mouseY, rowY);
                return true;
            }
            rowY += rh + ROW_GAP;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void click(Option option, double mouseX, double mouseY, int rowY) {
        if (option instanceof Toggle t) {
            t.set().accept(!t.get().getAsBoolean());
        } else if (option instanceof Slider s) {
            dragging = s;
            setSlider(s, mouseX);
        } else {
            if (option instanceof Choice ch) {
                int pillX = rowX + rowW - 8 - 78;
                boolean back = mouseX < pillX + 39;
                int count = ch.values().length;
                ch.set().accept(Math.floorMod(ch.get().getAsInt() + (back ? -1 : 1), count));
            } else if (option instanceof ColorPick cp) {
                for (int i = 0; i < SWATCHES.length; i++) {
                    int sx = swatchX(i);
                    if (mouseX >= sx - 2 && mouseX <= sx + 11 && mouseY >= rowY + 15) {
                        cp.set().accept(SWATCHES[i]);
                        break;
                    }
                }
            } else if (option instanceof Action action) {
                action.run().run();
            }
        }
        save();
    }

    private void setSlider(Slider s, double mouseX) {
        double t = Math.max(0.0, Math.min(1.0, (mouseX - (rowX + 8)) / Math.max(1.0, rowW - 16)));
        double raw = s.min() + t * (s.max() - s.min());
        double snapped = s.min() + Math.round((raw - s.min()) / s.step()) * s.step();
        s.set().accept((float) Math.max(s.min(), Math.min(s.max(), snapped)));
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && dragging != null) {
            setSlider(dragging, mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && dragging != null) {
            dragging = null;
            save();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        layout();
        if (FlintFixUi.inside(mouseX, mouseY, rowX - 4, listTop, rowW + 10, listBottom - listTop)) {
            scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.round(verticalAmount * 20.0)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void save() {
        FlintFixClient.CONFIG.save();
    }

    @Override
    public void close() {
        save();
        if (client != null) client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
