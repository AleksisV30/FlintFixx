package com.flintfix.client;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** In-game profile manager opened from the FlintFix sidebar. */
public final class FlintFixProfileScreen extends FlintFixScreen {
    private enum EditMode { CREATE, RENAME }

    private final Screen parent;
    private final long openedAt = System.currentTimeMillis();
    private int x, y, w, h;
    private int searchX, searchY, searchW;
    private int listTop, listBottom, maxScroll, scroll;
    private String search = "";
    private String editText = "";
    private String message = "Click a profile to select it.";
    private boolean searchFocused;
    private boolean nameFocused;
    private EditMode editMode;

    public FlintFixProfileScreen(Screen parent) {
        super(Component.literal("FlintFix Profiles"));
        this.parent = parent;
    }

    private void layout() {
        w = Math.min(286, Math.max(1, width - 20));
        h = Math.min(204, Math.max(1, height - 20));
        x = (width - w) / 2;
        y = (height - h) / 2;
        searchX = x + 12;
        searchY = y + 39;
        searchW = w - 24;
        listTop = y + 59;
        listBottom = y + h - 31;
        int rowsHeight = Math.max(1, listBottom - listTop);
        maxScroll = Math.max(0, visibleProfiles().size() * 19 - rowsHeight);
        scroll = clamp(scroll, 0, maxScroll);
    }

    private List<String> visibleProfiles() {
        List<String> result = new ArrayList<>();
        String query = search.trim().toLowerCase(Locale.ROOT);
        for (String name : FlintFixProfileStore.names()) {
            if (query.isEmpty() || name.toLowerCase(Locale.ROOT).contains(query)) result.add(name);
        }
        return result;
    }

    @Override
    protected void init() {
        layout();
    }

    @Override
    public void render(GuiGraphics c, int mouseX, int mouseY, float delta) {
        layout();
        float intro = FlintFixUi.openProgress(openedAt);
        blurBehind(delta);
        FlintFixUi.backdrop(c, width, height, intro);
        FlintFixUi.pushPanelIntro(c, x, y, w, h, intro);
        FlintFixUi.panelFrame(c, x, y, w, h);
        FlintFixUi.header(c, x + 12, y + 9, w - 24, "profiles", "Profiles",
            "Choose, search, or manage saved client setups");

        FlintFixUi.searchField(c, searchX, searchY, searchW, 14, search, "Search profiles", searchFocused);

        c.enableScissor(x + 9, listTop, x + w - 9, listBottom);
        List<String> profiles = visibleProfiles();
        int row = 0;
        for (String name : profiles) {
            int rowY = listTop + row * 19 - scroll;
            row++;
            if (rowY + 17 < listTop || rowY > listBottom) continue;
            boolean selected = name.equalsIgnoreCase(FlintFixProfileStore.selectedName());
            boolean hover = mouseY >= listTop && mouseY < listBottom
                && FlintFixUi.inside(mouseX, mouseY, x + 12, rowY, w - 24, 17);
            FlintFixUi.selectableRow(c, "manage-profile:" + name, x + 12, rowY, w - 24, 17, selected, hover);
            FlintFixUi.drawTrimmedExact(c, name, x + 20, FlintFixFont.centeredY(rowY, 17, 7), w - 88, 7,
                selected ? FlintFixUi.text() : FlintFixUi.muted(), selected);
            if (selected) FlintFixUi.badge(c, x + w - 16, rowY + 4, "ACTIVE", true);
        }
        c.disableScissor();

        if (profiles.isEmpty()) {
            FlintFixFont.drawCenteredExact(c, "No matching profiles", x + w / 2, listTop + 10, 6,
                FlintFixUi.muted(), false);
        }

        FlintFixUi.drawTrimmedExact(c, message, x + 12, y + h - 34, w - 24, 6, FlintFixUi.subtle(), false);
        int buttonY = y + h - 23;
        int bx = x + 12;
        profileButton(c, "NEW", bx, buttonY, 43, mouseX, mouseY, FlintFixUi.ButtonStyle.PRIMARY); bx += 47;
        profileButton(c, "RENAME", bx, buttonY, 53, mouseX, mouseY, FlintFixUi.ButtonStyle.SECONDARY); bx += 57;
        profileButton(c, "REMOVE", bx, buttonY, 51, mouseX, mouseY, FlintFixUi.ButtonStyle.DANGER);
        profileButton(c, "BACK", x + w - 57, buttonY, 45, mouseX, mouseY, FlintFixUi.ButtonStyle.SECONDARY);

        if (editMode != null) renderNameDialog(c, mouseX, mouseY);
        FlintFixUi.finishPanelIntro(c, x, y, w, h, intro);
    }

    private void profileButton(GuiGraphics c, String label, int bx, int by, int bw,
                              int mouseX, int mouseY, FlintFixUi.ButtonStyle style) {
        boolean hover = editMode == null && FlintFixUi.inside(mouseX, mouseY, bx, by, bw, 14);
        FlintFixUi.actionButton(c, bx, by, bw, 14, label, hover, style);
    }

    private void renderNameDialog(GuiGraphics c, int mouseX, int mouseY) {
        int dw = Math.min(210, w - 24);
        int dh = 66;
        int dx = x + (w - dw) / 2;
        int dy = y + (h - dh) / 2;
        c.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0x99000000);
        FlintFixUi.shadow(c, dx, dy, dw, dh, 1.0f);
        FlintFixUi.surface(c, dx, dy, dw, dh, FlintFixUi.panel(), FlintFixUi.border());
        String title = editMode == EditMode.CREATE ? "Create profile" : "Rename profile";
        FlintFixFont.drawExact(c, title, dx + 9, dy + 7, 8, FlintFixUi.text(), true);
        FlintFixUi.surface(c, dx + 8, dy + 20, dw - 16, 15, FlintFixUi.bg(),
            nameFocused ? FlintFixUi.accent() : FlintFixUi.border());
        String inputShown = editText.isEmpty() ? "Profile name" : editText;
        int textY = FlintFixFont.centeredY(dy + 20, 15, 7);
        FlintFixUi.drawTrimmedExact(c, inputShown, dx + 13, textY, dw - 30, 7,
            editText.isEmpty() ? FlintFixUi.subtle() : FlintFixUi.text(), false);
        if (nameFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int caretX = dx + 13 + Math.min(dw - 30, FlintFixFont.width(editText, 7, false));
            c.fill(caretX, textY, caretX + 1, textY + 7, FlintFixUi.text());
        }
        FlintFixUi.actionButton(c, dx + dw - 103, dy + 43, 44, 14, "CANCEL",
            FlintFixUi.inside(mouseX, mouseY, dx + dw - 103, dy + 43, 44, 14), FlintFixUi.ButtonStyle.SECONDARY);
        FlintFixUi.actionButton(c, dx + dw - 54, dy + 43, 46, 14, "SAVE",
            FlintFixUi.inside(mouseX, mouseY, dx + dw - 54, dy + 43, 46, 14), FlintFixUi.ButtonStyle.PRIMARY);
    }

    private void beginEdit(EditMode mode) {
        editMode = mode;
        editText = mode == EditMode.RENAME ? FlintFixProfileStore.selectedName() : "";
        nameFocused = true;
        searchFocused = false;
        message = "";
    }

    private void finishEdit() {
        boolean ok = editMode == EditMode.CREATE
            ? FlintFixProfileStore.create(editText)
            : FlintFixProfileStore.renameSelected(editText);
        if (ok) {
            editMode = null;
            nameFocused = false;
            message = "Profile saved and selected.";
            scroll = 0;
        } else {
            message = "Use a unique name (up to 24 characters).";
        }
    }

    private void cancelEdit() {
        editMode = null;
        nameFocused = false;
        message = "Click a profile to select it.";
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        layout();
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        if (editMode != null) {
            int dw = Math.min(210, w - 24);
            int dx = x + (w - dw) / 2;
            int dy = y + (h - 66) / 2;
            if (FlintFixUi.inside(mouseX, mouseY, dx + 8, dy + 20, dw - 16, 15)) {
                nameFocused = true;
                return true;
            }
            if (FlintFixUi.inside(mouseX, mouseY, dx + dw - 103, dy + 43, 44, 14)) {
                cancelEdit();
                return true;
            }
            if (FlintFixUi.inside(mouseX, mouseY, dx + dw - 54, dy + 43, 46, 14)) {
                finishEdit();
                return true;
            }
            return true;
        }

        searchFocused = FlintFixUi.inside(mouseX, mouseY, searchX, searchY, searchW, 14);
        if (searchFocused) return true;

        List<String> profiles = visibleProfiles();
        if (mouseY >= listTop && mouseY < listBottom) {
            for (int i = 0; i < profiles.size(); i++) {
                int rowY = listTop + i * 19 - scroll;
                if (FlintFixUi.inside(mouseX, mouseY, x + 12, rowY, w - 24, 17)) {
                    FlintFixProfileStore.select(profiles.get(i));
                    message = "Selected " + FlintFixProfileStore.selectedName() + ".";
                    return true;
                }
            }
        }

        int buttonY = y + h - 23;
        int bx = x + 12;
        if (FlintFixUi.inside(mouseX, mouseY, bx, buttonY, 43, 14)) {
            beginEdit(EditMode.CREATE);
            return true;
        }
        bx += 47;
        if (FlintFixUi.inside(mouseX, mouseY, bx, buttonY, 53, 14)) {
            beginEdit(EditMode.RENAME);
            return true;
        }
        bx += 57;
        if (FlintFixUi.inside(mouseX, mouseY, bx, buttonY, 51, 14)) {
            if (FlintFixProfileStore.delete(FlintFixProfileStore.selectedName())) {
                message = "Profile removed.";
                scroll = 0;
            } else {
                message = "Keep at least one profile.";
            }
            return true;
        }
        if (FlintFixUi.inside(mouseX, mouseY, x + w - 57, buttonY, 45, 14)) {
            closeToParent();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (editMode != null && nameFocused) {
            if (!Character.isISOControl(chr) && editText.length() < 24) editText += chr;
            return true;
        }
        if (searchFocused) {
            if (!Character.isISOControl(chr) && search.length() < 24) {
                search += chr;
                scroll = 0;
            }
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (editMode != null) cancelEdit();
            else closeToParent();
            return true;
        }
        if (editMode != null && nameFocused) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) finishEdit();
            else if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !editText.isEmpty()) editText = editText.substring(0, editText.length() - 1);
            return true;
        }
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) searchFocused = false;
            else if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) search = search.substring(0, search.length() - 1);
            else if (keyCode == GLFW.GLFW_KEY_DELETE) search = "";
            scroll = 0;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        layout();
        if (mouseY >= listTop && mouseY <= listBottom) {
            scroll = clamp(scroll - (int) Math.round(verticalAmount * 19.0), 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void closeToParent() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override public boolean isPauseScreen() { return false; }
}
