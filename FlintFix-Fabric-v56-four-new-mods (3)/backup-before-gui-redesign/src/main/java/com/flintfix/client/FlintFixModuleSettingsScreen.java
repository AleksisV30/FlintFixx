package com.flintfix.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public final class FlintFixModuleSettingsScreen extends Screen {
    public enum Module {
        CPS("CPS Counter", "Counts your left and right clicks per second."),
        COORDINATES("Coordinates", "Shows your position and current biome."),
        PING("Ping Display", "Shows your current server latency."),
        KEYSTROKES("Keystrokes", "Displays your movement and action keys."),
        ARMOR("Armor Stats", "Shows equipped armor and its durability."),
        CHUNKS("Chunk Borders", "Displays Minecraft's native 16-block chunk grid."),
        TRAJECTORY("Trajectory", "Predicts projectile paths to the first block collision."),
        HITBOXES("Show Hitboxes", "Draws outlines around nearby players, mobs and dropped items."),
        ZOOM("Zoom", "Hold a key and use the mouse wheel to adjust camera zoom."),
        LOOK_AROUND("Look Around", "Look freely in third person while your player keeps their heading."),
        SHULKERS("See in Shulkers", "Lists stored items when you hover over a shulker box.");

        private final String title;
        private final String description;
        Module(String title, String description) {
            this.title = title;
            this.description = description;
        }
        public String title() { return title; }
        public String description() { return description; }
    }

    private final Screen parent;
    private final Module module;
    private int x, y, w, h;
    private int listTop, listBottom;
    private int leftX, leftW, rightX, rightW;
    private int scroll, maxScroll;
    private boolean draggingOpacity;
    private boolean draggingScale;

    private static final int MAX_W = 320;
    private static final int MAX_H = 188;

    public FlintFixModuleSettingsScreen(Screen parent, Module module) {
        super(Text.literal(module.title() + " Settings"));
        this.parent = parent;
        this.module = module;
    }

    private void layout() {
        int margin = 12;
        w = Math.min(MAX_W, Math.max(1, width - margin * 2));
        h = Math.min(MAX_H, Math.max(1, height - margin * 2));
        x = (width - w) / 2;
        y = (height - h) / 2;

        leftX = x + 14;
        leftW = Math.max(120, w - 154);
        rightX = leftX + leftW + 10;
        rightW = Math.max(1, x + w - 14 - rightX);
        listTop = y + 50;
        listBottom = y + h - 32;
        int contentHeight = isSimpleModule() ? 76 : (module == Module.KEYSTROKES ? 160 : 128);
        maxScroll = Math.max(0, contentHeight - Math.max(1, listBottom - listTop));
        scroll = clamp(scroll, 0, maxScroll);
    }

    @Override
    protected void init() {
        layout();
    }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        layout();
        c.fill(0, 0, width, height, 0x18000000);
        FlintFixUi.fadeOutline(c, x, y, w, h, 11, FlintFixUi.ACCENT);
        FlintFixUi.rounded(c, x, y, w, h, 11, 0xFF10151D);

        FlintFixFont.draw(c, module.title(), x + 14, y + 12, 11, FlintFixClient.TEXT, true);
        FlintFixFont.draw(c, module.description(), x + 14, y + 28, 6, FlintFixClient.MUTED, false);
        FlintFixUi.divider(c, x + 14, y + 42, w - 28);

        c.enableScissor(leftX - 1, listTop, leftX + leftW + 1, listBottom);
        int cy = listTop - scroll;
        if (module == Module.CHUNKS) {
            toggleRow(c, cy, "Show chunk borders", enabled());
            FlintFixFont.draw(c, "Minecraft chunk grid", leftX + 7, cy + 31, 6, FlintFixClient.TEXT, true);
            FlintFixFont.draw(c, "Uses the native 16-block borders", leftX + 7, cy + 43, 5, FlintFixClient.MUTED, false);
        } else if (module == Module.TRAJECTORY) {
            toggleRow(c, cy, "Show predicted path", enabled());
            FlintFixFont.draw(c, "Pearl, bow, XP bottle, crossbow", leftX + 7, cy + 31, 6, FlintFixClient.TEXT, true);
            FlintFixFont.draw(c, "Path ends at the first block hit", leftX + 7, cy + 43, 5, FlintFixClient.MUTED, false);
        } else if (module == Module.HITBOXES) {
            toggleRow(c, cy, "Show entity hitboxes", enabled());
            FlintFixFont.draw(c, "Nearby players, mobs and items", leftX + 7, cy + 31, 6, FlintFixClient.TEXT, true);
            FlintFixFont.draw(c, "Wireframes are drawn in-world", leftX + 7, cy + 43, 5, FlintFixClient.MUTED, false);
        } else if (module == Module.ZOOM) {
            toggleRow(c, cy, "Enable hold-to-zoom", enabled());
            FlintFixFont.draw(c, "Hold key, scroll to adjust", leftX + 7, cy + 31, 6, FlintFixClient.TEXT, true);
            FlintFixFont.draw(c, "Key: " + keyLabel(FlintFixClient.getZoomKeyBinding()), leftX + 7, cy + 43, 5, FlintFixClient.MUTED, false);
            FlintFixFont.draw(c, "Rebind in Controls → FlintFix Client", leftX + 7, cy + 55, 5, FlintFixClient.MUTED, false);
        } else if (module == Module.LOOK_AROUND) {
            toggleRow(c, cy, "Enable look-around", enabled());
            FlintFixFont.draw(c, "Hold to move only the camera", leftX + 7, cy + 31, 6, FlintFixClient.TEXT, true);
            FlintFixFont.draw(c, "Key: " + keyLabel(FlintFixClient.getLookAroundKeyBinding()), leftX + 7, cy + 43, 5, FlintFixClient.MUTED, false);
            FlintFixFont.draw(c, "Rebind in Controls → FlintFix Client", leftX + 7, cy + 55, 5, FlintFixClient.MUTED, false);
        } else if (module == Module.SHULKERS) {
            toggleRow(c, cy, "Show shulker contents", enabled());
            FlintFixFont.draw(c, "Hover over a shulker box", leftX + 7, cy + 31, 6, FlintFixClient.TEXT, true);
            FlintFixFont.draw(c, "Stored items appear in its tooltip", leftX + 7, cy + 43, 5, FlintFixClient.MUTED, false);
        } else {
            toggleRow(c, cy, "Enabled", enabled());
            toggleRow(c, cy + 28, "Background", background());
            sliderRow(c, cy + 58, "Background opacity", opacity(), 0f, 1f, Math.round(opacity() * 100f) + "%");
            sliderRow(c, cy + 94, "Scale", scale(), 0.25f, 2.0f, Math.round(scale() * 100f) + "%");
            if (module == Module.KEYSTROKES) {
                toggleRow(c, cy + 132, "Show CPS in LMB / RMB", FlintFixClient.CONFIG.keystrokesShowCps);
            }
        }
        c.disableScissor();
        renderPreview(c);
        renderScrollbar(c);

        int fy = y + h - 27;
        String firstAction = isSimpleModule() ? (enabled() ? "HIDE" : "SHOW") : "HUD EDIT";
        FlintFixUi.compactButton(c, x + 14, fy, 66, 18, firstAction, FlintFixUi.inside(mouseX, mouseY, x + 14, fy, 66, 18), true);
        FlintFixUi.compactButton(c, x + w - 132, fy, 54, 18, "RESET", FlintFixUi.inside(mouseX, mouseY, x + w - 132, fy, 54, 18), false);
        FlintFixUi.compactButton(c, x + w - 70, fy, 56, 18, "BACK", FlintFixUi.inside(mouseX, mouseY, x + w - 70, fy, 56, 18), false);
    }

    private void toggleRow(DrawContext c, int rowY, String label, boolean value) {
        FlintFixUi.rounded(c, leftX, rowY, leftW, 22, 6, 0xFF1A222D);
        FlintFixUi.drawTrimmed(c, label, leftX + 8, rowY + 7, leftW - 48, 6, FlintFixClient.TEXT, true);
        FlintFixUi.compactToggle(c, leftX + leftW - 34, rowY + 4, value);
    }

    private void sliderRow(DrawContext c, int rowY, String label, float value, float min, float max, String valueText) {
        FlintFixUi.rounded(c, leftX, rowY, leftW, 30, 6, 0xFF1A222D);
        FlintFixFont.draw(c, label, leftX + 8, rowY + 5, 6, FlintFixClient.TEXT, true);
        FlintFixFont.draw(c, valueText, leftX + leftW - 8 - FlintFixFont.width(valueText, 6, true), rowY + 5, 6, FlintFixUi.ACCENT_BRIGHT, true);
        int sx = leftX + 8;
        int sw = leftW - 16;
        int barY = rowY + 19;
        float t = (value - min) / (max - min);
        int knob = sx + Math.round(t * sw);
        c.fill(sx, barY, sx + sw, barY + 3, 0xFF302D37);
        c.fill(sx, barY, knob, barY + 3, FlintFixUi.ACCENT);
        c.fill(knob - 3, barY - 3, knob + 3, barY + 6, 0xFFF7F4F9);
    }

    private void renderPreview(DrawContext c) {
        int previewH = Math.max(82, Math.min(132, listBottom - listTop));
        FlintFixUi.rounded(c, rightX, listTop, rightW, previewH, 8, 0xFF1A222D);
        FlintFixFont.draw(c, "PREVIEW", rightX + 8, listTop + 8, 6, FlintFixClient.TEXT, true);
        boolean active = enabled();
        FlintFixUi.rounded(c, rightX + rightW - 28, listTop + 7, 21, 10, 4,
            active ? 0xFF345541 : 0xFF583B38);
        FlintFixFont.drawCentered(c, active ? "ON" : "OFF", rightX + rightW - 17, listTop + 9, 4,
            active ? 0xFFD4F0D8 : 0xFFF0D0CA, true);

        int demoX = rightX + 7;
        int demoY = listTop + 25;
        int demoW = Math.max(1, rightW - 14);
        int demoH = Math.max(1, previewH - 31);
        c.enableScissor(demoX, demoY, demoX + demoW, demoY + demoH);
        if (module == Module.TRAJECTORY) {
            FlintFixFont.draw(c, "HELD ITEM PATH", demoX + 7, demoY + 6, 5, FlintFixClient.MUTED, true);
            int[][] points = {
                {demoX + 8, demoY + demoH - 12},
                {demoX + 21, demoY + demoH - 28},
                {demoX + 37, demoY + demoH - 34},
                {demoX + 54, demoY + demoH - 28},
                {demoX + 71, demoY + demoH - 13}
            };
            for (int i = 1; i < points.length; i++) {
                drawPreviewLine(c, points[i - 1][0], points[i - 1][1], points[i][0], points[i][1]);
            }
            FlintFixUi.rounded(c, points[points.length - 1][0] - 2,
                points[points.length - 1][1] - 2, 5, 5, 2, 0xFFFFE5A8);
        } else if (module == Module.HITBOXES || module == Module.ZOOM || module == Module.LOOK_AROUND || module == Module.SHULKERS) {
            String[] lines = switch (module) {
                case HITBOXES -> new String[] {"PLAYER", "MOB", "ITEM"};
                case ZOOM -> new String[] {"HOLD " + keyLabel(FlintFixClient.getZoomKeyBinding()), "SCROLL TO ZOOM"};
                case LOOK_AROUND -> new String[] {"HOLD " + keyLabel(FlintFixClient.getLookAroundKeyBinding()), "CAMERA ONLY"};
                case SHULKERS -> new String[] {"SHULKER BOX", "STONE × 32", "TORCH × 12"};
                default -> new String[0];
            };
            int lineY = demoY + 12;
            for (String line : lines) {
                FlintFixUi.rounded(c, demoX + 5, lineY - 2, Math.max(1, demoW - 10), 13, 3, 0xFF26313D);
                FlintFixUi.drawTrimmed(c, line, demoX + 10, lineY + 2, demoW - 20, 5, FlintFixClient.TEXT, false);
                lineY += 17;
            }
        } else {
            FlintFixHudPreview.render(c, MinecraftClient.getInstance(),
                FlintFixHudPreview.Widget.valueOf(module.name()), demoX, demoY, demoW, demoH);
        }
        c.disableScissor();
    }

    private static void drawPreviewLine(DrawContext c, int x0, int y0, int x1, int y1) {
        int dx = Math.abs(x1 - x0), sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0), sy = y0 < y1 ? 1 : -1;
        int error = dx + dy;
        while (true) {
            c.fill(x0, y0, x0 + 2, y0 + 2, 0xFFB77BFF);
            if (x0 == x1 && y0 == y1) break;
            int twice = 2 * error;
            if (twice >= dy) { error += dy; x0 += sx; }
            if (twice <= dx) { error += dx; y0 += sy; }
        }
    }

    private void renderScrollbar(DrawContext c) {
        if (maxScroll <= 0) return;
        int trackH = listBottom - listTop;
        int thumbH = Math.max(16, trackH * trackH / (trackH + maxScroll));
        int thumbY = listTop + (trackH - thumbH) * scroll / maxScroll;
        int sx = leftX + leftW - 2;
        FlintFixUi.rounded(c, sx, listTop, 2, trackH, 1, 0x30312E38);
        FlintFixUi.rounded(c, sx, thumbY, 2, thumbH, 1, FlintFixUi.ACCENT);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        layout();
        if (mouseX >= leftX - 4 && mouseX <= leftX + leftW + 4 && mouseY >= listTop && mouseY <= listBottom) {
            scroll = clamp(scroll - (int)Math.round(verticalAmount * 24.0), 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        layout();
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        int cy = listTop - scroll;

        if (mouseY >= listTop && mouseY <= listBottom) {
            if (isSimpleModule()) {
                if (FlintFixUi.inside(mouseX, mouseY, leftX, cy, leftW, 22)) {
                    setEnabled(!enabled()); save(); return true;
                }
            } else {
                if (FlintFixUi.inside(mouseX, mouseY, leftX, cy, leftW, 22)) {
                    setEnabled(!enabled()); save(); return true;
                }
                if (FlintFixUi.inside(mouseX, mouseY, leftX, cy + 28, leftW, 22)) {
                    setBackground(!background()); save(); return true;
                }
                if (sliderHit(mouseX, mouseY, cy + 58)) {
                    draggingOpacity = true; setSlider(mouseX, true); return true;
                }
                if (sliderHit(mouseX, mouseY, cy + 94)) {
                    draggingScale = true; setSlider(mouseX, false); return true;
                }
                if (module == Module.KEYSTROKES && FlintFixUi.inside(mouseX, mouseY, leftX, cy + 132, leftW, 22)) {
                    FlintFixClient.CONFIG.keystrokesShowCps = !FlintFixClient.CONFIG.keystrokesShowCps;
                    save(); return true;
                }
            }
        }

        int fy = y + h - 27;
        if (FlintFixUi.inside(mouseX, mouseY, x + 14, fy, 66, 18)) {
            if (isSimpleModule()) {
                setEnabled(!enabled());
                save();
            } else if (client != null) {
                client.setScreen(new FlintFixHudEditorScreen(this));
            }
            return true;
        }
        if (FlintFixUi.inside(mouseX, mouseY, x + w - 132, fy, 54, 18)) {
            resetModule(); return true;
        }
        if (FlintFixUi.inside(mouseX, mouseY, x + w - 70, fy, 56, 18)) {
            returnToModules(); return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && (draggingOpacity || draggingScale)) {
            setSlider(mouseX, draggingOpacity);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && (draggingOpacity || draggingScale)) {
            draggingOpacity = false;
            draggingScale = false;
            save();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean sliderHit(double mouseX, double mouseY, int rowY) {
        return mouseX >= leftX && mouseX <= leftX + leftW && mouseY >= rowY + 13 && mouseY <= rowY + 30 && mouseY >= listTop && mouseY <= listBottom;
    }

    private void setSlider(double mouseX, boolean opacitySlider) {
        int sx = leftX + 8;
        int sw = Math.max(1, leftW - 16);
        float t = (float)Math.max(0.0, Math.min(1.0, (mouseX - sx) / (double)sw));
        if (opacitySlider) setOpacity(t);
        else setScale(0.25f + t * 1.75f);
    }

    private boolean isSimpleModule() {
        return module == Module.CHUNKS || module == Module.TRAJECTORY || module == Module.HITBOXES
            || module == Module.ZOOM || module == Module.LOOK_AROUND || module == Module.SHULKERS;
    }

    private boolean enabled() {
        return switch (module) {
            case CPS -> FlintFixClient.CONFIG.cpsEnabled;
            case COORDINATES -> FlintFixClient.CONFIG.coordinatesEnabled;
            case PING -> FlintFixClient.CONFIG.pingEnabled;
            case KEYSTROKES -> FlintFixClient.CONFIG.keystrokesEnabled;
            case ARMOR -> FlintFixClient.CONFIG.armorEnabled;
            case CHUNKS -> FlintFixClient.CONFIG.chunksEnabled;
            case TRAJECTORY -> FlintFixClient.CONFIG.trajectoryEnabled;
            case HITBOXES -> FlintFixClient.CONFIG.hitboxesEnabled;
            case ZOOM -> FlintFixClient.CONFIG.zoomEnabled;
            case LOOK_AROUND -> FlintFixClient.CONFIG.lookAroundEnabled;
            case SHULKERS -> FlintFixClient.CONFIG.shulkerPreviewEnabled;
        };
    }

    private void setEnabled(boolean v) {
        switch (module) {
            case CPS -> FlintFixClient.CONFIG.cpsEnabled = v;
            case COORDINATES -> FlintFixClient.CONFIG.coordinatesEnabled = v;
            case PING -> FlintFixClient.CONFIG.pingEnabled = v;
            case KEYSTROKES -> FlintFixClient.CONFIG.keystrokesEnabled = v;
            case ARMOR -> FlintFixClient.CONFIG.armorEnabled = v;
            case CHUNKS -> FlintFixClient.CONFIG.chunksEnabled = v;
            case TRAJECTORY -> FlintFixClient.CONFIG.trajectoryEnabled = v;
            case HITBOXES -> FlintFixClient.CONFIG.hitboxesEnabled = v;
            case ZOOM -> FlintFixClient.CONFIG.zoomEnabled = v;
            case LOOK_AROUND -> FlintFixClient.CONFIG.lookAroundEnabled = v;
            case SHULKERS -> FlintFixClient.CONFIG.shulkerPreviewEnabled = v;
        }
    }

    private boolean background() {
        return switch (module) {
            case CPS -> FlintFixClient.CONFIG.cpsBackground;
            case COORDINATES -> FlintFixClient.CONFIG.coordinatesBackground;
            case PING -> FlintFixClient.CONFIG.pingBackground;
            case KEYSTROKES -> FlintFixClient.CONFIG.keystrokesBackground;
            case ARMOR -> FlintFixClient.CONFIG.armorBackground;
            case CHUNKS, TRAJECTORY, HITBOXES, ZOOM, LOOK_AROUND, SHULKERS -> false;
        };
    }

    private void setBackground(boolean v) {
        switch (module) {
            case CPS -> FlintFixClient.CONFIG.cpsBackground = v;
            case COORDINATES -> FlintFixClient.CONFIG.coordinatesBackground = v;
            case PING -> FlintFixClient.CONFIG.pingBackground = v;
            case KEYSTROKES -> FlintFixClient.CONFIG.keystrokesBackground = v;
            case ARMOR -> FlintFixClient.CONFIG.armorBackground = v;
            case CHUNKS, TRAJECTORY, HITBOXES, ZOOM, LOOK_AROUND, SHULKERS -> { }
        }
    }

    private float opacity() {
        return switch (module) {
            case CPS -> FlintFixClient.CONFIG.cpsBackgroundOpacity;
            case COORDINATES -> FlintFixClient.CONFIG.coordinatesBackgroundOpacity;
            case PING -> FlintFixClient.CONFIG.pingBackgroundOpacity;
            case KEYSTROKES -> FlintFixClient.CONFIG.keystrokesBackgroundOpacity;
            case ARMOR -> FlintFixClient.CONFIG.armorBackgroundOpacity;
            case CHUNKS, TRAJECTORY, HITBOXES, ZOOM, LOOK_AROUND, SHULKERS -> 0.0f;
        };
    }

    private void setOpacity(float v) {
        switch (module) {
            case CPS -> FlintFixClient.CONFIG.cpsBackgroundOpacity = v;
            case COORDINATES -> FlintFixClient.CONFIG.coordinatesBackgroundOpacity = v;
            case PING -> FlintFixClient.CONFIG.pingBackgroundOpacity = v;
            case KEYSTROKES -> FlintFixClient.CONFIG.keystrokesBackgroundOpacity = v;
            case ARMOR -> FlintFixClient.CONFIG.armorBackgroundOpacity = v;
            case CHUNKS, TRAJECTORY, HITBOXES, ZOOM, LOOK_AROUND, SHULKERS -> { }
        }
    }

    private float scale() {
        return switch (module) {
            case CPS -> FlintFixClient.CONFIG.cpsScale;
            case COORDINATES -> FlintFixClient.CONFIG.coordinatesScale;
            case PING -> FlintFixClient.CONFIG.pingScale;
            case KEYSTROKES -> FlintFixClient.CONFIG.keystrokesScale;
            case ARMOR -> FlintFixClient.CONFIG.armorScale;
            case CHUNKS, TRAJECTORY, HITBOXES, ZOOM, LOOK_AROUND, SHULKERS -> 1.0f;
        };
    }

    private void setScale(float v) {
        switch (module) {
            case CPS -> FlintFixClient.CONFIG.cpsScale = v;
            case COORDINATES -> FlintFixClient.CONFIG.coordinatesScale = v;
            case PING -> FlintFixClient.CONFIG.pingScale = v;
            case KEYSTROKES -> FlintFixClient.CONFIG.keystrokesScale = v;
            case ARMOR -> FlintFixClient.CONFIG.armorScale = v;
            case CHUNKS, TRAJECTORY, HITBOXES, ZOOM, LOOK_AROUND, SHULKERS -> { }
        }
    }

    private void resetModule() {
        switch (module) {
            case CPS -> FlintFixClient.CONFIG.resetCps();
            case COORDINATES -> FlintFixClient.CONFIG.resetCoordinates();
            case PING -> FlintFixClient.CONFIG.resetPing();
            case KEYSTROKES -> FlintFixClient.CONFIG.resetKeystrokes();
            case ARMOR -> FlintFixClient.CONFIG.resetArmor();
            case CHUNKS -> FlintFixClient.CONFIG.resetChunks();
            case TRAJECTORY -> { FlintFixClient.CONFIG.trajectoryEnabled = false; save(); }
            case HITBOXES -> { FlintFixClient.CONFIG.hitboxesEnabled = false; save(); }
            case ZOOM -> { FlintFixClient.CONFIG.zoomEnabled = false; save(); }
            case LOOK_AROUND -> { FlintFixClient.CONFIG.lookAroundEnabled = false; save(); }
            case SHULKERS -> { FlintFixClient.CONFIG.shulkerPreviewEnabled = false; save(); }
        }
    }

    private void save() { FlintFixClient.CONFIG.save(); }

    private static String keyLabel(net.minecraft.client.option.KeyBinding binding) {
        return binding == null ? "Unbound" : binding.getBoundKeyLocalizedText().getString();
    }

    private void returnToModules() {
        save();
        if (client != null) {
            client.setScreen(parent instanceof FlintFixSettingsScreen ? parent : new FlintFixSettingsScreen(null));
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void close() {
        returnToModules();
    }
}
