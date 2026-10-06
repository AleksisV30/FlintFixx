package com.flintfix.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public final class FlintFixModuleSettingsScreen extends FlintFixScreen {
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
        SHULKERS("See in Shulkers", "Lists stored items when you hover over a shulker box."),
        INSPECT("Item Inspect", "Press a key to show off the item in your hand."),
        SHOW_HAND("Show Hand", "Draws your arm holding the item in first person."),
        FULLBRIGHT("Fullbright", "Lights every block fully, even in caves and at night.");

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
    private final long openedAt = System.currentTimeMillis();

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
        float intro = FlintFixUi.openProgress(openedAt);
        blurBehind(delta);
        FlintFixUi.backdrop(c, width, height, intro);
        FlintFixUi.pushPanelIntro(c, x, y, w, h, intro);
        FlintFixUi.panelFrame(c, x, y, w, h);

        FlintFixUi.header(c, x + 14, y + 11, w - 28 - 22, iconId(), module.title(), module.description());
        FlintFixUi.iconButton(c, "module-close", closeX(), y + 13, 15, "close",
            FlintFixUi.inside(mouseX, mouseY, closeX(), y + 13, 15, 15));
        FlintFixUi.hairline(c, x + 14, y + 42, w - 28);

        c.enableScissor(leftX - 1, listTop, leftX + leftW + 1, listBottom);
        int cy = listTop - scroll;
        if (module == Module.CHUNKS) {
            toggleRow(c, cy, "Show chunk borders", enabled(), mouseX, mouseY);
            infoCard(c, cy + 28, "Minecraft chunk grid", "Uses the native 16-block borders");
        } else if (module == Module.TRAJECTORY) {
            toggleRow(c, cy, "Show predicted path", enabled(), mouseX, mouseY);
            infoCard(c, cy + 28, "Pearl, bow, XP bottle, crossbow", "Path ends at the first block hit");
        } else if (module == Module.HITBOXES) {
            toggleRow(c, cy, "Show entity hitboxes", enabled(), mouseX, mouseY);
            infoCard(c, cy + 28, "Nearby players, mobs and items", "Wireframes are drawn in-world");
        } else if (module == Module.ZOOM) {
            toggleRow(c, cy, "Enable hold-to-zoom", enabled(), mouseX, mouseY);
            infoCard(c, cy + 28, "Hold key, scroll to adjust",
                "Key: " + keyLabel(FlintFixClient.getZoomKeyBinding()), "Rebind in Controls → FlintFix Client");
        } else if (module == Module.LOOK_AROUND) {
            toggleRow(c, cy, "Enable look-around", enabled(), mouseX, mouseY);
            infoCard(c, cy + 28, "Hold to move only the camera",
                "Key: " + keyLabel(FlintFixClient.getLookAroundKeyBinding()), "Rebind in Controls → FlintFix Client");
        } else if (module == Module.SHULKERS) {
            toggleRow(c, cy, "Show shulker contents", enabled(), mouseX, mouseY);
            infoCard(c, cy + 28, "Hover over a shulker box", "Stored items appear in its tooltip");
        } else if (module == Module.SHOW_HAND) {
            toggleRow(c, cy, "Show arm with items", enabled(), mouseX, mouseY);
            infoCard(c, cy + 28, "Your arm holds every item", "Item Inspect keeps the arm on screen");
        } else if (module == Module.FULLBRIGHT) {
            toggleRow(c, cy, "Enable fullbright", enabled(), mouseX, mouseY);
            infoCard(c, cy + 28, "Caves and nights stay fully lit", "Your Brightness setting is not changed");
        } else if (module == Module.INSPECT) {
            toggleRow(c, cy, "Enable item inspect", enabled(), mouseX, mouseY);
            infoCard(c, cy + 28, "Press the key to spin your item",
                "Key: " + keyLabel(FlintFixClient.getInspectKeyBinding()), "Rebind in Controls → FlintFix Client");
        } else {
            toggleRow(c, cy, "Enabled", enabled(), mouseX, mouseY);
            toggleRow(c, cy + 28, "Background", background(), mouseX, mouseY);
            sliderRow(c, cy + 58, "Background opacity", opacity(), 0f, 1f, Math.round(opacity() * 100f) + "%", draggingOpacity);
            sliderRow(c, cy + 94, "Scale", scale(), 0.25f, 2.0f, Math.round(scale() * 100f) + "%", draggingScale);
            if (module == Module.KEYSTROKES) {
                toggleRow(c, cy + 132, "Show CPS in LMB / RMB", FlintFixClient.CONFIG.keystrokesShowCps, mouseX, mouseY);
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
        FlintFixUi.finishPanelIntro(c, x, y, w, h, intro);
    }

    private int closeX() { return x + w - 14 - 15; }

    private String iconId() {
        return module.name().toLowerCase(java.util.Locale.ROOT).replace("_", "");
    }

    private void toggleRow(DrawContext c, int rowY, String label, boolean value, int mouseX, int mouseY) {
        boolean hover = mouseY >= listTop && mouseY <= listBottom
            && FlintFixUi.inside(mouseX, mouseY, leftX, rowY, leftW, 22);
        float t = FlintFixUi.hoverProgress("module-row:" + module + ":" + label, hover);
        FlintFixUi.surface(c, leftX, rowY, leftW, 22,
            FlintFixUi.blendColors(FlintFixUi.card(), FlintFixUi.raised(), t * 0.6f), FlintFixUi.border());
        FlintFixUi.drawTrimmedExact(c, label, leftX + 8, FlintFixFont.centeredY(rowY, 22, 7), leftW - 42, 7,
            FlintFixUi.text(), true);
        FlintFixUi.switchToggle(c, module + ":" + label, leftX + leftW - 28, rowY + 6, value);
    }

    private void sliderRow(DrawContext c, int rowY, String label, float value, float min, float max,
                           String valueText, boolean dragging) {
        FlintFixUi.surface(c, leftX, rowY, leftW, 30, FlintFixUi.card(), FlintFixUi.border());
        FlintFixFont.drawExact(c, label, leftX + 8, rowY + 6, 7, FlintFixUi.text(), true);
        FlintFixFont.drawExact(c, valueText, leftX + leftW - 8 - FlintFixFont.width(valueText, 7, true), rowY + 6, 7,
            FlintFixUi.accentBright(), true);
        FlintFixUi.slider(c, leftX + 8, rowY + 20, leftW - 16, (value - min) / (max - min), dragging);
    }

    private void infoCard(DrawContext c, int cardY, String title, String... lines) {
        int cardH = 22 + lines.length * 11;
        FlintFixUi.surface(c, leftX, cardY, leftW, cardH, FlintFixUi.panel(), FlintFixUi.border());
        FlintFixUi.drawTrimmedExact(c, title, leftX + 8, cardY + 7, leftW - 16, 7, FlintFixUi.text(), true);
        for (int i = 0; i < lines.length; i++) {
            FlintFixUi.drawTrimmedExact(c, lines[i], leftX + 8, cardY + 19 + i * 11, leftW - 16, 6,
                FlintFixUi.muted(), false);
        }
    }

    private void renderPreview(DrawContext c) {
        int previewH = Math.max(82, Math.min(132, listBottom - listTop));
        FlintFixUi.surface(c, rightX, listTop, rightW, previewH, FlintFixUi.panel(), FlintFixUi.border());
        FlintFixUi.sectionLabel(c, "PREVIEW", rightX + 8, listTop + 8);
        boolean active = enabled();
        FlintFixUi.badge(c, rightX + rightW - 7, listTop + 6, active ? "ON" : "OFF", active);

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
                points[points.length - 1][1] - 2, 5, 5, 2, FlintFixUi.text());
        } else if (module == Module.HITBOXES || module == Module.ZOOM || module == Module.LOOK_AROUND || module == Module.SHULKERS || module == Module.INSPECT
            || module == Module.SHOW_HAND || module == Module.FULLBRIGHT) {
            String[] lines = switch (module) {
                case HITBOXES -> new String[] {"PLAYER", "MOB", "ITEM"};
                case ZOOM -> new String[] {"HOLD " + keyLabel(FlintFixClient.getZoomKeyBinding()), "SCROLL TO ZOOM"};
                case LOOK_AROUND -> new String[] {"HOLD " + keyLabel(FlintFixClient.getLookAroundKeyBinding()), "CAMERA ONLY"};
                case SHULKERS -> new String[] {"SHULKER BOX", "STONE × 32", "TORCH × 12"};
                case SHOW_HAND -> new String[] {"ARM + ITEM", "INSPECT KEEPS HAND"};
                case FULLBRIGHT -> new String[] {"BRIGHTNESS 1600%", "NO DARK CAVES"};
                case INSPECT -> new String[] {"PRESS " + keyLabel(FlintFixClient.getInspectKeyBinding()), "ITEM SPINS IN HAND"};
                default -> new String[0];
            };
            int lineY = demoY + 12;
            for (String line : lines) {
                FlintFixUi.surface(c, demoX + 5, lineY - 2, Math.max(1, demoW - 10), 13, FlintFixUi.card(), FlintFixUi.border());
                FlintFixUi.drawTrimmedExact(c, line, demoX + 10, FlintFixFont.centeredY(lineY - 2, 13, 6), demoW - 20, 6,
                    FlintFixUi.text(), false);
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
            c.fill(x0, y0, x0 + 2, y0 + 2, FlintFixUi.accentBright());
            if (x0 == x1 && y0 == y1) break;
            int twice = 2 * error;
            if (twice >= dy) { error += dy; x0 += sx; }
            if (twice <= dx) { error += dx; y0 += sy; }
        }
    }

    private void renderScrollbar(DrawContext c) {
        FlintFixUi.scrollbar(c, leftX + leftW + 3, listTop, listBottom - listTop, scroll, maxScroll);
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
        if (FlintFixUi.inside(mouseX, mouseY, closeX(), y + 13, 15, 15)) {
            returnToModules();
            return true;
        }
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
            || module == Module.ZOOM || module == Module.LOOK_AROUND || module == Module.SHULKERS
            || module == Module.INSPECT || module == Module.SHOW_HAND || module == Module.FULLBRIGHT;
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
            case INSPECT -> FlintFixClient.CONFIG.itemInspectEnabled;
            case SHOW_HAND -> FlintFixClient.CONFIG.showHandEnabled;
            case FULLBRIGHT -> FlintFixClient.CONFIG.fullbrightEnabled;
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
            case INSPECT -> FlintFixClient.CONFIG.itemInspectEnabled = v;
            case SHOW_HAND -> FlintFixClient.CONFIG.showHandEnabled = v;
            case FULLBRIGHT -> FlintFixClient.CONFIG.fullbrightEnabled = v;
        }
    }

    private boolean background() {
        return switch (module) {
            case CPS -> FlintFixClient.CONFIG.cpsBackground;
            case COORDINATES -> FlintFixClient.CONFIG.coordinatesBackground;
            case PING -> FlintFixClient.CONFIG.pingBackground;
            case KEYSTROKES -> FlintFixClient.CONFIG.keystrokesBackground;
            case ARMOR -> FlintFixClient.CONFIG.armorBackground;
            case CHUNKS, TRAJECTORY, HITBOXES, ZOOM, LOOK_AROUND, SHULKERS, INSPECT, SHOW_HAND, FULLBRIGHT -> false;
        };
    }

    private void setBackground(boolean v) {
        switch (module) {
            case CPS -> FlintFixClient.CONFIG.cpsBackground = v;
            case COORDINATES -> FlintFixClient.CONFIG.coordinatesBackground = v;
            case PING -> FlintFixClient.CONFIG.pingBackground = v;
            case KEYSTROKES -> FlintFixClient.CONFIG.keystrokesBackground = v;
            case ARMOR -> FlintFixClient.CONFIG.armorBackground = v;
            case CHUNKS, TRAJECTORY, HITBOXES, ZOOM, LOOK_AROUND, SHULKERS, INSPECT, SHOW_HAND, FULLBRIGHT -> { }
        }
    }

    private float opacity() {
        return switch (module) {
            case CPS -> FlintFixClient.CONFIG.cpsBackgroundOpacity;
            case COORDINATES -> FlintFixClient.CONFIG.coordinatesBackgroundOpacity;
            case PING -> FlintFixClient.CONFIG.pingBackgroundOpacity;
            case KEYSTROKES -> FlintFixClient.CONFIG.keystrokesBackgroundOpacity;
            case ARMOR -> FlintFixClient.CONFIG.armorBackgroundOpacity;
            case CHUNKS, TRAJECTORY, HITBOXES, ZOOM, LOOK_AROUND, SHULKERS, INSPECT, SHOW_HAND, FULLBRIGHT -> 0.0f;
        };
    }

    private void setOpacity(float v) {
        switch (module) {
            case CPS -> FlintFixClient.CONFIG.cpsBackgroundOpacity = v;
            case COORDINATES -> FlintFixClient.CONFIG.coordinatesBackgroundOpacity = v;
            case PING -> FlintFixClient.CONFIG.pingBackgroundOpacity = v;
            case KEYSTROKES -> FlintFixClient.CONFIG.keystrokesBackgroundOpacity = v;
            case ARMOR -> FlintFixClient.CONFIG.armorBackgroundOpacity = v;
            case CHUNKS, TRAJECTORY, HITBOXES, ZOOM, LOOK_AROUND, SHULKERS, INSPECT, SHOW_HAND, FULLBRIGHT -> { }
        }
    }

    private float scale() {
        return switch (module) {
            case CPS -> FlintFixClient.CONFIG.cpsScale;
            case COORDINATES -> FlintFixClient.CONFIG.coordinatesScale;
            case PING -> FlintFixClient.CONFIG.pingScale;
            case KEYSTROKES -> FlintFixClient.CONFIG.keystrokesScale;
            case ARMOR -> FlintFixClient.CONFIG.armorScale;
            case CHUNKS, TRAJECTORY, HITBOXES, ZOOM, LOOK_AROUND, SHULKERS, INSPECT, SHOW_HAND, FULLBRIGHT -> 1.0f;
        };
    }

    private void setScale(float v) {
        switch (module) {
            case CPS -> FlintFixClient.CONFIG.cpsScale = v;
            case COORDINATES -> FlintFixClient.CONFIG.coordinatesScale = v;
            case PING -> FlintFixClient.CONFIG.pingScale = v;
            case KEYSTROKES -> FlintFixClient.CONFIG.keystrokesScale = v;
            case ARMOR -> FlintFixClient.CONFIG.armorScale = v;
            case CHUNKS, TRAJECTORY, HITBOXES, ZOOM, LOOK_AROUND, SHULKERS, INSPECT, SHOW_HAND, FULLBRIGHT -> { }
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
            case INSPECT -> { FlintFixClient.CONFIG.itemInspectEnabled = true; save(); }
            case SHOW_HAND -> { FlintFixClient.CONFIG.showHandEnabled = false; save(); }
            case FULLBRIGHT -> { FlintFixClient.CONFIG.fullbrightEnabled = false; save(); }
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
