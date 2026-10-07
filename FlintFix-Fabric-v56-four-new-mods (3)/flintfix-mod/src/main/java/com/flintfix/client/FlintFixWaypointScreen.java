package com.flintfix.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Lists the waypoints of the current world: add, show or hide, recolor and delete. */
public final class FlintFixWaypointScreen extends FlintFixScreen {
    private static final int MAX_W = 340;
    private static final int MAX_H = 236;
    private static final int ROW_H = 24;

    private final Screen parent;
    private final long openedAt = System.currentTimeMillis();
    private EditBox nameField;
    private int x, y, w, h, listTop, listBottom;
    private int scroll, maxScroll;

    public FlintFixWaypointScreen(Screen parent) {
        super(Component.literal("Waypoints"));
        this.parent = parent;
    }

    private void layout() {
        w = Math.min(MAX_W, Math.max(1, width - 24));
        h = Math.min(MAX_H, Math.max(1, height - 24));
        x = (width - w) / 2;
        y = (height - h) / 2;
        listTop = y + 72;
        listBottom = y + h - 30;
        int count = Minecraft.getInstance().level == null ? 0 : FlintFixWaypoints.currentWorld(Minecraft.getInstance()).size();
        maxScroll = Math.max(0, count * ROW_H - (listBottom - listTop));
        scroll = Math.max(0, Math.min(maxScroll, scroll));
    }

    @Override
    protected void init() {
        layout();
        // init() runs again on resize; keep what was typed.
        String typed = nameField == null ? "" : nameField.getValue();
        nameField = new EditBox(font, x + 14, y + 50, w - 28 - 72, 16, Component.literal("Waypoint name"));
        nameField.setMaxLength(32);
        nameField.setValue(typed);
        nameField.setHint(Component.literal("Name (optional)"));
        addRenderableWidget(nameField);
    }

    @Override
    public void render(GuiGraphics c, int mouseX, int mouseY, float delta) {
        layout();
        float intro = FlintFixUi.openProgress(openedAt);
        blurBehind(c, delta);
        FlintFixUi.backdrop(c, width, height, intro);
        FlintFixUi.panelFrame(c, x, y, w, h);
        Minecraft mc = Minecraft.getInstance();
        String where = mc.level == null ? "Join a world to add waypoints"
            : FlintFixWaypoints.worldKey(mc).replaceFirst("^(sp|mp):", "") + "  ·  " + prettyDimension(FlintFixWaypoints.dimension(mc));
        FlintFixUi.header(c, x + 14, y + 11, w - 28 - 22, "waypoints", "Waypoints", where);
        FlintFixUi.iconButton(c, "waypoints-close", closeX(), y + 13, 15, "close",
            FlintFixUi.inside(mouseX, mouseY, closeX(), y + 13, 15, 15));
        FlintFixUi.hairline(c, x + 14, y + 42, w - 28);

        // Drawn directly: Screen#render would blur the background a second time this frame.
        nameField.render(c, mouseX, mouseY, delta);
        int addX = x + w - 14 - 66;
        FlintFixUi.actionButton(c, addX, y + 50, 66, 16, "ADD HERE",
            FlintFixUi.inside(mouseX, mouseY, addX, y + 50, 66, 16), FlintFixUi.ButtonStyle.PRIMARY);

        List<FlintFixWaypoints.Waypoint> list = mc.level == null ? List.of() : FlintFixWaypoints.currentWorld(mc);
        c.enableScissor(x + 10, listTop, x + w - 10, listBottom);
        int rowY = listTop - scroll;
        for (int i = 0; i < list.size(); i++) {
            FlintFixWaypoints.Waypoint waypoint = list.get(i);
            if (rowY + ROW_H >= listTop && rowY <= listBottom) renderRow(c, mc, waypoint, rowY, mouseX, mouseY);
            rowY += ROW_H;
        }
        if (list.isEmpty()) {
            FlintFixFont.drawCenteredExact(c, "No waypoints yet. Press " + keyLabel() + " in game or use Add here.",
                x + w / 2, listTop + 16, 6, FlintFixUi.muted(), false);
        }
        c.disableScissor();
        FlintFixUi.scrollbar(c, x + w - 9, listTop, listBottom - listTop, scroll, maxScroll);

        FlintFixUi.actionButton(c, x + w - 68, y + h - 24, 54, 16, "BACK",
            FlintFixUi.inside(mouseX, mouseY, x + w - 68, y + h - 24, 54, 16), FlintFixUi.ButtonStyle.SECONDARY);
    }

    private void renderRow(GuiGraphics c, Minecraft mc, FlintFixWaypoints.Waypoint waypoint, int rowY, int mouseX, int mouseY) {
        int rowX = x + 14;
        int rowW = w - 28 - 6;
        boolean hover = mouseY >= listTop && mouseY <= listBottom && FlintFixUi.inside(mouseX, mouseY, rowX, rowY, rowW, ROW_H - 3);
        FlintFixUi.surface(c, rowX, rowY, rowW, ROW_H - 3,
            FlintFixUi.blendColors(FlintFixUi.card(), FlintFixUi.raised(), hover ? 0.5f : 0.0f), FlintFixUi.border());
        FlintFixUi.roundedRaw(c, rowX + 6, rowY + 5, 11, 11, 3, waypoint.color);
        boolean here = mc.level != null && FlintFixWaypoints.dimension(mc).equals(waypoint.dimension);
        int textColor = waypoint.visible ? FlintFixUi.text() : FlintFixUi.subtle();
        FlintFixUi.drawTrimmedExact(c, waypoint.name, rowX + 23, rowY + 3, rowW - 140, 7, textColor, true);
        String coords = waypoint.x + ", " + waypoint.y + ", " + waypoint.z
            + (here ? "" : "  ·  " + prettyDimension(waypoint.dimension));
        FlintFixUi.drawTrimmedExact(c, coords, rowX + 23, rowY + 12, rowW - 140, 6, FlintFixUi.muted(), false);
        if (here && mc.player != null) {
            double distance = Math.sqrt(mc.player.distanceToSqr(waypoint.x + 0.5, waypoint.y, waypoint.z + 0.5));
            String text = Math.round(distance) + " m";
            FlintFixFont.drawExact(c, text, rowX + rowW - 66 - FlintFixFont.width(text, 6, true), FlintFixFont.centeredY(rowY, ROW_H - 3, 6), 6,
                FlintFixUi.muted(), true);
        }
        FlintFixUi.switchToggle(c, "waypoint:" + System.identityHashCode(waypoint), rowX + rowW - 52, rowY + 5, waypoint.visible);
        FlintFixUi.iconButton(c, "waypoint-del:" + System.identityHashCode(waypoint), rowX + rowW - 24, rowY + 3, 15, "close",
            FlintFixUi.inside(mouseX, mouseY, rowX + rowW - 24, rowY + 3, 15, 15));
    }

    private int closeX() {
        return x + w - 14 - 15;
    }

    private static String keyLabel() {
        return FlintFixClient.getWaypointKeyBinding() == null ? "B"
            : FlintFixClient.getWaypointKeyBinding().getTranslatedKeyMessage().getString();
    }

    private static String prettyDimension(String id) {
        if (id == null) return "";
        return switch (id) {
            case "minecraft:overworld" -> "Overworld";
            case "minecraft:the_nether" -> "Nether";
            case "minecraft:the_end" -> "The End";
            default -> id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        layout();
        if (button == 0) {
            Minecraft mc = Minecraft.getInstance();
            if (FlintFixUi.inside(mouseX, mouseY, closeX(), y + 13, 15, 15)
                || FlintFixUi.inside(mouseX, mouseY, x + w - 68, y + h - 24, 54, 16)) {
                onClose();
                return true;
            }
            int addX = x + w - 14 - 66;
            if (FlintFixUi.inside(mouseX, mouseY, addX, y + 50, 66, 16)) {
                if (FlintFixWaypoints.addHere(mc, nameField.getValue()) != null) nameField.setValue("");
                return true;
            }
            if (mc.level != null && mouseY >= listTop && mouseY <= listBottom) {
                List<FlintFixWaypoints.Waypoint> list = FlintFixWaypoints.currentWorld(mc);
                int rowX = x + 14;
                int rowW = w - 28 - 6;
                int rowY = listTop - scroll;
                for (FlintFixWaypoints.Waypoint waypoint : list) {
                    if (mouseY >= rowY && mouseY < rowY + ROW_H - 3 && mouseX >= rowX && mouseX <= rowX + rowW) {
                        if (mouseX >= rowX + rowW - 24) {
                            FlintFixWaypoints.remove(mc, waypoint);
                        } else if (mouseX >= rowX + rowW - 54) {
                            waypoint.visible = !waypoint.visible;
                            FlintFixWaypoints.save();
                        } else if (mouseX <= rowX + 20) {
                            int index = 0;
                            for (int i = 0; i < FlintFixWaypoints.COLORS.length; i++) {
                                if (FlintFixWaypoints.COLORS[i] == waypoint.color) index = i;
                            }
                            waypoint.color = FlintFixWaypoints.COLORS[(index + 1) % FlintFixWaypoints.COLORS.length];
                            FlintFixWaypoints.save();
                        }
                        return true;
                    }
                    rowY += ROW_H;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        layout();
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.round(verticalAmount * ROW_H)));
        return true;
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
