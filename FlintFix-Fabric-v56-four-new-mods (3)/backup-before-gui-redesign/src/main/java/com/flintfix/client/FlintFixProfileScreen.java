package com.flintfix.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** In-game profile manager opened from the FlintFix sidebar. */
public final class FlintFixProfileScreen extends Screen {
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
        super(Text.literal("FlintFix Profiles"));
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
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        layout();
        float intro = FlintFixUi.openProgress(openedAt);
        c.fill(0, 0, width, height, FlintFixUi.opacity(0xA0000000, intro));
        FlintFixUi.pushPanelIntro(c, x, y, w, h, intro);
        FlintFixUi.fadeOutline(c, x, y, w, h, 6, FlintFixUi.ACCENT);
        FlintFixUi.rounded(c, x, y, w, h, 6, 0xFF10151D);
        FlintFixFont.draw(c, "Profiles", x + 12, y + 9, 10, 0xFFF1F2F4, true);
        FlintFixFont.draw(c, "Choose, search, or manage saved client setups", x + 12, y + 23,
            5, 0xFFA4A4A4, false);

        FlintFixUi.outlinedBox(c, searchX, searchY, searchW, 14, 3,
            searchFocused ? FlintFixUi.interactiveBorder(true) : FlintFixUi.interactiveBorder(false),
            searchFocused ? 0xFF303239 : 0xFF1E2024);
        FlintFixIcons.draw(c, "search", searchX + 5, searchY + 4, 6, 0xFFB1BDCC);
        String shown = search.isEmpty() ? "Search profiles" : search;
        FlintFixUi.drawTrimmed(c, shown, searchX + 15, searchY + 4, searchW - 20, 5,
            search.isEmpty() ? 0xFF8A909B : 0xFFE6EAF0, false);

        c.enableScissor(x + 9, listTop, x + w - 9, listBottom);
        List<String> profiles = visibleProfiles();
        int row = 0;
        for (String name : profiles) {
            int rowY = listTop + row * 19 - scroll;
            row++;
            if (rowY + 17 < listTop || rowY > listBottom) continue;
            boolean selected = name.equalsIgnoreCase(FlintFixProfileStore.selectedName());
            boolean hover = FlintFixUi.inside(mouseX, mouseY, x + 12, rowY, w - 24, 17);
            float rowMotion = FlintFixUi.hoverProgress("manage-profile:" + name, selected || hover);
            if (rowMotion > 0.01f) {
                int selectedFill = FlintFixUi.activeTheme() == FlintFixTheme.LIGHT ? 0xFFDEE2E6 : 0xFF303239;
                int rowFill = FlintFixUi.blendColors(0xFF1E2024, selectedFill, rowMotion);
                FlintFixUi.outlinedBox(c, x + 12, rowY, w - 24, 17, 3,
                    FlintFixUi.interactiveBorder(rowMotion > 0.3f), rowFill);
            }
            if (selected) c.fill(x + 14, rowY + 4, x + 16, rowY + 13, FlintFixUi.ACCENT_BRIGHT);
            FlintFixUi.drawTrimmed(c, name, x + 22, rowY + 5, w - 88, 6,
                selected ? 0xFFF1F2F4 : 0xFFC7CDD6, selected);
            if (selected) FlintFixFont.draw(c, "ACTIVE", x + w - 49, rowY + 6, 4, FlintFixUi.ACCENT_BRIGHT, true);
        }
        c.disableScissor();

        if (profiles.isEmpty()) {
            FlintFixFont.drawCentered(c, "No matching profiles", x + w / 2, listTop + 10, 6, 0xFF9AA4B1, false);
        }

        FlintFixFont.draw(c, message, x + 12, y + h - 34, 5, 0xFF929AA6, false);
        int buttonY = y + h - 23;
        int bx = x + 12;
        profileButton(c, "NEW", bx, buttonY, 43, mouseX, mouseY, true); bx += 47;
        profileButton(c, "RENAME", bx, buttonY, 53, mouseX, mouseY, false); bx += 57;
        profileButton(c, "REMOVE", bx, buttonY, 51, mouseX, mouseY, false);
        profileButton(c, "BACK", x + w - 57, buttonY, 45, mouseX, mouseY, false);

        if (editMode != null) renderNameDialog(c, mouseX, mouseY);
        FlintFixUi.finishPanelIntro(c, x, y, w, h, intro);
    }

    private void profileButton(DrawContext c, String label, int bx, int by, int bw,
                              int mouseX, int mouseY, boolean primary) {
        FlintFixUi.compactButton(c, bx, by, bw, 14, label,
            FlintFixUi.inside(mouseX, mouseY, bx, by, bw, 14), primary);
    }

    private void renderNameDialog(DrawContext c, int mouseX, int mouseY) {
        int dw = Math.min(210, w - 24);
        int dh = 66;
        int dx = x + (w - dw) / 2;
        int dy = y + (h - dh) / 2;
        c.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xA0000000);
        FlintFixUi.rounded(c, dx, dy, dw, dh, 6, 0xFF303239);
        FlintFixUi.rounded(c, dx + 1, dy + 1, dw - 2, dh - 2, 5, 0xFF171E28);
        String title = editMode == EditMode.CREATE ? "Create profile" : "Rename profile";
        FlintFixFont.draw(c, title, dx + 9, dy + 7, 7, 0xFFF1F2F4, true);
        FlintFixUi.outlinedBox(c, dx + 8, dy + 20, dw - 16, 15, 2,
            FlintFixUi.interactiveBorder(nameFocused), 0xFF10151D);
        String inputShown = editText.isEmpty() ? "Profile name" : editText;
        FlintFixUi.drawTrimmed(c, inputShown, dx + 13, dy + 24, dw - 30, 6,
            editText.isEmpty() ? 0xFF808995 : 0xFFF1F2F4, false);
        if (nameFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int caretX = dx + 13 + Math.min(dw - 30, FlintFixFont.width(editText, 6, false));
            c.fill(caretX, dy + 24, caretX + 1, dy + 31, 0xFFF1F2F4);
        }
        FlintFixUi.compactButton(c, dx + dw - 103, dy + 43, 44, 14, "CANCEL",
            FlintFixUi.inside(mouseX, mouseY, dx + dw - 103, dy + 43, 44, 14), false);
        FlintFixUi.compactButton(c, dx + dw - 54, dy + 43, 46, 14, "SAVE",
            FlintFixUi.inside(mouseX, mouseY, dx + dw - 54, dy + 43, 46, 14), true);
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
        if (client != null) client.setScreen(parent);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override public boolean shouldPause() { return false; }
}
