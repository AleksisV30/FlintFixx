package com.flintfix.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Lucide icon assets used by the in-game FlintFix UI. */
public final class FlintFixIcons {
    private static final int ICON_SIZE = 64;
    private static final Map<String, String> ICONS = Map.ofEntries(
        Map.entry("modules", "layout-grid"),
        Map.entry("appearance", "sun"),
        Map.entry("hud", "crosshair"),
        Map.entry("settings", "sliders-horizontal"),
        Map.entry("profiles", "user-round"),
        Map.entry("about", "info"),
        Map.entry("fps", "monitor"),
        Map.entry("cps", "mouse"),
        Map.entry("coordinates", "locate-fixed"),
        Map.entry("keystrokes", "keyboard"),
        Map.entry("ping", "signal"),
        Map.entry("armor", "shield"),
        Map.entry("chunks", "layout-grid"),
        Map.entry("freecam", "crosshair"),
        Map.entry("trajectory", "locate-fixed"),
        Map.entry("hitboxes", "crosshair"),
        Map.entry("zoom", "search"),
        Map.entry("lookaround", "user-round"),
        Map.entry("shulkers", "layout-grid"),
        Map.entry("search", "search")
    );
    private static final Set<Identifier> FILTERED = ConcurrentHashMap.newKeySet();

    private FlintFixIcons() {}

    public static void draw(DrawContext context, String id, int x, int y, int size, int color) {
        color = FlintFixUi.themedText(color);
        String name = ICONS.getOrDefault(id, "layout-grid");
        Identifier texture = Identifier.of("flintfix", "textures/gui/icons/" + name + ".png");
        if (FILTERED.add(texture)) {
            MinecraftClient.getInstance().getTextureManager().getTexture(texture).setFilter(true, false);
        }

        float a = ((color >>> 24) & 0xFF) / 255.0f;
        float r = ((color >>> 16) & 0xFF) / 255.0f;
        float g = ((color >>> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(r, g, b, a);
        try {
            context.drawTexture(texture, x, y, size, size, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        } finally {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }
    }
}
