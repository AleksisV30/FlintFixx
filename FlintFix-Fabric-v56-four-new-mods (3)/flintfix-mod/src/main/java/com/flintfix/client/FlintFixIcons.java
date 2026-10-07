package com.flintfix.client;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Lucide icon assets used by the in-game FlintFix UI. */
public final class FlintFixIcons {
    private static final int ICON_SIZE = 64;
    private static final Map<String, String> ICONS = Map.ofEntries(
        Map.entry("modules", "layout-grid"),
        Map.entry("appearance", "sun"),
        Map.entry("themes", "sun"),
        Map.entry("hud", "layout-dashboard"),
        Map.entry("layout", "layout-dashboard"),
        Map.entry("settings", "sliders-horizontal"),
        Map.entry("profiles", "user-round"),
        Map.entry("about", "info"),
        Map.entry("close", "x"),
        Map.entry("back", "chevron-left"),
        Map.entry("next", "chevron-right"),
        Map.entry("check", "check"),
        Map.entry("fps", "monitor"),
        Map.entry("cps", "mouse"),
        Map.entry("coordinates", "locate-fixed"),
        Map.entry("keystrokes", "keyboard"),
        Map.entry("ping", "signal"),
        Map.entry("armor", "shield"),
        Map.entry("chunks", "grid-3x3"),
        Map.entry("freecam", "video"),
        Map.entry("trajectory", "spline"),
        Map.entry("hitboxes", "scan"),
        Map.entry("zoom", "zoom-in"),
        Map.entry("lookaround", "eye"),
        Map.entry("shulkers", "box"),
        Map.entry("search", "search"),
        Map.entry("sky", "moon-star"),
        Map.entry("inspect", "rotate-cw"),
        Map.entry("video", "monitor"),
        Map.entry("showhand", "hand"),
        Map.entry("fullbright", "lightbulb"),
        Map.entry("potions", "flask-conical"),
        Map.entry("speed", "gauge"),
        Map.entry("compass", "compass"),
        Map.entry("outline", "square-dashed"),
        Map.entry("crosshair", "crosshair"),
        Map.entry("lowoverlays", "flame"),
        Map.entry("damage", "swords"),
        Map.entry("weather", "cloud-off"),
        Map.entry("motionblur", "wind"),
        Map.entry("teamglow", "users"),
        Map.entry("waypoints", "map-pin"),
        Map.entry("serverprofiles", "server-cog")
    );
    private static final Set<ResourceLocation> FILTERED = ConcurrentHashMap.newKeySet();

    private FlintFixIcons() {}

    public static void draw(GuiGraphics context, String id, int x, int y, int size, int color) {
        drawExact(context, id, x, y, size, FlintFixUi.themedText(color));
    }

    /** Draws with the given tint as-is, for colors already taken from the active theme. */
    public static void drawExact(GuiGraphics context, String id, int x, int y, int size, int color) {
        String name = ICONS.getOrDefault(id, "layout-grid");
        ResourceLocation texture = FlintFixCompat.id("flintfix", "textures/gui/icons/" + name + ".png");
        if (FILTERED.add(texture)) {
            Minecraft.getInstance().getTextureManager().getTexture(texture).setFilter(true, false);
        }
        FlintFixCompat.drawTexture(context, texture, x, y, size, size, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE,
            color);
    }
}
