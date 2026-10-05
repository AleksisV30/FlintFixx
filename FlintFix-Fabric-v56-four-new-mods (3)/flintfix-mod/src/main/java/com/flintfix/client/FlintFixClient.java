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

    public static HudBounds renderFpsHud(DrawContext context, MinecraftClient client, boolean editor, boolean selected) {
        return renderFpsHud(context, client, editor, selected, false);
    }

    static HudBounds renderFpsHud(DrawContext context, MinecraftClient client, boolean editor, boolean selected, boolean preview) {
        if (!CONFIG.fpsEnabled && !editor && !preview) return emptyBounds();

        int fps = client.getCurrentFps();
        String value = Integer.toString(fps);
        String label = "FPS";
        float scale = CONFIG.fpsScale;
        int valueWidth = FlintFixFont.width(value, 13, true);
        int labelWidth = FlintFixFont.width(label, 7, true);
        int rawWidth = valueWidth + labelWidth + 32;
        int rawHeight = 23;
        HudPlacement p = placement(client, CONFIG.fpsX, CONFIG.fpsY, rawWidth, rawHeight, scale);

        context.getMatrices().push();
        context.getMatrices().translate(p.x, p.y, 0);
        context.getMatrices().scale(scale, scale, 1.0f);

        drawHudPanel(context, rawWidth, rawHeight, panelOpacity(CONFIG.fpsBackground, CONFIG.fpsBackgroundOpacity, editor));
        FlintFixUi.roundedRaw(context, 4, 6, 2, 11, 1, FlintFixUi.accent());
        if (CONFIG.fpsTextShadow) {
            FlintFixFont.drawExact(context, value, 11, 6, 13, 0x66000000, true);
        }
        FlintFixFont.drawExact(context, value, 10, 5, 13, FlintFixUi.text(), true);
        FlintFixFont.drawExact(context, label, 13 + valueWidth, 9, 7, FlintFixUi.accentBright(), true);
        int status = fps >= 60 ? 0xFF7EE0A0 : (fps >= 30 ? 0xFFE8C46A : 0xFFE87878);
        FlintFixUi.roundedRaw(context, rawWidth - 9, rawHeight / 2 - 2, 4, 4, 2, status);

        context.getMatrices().pop();
        return new HudBounds(p.x, p.y, p.width, p.height);
    }

    public static HudBounds renderCpsHud(DrawContext context, MinecraftClient client, boolean editor, boolean selected) {
        return renderCpsHud(context, client, editor, selected, false);
    }

    static HudBounds renderCpsHud(DrawContext context, MinecraftClient client, boolean editor, boolean selected, boolean preview) {
        if (!CONFIG.cpsEnabled && !editor && !preview) return emptyBounds();
        if (!editor && !preview && client.player == null) return emptyBounds();

        int leftCps = getLeftCps();
        int rightCps = getRightCps();
        float scale = CONFIG.cpsScale;
        int rawWidth = 74;
        int rawHeight = 31;
        HudPlacement p = placement(client, CONFIG.cpsX, CONFIG.cpsY, rawWidth, rawHeight, scale);

        context.getMatrices().push();
        context.getMatrices().translate(p.x, p.y, 0);
        context.getMatrices().scale(scale, scale, 1.0f);
        drawHudPanel(context, rawWidth, rawHeight, panelOpacity(CONFIG.cpsBackground, CONFIG.cpsBackgroundOpacity, editor));
        int half = rawWidth / 2;
        drawCpsColumn(context, 7, half - 12, "LMB", leftCps);
        context.fill(half, 6, half + 1, rawHeight - 6, FlintFixUi.opacity(FlintFixUi.border(), 0.8f));
        drawCpsColumn(context, half + 6, half - 12, "RMB", rightCps);
        context.getMatrices().pop();
        return new HudBounds(p.x, p.y, p.width, p.height);
    }

    private static void drawCpsColumn(DrawContext context, int x, int width, String label, int cps) {
        FlintFixFont.drawExact(context, label, x, 4, 6, FlintFixUi.muted(), true);
        FlintFixFont.drawExact(context, Integer.toString(cps), x, 12, 11, FlintFixUi.text(), true);
        int barY = 25;
        FlintFixUi.roundedRaw(context, x, barY, width, 2, 1, FlintFixUi.opacity(FlintFixUi.raised(), 0.9f));
        int fill = Math.round(width * Math.min(1.0f, cps / 15.0f));
        if (fill > 0) FlintFixUi.roundedRaw(context, x, barY, fill, 2, 1, FlintFixUi.accent());
    }

    public static HudBounds renderCoordinatesHud(DrawContext context, MinecraftClient client, boolean editor, boolean selected) {
        return renderCoordinatesHud(context, client, editor, selected, false);
    }

    static HudBounds renderCoordinatesHud(DrawContext context, MinecraftClient client, boolean editor, boolean selected, boolean preview) {
        if (!CONFIG.coordinatesEnabled && !editor && !preview) return emptyBounds();
        if (!editor && !preview && client.player == null) return emptyBounds();

        int px = client.player == null ? 0 : (int)Math.floor(client.player.getX());
        int py = client.player == null ? 64 : (int)Math.floor(client.player.getY());
        int pz = client.player == null ? 0 : (int)Math.floor(client.player.getZ());
        String facing = client.player == null ? "N" : client.player.getHorizontalFacing().asString().toUpperCase(Locale.ROOT);
        String biome = "Plains";
        if (client.player != null && client.world != null) {
            String key = client.world.getBiome(client.player.getBlockPos()).getKey()
                .map(registryKey -> registryKey.getValue().getPath())
                .orElse("unknown");
            biome = titleCase(key.replace('_', ' '));
        }
        String[] labels = {"X", "Y", "Z", "DIR", "BIOME"};
        String[] values = {Integer.toString(px), Integer.toString(py), Integer.toString(pz), facing, biome};
        float scale = CONFIG.coordinatesScale;
        int rawWidth = 104;
        int rawHeight = 47;
        HudPlacement p = placement(client, CONFIG.coordinatesX, CONFIG.coordinatesY, rawWidth, rawHeight, scale);

        context.getMatrices().push();
        context.getMatrices().translate(p.x, p.y, 0);
        context.getMatrices().scale(scale, scale, 1.0f);
        drawHudPanel(context, rawWidth, rawHeight, panelOpacity(CONFIG.coordinatesBackground, CONFIG.coordinatesBackgroundOpacity, editor));
        int[] axisColors = {0xFFE68C8C, 0xFF91D89A, 0xFF8CB2F4, FlintFixUi.accentBright(), FlintFixUi.accentBright()};
        for (int line = 0; line < labels.length; line++) {
            int lineY = 4 + line * 8;
            FlintFixUi.roundedRaw(context, 5, lineY + 1, 2, 5, 1, axisColors[line]);
            FlintFixFont.drawExact(context, labels[line], 10, lineY, 6, FlintFixUi.muted(), true);
            FlintFixUi.drawTrimmedExact(context, values[line], 38, lineY, rawWidth - 44, 6, FlintFixUi.text(), true);
            if (line < labels.length - 1) {
                context.fill(10, lineY + 7, rawWidth - 6, lineY + 8, FlintFixUi.opacity(FlintFixUi.border(), 0.35f));
            }
        }
        context.getMatrices().pop();
        return new HudBounds(p.x, p.y, p.width, p.height);
    }

    public static HudBounds renderPingHud(DrawContext context, MinecraftClient client, boolean editor, boolean selected) {
        return renderPingHud(context, client, editor, selected, false);
    }

    static HudBounds renderPingHud(DrawContext context, MinecraftClient client, boolean editor, boolean selected, boolean preview) {
        if (!CONFIG.pingEnabled && !editor && !preview) return emptyBounds();
        if (!editor && !preview && client.player == null) return emptyBounds();

        int ping = getPing(client);
        String text = ping < 0 ? "--" : Integer.toString(ping);
        int signalColor = ping < 0 ? FlintFixUi.muted()
            : (ping <= 80 ? 0xFF7EE0A0 : (ping <= 160 ? 0xFFE8C46A : 0xFFE87878));
        float scale = CONFIG.pingScale;
        int valueWidth = FlintFixFont.width(text, 11, true);
        int rawWidth = Math.max(60, valueWidth + FlintFixFont.width("ms", 6, true) + 36);
        int rawHeight = 26;
        HudPlacement p = placement(client, CONFIG.pingX, CONFIG.pingY, rawWidth, rawHeight, scale);

        context.getMatrices().push();
        context.getMatrices().translate(p.x, p.y, 0);
        context.getMatrices().scale(scale, scale, 1.0f);
        drawHudPanel(context, rawWidth, rawHeight, panelOpacity(CONFIG.pingBackground, CONFIG.pingBackgroundOpacity, editor));
        FlintFixFont.drawExact(context, "PING", 7, 4, 6, FlintFixUi.muted(), true);
        FlintFixFont.drawExact(context, text, 7, 12, 11, FlintFixUi.text(), true);
        FlintFixFont.drawExact(context, "ms", 9 + valueWidth, 15, 6, FlintFixUi.muted(), true);
        int activeBars = ping < 0 ? 0 : (ping <= 60 ? 4 : (ping <= 110 ? 3 : (ping <= 180 ? 2 : 1)));
        for (int bar = 0; bar < 4; bar++) {
            int barHeight = 4 + bar * 3;
            int barX = rawWidth - 24 + bar * 4;
            int barY = 20 - barHeight;
            FlintFixUi.roundedRaw(context, barX, barY, 3, barHeight, 1,
                bar < activeBars ? signalColor : FlintFixUi.opacity(FlintFixUi.raised(), 0.9f));
        }
        context.getMatrices().pop();
        return new HudBounds(p.x, p.y, p.width, p.height);
    }

    public static HudBounds renderKeystrokesHud(DrawContext context, MinecraftClient client, boolean editor, boolean selected) {
        return renderKeystrokesHud(context, client, editor, selected, false);
    }

    static HudBounds renderKeystrokesHud(DrawContext context, MinecraftClient client, boolean editor, boolean selected, boolean preview) {
        if (!CONFIG.keystrokesEnabled && !editor && !preview) return emptyBounds();
        if (!editor && !preview && client.player == null) return emptyBounds();

        int key = 18;
        int gap = 3;
        int pad = 4;
        int rawWidth = key * 3 + gap * 2 + pad * 2;
        int mouseH = CONFIG.keystrokesShowCps ? 18 : 14;
        int rawHeight = key * 2 + gap + mouseH + gap + pad * 2;
        int mouseW = (rawWidth - pad * 2 - gap) / 2;
        float scale = CONFIG.keystrokesScale;
        HudPlacement p = placement(client, CONFIG.keystrokesX, CONFIG.keystrokesY, rawWidth, rawHeight, scale);

        context.getMatrices().push();
        context.getMatrices().translate(p.x, p.y, 0);
        context.getMatrices().scale(scale, scale, 1.0f);

        drawHudPanel(context, rawWidth, rawHeight,
            panelOpacity(CONFIG.keystrokesBackground, CONFIG.keystrokesBackgroundOpacity, editor));
        drawKey(context, pad + key + gap, pad, key, key, "W", client.options.forwardKey.isPressed());
        drawKey(context, pad, pad + key + gap, key, key, "A", client.options.leftKey.isPressed());
        drawKey(context, pad + key + gap, pad + key + gap, key, key, "S", client.options.backKey.isPressed());
        drawKey(context, pad + (key + gap) * 2, pad + key + gap, key, key, "D", client.options.rightKey.isPressed());

        int mouseY = pad + (key + gap) * 2;
        drawMouseKey(context, pad, mouseY, mouseW, mouseH, "LMB", getLeftCps(), client.options.attackKey.isPressed());
        drawMouseKey(context, pad + mouseW + gap, mouseY, mouseW, mouseH, "RMB", getRightCps(), client.options.useKey.isPressed());

        context.getMatrices().pop();
        return new HudBounds(p.x, p.y, p.width, p.height);
    }

    public static HudBounds renderArmorHud(DrawContext context, MinecraftClient client, boolean editor, boolean selected) {
        return renderArmorHud(context, client, editor, selected, false);
    }

    static HudBounds renderArmorHud(DrawContext context, MinecraftClient client, boolean editor, boolean selected, boolean preview) {
        if (!CONFIG.armorEnabled && !editor && !preview) return emptyBounds();

        List<ArmorHudItem> items = armorHudItems(client, editor || preview);
        int rawWidth = 62;
        int rawHeight = items.isEmpty() ? 24 : 4 + items.size() * 19;
        float scale = CONFIG.armorScale;
        HudPlacement p = placement(client, CONFIG.armorX, CONFIG.armorY, rawWidth, rawHeight, scale);

        context.getMatrices().push();
        context.getMatrices().translate(p.x, p.y, 0);
        context.getMatrices().scale(scale, scale, 1.0f);
        drawHudPanel(context, rawWidth, rawHeight, panelOpacity(CONFIG.armorBackground, CONFIG.armorBackgroundOpacity, editor));

        if (items.isEmpty()) {
            FlintFixFont.drawExact(context, "NO ARMOR", 8, FlintFixFont.centeredY(0, rawHeight, 7), 7, FlintFixUi.muted(), true);
        } else {
            for (int i = 0; i < items.size(); i++) {
                ArmorHudItem item = items.get(i);
                int rowY = 2 + i * 19;
                int color = durabilityColor(item.remaining(), item.max());
                context.drawItem(item.stack(), 3, rowY + 1);
                int textX = 23;
                int barW = rawWidth - textX - 5;
                String value = Integer.toString(item.remaining());
                FlintFixFont.drawExact(context, value, textX, rowY + 3, 8, color, true);
                String percent = Math.round(item.remaining() * 100.0f / Math.max(1, item.max())) + "%";
                FlintFixFont.drawExact(context, percent, rawWidth - 5 - FlintFixFont.width(percent, 6, false),
                    rowY + 4, 6, FlintFixUi.muted(), false);
                int barY = rowY + 13;
                FlintFixUi.roundedRaw(context, textX, barY, barW, 2, 1, FlintFixUi.opacity(FlintFixUi.raised(), 0.9f));
                int fillWidth = Math.round(barW * item.remaining() / (float)item.max());
                if (fillWidth > 0) FlintFixUi.roundedRaw(context, textX, barY, fillWidth, 2, 1, color);
            }
        }

        context.getMatrices().pop();
        return new HudBounds(p.x, p.y, p.width, p.height);
    }

    private static List<ArmorHudItem> armorHudItems(MinecraftClient client, boolean editor) {
        ArrayList<ArmorHudItem> result = new ArrayList<>(5);
        if (client == null || client.player == null) {
            if (editor) {
                addDemoArmorItem(result, Items.DIAMOND_HELMET, 5);
                addDemoArmorItem(result, Items.DIAMOND_CHESTPLATE, 321);
                addDemoArmorItem(result, Items.DIAMOND_LEGGINGS, 440);
                addDemoArmorItem(result, Items.DIAMOND_BOOTS, 390);
                addDemoArmorItem(result, Items.DIAMOND_SWORD, 12);
            }
            return result;
        }

        addArmorItem(result, client.player.getEquippedStack(EquipmentSlot.HEAD));
        addArmorItem(result, client.player.getEquippedStack(EquipmentSlot.CHEST));
        addArmorItem(result, client.player.getEquippedStack(EquipmentSlot.LEGS));
        addArmorItem(result, client.player.getEquippedStack(EquipmentSlot.FEET));
        addArmorItem(result, client.player.getMainHandStack());
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
        float ratio = max <= 0 ? 0.0f : remaining / (float)max;
        if (ratio <= 0.20f) return 0xFFFF6268;
        if (ratio <= 0.40f) return 0xFFFF963D;
        if (ratio <= 0.65f) return 0xFFFFD34F;
        return 0xFF71D687;
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

    /** The editor always shows a panel so widgets stay visible while they are arranged. */
    private static int panelOpacity(boolean background, float opacity, boolean editor) {
        if (editor) return 200;
        return background ? Math.round(opacity * 255.0f) : 0;
    }

    /** Themed widget panel: soft edge, background fill and a faint top highlight. */
    private static void drawHudPanel(DrawContext context, int width, int height, int opacity) {
        if (opacity <= 0 || width <= 0 || height <= 0) return;
        float a = Math.max(0, Math.min(255, opacity)) / 255.0f;
        FlintFixUi.roundedRaw(context, 0, 0, width, height, 3, FlintFixUi.opacity(FlintFixUi.border(), Math.min(1.0f, a * 0.9f)));
        FlintFixUi.roundedRaw(context, 1, 1, width - 2, height - 2, 2, FlintFixUi.opacity(FlintFixUi.bg(), a));
        context.fill(3, 1, width - 3, 2, FlintFixUi.opacity(0xFFFFFFFF, a * 0.07f));
    }

    /** Rounded key tile that eases into the accent color while pressed. */
    private static void drawKey(DrawContext c, int x, int y, int w, int h, String label, boolean pressed) {
        float t = FlintFixUi.hoverProgress("hud-key:" + label, pressed);
        int fill = FlintFixUi.blendColors(FlintFixUi.opacity(FlintFixUi.raised(), 0.55f), FlintFixUi.accent(), t);
        int edge = FlintFixUi.blendColors(FlintFixUi.opacity(FlintFixUi.border(), 0.9f), FlintFixUi.accentBright(), t);
        FlintFixUi.roundedRaw(c, x, y, w, h, 3, edge);
        FlintFixUi.roundedRaw(c, x + 1, y + 1, w - 2, h - 2, 2, fill);
        int ink = FlintFixUi.blendColors(FlintFixUi.text(), FlintFixUi.onAccent(), t);
        FlintFixFont.drawCenteredExact(c, label, x + w / 2, FlintFixFont.centeredY(y, h, 10), 10, ink, true);
    }

    private static void drawMouseKey(DrawContext c, int x, int y, int w, int h, String label, int cps, boolean pressed) {
        float t = FlintFixUi.hoverProgress("hud-key:" + label, pressed);
        int fill = FlintFixUi.blendColors(FlintFixUi.opacity(FlintFixUi.raised(), 0.55f), FlintFixUi.accent(), t);
        int edge = FlintFixUi.blendColors(FlintFixUi.opacity(FlintFixUi.border(), 0.9f), FlintFixUi.accentBright(), t);
        FlintFixUi.roundedRaw(c, x, y, w, h, 3, edge);
        FlintFixUi.roundedRaw(c, x + 1, y + 1, w - 2, h - 2, 2, fill);
        int ink = FlintFixUi.blendColors(FlintFixUi.text(), FlintFixUi.onAccent(), t);
        if (CONFIG.keystrokesShowCps) {
            FlintFixFont.drawCenteredExact(c, label, x + w / 2, y + 2, 8, ink, true);
            FlintFixFont.drawCenteredExact(c, cps + " CPS", x + w / 2, y + 10, 6,
                FlintFixUi.blendColors(FlintFixUi.muted(), ink, t), false);
        } else {
            FlintFixFont.drawCenteredExact(c, label, x + w / 2, FlintFixFont.centeredY(y, h, 8), 8, ink, true);
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
