package com.flintfix.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public final class FlintFixHudEditorScreen extends Screen {
    private enum Module { FPS, CPS, COORDINATES, PING, KEYSTROKES, ARMOR }
    private enum ResizeCorner { NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }
    private static final int GRID_SIZE = 8;
    private static final int SNAP_RANGE = 10;
    private static final int CONNECT_GAP = 0;

    private final Screen parent;
    private Module selected;
    private boolean dragging;
    private boolean resizing;
    private ResizeCorner resizeCorner = ResizeCorner.NONE;
    private double offsetX, offsetY;
    private double anchorX, anchorY, resizeStartDistance;
    private int resizeStartWidth, resizeStartHeight;
    private float resizeStartScale;
    private int snapGuideX = -1;
    private int snapGuideY = -1;
    private boolean snappedToWidget;

    private FlintFixClient.HudBounds fpsBounds = new FlintFixClient.HudBounds(0,0,0,0);
    private FlintFixClient.HudBounds cpsBounds = new FlintFixClient.HudBounds(0,0,0,0);
    private FlintFixClient.HudBounds coordinatesBounds = new FlintFixClient.HudBounds(0,0,0,0);
    private FlintFixClient.HudBounds pingBounds = new FlintFixClient.HudBounds(0,0,0,0);
    private FlintFixClient.HudBounds keysBounds = new FlintFixClient.HudBounds(0,0,0,0);
    private FlintFixClient.HudBounds armorBounds = new FlintFixClient.HudBounds(0,0,0,0);

    private boolean snapshotTaken;
    private final long transitionStartedAt = System.currentTimeMillis();
    private static final long TRANSITION_MS = 190L;
    private float oldFpsX, oldFpsY, oldCpsX, oldCpsY, oldCoordinatesX, oldCoordinatesY;
    private float oldPingX, oldPingY, oldKeysX, oldKeysY, oldArmorX, oldArmorY;
    private float oldFpsScale, oldCpsScale, oldCoordinatesScale, oldPingScale, oldKeysScale, oldArmorScale;

    public FlintFixHudEditorScreen(Screen parent) {
        super(Text.literal("FlintFix HUD Editor"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (!snapshotTaken) {
            captureSnapshot();
            snapshotTaken = true;
        }
    }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        c.fill(0, 0, width, height, 0x52000000);
        int step = GRID_SIZE * 2;
        for (int gx = 0; gx < width; gx += step) c.fill(gx, 0, gx + 1, height, 0x12222222);
        for (int gy = 0; gy < height; gy += step) c.fill(0, gy, width, gy + 1, 0x12222222);

        float t = Math.min(1.0f, (System.currentTimeMillis() - transitionStartedAt) / (float) TRANSITION_MS);
        float eased = t * t * (3.0f - 2.0f * t);
        float scale = 0.96f + 0.04f * eased;
        c.getMatrices().push();
        c.getMatrices().translate(width / 2.0f, height / 2.0f + (1.0f - eased) * 4.0f, 0.0f);
        c.getMatrices().scale(scale, scale, 1.0f);
        c.getMatrices().translate(-width / 2.0f, -height / 2.0f, 0.0f);

        int topW = Math.max(1, Math.min(280, width - 24));
        int topX = (width - topW) / 2;
        FlintFixUi.fadeOutline(c, topX, 8, topW, 36, 7, FlintFixUi.ACCENT);
        FlintFixUi.rounded(c, topX, 8, topW, 36, 7, 0xFF10151D);
        FlintFixFont.drawCentered(c, "HUD EDITOR", width / 2, 12, 10, FlintFixClient.TEXT, true);
        FlintFixFont.drawCentered(c, "8px GRID  |  MAGNETIC ALIGN  |  DRAG CORNERS TO RESIZE", width / 2, 27, 6,
            FlintFixClient.MUTED, false);

        MinecraftClient mc = MinecraftClient.getInstance();
        fpsBounds = FlintFixClient.renderFpsHud(c, mc, true, selected == Module.FPS);
        cpsBounds = FlintFixClient.renderCpsHud(c, mc, true, selected == Module.CPS);
        coordinatesBounds = FlintFixClient.renderCoordinatesHud(c, mc, true, selected == Module.COORDINATES);
        pingBounds = FlintFixClient.renderPingHud(c, mc, true, selected == Module.PING);
        keysBounds = FlintFixClient.renderKeystrokesHud(c, mc, true, selected == Module.KEYSTROKES);
        armorBounds = FlintFixClient.renderArmorHud(c, mc, true, selected == Module.ARMOR);
        drawConnectedBorders(c);
        drawSnapGuides(c);
        drawResizeHandles(c);

        int bottomW = Math.max(1, Math.min(244, width - 20));
        int bottomX = (width - bottomW) / 2;
        int bottomY = Math.max(4, height - 35);
        FlintFixUi.fadeOutline(c, bottomX, bottomY, bottomW, 27, 7, FlintFixUi.ACCENT);
        FlintFixUi.rounded(c, bottomX, bottomY, bottomW, 27, 7, 0xFF10151D);

        int saveX = bottomX + bottomW - 55;
        int resetX = saveX - 52;
        int cancelX = resetX - 52;
        FlintFixUi.compactButton(c, saveX, bottomY + 5, 48, 17, "SAVE", FlintFixUi.inside(mouseX, mouseY, saveX, bottomY + 5, 48, 17), true);
        FlintFixUi.compactButton(c, resetX, bottomY + 5, 48, 17, "RESET", FlintFixUi.inside(mouseX, mouseY, resetX, bottomY + 5, 48, 17), false);
        FlintFixUi.compactButton(c, cancelX, bottomY + 5, 48, 17, "CANCEL", FlintFixUi.inside(mouseX, mouseY, cancelX, bottomY + 5, 48, 17), false);
        FlintFixUi.drawTrimmed(c, selected == null ? "SELECT A HUD" : selected.name(), bottomX + 8,
            bottomY + 10, Math.max(0, cancelX - bottomX - 14), 5, 0xFFB7AFBE, true);
        c.getMatrices().pop();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (System.currentTimeMillis() - transitionStartedAt < TRANSITION_MS) return true;
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int bottomW = Math.max(1, Math.min(244, width - 20));
        int bottomX = (width - bottomW) / 2;
        int bottomY = Math.max(4, height - 35);
        int saveX = bottomX + bottomW - 55;
        int resetX = saveX - 52;
        int cancelX = resetX - 52;

        if (FlintFixUi.inside(mouseX, mouseY, saveX, bottomY + 5, 48, 17)) {
            saveAndClose(); return true;
        }
        if (FlintFixUi.inside(mouseX, mouseY, resetX, bottomY + 5, 48, 17)) {
            FlintFixClient.CONFIG.resetHudPositions();
            selected = null;
            dragging = false;
            resizing = false;
            resizeCorner = ResizeCorner.NONE;
            snapGuideX = -1;
            snapGuideY = -1;
            snappedToWidget = false;
            return true;
        }
        if (FlintFixUi.inside(mouseX, mouseY, cancelX, bottomY + 5, 48, 17)) {
            cancelAndClose(); return true;
        }

        // The selected handles extend a few pixels beyond the widget itself, so
        // test them first to make every part of a corner handle easy to grab.
        if (selected != null) {
            FlintFixClient.HudBounds selectedBounds = boundsFor(selected);
            ResizeCorner selectedCorner = hitCorner(mouseX, mouseY, selectedBounds);
            if (selectedCorner != ResizeCorner.NONE) {
                beginResize(selected, selectedCorner, selectedBounds, mouseX, mouseY);
                return true;
            }
        }

        Module hit = hitModule(mouseX, mouseY);
        if (hit != null) {
            selected = hit;
            FlintFixClient.HudBounds b = boundsFor(hit);
            ResizeCorner corner = hitCorner(mouseX, mouseY, b);
            if (corner != ResizeCorner.NONE) {
                beginResize(hit, corner, b, mouseX, mouseY);
                return true;
            }
            resizing = false;
            resizeCorner = ResizeCorner.NONE;
            dragging = true;
            snapGuideX = -1;
            snapGuideY = -1;
            snappedToWidget = false;
            offsetX = mouseX - b.x();
            offsetY = mouseY - b.y();
            return true;
        }
        selected = null;
        dragging = false;
        resizing = false;
        resizeCorner = ResizeCorner.NONE;
        snapGuideX = -1;
        snapGuideY = -1;
        snappedToWidget = false;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && resizing && selected != null) {
            resizeSelected(mouseX, mouseY);
            return true;
        }
        if (button == 0 && dragging && selected != null) {
            moveSelected(mouseX - offsetX, mouseY - offsetY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && (dragging || resizing)) {
            if (selected != null) {
                if (resizing) resizeSelected(mouseX, mouseY);
                else moveSelected(mouseX - offsetX, mouseY - offsetY);
            }
            dragging = false;
            resizing = false;
            resizeCorner = ResizeCorner.NONE;
            snapGuideX = -1;
            snapGuideY = -1;
            snappedToWidget = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private Module hitModule(double mx, double my) {
        if (armorBounds.contains(mx, my)) return Module.ARMOR;
        if (keysBounds.contains(mx, my)) return Module.KEYSTROKES;
        if (pingBounds.contains(mx, my)) return Module.PING;
        if (coordinatesBounds.contains(mx, my)) return Module.COORDINATES;
        if (cpsBounds.contains(mx, my)) return Module.CPS;
        if (fpsBounds.contains(mx, my)) return Module.FPS;
        return null;
    }

    private ResizeCorner hitCorner(double mx, double my, FlintFixClient.HudBounds b) {
        int handle = 9;
        if (FlintFixUi.inside(mx, my, b.x() - handle / 2, b.y() - handle / 2, handle, handle)) {
            return ResizeCorner.TOP_LEFT;
        }
        if (FlintFixUi.inside(mx, my, b.x() + b.width() - handle / 2, b.y() - handle / 2, handle, handle)) {
            return ResizeCorner.TOP_RIGHT;
        }
        if (FlintFixUi.inside(mx, my, b.x() - handle / 2, b.y() + b.height() - handle / 2, handle, handle)) {
            return ResizeCorner.BOTTOM_LEFT;
        }
        if (FlintFixUi.inside(mx, my, b.x() + b.width() - handle / 2,
            b.y() + b.height() - handle / 2, handle, handle)) {
            return ResizeCorner.BOTTOM_RIGHT;
        }
        return ResizeCorner.NONE;
    }

    private void drawResizeHandles(DrawContext c) {
        if (selected == null) return;
        FlintFixClient.HudBounds b = boundsFor(selected);
        if (b.width() <= 0 || b.height() <= 0) return;
        int size = 6;
        int left = b.x() - size / 2;
        int top = b.y() - size / 2;
        int right = b.x() + b.width() - size / 2;
        int bottom = b.y() + b.height() - size / 2;
        int handle = FlintFixUi.ACCENT_BRIGHT;
        FlintFixUi.rounded(c, left, top, size, size, 2, handle);
        FlintFixUi.rounded(c, right, top, size, size, 2, handle);
        FlintFixUi.rounded(c, left, bottom, size, size, 2, handle);
        FlintFixUi.rounded(c, right, bottom, size, size, 2, handle);
    }

    private void beginResize(Module module, ResizeCorner corner, FlintFixClient.HudBounds b,
                             double mouseX, double mouseY) {
        resizing = true;
        dragging = false;
        resizeCorner = corner;
        resizeStartScale = scaleFor(module);
        resizeStartWidth = b.width();
        resizeStartHeight = b.height();
        boolean left = corner == ResizeCorner.TOP_LEFT || corner == ResizeCorner.BOTTOM_LEFT;
        boolean top = corner == ResizeCorner.TOP_LEFT || corner == ResizeCorner.TOP_RIGHT;
        anchorX = left ? b.x() + b.width() : b.x();
        anchorY = top ? b.y() + b.height() : b.y();
        resizeStartDistance = Math.max(1.0, Math.hypot(mouseX - anchorX, mouseY - anchorY));
    }

    private void resizeSelected(double mouseX, double mouseY) {
        if (selected == null || resizeCorner == ResizeCorner.NONE || resizeStartScale <= 0.0f) return;
        double distance = Math.max(1.0, Math.hypot(mouseX - anchorX, mouseY - anchorY));
        float nextScale = (float)(resizeStartScale * distance / resizeStartDistance);
        nextScale = Math.max(0.25f, Math.min(2.0f, nextScale));
        setScale(selected, nextScale);

        int newWidth = Math.max(1, Math.round(resizeStartWidth * nextScale / resizeStartScale));
        int newHeight = Math.max(1, Math.round(resizeStartHeight * nextScale / resizeStartScale));
        boolean left = resizeCorner == ResizeCorner.TOP_LEFT || resizeCorner == ResizeCorner.BOTTOM_LEFT;
        boolean top = resizeCorner == ResizeCorner.TOP_LEFT || resizeCorner == ResizeCorner.TOP_RIGHT;
        int newX = left ? (int)Math.round(anchorX - newWidth) : (int)Math.round(anchorX);
        int newY = top ? (int)Math.round(anchorY - newHeight) : (int)Math.round(anchorY);
        newX = Math.max(0, Math.min(Math.max(0, width - newWidth), newX));
        newY = Math.max(0, Math.min(Math.max(0, height - newHeight), newY));
        setNormalizedPosition(selected, newX, newY, newWidth, newHeight);
    }

    private float scaleFor(Module module) {
        return switch (module) {
            case FPS -> FlintFixClient.CONFIG.fpsScale;
            case CPS -> FlintFixClient.CONFIG.cpsScale;
            case COORDINATES -> FlintFixClient.CONFIG.coordinatesScale;
            case PING -> FlintFixClient.CONFIG.pingScale;
            case KEYSTROKES -> FlintFixClient.CONFIG.keystrokesScale;
            case ARMOR -> FlintFixClient.CONFIG.armorScale;
        };
    }

    private void setScale(Module module, float scale) {
        switch (module) {
            case FPS -> FlintFixClient.CONFIG.fpsScale = scale;
            case CPS -> FlintFixClient.CONFIG.cpsScale = scale;
            case COORDINATES -> FlintFixClient.CONFIG.coordinatesScale = scale;
            case PING -> FlintFixClient.CONFIG.pingScale = scale;
            case KEYSTROKES -> FlintFixClient.CONFIG.keystrokesScale = scale;
            case ARMOR -> FlintFixClient.CONFIG.armorScale = scale;
        }
    }

    private FlintFixClient.HudBounds boundsFor(Module module) {
        return switch (module) {
            case FPS -> fpsBounds;
            case CPS -> cpsBounds;
            case COORDINATES -> coordinatesBounds;
            case PING -> pingBounds;
            case KEYSTROKES -> keysBounds;
            case ARMOR -> armorBounds;
        };
    }

    private void moveSelected(double newX, double newY) {
        FlintFixClient.HudBounds b = boundsFor(selected);
        int availableW = Math.max(1, width - b.width());
        int availableH = Math.max(1, height - b.height());
        int x = (int)Math.round(Math.max(0, Math.min(availableW, newX)));
        int y = (int)Math.round(Math.max(0, Math.min(availableH, newY)));
        x = clampPosition(snapToGrid(x), 0, availableW);
        y = clampPosition(snapToGrid(y), 0, availableH);

        SnapPoint connection = findConnectionSnap(x, y, b);
        snappedToWidget = connection != null;
        if (connection != null) {
            x = connection.x();
            y = connection.y();
        }
        x = clampPosition(x, 0, availableW);
        y = clampPosition(y, 0, availableH);
        snapGuideX = x;
        snapGuideY = y;
        setNormalizedPosition(selected, x, y, b.width(), b.height());
    }

    private int snapToGrid(int value) {
        return Math.round(value / (float)GRID_SIZE) * GRID_SIZE;
    }

    private SnapPoint findConnectionSnap(int x, int y, FlintFixClient.HudBounds moving) {
        SnapPoint best = null;
        for (Module otherModule : Module.values()) {
            if (otherModule == selected) continue;
            FlintFixClient.HudBounds other = boundsFor(otherModule);
            if (other.width() <= 0 || other.height() <= 0) continue;

            int[] alignedY = {
                other.y(),
                other.y() + (other.height() - moving.height()) / 2,
                other.y() + other.height() - moving.height()
            };
            int[] adjacentX = {
                other.x() - moving.width() - CONNECT_GAP,
                other.x() + other.width() + CONNECT_GAP
            };
            for (int candidateX : adjacentX) {
                for (int candidateY : alignedY) {
                    best = chooseSnap(best, x, y, candidateX, candidateY);
                }
            }

            int[] alignedX = {
                other.x(),
                other.x() + (other.width() - moving.width()) / 2,
                other.x() + other.width() - moving.width()
            };
            int[] adjacentY = {
                other.y() - moving.height() - CONNECT_GAP,
                other.y() + other.height() + CONNECT_GAP
            };
            for (int candidateY : adjacentY) {
                for (int candidateX : alignedX) {
                    best = chooseSnap(best, x, y, candidateX, candidateY);
                }
            }
        }
        return best;
    }

    private SnapPoint chooseSnap(SnapPoint best, int x, int y, int candidateX, int candidateY) {
        int dx = Math.abs(candidateX - x);
        int dy = Math.abs(candidateY - y);
        if (dx > SNAP_RANGE || dy > SNAP_RANGE) return best;
        int distance = dx + dy;
        if (best == null || distance < best.distance()) {
            return new SnapPoint(clampPosition(candidateX, 0, Math.max(0, width)),
                clampPosition(candidateY, 0, Math.max(0, height)), distance);
        }
        return best;
    }

    private void drawSnapGuides(DrawContext c) {
        if (!dragging || selected == null || snapGuideX < 0 || snapGuideY < 0) return;
        int color = snappedToWidget ? 0xA671D687 : 0x706E879E;
        c.fill(snapGuideX, 0, snapGuideX + 1, height, color);
        c.fill(0, snapGuideY, width, snapGuideY + 1, color);
        if (snappedToWidget) drawWidgetBorder(c, boundsFor(selected));
    }

    private void drawConnectedBorders(DrawContext c) {
        Module[] modules = Module.values();
        for (int i = 0; i < modules.length; i++) {
            FlintFixClient.HudBounds a = boundsFor(modules[i]);
            if (a.width() <= 0 || a.height() <= 0) continue;
            for (int j = i + 1; j < modules.length; j++) {
                FlintFixClient.HudBounds b = boundsFor(modules[j]);
                if (b.width() <= 0 || b.height() <= 0) continue;
                if (isSideConnected(a, b)) {
                    drawWidgetBorder(c, a);
                    drawWidgetBorder(c, b);
                    drawConnectionBridge(c, a, b);
                }
            }
        }
    }

    private boolean isSideConnected(FlintFixClient.HudBounds a, FlintFixClient.HudBounds b) {
        int verticalOverlap = Math.min(a.y() + a.height(), b.y() + b.height()) - Math.max(a.y(), b.y());
        int horizontalOverlap = Math.min(a.x() + a.width(), b.x() + b.width()) - Math.max(a.x(), b.x());
        boolean horizontal = verticalOverlap >= 8 && (
            Math.abs(a.x() + a.width() + CONNECT_GAP - b.x()) <= 1 ||
            Math.abs(b.x() + b.width() + CONNECT_GAP - a.x()) <= 1);
        boolean vertical = horizontalOverlap >= 8 && (
            Math.abs(a.y() + a.height() + CONNECT_GAP - b.y()) <= 1 ||
            Math.abs(b.y() + b.height() + CONNECT_GAP - a.y()) <= 1);
        return horizontal || vertical;
    }

    private void drawWidgetBorder(DrawContext c, FlintFixClient.HudBounds b) {
        int color = 0xB97C8793;
        c.fill(b.x(), b.y(), b.x() + b.width(), b.y() + 1, color);
        c.fill(b.x(), b.y() + b.height() - 1, b.x() + b.width(), b.y() + b.height(), color);
        c.fill(b.x(), b.y(), b.x() + 1, b.y() + b.height(), color);
        c.fill(b.x() + b.width() - 1, b.y(), b.x() + b.width(), b.y() + b.height(), color);
    }

    private void drawConnectionBridge(DrawContext c, FlintFixClient.HudBounds a, FlintFixClient.HudBounds b) {
        int color = 0xCC71D687;
        int verticalOverlapTop = Math.max(a.y(), b.y()) + 3;
        int verticalOverlapBottom = Math.min(a.y() + a.height(), b.y() + b.height()) - 3;
        if (Math.abs(a.x() + a.width() + CONNECT_GAP - b.x()) <= 1) {
            if (verticalOverlapBottom <= verticalOverlapTop) return;
            int x = a.x() + a.width() + CONNECT_GAP / 2;
            c.fill(x, verticalOverlapTop, x + 1, verticalOverlapBottom, color);
        } else if (Math.abs(b.x() + b.width() + CONNECT_GAP - a.x()) <= 1) {
            if (verticalOverlapBottom <= verticalOverlapTop) return;
            int x = b.x() + b.width() + CONNECT_GAP / 2;
            c.fill(x, verticalOverlapTop, x + 1, verticalOverlapBottom, color);
        } else {
            int left = Math.max(a.x(), b.x()) + 3;
            int right = Math.min(a.x() + a.width(), b.x() + b.width()) - 3;
            if (right <= left) return;
            int y = Math.abs(a.y() + a.height() + CONNECT_GAP - b.y()) <= 1
                ? a.y() + a.height() + CONNECT_GAP / 2
                : b.y() + b.height() + CONNECT_GAP / 2;
            c.fill(left, y, right, y + 1, color);
        }
    }

    private int clampPosition(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record SnapPoint(int x, int y, int distance) {}

    private void setNormalizedPosition(Module module, int x, int y, int objectW, int objectH) {
        int availableW = Math.max(1, width - objectW);
        int availableH = Math.max(1, height - objectH);
        float nx = (float)Math.max(0, Math.min(availableW, x)) / availableW;
        float ny = (float)Math.max(0, Math.min(availableH, y)) / availableH;
        switch (module) {
            case FPS -> { FlintFixClient.CONFIG.fpsX = nx; FlintFixClient.CONFIG.fpsY = ny; }
            case CPS -> { FlintFixClient.CONFIG.cpsX = nx; FlintFixClient.CONFIG.cpsY = ny; }
            case COORDINATES -> { FlintFixClient.CONFIG.coordinatesX = nx; FlintFixClient.CONFIG.coordinatesY = ny; }
            case PING -> { FlintFixClient.CONFIG.pingX = nx; FlintFixClient.CONFIG.pingY = ny; }
            case KEYSTROKES -> { FlintFixClient.CONFIG.keystrokesX = nx; FlintFixClient.CONFIG.keystrokesY = ny; }
            case ARMOR -> { FlintFixClient.CONFIG.armorX = nx; FlintFixClient.CONFIG.armorY = ny; }
        }
    }

    private void captureSnapshot() {
        oldFpsX = FlintFixClient.CONFIG.fpsX; oldFpsY = FlintFixClient.CONFIG.fpsY;
        oldCpsX = FlintFixClient.CONFIG.cpsX; oldCpsY = FlintFixClient.CONFIG.cpsY;
        oldCoordinatesX = FlintFixClient.CONFIG.coordinatesX; oldCoordinatesY = FlintFixClient.CONFIG.coordinatesY;
        oldPingX = FlintFixClient.CONFIG.pingX; oldPingY = FlintFixClient.CONFIG.pingY;
        oldKeysX = FlintFixClient.CONFIG.keystrokesX; oldKeysY = FlintFixClient.CONFIG.keystrokesY;
        oldArmorX = FlintFixClient.CONFIG.armorX; oldArmorY = FlintFixClient.CONFIG.armorY;
        oldFpsScale = FlintFixClient.CONFIG.fpsScale; oldCpsScale = FlintFixClient.CONFIG.cpsScale;
        oldCoordinatesScale = FlintFixClient.CONFIG.coordinatesScale; oldPingScale = FlintFixClient.CONFIG.pingScale;
        oldKeysScale = FlintFixClient.CONFIG.keystrokesScale; oldArmorScale = FlintFixClient.CONFIG.armorScale;
    }

    private void restoreSnapshot() {
        FlintFixClient.CONFIG.fpsX = oldFpsX; FlintFixClient.CONFIG.fpsY = oldFpsY;
        FlintFixClient.CONFIG.cpsX = oldCpsX; FlintFixClient.CONFIG.cpsY = oldCpsY;
        FlintFixClient.CONFIG.coordinatesX = oldCoordinatesX; FlintFixClient.CONFIG.coordinatesY = oldCoordinatesY;
        FlintFixClient.CONFIG.pingX = oldPingX; FlintFixClient.CONFIG.pingY = oldPingY;
        FlintFixClient.CONFIG.keystrokesX = oldKeysX; FlintFixClient.CONFIG.keystrokesY = oldKeysY;
        FlintFixClient.CONFIG.armorX = oldArmorX; FlintFixClient.CONFIG.armorY = oldArmorY;
        FlintFixClient.CONFIG.fpsScale = oldFpsScale; FlintFixClient.CONFIG.cpsScale = oldCpsScale;
        FlintFixClient.CONFIG.coordinatesScale = oldCoordinatesScale; FlintFixClient.CONFIG.pingScale = oldPingScale;
        FlintFixClient.CONFIG.keystrokesScale = oldKeysScale; FlintFixClient.CONFIG.armorScale = oldArmorScale;
    }

    private void saveAndClose() {
        FlintFixClient.CONFIG.save();
        captureSnapshot();
        if (client != null) client.setScreen(parent);
    }

    private void cancelAndClose() {
        restoreSnapshot();
        if (client != null) client.setScreen(parent);
    }

    public void cancelToGame() {
        restoreSnapshot();
        if (client != null) client.setScreen(null);
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void close() {
        cancelAndClose();
    }
}
