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
    // HUD widgets. Every layout is measured from the real rendered text size
    // (FlintFixFont snaps small text to whole pixels, which changes with the
    // GUI scale), so nothing overlaps at any scale.
    // ------------------------------------------------------------------

    private static final int PAD = 5;
    private static final int GOOD = 0xFF6FDC9A;
    private static final int WARN = 0xFFF0C25E;
    private static final int BAD = 0xFFF07373;

    public static HudBounds renderFpsHud(DrawContext context, MinecraftClient client, boolean editor, boolean selected) {
        return renderFpsHud(context, client, editor, selected, false);
    }

    static HudBounds renderFpsHud(DrawContext context, MinecraftClient client, boolean editor, boolean selected, boolean preview) {
        if (!CONFIG.fpsEnabled && !editor && !preview) return emptyBounds();

        int fps = client.getCurrentFps();
        String value = Integer.toString(fps);
        String label = "FPS";
        int valueSize = 12;
        int labelSize = 6;
        int valueWidth = FlintFixFont.width(value, valueSize, true);
        int labelWidth = FlintFixFont.width(label, labelSize, true);
        int valueLine = FlintFixFont.lineHeight(valueSize);
        int rawWidth = PAD + 8 + valueWidth + 3 + labelWidth + PAD + 1;
        int rawHeight = valueLine + PAD * 2 - 1;
        float scale = CONFIG.fpsScale;
        HudPlacement p = placement(client, CONFIG.fpsX, CONFIG.fpsY, rawWidth, rawHeight, scale);

        beginWidget(context, p, scale);
        int opacity = panelOpacity(CONFIG.fpsBackground, CONFIG.fpsBackgroundOpacity, editor);
        boolean shadow = CONFIG.fpsTextShadow && opacity < 110;
        drawHudPanel(context, rawWidth, rawHeight, opacity);
        int status = statusColor(fps >= 60 ? GOOD : (fps >= 30 ? WARN : BAD));
        FlintFixUi.roundedRaw(context, PAD, rawHeight / 2 - 2, 5, 5, 2, status);
        int valueY = FlintFixFont.centeredY(0, rawHeight, valueSize);
        FlintFixFont.drawExact(context, value, PAD + 8, valueY, valueSize, FlintFixUi.text(), true, shadow);
        int labelY = valueY + capBottom(valueSize) - capBottom(labelSize);
        FlintFixFont.drawExact(context, label, PAD + 8 + valueWidth + 3, labelY, labelSize, FlintFixUi.accentBright(), true, shadow);
        endWidget(context);
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
        int labelLine = FlintFixFont.lineHeight(6);
        int valueLine = FlintFixFont.lineHeight(11);
        int column = Math.max(26, Math.max(FlintFixFont.width("LMB", 6, true), FlintFixFont.width("88", 11, true)));
        int rawWidth = PAD + column + 9 + column + PAD;
        int rawHeight = PAD + labelLine + valueLine + 4 + PAD - 2;
        float scale = CONFIG.cpsScale;
        HudPlacement p = placement(client, CONFIG.cpsX, CONFIG.cpsY, rawWidth, rawHeight, scale);

        beginWidget(context, p, scale);
        int opacity = panelOpacity(CONFIG.cpsBackground, CONFIG.cpsBackgroundOpacity, editor);
        boolean shadow = opacity < 110;
        drawHudPanel(context, rawWidth, rawHeight, opacity);
        drawCpsColumn(context, PAD, column, labelLine, valueLine, "LMB", leftCps, shadow);
        int dividerX = PAD + column + 4;
        context.fill(dividerX, PAD, dividerX + 1, rawHeight - PAD, FlintFixUi.opacity(FlintFixUi.border(), 0.7f));
        drawCpsColumn(context, dividerX + 5, column, labelLine, valueLine, "RMB", rightCps, shadow);
        endWidget(context);
        return new HudBounds(p.x, p.y, p.width, p.height);
    }

    private static void drawCpsColumn(DrawContext context, int x, int width, int labelLine, int valueLine,
                                      String label, int cps, boolean shadow) {
        int top = PAD - 1;
        FlintFixFont.drawExact(context, label, x, top, 6, FlintFixUi.muted(), true, shadow);
        FlintFixFont.drawExact(context, Integer.toString(cps), x, top + labelLine, 11, FlintFixUi.text(), true, shadow);
        int barY = top + labelLine + valueLine + 1;
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

        int px = client.player == null ? 0 : (int) Math.floor(client.player.getX());
        int py = client.player == null ? 64 : (int) Math.floor(client.player.getY());
        int pz = client.player == null ? 0 : (int) Math.floor(client.player.getZ());
        String facing = "North";
        String axis = "-Z";
        if (client.player != null) {
            switch (client.player.getHorizontalFacing()) {
                case SOUTH -> { facing = "South"; axis = "+Z"; }
                case EAST -> { facing = "East"; axis = "+X"; }
                case WEST -> { facing = "West"; axis = "-X"; }
                default -> { facing = "North"; axis = "-Z"; }
            }
        }
        String biome = "Plains";
        if (client.player != null && client.world != null) {
            String key = client.world.getBiome(client.player.getBlockPos()).getKey()
                .map(registryKey -> registryKey.getValue().getPath())
                .orElse("unknown");
            biome = titleCase(key.replace('_', ' '));
        }
        String[] labels = {"X", "Y", "Z", "FACING", "BIOME"};
        String[] values = {Integer.toString(px), Integer.toString(py), Integer.toString(pz), facing + "  " + axis, biome};
        int[] pips = {0xFFE68C8C, 0xFF91D89A, 0xFF8CB2F4, FlintFixUi.accent(), FlintFixUi.accent()};

        int labelW = 0;
        for (String label : labels) labelW = Math.max(labelW, FlintFixFont.width(label, 6, true));
        int valueW = 40;
        for (String value : values) valueW = Math.max(valueW, FlintFixFont.width(value, 7, true));
        valueW = Math.min(valueW, 112);
        int rowH = Math.max(FlintFixFont.lineHeight(6), FlintFixFont.lineHeight(7)) + 1;
        int rawWidth = PAD + 5 + labelW + 6 + valueW + PAD;
        int rawHeight = PAD * 2 + rowH * labels.length - 2;
        float scale = CONFIG.coordinatesScale;
        HudPlacement p = placement(client, CONFIG.coordinatesX, CONFIG.coordinatesY, rawWidth, rawHeight, scale);

        beginWidget(context, p, scale);
        int opacity = panelOpacity(CONFIG.coordinatesBackground, CONFIG.coordinatesBackgroundOpacity, editor);
        boolean shadow = opacity < 110;
        drawHudPanel(context, rawWidth, rawHeight, opacity);
        int valueX = PAD + 5 + labelW + 6;
        for (int line = 0; line < labels.length; line++) {
            int rowY = PAD - 1 + line * rowH;
            FlintFixUi.roundedRaw(context, PAD, rowY + rowH / 2 - 2, 2, 4, 1, pips[line]);
            FlintFixFont.drawExact(context, labels[line], PAD + 5, FlintFixFont.centeredY(rowY, rowH, 6), 6,
                FlintFixUi.muted(), true, shadow);
            FlintFixFont.drawExact(context, FlintFixFont.trim(values[line], valueW, 7, true), valueX,
                FlintFixFont.centeredY(rowY, rowH, 7), 7, FlintFixUi.text(), true, shadow);
            if (line == 2) {
                context.fill(PAD, rowY + rowH, rawWidth - PAD, rowY + rowH + 1,
                    FlintFixUi.opacity(FlintFixUi.border(), 0.45f));
            }
        }
        endWidget(context);
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
        int signal = ping < 0 ? FlintFixUi.muted()
            : statusColor(ping <= 80 ? GOOD : (ping <= 160 ? WARN : BAD));
        int labelLine = FlintFixFont.lineHeight(6);
        int valueLine = FlintFixFont.lineHeight(11);
        int valueWidth = FlintFixFont.width(text, 11, true);
        int unitWidth = FlintFixFont.width("ms", 6, true);
        int textW = Math.max(FlintFixFont.width("PING", 6, true), valueWidth + 2 + unitWidth);
        int barsW = 4 * 3 + 3;
        int rawWidth = PAD + textW + 8 + barsW + PAD;
        int rawHeight = Math.max(labelLine + valueLine - 1, 15) + PAD * 2 - 2;
        float scale = CONFIG.pingScale;
        HudPlacement p = placement(client, CONFIG.pingX, CONFIG.pingY, rawWidth, rawHeight, scale);

        beginWidget(context, p, scale);
        int opacity = panelOpacity(CONFIG.pingBackground, CONFIG.pingBackgroundOpacity, editor);
        boolean shadow = opacity < 110;
        drawHudPanel(context, rawWidth, rawHeight, opacity);
        int top = PAD - 1;
        FlintFixFont.drawExact(context, "PING", PAD, top, 6, FlintFixUi.muted(), true, shadow);
        int valueY = top + labelLine - 1;
        FlintFixFont.drawExact(context, text, PAD, valueY, 11, FlintFixUi.text(), true, shadow);
        FlintFixFont.drawExact(context, "ms", PAD + valueWidth + 2, valueY + capBottom(11) - capBottom(6), 6,
            FlintFixUi.muted(), true, shadow);
        int active = ping < 0 ? 0 : (ping <= 60 ? 4 : (ping <= 110 ? 3 : (ping <= 180 ? 2 : 1)));
        int bottom = rawHeight - PAD;
        for (int bar = 0; bar < 4; bar++) {
            int barHeight = 4 + bar * 3;
            int barX = rawWidth - PAD - barsW + bar * 4;
            FlintFixUi.roundedRaw(context, barX, bottom - barHeight, 3, barHeight, 1,
                bar < active ? signal : FlintFixUi.opacity(FlintFixUi.raised(), 0.9f));
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

        int key = Math.max(18, FlintFixFont.lineHeight(10) + 6);
        int gap = 2;
        int pad = 3;
        int rawWidth = key * 3 + gap * 2 + pad * 2;
        int mouseH = CONFIG.keystrokesShowCps
            ? Math.max(18, FlintFixFont.lineHeight(8) + FlintFixFont.lineHeight(6) + 3)
            : Math.max(14, FlintFixFont.lineHeight(8) + 4);
        int spaceH = 7;
        int rawHeight = key * 2 + gap + mouseH + gap + spaceH + gap + pad * 2;
        int mouseW = (rawWidth - pad * 2 - gap) / 2;
        float scale = CONFIG.keystrokesScale;
        HudPlacement p = placement(client, CONFIG.keystrokesX, CONFIG.keystrokesY, rawWidth, rawHeight, scale);

        beginWidget(context, p, scale);
        drawHudPanel(context, rawWidth, rawHeight,
            panelOpacity(CONFIG.keystrokesBackground, CONFIG.keystrokesBackgroundOpacity, editor));
        drawKey(context, pad + key + gap, pad, key, key, "W", client.options.forwardKey.isPressed());
        drawKey(context, pad, pad + key + gap, key, key, "A", client.options.leftKey.isPressed());
        drawKey(context, pad + key + gap, pad + key + gap, key, key, "S", client.options.backKey.isPressed());
        drawKey(context, pad + (key + gap) * 2, pad + key + gap, key, key, "D", client.options.rightKey.isPressed());

        int mouseY = pad + (key + gap) * 2;
        drawMouseKey(context, pad, mouseY, mouseW, mouseH, "LMB", getLeftCps(), client.options.attackKey.isPressed());
        drawMouseKey(context, pad + mouseW + gap, mouseY, mouseW, mouseH, "RMB", getRightCps(), client.options.useKey.isPressed());
        drawSpaceBar(context, pad, mouseY + mouseH + gap, rawWidth - pad * 2, spaceH, client.options.jumpKey.isPressed());
        endWidget(context);
        return new HudBounds(p.x, p.y, p.width, p.height);
    }

    public static HudBounds renderArmorHud(DrawContext context, MinecraftClient client, boolean editor, boolean selected) {
        return renderArmorHud(context, client, editor, selected, false);
    }

    /**
     * One row per damageable piece: item icon, remaining durability in its
     * status color with the percentage beside it, and a bar underneath. Text
     * widths are measured, so long values never run into each other.
     */
    static HudBounds renderArmorHud(DrawContext context, MinecraftClient client, boolean editor, boolean selected, boolean preview) {
        if (!CONFIG.armorEnabled && !editor && !preview) return emptyBounds();

        List<ArmorHudItem> items = armorHudItems(client, editor || preview);
        int valueSize = 7;
        int percentSize = 6;
        int valueLine = FlintFixFont.lineHeight(valueSize);
        int rowH = Math.max(18, valueLine + 6);
        int textX = PAD + 16 + 4;
        int valueW = 0;
        int percentW = FlintFixFont.width("100%", percentSize, true);
        for (ArmorHudItem item : items) valueW = Math.max(valueW, FlintFixFont.width(Integer.toString(item.remaining()), valueSize, true));
        int contentW = Math.max(36, valueW + 4 + percentW);
        int rawWidth = items.isEmpty() ? FlintFixFont.width("NO ARMOR", 6, true) + PAD * 2 + 4 : textX + contentW + PAD;
        int rawHeight = items.isEmpty() ? FlintFixFont.lineHeight(6) + PAD * 2 : 3 + items.size() * rowH + 2;
        float scale = CONFIG.armorScale;
        HudPlacement p = placement(client, CONFIG.armorX, CONFIG.armorY, rawWidth, rawHeight, scale);

        beginWidget(context, p, scale);
        int opacity = panelOpacity(CONFIG.armorBackground, CONFIG.armorBackgroundOpacity, editor);
        boolean shadow = opacity < 110;
        drawHudPanel(context, rawWidth, rawHeight, opacity);

        if (items.isEmpty()) {
            FlintFixFont.drawCenteredExact(context, "NO ARMOR", rawWidth / 2, FlintFixFont.centeredY(0, rawHeight, 6), 6,
                FlintFixUi.muted(), true);
        } else {
            long now = System.currentTimeMillis();
            for (int i = 0; i < items.size(); i++) {
                ArmorHudItem item = items.get(i);
                int rowY = 3 + i * rowH;
                float ratio = item.remaining() / (float) Math.max(1, item.max());
                int color = statusColor(durabilityColor(item.remaining(), item.max()));
                if (ratio <= 0.10f) {
                    // Nearly broken: the row softly pulses red.
                    float pulse = 0.5f + 0.5f * (float) Math.sin(now / 160.0);
                    FlintFixUi.roundedRaw(context, 2, rowY, rawWidth - 4, rowH - 1, 2,
                        FlintFixUi.opacity(BAD, 0.10f + 0.14f * pulse));
                }
                context.drawItem(item.stack(), PAD - 1, rowY + (rowH - 16) / 2);
                int textY = rowY + Math.max(1, (rowH - valueLine - 4) / 2);
                String value = Integer.toString(item.remaining());
                FlintFixFont.drawExact(context, value, textX, textY, valueSize, color, true, shadow);
                String percent = Math.round(ratio * 100.0f) + "%";
                int percentX = rawWidth - PAD - FlintFixFont.width(percent, percentSize, true);
                FlintFixFont.drawExact(context, percent, percentX, textY + capBottom(valueSize) - capBottom(percentSize),
                    percentSize, FlintFixUi.muted(), true, shadow);
                int barY = textY + valueLine + 1;
                int barW = rawWidth - PAD - textX;
                FlintFixUi.roundedRaw(context, textX, barY, barW, 2, 1, FlintFixUi.opacity(FlintFixUi.raised(), 0.9f));
                int fillWidth = Math.round(barW * ratio);
                if (fillWidth > 0) FlintFixUi.roundedRaw(context, textX, barY, fillWidth, 2, 1, color);
            }
        }
        endWidget(context);
        return new HudBounds(p.x, p.y, p.width, p.height);
    }

    private static List<ArmorHudItem> armorHudItems(MinecraftClient client, boolean editor) {
        ArrayList<ArmorHudItem> result = new ArrayList<>(5);
        if (client == null || client.player == null) {
            if (editor) {
                addDemoArmorItem(result, Items.DIAMOND_HELMET, 300);
                addDemoArmorItem(result, Items.DIAMOND_CHESTPLATE, 321);
                addDemoArmorItem(result, Items.NETHERITE_LEGGINGS, 512);
                addDemoArmorItem(result, Items.DIAMOND_BOOTS, 190);
                addDemoArmorItem(result, Items.NETHERITE_SWORD, 120);
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
        if (ratio <= 0.40f) return 0xFFF59A52;
        if (ratio <= 0.65f) return WARN;
        return GOOD;
    }

    /** Status colors are tuned for dark panels; deepen them so they stay readable on the light theme. */
    private static int statusColor(int color) {
        if (FlintFixUi.activeTheme() != FlintFixTheme.LIGHT) return color;
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

    /** Themed widget panel: soft edge, background fill, a faint top highlight and an accent tick. */
    private static void drawHudPanel(DrawContext context, int width, int height, int opacity) {
        if (opacity <= 0 || width <= 0 || height <= 0) return;
        float a = Math.max(0, Math.min(255, opacity)) / 255.0f;
        FlintFixUi.roundedRaw(context, 0, 0, width, height, 3,
            FlintFixUi.opacity(FlintFixUi.border(), Math.min(1.0f, a * 0.75f)));
        FlintFixUi.roundedRaw(context, 1, 1, width - 2, height - 2, 2, FlintFixUi.opacity(FlintFixUi.bg(), a));
        context.fill(3, 1, width - 3, 2, FlintFixUi.opacity(0xFFFFFFFF, a * 0.08f));
        context.fill(1, 3, 2, height - 3, FlintFixUi.opacity(FlintFixUi.accent(), Math.min(1.0f, a * 1.4f)));
    }

    /** Rounded key tile that eases into the accent color while pressed and dips one pixel. */
    private static void drawKey(DrawContext c, int x, int y, int w, int h, String label, boolean pressed) {
        float t = FlintFixUi.hoverProgress("hud-key:" + label, pressed);
        int fill = FlintFixUi.blendColors(FlintFixUi.opacity(FlintFixUi.raised(), 0.6f), FlintFixUi.accent(), t);
        int edge = FlintFixUi.blendColors(FlintFixUi.opacity(FlintFixUi.border(), 0.9f), FlintFixUi.accentBright(), t);
        int dip = Math.round(t);
        FlintFixUi.roundedRaw(c, x, y + dip, w, h - dip, 3, edge);
        FlintFixUi.roundedRaw(c, x + 1, y + 1 + dip, w - 2, h - 2 - dip, 2, fill);
        if (dip == 0) c.fill(x + 2, y + h - 1, x + w - 2, y + h, FlintFixUi.opacity(0xFF000000, 0.25f));
        int ink = FlintFixUi.blendColors(FlintFixUi.text(), FlintFixUi.onAccent(), t);
        FlintFixFont.drawCenteredExact(c, label, x + w / 2, FlintFixFont.centeredY(y + dip, h - dip, 10), 10, ink, true);
    }

    private static void drawMouseKey(DrawContext c, int x, int y, int w, int h, String label, int cps, boolean pressed) {
        float t = FlintFixUi.hoverProgress("hud-key:" + label, pressed);
        int fill = FlintFixUi.blendColors(FlintFixUi.opacity(FlintFixUi.raised(), 0.6f), FlintFixUi.accent(), t);
        int edge = FlintFixUi.blendColors(FlintFixUi.opacity(FlintFixUi.border(), 0.9f), FlintFixUi.accentBright(), t);
        FlintFixUi.roundedRaw(c, x, y, w, h, 3, edge);
        FlintFixUi.roundedRaw(c, x + 1, y + 1, w - 2, h - 2, 2, fill);
        int ink = FlintFixUi.blendColors(FlintFixUi.text(), FlintFixUi.onAccent(), t);
        if (CONFIG.keystrokesShowCps) {
            int labelLine = FlintFixFont.lineHeight(8);
            int block = labelLine + FlintFixFont.lineHeight(6) - 1;
            int top = y + Math.max(1, (h - block) / 2);
            FlintFixFont.drawCenteredExact(c, label, x + w / 2, top, 8, ink, true);
            FlintFixFont.drawCenteredExact(c, cps + " CPS", x + w / 2, top + labelLine - 1, 6,
                FlintFixUi.blendColors(FlintFixUi.muted(), ink, t), true);
        } else {
            FlintFixFont.drawCenteredExact(c, label, x + w / 2, FlintFixFont.centeredY(y, h, 8), 8, ink, true);
        }
    }

    private static void drawSpaceBar(DrawContext c, int x, int y, int w, int h, boolean pressed) {
        float t = FlintFixUi.hoverProgress("hud-key:space", pressed);
        int fill = FlintFixUi.blendColors(FlintFixUi.opacity(FlintFixUi.raised(), 0.6f), FlintFixUi.accent(), t);
        int edge = FlintFixUi.blendColors(FlintFixUi.opacity(FlintFixUi.border(), 0.9f), FlintFixUi.accentBright(), t);
        FlintFixUi.roundedRaw(c, x, y, w, h, 3, edge);
        FlintFixUi.roundedRaw(c, x + 1, y + 1, w - 2, h - 2, 2, fill);
        int bar = Math.max(8, w / 3);
        c.fill(x + (w - bar) / 2, y + h / 2, x + (w + bar) / 2, y + h / 2 + 1,
            FlintFixUi.blendColors(FlintFixUi.muted(), FlintFixUi.onAccent(), t));
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
