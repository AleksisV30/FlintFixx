package com.flintfix.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.option.VideoOptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.render.debug.ChunkBorderDebugRenderer;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

public final class FlintFixClient implements ClientModInitializer {
    public static final int ACCENT = 0xFFB0B0B0;
    public static final int TEXT = 0xFFF1F2F4;
    public static final int MUTED = 0xFFA4A4A4;
    public static final int SURFACE = 0xF20D0D10;
    public static FlintFixConfig CONFIG;

    private static KeyBinding settingsKey;
    private static KeyBinding freecamKey;
    private static KeyBinding zoomKey;
    private static KeyBinding lookAroundKey;
    private static KeyBinding inspectKey;
    private static ChunkBorderDebugRenderer chunkBorderRenderer;
    private static final Deque<Long> LEFT_CLICKS = new ArrayDeque<>();
    private static final Deque<Long> RIGHT_CLICKS = new ArrayDeque<>();

    @Override
    public void onInitializeClient() {
        CONFIG = FlintFixConfig.load();
        FlintFixProfileStore.initialize();
        FlintFixUi.applyTheme();
        chunkBorderRenderer = new ChunkBorderDebugRenderer(MinecraftClient.getInstance());

        // Add a small, native entry point to the standard Minecraft video page.
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof VideoOptionsScreen) {
                int buttonWidth = 88;
                int buttonX = Math.max(4, screen.width - buttonWidth - 8);
                ButtonWidget videoButton = ButtonWidget.builder(Text.literal("FlintFix"), button ->
                    client.setScreen(new FlintFixVideoSettingsScreen(screen))
                ).dimensions(buttonX, 5, buttonWidth, 20).build();
                Screens.getButtons(screen).add(videoButton);
            }
        });

        settingsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.flintfix.open_client",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            "category.flintfix"
        ));
        freecamKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.flintfix.freecam",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "category.flintfix"
        ));
        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.flintfix.zoom",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            "category.flintfix"
        ));
        lookAroundKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.flintfix.look_around",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "category.flintfix"
        ));
        inspectKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.flintfix.inspect",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_I,
            "category.flintfix"
        ));
        FlintFixShulkerPreview.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.currentScreen instanceof FlintFixFreecamSettingsScreen) {
                while (freecamKey.wasPressed()) { /* key is being rebound in this screen */ }
            } else {
                while (freecamKey.wasPressed()) {
                    if (FlintFixFreecam.isActive()) FlintFixFreecam.disable(client);
                    else if (client.currentScreen == null) FlintFixFreecam.requestEnable(client, null);
                }
            }
            while (settingsKey.wasPressed()) {
                if (client.currentScreen instanceof FlintFixHudEditorScreen editor) {
                    editor.cancelToGame();
                } else if (client.currentScreen instanceof FlintFixHomeScreen home) {
                    home.close();
                } else if (client.currentScreen instanceof FlintFixSettingsScreen settings) {
                    settings.close();
                } else {
                    client.setScreen(new FlintFixHomeScreen(client.currentScreen));
                }
            }
            FlintFixFreecam.tick(client);
            boolean canUseCameraModes = client.currentScreen == null && client.player != null
                && !FlintFixFreecam.isActive();
            FlintFixZoom.setActive(CONFIG.zoomEnabled && canUseCameraModes && zoomKey.isPressed());
            boolean lookAroundActive = CONFIG.lookAroundEnabled && canUseCameraModes && lookAroundKey.isPressed();
            if (lookAroundActive) FlintFixLookAround.begin(client);
            else FlintFixLookAround.end(client);
            while (inspectKey.wasPressed()) {
                if (client.currentScreen == null) FlintFixInspect.start(client);
            }
            FlintFixInspect.tick(client);
            pruneClicks(System.currentTimeMillis());
            FlintFixSocialBridge.tick(client);
        });

        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.currentScreen instanceof FlintFixHudEditorScreen) return;
            renderFpsHud(context, client, false, false);
            renderCpsHud(context, client, false, false);
            renderCoordinatesHud(context, client, false, false);
            renderPingHud(context, client, false, false);
            renderKeystrokesHud(context, client, false, false);
            renderArmorHud(context, client, false, false);
            FlintFixSocialBridge.render(context, client);
        });

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (CONFIG == null) return;
            if (CONFIG.chunksEnabled && context.consumers() != null) {
                var cameraPos = context.camera().getPos();
                chunkBorderRenderer.render(context.matrixStack(), context.consumers(),
                    cameraPos.x, cameraPos.y, cameraPos.z);
            }
            if (CONFIG.trajectoryEnabled) FlintFixTrajectory.render(context);
            if (CONFIG.hitboxesEnabled) FlintFixHitboxes.render(context);
        });
    }

    public static String getSettingsKeyLabel() {
        return settingsKey == null ? "RSHIFT" : settingsKey.getBoundKeyLocalizedText().getString();
    }

    public static KeyBinding getFreecamKeyBinding() {
        return freecamKey;
    }

    public static KeyBinding getZoomKeyBinding() { return zoomKey; }
    public static KeyBinding getLookAroundKeyBinding() { return lookAroundKey; }
    public static KeyBinding getInspectKeyBinding() { return inspectKey; }

    public static synchronized void recordClick(int button) {
        long now = System.currentTimeMillis();
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) LEFT_CLICKS.addLast(now);
        else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) RIGHT_CLICKS.addLast(now);
        pruneClicks(now);
    }

    public static synchronized int getLeftCps() {
        pruneClicks(System.currentTimeMillis());
        return LEFT_CLICKS.size();
    }

    public static synchronized int getRightCps() {
        pruneClicks(System.currentTimeMillis());
        return RIGHT_CLICKS.size();
    }

    private static synchronized void pruneClicks(long now) {
        long cutoff = now - 1000L;
        while (!LEFT_CLICKS.isEmpty() && LEFT_CLICKS.peekFirst() < cutoff) LEFT_CLICKS.removeFirst();
        while (!RIGHT_CLICKS.isEmpty() && RIGHT_CLICKS.peekFirst() < cutoff) RIGHT_CLICKS.removeFirst();
    }

    /**
     * Returns the best latency value available for the current connection.
     * Player-list latency is preferred because it is the live in-game value.
     * For single-player there is no remote network hop, so 0 ms is shown.
     */
    public static int getPing(MinecraftClient client) {
        if (client == null || client.player == null) return -1;
        if (client.isInSingleplayer()) return 0;

        if (client.getNetworkHandler() != null) {
            PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
            if (entry == null) {
                String playerName = client.player.getName().getString();
                for (PlayerListEntry candidate : client.getNetworkHandler().getPlayerList()) {
                    if (candidate.getProfile().getId().equals(client.player.getUuid()) ||
                        candidate.getProfile().getName().equalsIgnoreCase(playerName)) {
                        entry = candidate;
                        break;
                    }
                }
            }
            if (entry != null && entry.getLatency() >= 0) return entry.getLatency();
        }

        ServerInfo server = client.getCurrentServerEntry();
        if (server != null && server.ping >= 0L && server.ping <= Integer.MAX_VALUE) {
            return (int) server.ping;
        }
        return -1;
    }

    // ------------------------------------------------------------------
    // HUD widgets: simple flat tiles with a muted label and a bold value.
    // Layouts are measured from the real rendered text size (FlintFixFont
    // snaps small text to whole pixels, which changes with the GUI scale),
    // so nothing overlaps at any scale.
    // ------------------------------------------------------------------

    private static final int PAD_X = 6;
    private static final int PAD_Y = 4;
    private static final int LABEL = 6;
    private static final int VALUE = 8;
    private static final int GOOD = 0xFF6FDC9A;
    private static final int WARN = 0xFFF0C25E;
    private static final int BAD = 0xFFF07373;

    public static HudBounds renderFpsHud(DrawContext context, MinecraftClient client, boolean editor, boolean selected) {
        return renderFpsHud(context, client, editor, selected, false);
    }

    static HudBounds renderFpsHud(DrawContext context, MinecraftClient client, boolean editor, boolean selected, boolean preview) {
        if (!CONFIG.fpsEnabled && !editor && !preview) return emptyBounds();
        int fps = client.getCurrentFps();
        return lineTile(context, client, editor, CONFIG.fpsX, CONFIG.fpsY, CONFIG.fpsScale,
            CONFIG.fpsBackground, CONFIG.fpsBackgroundOpacity, CONFIG.fpsTextShadow,
            "FPS", Integer.toString(fps), FlintFixUi.text());
    }

    public static HudBounds renderCpsHud(DrawContext context, MinecraftClient client, boolean editor, boolean selected) {
        return renderCpsHud(context, client, editor, selected, false);
    }

    static HudBounds renderCpsHud(DrawContext context, MinecraftClient client, boolean editor, boolean selected, boolean preview) {
        if (!CONFIG.cpsEnabled && !editor && !preview) return emptyBounds();
        if (!editor && !preview && client.player == null) return emptyBounds();
        return lineTile(context, client, editor, CONFIG.cpsX, CONFIG.cpsY, CONFIG.cpsScale,
            CONFIG.cpsBackground, CONFIG.cpsBackgroundOpacity, true,
            "CPS", getLeftCps() + " | " + getRightCps(), FlintFixUi.text());
    }

    public static HudBounds renderPingHud(DrawContext context, MinecraftClient client, boolean editor, boolean selected) {
        return renderPingHud(context, client, editor, selected, false);
    }

    static HudBounds renderPingHud(DrawContext context, MinecraftClient client, boolean editor, boolean selected, boolean preview) {
        if (!CONFIG.pingEnabled && !editor && !preview) return emptyBounds();
        if (!editor && !preview && client.player == null) return emptyBounds();
        int ping = getPing(client);
        String value = ping < 0 ? "--" : ping + " ms";
        int color = ping < 0 ? FlintFixUi.muted() : statusColor(ping <= 80 ? GOOD : (ping <= 160 ? WARN : BAD));
        return lineTile(context, client, editor, CONFIG.pingX, CONFIG.pingY, CONFIG.pingScale,
            CONFIG.pingBackground, CONFIG.pingBackgroundOpacity, true, "PING", value, color);
    }

    /** One line: "LABEL value", e.g. "FPS 144". */
    private static HudBounds lineTile(DrawContext context, MinecraftClient client, boolean editor, float nx, float ny,
                                      float scale, boolean background, float backgroundOpacity, boolean textShadow,
                                      String label, String value, int valueColor) {
        int labelW = FlintFixFont.width(label, LABEL, true);
        int valueW = FlintFixFont.width(value, VALUE, true);
        int line = FlintFixFont.lineHeight(VALUE);
        int rawWidth = PAD_X + labelW + 4 + valueW + PAD_X;
        int rawHeight = line + PAD_Y * 2 - 1;
        HudPlacement p = placement(client, nx, ny, rawWidth, rawHeight, scale);

        beginWidget(context, p, scale);
        int opacity = panelOpacity(background, backgroundOpacity, editor);
        boolean shadow = textShadow && opacity < 110;
        drawHudPanel(context, rawWidth, rawHeight, opacity);
        int valueY = FlintFixFont.centeredY(0, rawHeight, VALUE);
        int labelY = valueY + capBottom(VALUE) - capBottom(LABEL);
        FlintFixFont.drawExact(context, label, PAD_X, labelY, LABEL, FlintFixUi.muted(), true, shadow);
        FlintFixFont.drawExact(context, value, PAD_X + labelW + 4, valueY, VALUE, valueColor, true, shadow);
        endWidget(context);
        return new HudBounds(p.x, p.y, p.width, p.height);
    }

    public static HudBounds renderCoordinatesHud(DrawContext context, MinecraftClient client, boolean editor, boolean selected) {
        return renderCoordinatesHud(context, client, editor, selected, false);
    }

    static HudBounds renderCoordinatesHud(DrawContext context, MinecraftClient client, boolean editor, boolean selected, boolean preview) {
        if (!CONFIG.coordinatesEnabled && !editor && !preview) return emptyBounds();
        if (!editor && !preview && client.player == null) return emptyBounds();

        int px = client.player == null ? 0 : (int) Math.floor(client.player.getX());
        int py = client.player == null ? 64 : (int) Math.floor(client.player.getY());
        int pz = client.player == null ? 0 : (int) Math.floor(client.player.getZ());
        String facing = "North";
        if (client.player != null) {
            switch (client.player.getHorizontalFacing()) {
                case SOUTH -> facing = "South";
                case EAST -> facing = "East";
                case WEST -> facing = "West";
                default -> facing = "North";
            }
        }
        String biome = "Plains";
        if (client.player != null && client.world != null) {
            String key = client.world.getBiome(client.player.getBlockPos()).getKey()
                .map(registryKey -> registryKey.getValue().getPath())
                .orElse("unknown");
            biome = titleCase(key.replace('_', ' '));
        }
        String[] labels = {"XYZ", "DIR", "BIOME"};
        String[] values = {px + " " + py + " " + pz, facing, biome};

        int labelW = 0;
        for (String label : labels) labelW = Math.max(labelW, FlintFixFont.width(label, LABEL, true));
        int valueW = 0;
        for (String value : values) valueW = Math.max(valueW, FlintFixFont.width(value, VALUE, true));
        valueW = Math.min(valueW, 120);
        int rowH = FlintFixFont.lineHeight(VALUE) + 1;
        int rawWidth = PAD_X + labelW + 5 + valueW + PAD_X;
        int rawHeight = PAD_Y * 2 + rowH * labels.length - 1;
        float scale = CONFIG.coordinatesScale;
        HudPlacement p = placement(client, CONFIG.coordinatesX, CONFIG.coordinatesY, rawWidth, rawHeight, scale);

        beginWidget(context, p, scale);
        int opacity = panelOpacity(CONFIG.coordinatesBackground, CONFIG.coordinatesBackgroundOpacity, editor);
        boolean shadow = opacity < 110;
        drawHudPanel(context, rawWidth, rawHeight, opacity);
        for (int line = 0; line < labels.length; line++) {
            int valueY = PAD_Y + line * rowH;
            int labelY = valueY + capBottom(VALUE) - capBottom(LABEL);
            FlintFixFont.drawExact(context, labels[line], PAD_X, labelY, LABEL, FlintFixUi.muted(), true, shadow);
            FlintFixFont.drawExact(context, FlintFixFont.trim(values[line], valueW, VALUE, true),
                PAD_X + labelW + 5, valueY, VALUE, FlintFixUi.text(), true, shadow);
        }
        endWidget(context);
        return new HudBounds(p.x, p.y, p.width, p.height);
    }

    public static HudBounds renderKeystrokesHud(DrawContext context, MinecraftClient client, boolean editor, boolean selected) {
        return renderKeystrokesHud(context, client, editor, selected, false);
    }

    static HudBounds renderKeystrokesHud(DrawContext context, MinecraftClient client, boolean editor, boolean selected, boolean preview) {
        if (!CONFIG.keystrokesEnabled && !editor && !preview) return emptyBounds();
        if (!editor && !preview && client.player == null) return emptyBounds();

        int key = Math.max(17, FlintFixFont.lineHeight(VALUE) + 7);
        int gap = 2;
        int pad = 0;
        int rawWidth = key * 3 + gap * 2;
        int mouseH = CONFIG.keystrokesShowCps
            ? Math.max(17, FlintFixFont.lineHeight(VALUE) + FlintFixFont.lineHeight(LABEL) + 2)
            : key - 4;
        int rawHeight = key * 2 + gap + mouseH + gap;
        int mouseW = (rawWidth - gap) / 2;
        float scale = CONFIG.keystrokesScale;
        HudPlacement p = placement(client, CONFIG.keystrokesX, CONFIG.keystrokesY, rawWidth, rawHeight, scale);
        // The keys are the panels here, so the background setting controls their fill.
        float fill = editor ? 0.8f : (CONFIG.keystrokesBackground ? CONFIG.keystrokesBackgroundOpacity : 0.0f);

        beginWidget(context, p, scale);
        drawKey(context, pad + key + gap, pad, key, key, "W", client.options.forwardKey.isPressed(), fill);
        drawKey(context, pad, pad + key + gap, key, key, "A", client.options.leftKey.isPressed(), fill);
        drawKey(context, pad + key + gap, pad + key + gap, key, key, "S", client.options.backKey.isPressed(), fill);
        drawKey(context, pad + (key + gap) * 2, pad + key + gap, key, key, "D", client.options.rightKey.isPressed(), fill);
        int mouseY = pad + (key + gap) * 2;
        drawMouseKey(context, pad, mouseY, mouseW, mouseH, "LMB", getLeftCps(), client.options.attackKey.isPressed(), fill);
        drawMouseKey(context, pad + mouseW + gap, mouseY, mouseW, mouseH, "RMB", getRightCps(), client.options.useKey.isPressed(), fill);
        endWidget(context);
        return new HudBounds(p.x, p.y, p.width, p.height);
    }

    public static HudBounds renderArmorHud(DrawContext context, MinecraftClient client, boolean editor, boolean selected) {
        return renderArmorHud(context, client, editor, selected, false);
    }

    /** One row per damageable piece: the item icon and its remaining durability, colored by condition. */
    static HudBounds renderArmorHud(DrawContext context, MinecraftClient client, boolean editor, boolean selected, boolean preview) {
        if (!CONFIG.armorEnabled && !editor && !preview) return emptyBounds();

        List<ArmorHudItem> items = armorHudItems(client, editor || preview);
        int rowH = 17;
        int textX = 3 + 16 + 3;
        int valueW = 0;
        for (ArmorHudItem item : items) valueW = Math.max(valueW, FlintFixFont.width(Integer.toString(item.remaining()), VALUE, true));
        int rawWidth = items.isEmpty() ? FlintFixFont.width("NO ARMOR", LABEL, true) + PAD_X * 2 : textX + valueW + PAD_X;
        int rawHeight = items.isEmpty() ? FlintFixFont.lineHeight(LABEL) + PAD_Y * 2 : 2 + items.size() * rowH;
        float scale = CONFIG.armorScale;
        HudPlacement p = placement(client, CONFIG.armorX, CONFIG.armorY, rawWidth, rawHeight, scale);

        beginWidget(context, p, scale);
        int opacity = panelOpacity(CONFIG.armorBackground, CONFIG.armorBackgroundOpacity, editor);
        boolean shadow = opacity < 110;
        drawHudPanel(context, rawWidth, rawHeight, opacity);
        if (items.isEmpty()) {
            FlintFixFont.drawExact(context, "NO ARMOR", PAD_X, FlintFixFont.centeredY(0, rawHeight, LABEL), LABEL,
                FlintFixUi.muted(), true, shadow);
        } else {
            for (int i = 0; i < items.size(); i++) {
                ArmorHudItem item = items.get(i);
                int rowY = 1 + i * rowH;
                context.drawItem(item.stack(), 3, rowY + (rowH - 16) / 2);
                int color = statusColor(durabilityColor(item.remaining(), item.max()));
                FlintFixFont.drawExact(context, Integer.toString(item.remaining()), textX,
                    FlintFixFont.centeredY(rowY, rowH, VALUE), VALUE, color, true, shadow);
            }
        }
        endWidget(context);
        return new HudBounds(p.x, p.y, p.width, p.height);
    }

    private static List<ArmorHudItem> armorHudItems(MinecraftClient client, boolean editor) {
        ArrayList<ArmorHudItem> result = new ArrayList<>(6);
        if (client == null || client.player == null) {
            if (editor) {
                addDemoArmorItem(result, Items.DIAMOND_HELMET, 300);
                addDemoArmorItem(result, Items.DIAMOND_CHESTPLATE, 321);
                addDemoArmorItem(result, Items.NETHERITE_LEGGINGS, 512);
                addDemoArmorItem(result, Items.DIAMOND_BOOTS, 90);
                addDemoArmorItem(result, Items.NETHERITE_SWORD, 1800);
            }
            return result;
        }

        addArmorItem(result, client.player.getEquippedStack(EquipmentSlot.HEAD));
        addArmorItem(result, client.player.getEquippedStack(EquipmentSlot.CHEST));
        addArmorItem(result, client.player.getEquippedStack(EquipmentSlot.LEGS));
        addArmorItem(result, client.player.getEquippedStack(EquipmentSlot.FEET));
        addArmorItem(result, client.player.getMainHandStack());
        addArmorItem(result, client.player.getOffHandStack());
        return result;
    }

    private static void addDemoArmorItem(List<ArmorHudItem> items, Item item, int remaining) {
        ItemStack stack = new ItemStack(item);
        int max = stack.getMaxDamage();
        int value = Math.max(0, Math.min(max, remaining));
        stack.setDamage(max - value);
        items.add(new ArmorHudItem(stack, value, max));
    }

    private static void addArmorItem(List<ArmorHudItem> items, ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.isDamageable()) return;
        int max = stack.getMaxDamage();
        int remaining = Math.max(0, max - stack.getDamage());
        items.add(new ArmorHudItem(stack, remaining, max));
    }

    private static int durabilityColor(int remaining, int max) {
        float ratio = max <= 0 ? 0.0f : remaining / (float) max;
        if (ratio <= 0.20f) return BAD;
        if (ratio <= 0.50f) return WARN;
        return FlintFixUi.text();
    }

    /** Status colors are tuned for dark panels; deepen them so they stay readable on the light theme. */
    private static int statusColor(int color) {
        if (FlintFixUi.activeTheme() != FlintFixTheme.LIGHT || color == FlintFixUi.text()) return color;
        return FlintFixUi.blendColors(color, 0xFF000000, 0.32f);
    }

    private record ArmorHudItem(ItemStack stack, int remaining, int max) {}

    private static String titleCase(String text) {
        if (text == null || text.isBlank()) return "Unknown";
        String[] words = text.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.isEmpty() ? "Unknown" : result.toString();
    }

    /** Offset from a text line's top to the bottom of its capitals, for lining up two sizes. */
    private static int capBottom(int size) {
        return Math.round(FlintFixFont.lineHeight(size) * 0.80f);
    }

    private static void beginWidget(DrawContext context, HudPlacement p, float scale) {
        context.getMatrices().push();
        context.getMatrices().translate(p.x, p.y, 0);
        context.getMatrices().scale(scale, scale, 1.0f);
    }

    private static void endWidget(DrawContext context) {
        context.getMatrices().pop();
    }

    /** The editor always shows a panel so widgets stay visible while they are arranged. */
    private static int panelOpacity(boolean background, float opacity, boolean editor) {
        if (editor) return 200;
        return background ? Math.round(opacity * 255.0f) : 0;
    }

    /** A single flat, rounded, translucent panel. */
    private static void drawHudPanel(DrawContext context, int width, int height, int opacity) {
        if (opacity <= 0 || width <= 0 || height <= 0) return;
        float a = Math.max(0, Math.min(255, opacity)) / 255.0f;
        FlintFixUi.roundedRaw(context, 0, 0, width, height, 3, FlintFixUi.opacity(FlintFixUi.bg(), a));
    }

    /** Flat key tile that fills with the accent color while pressed. */
    private static void drawKey(DrawContext c, int x, int y, int w, int h, String label, boolean pressed, float fill) {
        float t = FlintFixUi.hoverProgress("hud-key:" + label, pressed);
        int idle = FlintFixUi.opacity(FlintFixUi.bg(), fill);
        FlintFixUi.roundedRaw(c, x, y, w, h, 3, FlintFixUi.blendColors(idle, FlintFixUi.accent(), t));
        int ink = FlintFixUi.blendColors(FlintFixUi.text(), FlintFixUi.onAccent(), t);
        FlintFixFont.drawCenteredExact(c, label, x + w / 2, FlintFixFont.centeredY(y, h, VALUE), VALUE, ink, true);
    }

    private static void drawMouseKey(DrawContext c, int x, int y, int w, int h, String label, int cps, boolean pressed,
                                     float fill) {
        float t = FlintFixUi.hoverProgress("hud-key:" + label, pressed);
        int idle = FlintFixUi.opacity(FlintFixUi.bg(), fill);
        FlintFixUi.roundedRaw(c, x, y, w, h, 3, FlintFixUi.blendColors(idle, FlintFixUi.accent(), t));
        int ink = FlintFixUi.blendColors(FlintFixUi.text(), FlintFixUi.onAccent(), t);
        if (CONFIG.keystrokesShowCps) {
            int valueLine = FlintFixFont.lineHeight(VALUE);
            int top = y + Math.max(1, (h - valueLine - FlintFixFont.lineHeight(LABEL) + 1) / 2);
            FlintFixFont.drawCenteredExact(c, label, x + w / 2, top, VALUE, ink, true);
            FlintFixFont.drawCenteredExact(c, cps + " CPS", x + w / 2, top + valueLine - 1, LABEL,
                FlintFixUi.blendColors(FlintFixUi.muted(), ink, t), true);
        } else {
            FlintFixFont.drawCenteredExact(c, label, x + w / 2, FlintFixFont.centeredY(y, h, VALUE), VALUE, ink, true);
        }
    }

    private static HudPlacement placement(MinecraftClient client, float nx, float ny, int rawWidth, int rawHeight, float scale) {
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int width = Math.max(1, Math.round(rawWidth * scale));
        int height = Math.max(1, Math.round(rawHeight * scale));
        int x = Math.round(nx * Math.max(1, screenWidth - width));
        int y = Math.round(ny * Math.max(1, screenHeight - height));
        x = Math.max(0, Math.min(Math.max(0, screenWidth - width), x));
        y = Math.max(0, Math.min(Math.max(0, screenHeight - height), y));
        return new HudPlacement(x, y, width, height);
    }

    private static HudBounds emptyBounds() {
        return new HudBounds(0, 0, 0, 0);
    }

    private record HudPlacement(int x, int y, int width, int height) {}

    public record HudBounds(int x, int y, int width, int height) {
        public boolean contains(double mouseX, double mouseY) {
            return width > 0 && height > 0 && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }
}
