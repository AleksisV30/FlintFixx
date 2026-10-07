package com.flintfix.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
//? if >=1.21 {
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
//?} else {
/*import net.minecraft.client.gui.screens.VideoSettingsScreen;
*///?}
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.debug.ChunkBorderRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.platform.InputConstants;
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

    private static KeyMapping settingsKey;
    private static KeyMapping freecamKey;
    private static KeyMapping zoomKey;
    private static KeyMapping lookAroundKey;
    private static KeyMapping inspectKey;
    private static KeyMapping waypointKey;
    private static ChunkBorderRenderer chunkBorderRenderer;
    private static final Deque<Long> LEFT_CLICKS = new ArrayDeque<>();
    private static final Deque<Long> RIGHT_CLICKS = new ArrayDeque<>();

    @Override
    public void onInitializeClient() {
        CONFIG = FlintFixConfig.load();
        FlintFixProfileStore.initialize();
        FlintFixUi.applyTheme();
        chunkBorderRenderer = new ChunkBorderRenderer(Minecraft.getInstance());

        if (Boolean.getBoolean("flintfix.auditMixins")) registerMixinAudit();

        // Add a small, native entry point to the standard Minecraft video page.
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof VideoSettingsScreen) {
                int buttonWidth = 88;
                int buttonX = Math.max(4, screen.width - buttonWidth - 8);
                Button videoButton = Button.builder(Component.literal("FlintFix"), button ->
                    client.setScreen(new FlintFixVideoSettingsScreen(screen))
                ).bounds(buttonX, 5, buttonWidth, 20).build();
                Screens.getButtons(screen).add(videoButton);
            }
        });

        settingsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.flintfix.open_client",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            "category.flintfix"
        ));
        freecamKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.flintfix.freecam",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "category.flintfix"
        ));
        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.flintfix.zoom",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            "category.flintfix"
        ));
        lookAroundKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.flintfix.look_around",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "category.flintfix"
        ));
        inspectKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.flintfix.inspect",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_I,
            "category.flintfix"
        ));
        waypointKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.flintfix.waypoint",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            "category.flintfix"
        ));
        FlintFixShulkerPreview.register();
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> FlintFixProfileStore.onJoinServer(client));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(FlintFixProfileStore::onLeaveServer));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.screen instanceof FlintFixFreecamSettingsScreen) {
                while (freecamKey.consumeClick()) { /* key is being rebound in this screen */ }
            } else {
                while (freecamKey.consumeClick()) {
                    if (FlintFixFreecam.isActive()) FlintFixFreecam.disable(client);
                    else if (client.screen == null) FlintFixFreecam.requestEnable(client, null);
                }
            }
            while (settingsKey.consumeClick()) {
                if (client.screen instanceof FlintFixHudEditorScreen editor) {
                    editor.cancelToGame();
                } else if (client.screen instanceof FlintFixHomeScreen home) {
                    home.onClose();
                } else if (client.screen instanceof FlintFixSettingsScreen settings) {
                    settings.onClose();
                } else {
                    client.setScreen(new FlintFixHomeScreen(client.screen));
                }
            }
            FlintFixFreecam.tick(client);
            boolean canUseCameraModes = client.screen == null && client.player != null
                && !FlintFixFreecam.isActive();
            FlintFixZoom.setActive(CONFIG.zoomEnabled && canUseCameraModes && zoomKey.isDown());
            boolean lookAroundActive = CONFIG.lookAroundEnabled && canUseCameraModes && lookAroundKey.isDown();
            if (lookAroundActive) FlintFixLookAround.begin(client);
            else FlintFixLookAround.end(client);
            while (inspectKey.consumeClick()) {
                if (client.screen == null) FlintFixInspect.start(client);
            }
            FlintFixInspect.tick(client);
            while (waypointKey.consumeClick()) {
                if (client.screen != null || client.player == null) continue;
                if (!CONFIG.waypointsEnabled) {
                    client.player.displayClientMessage(Component.literal("Waypoints are turned off in FlintFix"), true);
                    continue;
                }
                FlintFixWaypoints.Waypoint added = FlintFixWaypoints.addHere(client, null);
                client.player.displayClientMessage(Component.literal(added == null ? "This world already has 100 waypoints"
                    : "Waypoint \"" + added.name + "\" added at " + added.x + ", " + added.y + ", " + added.z), true);
            }
            FlintFixWaypoints.tick(client);
            pruneClicks(System.currentTimeMillis());
            FlintFixSocialBridge.tick(client);
            tickSpeed(client);
            FlintFixDamageNumbers.tick(client);
            FlintFixTeammates.tick(client);
        });

        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.screen instanceof FlintFixHudEditorScreen) return;
            // The callback still fires with the HUD hidden (F1), so respect it here.
            if (client.options.hideGui) return;
            renderFpsHud(context, client, false, false);
            renderCpsHud(context, client, false, false);
            renderCoordinatesHud(context, client, false, false);
            renderPingHud(context, client, false, false);
            renderKeystrokesHud(context, client, false, false);
            renderArmorHud(context, client, false, false);
            renderPotionsHud(context, client, false, false);
            renderSpeedHud(context, client, false, false);
            renderCompassHud(context, client, false, false);
            FlintFixSocialBridge.render(context, client);
        });

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (CONFIG == null) return;
            if (CONFIG.chunksEnabled && context.consumers() != null) {
                var cameraPos = context.camera().getPosition();
                chunkBorderRenderer.render(context.matrixStack(), context.consumers(),
                    cameraPos.x, cameraPos.y, cameraPos.z);
            }
            if (CONFIG.trajectoryEnabled) FlintFixTrajectory.render(context);
            if (CONFIG.hitboxesEnabled) FlintFixHitboxes.render(context);
            if (CONFIG.damageNumbersEnabled) FlintFixDamageNumbers.render(context);
            if (CONFIG.waypointsEnabled) FlintFixWaypoints.render(context);
        });
        WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register(FlintFixBlockOutline::render);
        WorldRenderEvents.END.register(context -> FlintFixMotionBlur.render());
        // Client-side hits drive the crosshair hit marker.
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClientSide()) FlintFixCrosshair.onHit();
            return InteractionResult.PASS;
        });
    }

    /**
     * Build check, enabled with -Dflintfix.auditMixins=true: once the first screen
     * is up (title, or the first-launch accessibility screen), force every mixin
     * onto its target (a wrong target fails here instead of mid-game), log the
     * result and quit.
     */
    private static void registerMixinAudit() {
        java.util.concurrent.atomic.AtomicBoolean done = new java.util.concurrent.atomic.AtomicBoolean();
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (done.getAndSet(true)) return;
            org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger("FlintFix");
            try {
                org.spongepowered.asm.mixin.MixinEnvironment.getCurrentEnvironment().audit();
                log.info("FLINTFIX_AUDIT_OK");
            } catch (Throwable error) {
                log.error("FLINTFIX_AUDIT_FAILED", error);
            }
        });
        // Quit once resource loading has finished; stopping mid-load can crash the GL driver on exit.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (done.get() && client.getOverlay() == null) client.stop();
        });
    }

    public static String getSettingsKeyLabel() {
        return settingsKey == null ? "RSHIFT" : settingsKey.getTranslatedKeyMessage().getString();
    }

    /** Key bindings don't fire while a screen is open, so FlintFix screens check the settings key themselves. */
    public static boolean isSettingsKey(int keyCode, int scanCode) {
        return settingsKey != null && settingsKey.matches(keyCode, scanCode);
    }

    public static KeyMapping getFreecamKeyBinding() {
        return freecamKey;
    }

    public static KeyMapping getZoomKeyBinding() { return zoomKey; }
    public static KeyMapping getLookAroundKeyBinding() { return lookAroundKey; }
    public static KeyMapping getInspectKeyBinding() { return inspectKey; }
    public static KeyMapping getWaypointKeyBinding() { return waypointKey; }

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
    public static int getPing(Minecraft client) {
        if (client == null || client.player == null) return -1;
        if (client.isLocalServer()) return 0;

        if (client.getConnection() != null) {
            PlayerInfo entry = client.getConnection().getPlayerInfo(client.player.getUUID());
            if (entry == null) {
                String playerName = client.player.getName().getString();
                for (PlayerInfo candidate : client.getConnection().getOnlinePlayers()) {
                    if (candidate.getProfile().getId().equals(client.player.getUUID()) ||
                        candidate.getProfile().getName().equalsIgnoreCase(playerName)) {
                        entry = candidate;
                        break;
                    }
                }
            }
            if (entry != null && entry.getLatency() >= 0) return entry.getLatency();
        }

        ServerData server = client.getCurrentServer();
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

    public static HudBounds renderFpsHud(GuiGraphics context, Minecraft client, boolean editor, boolean selected) {
        return renderFpsHud(context, client, editor, selected, false);
    }

    static HudBounds renderFpsHud(GuiGraphics context, Minecraft client, boolean editor, boolean selected, boolean preview) {
        if (!CONFIG.fpsEnabled && !editor && !preview) return emptyBounds();
        int fps = client.getFps();
        return lineTile(context, client, editor, CONFIG.fpsX, CONFIG.fpsY, CONFIG.fpsScale,
            CONFIG.fpsBackground, CONFIG.fpsBackgroundOpacity, CONFIG.fpsTextShadow,
            "FPS", Integer.toString(fps), FlintFixUi.text());
    }

    public static HudBounds renderCpsHud(GuiGraphics context, Minecraft client, boolean editor, boolean selected) {
        return renderCpsHud(context, client, editor, selected, false);
    }

    static HudBounds renderCpsHud(GuiGraphics context, Minecraft client, boolean editor, boolean selected, boolean preview) {
        if (!CONFIG.cpsEnabled && !editor && !preview) return emptyBounds();
        if (!editor && !preview && client.player == null) return emptyBounds();
        return lineTile(context, client, editor, CONFIG.cpsX, CONFIG.cpsY, CONFIG.cpsScale,
            CONFIG.cpsBackground, CONFIG.cpsBackgroundOpacity, true,
            "CPS", getLeftCps() + " | " + getRightCps(), FlintFixUi.text());
    }

    public static HudBounds renderPingHud(GuiGraphics context, Minecraft client, boolean editor, boolean selected) {
        return renderPingHud(context, client, editor, selected, false);
    }

    static HudBounds renderPingHud(GuiGraphics context, Minecraft client, boolean editor, boolean selected, boolean preview) {
        if (!CONFIG.pingEnabled && !editor && !preview) return emptyBounds();
        if (!editor && !preview && client.player == null) return emptyBounds();
        int ping = getPing(client);
        String value = ping < 0 ? "--" : ping + " ms";
        int color = ping < 0 ? FlintFixUi.muted() : statusColor(ping <= 80 ? GOOD : (ping <= 160 ? WARN : BAD));
        return lineTile(context, client, editor, CONFIG.pingX, CONFIG.pingY, CONFIG.pingScale,
            CONFIG.pingBackground, CONFIG.pingBackgroundOpacity, true, "PING", value, color);
    }

    /** One line: "LABEL value", e.g. "FPS 144". */
    private static HudBounds lineTile(GuiGraphics context, Minecraft client, boolean editor, float nx, float ny,
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

    public static HudBounds renderCoordinatesHud(GuiGraphics context, Minecraft client, boolean editor, boolean selected) {
        return renderCoordinatesHud(context, client, editor, selected, false);
    }

    static HudBounds renderCoordinatesHud(GuiGraphics context, Minecraft client, boolean editor, boolean selected, boolean preview) {
        if (!CONFIG.coordinatesEnabled && !editor && !preview) return emptyBounds();
        if (!editor && !preview && client.player == null) return emptyBounds();

        int px = client.player == null ? 0 : (int) Math.floor(client.player.getX());
        int py = client.player == null ? 64 : (int) Math.floor(client.player.getY());
        int pz = client.player == null ? 0 : (int) Math.floor(client.player.getZ());
        String facing = "North";
        if (client.player != null) {
            switch (client.player.getDirection()) {
                case SOUTH -> facing = "South";
                case EAST -> facing = "East";
                case WEST -> facing = "West";
                default -> facing = "North";
            }
        }
        String biome = "Plains";
        if (client.player != null && client.level != null) {
            String key = client.level.getBiome(client.player.blockPosition()).unwrapKey()
                .map(registryKey -> registryKey.location().getPath())
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

    public static HudBounds renderKeystrokesHud(GuiGraphics context, Minecraft client, boolean editor, boolean selected) {
        return renderKeystrokesHud(context, client, editor, selected, false);
    }

    static HudBounds renderKeystrokesHud(GuiGraphics context, Minecraft client, boolean editor, boolean selected, boolean preview) {
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
        drawKey(context, pad + key + gap, pad, key, key, "W", client.options.keyUp.isDown(), fill);
        drawKey(context, pad, pad + key + gap, key, key, "A", client.options.keyLeft.isDown(), fill);
        drawKey(context, pad + key + gap, pad + key + gap, key, key, "S", client.options.keyDown.isDown(), fill);
        drawKey(context, pad + (key + gap) * 2, pad + key + gap, key, key, "D", client.options.keyRight.isDown(), fill);
        int mouseY = pad + (key + gap) * 2;
        drawMouseKey(context, pad, mouseY, mouseW, mouseH, "LMB", getLeftCps(), client.options.keyAttack.isDown(), fill);
        drawMouseKey(context, pad + mouseW + gap, mouseY, mouseW, mouseH, "RMB", getRightCps(), client.options.keyUse.isDown(), fill);
        endWidget(context);
        return new HudBounds(p.x, p.y, p.width, p.height);
    }

    public static HudBounds renderArmorHud(GuiGraphics context, Minecraft client, boolean editor, boolean selected) {
        return renderArmorHud(context, client, editor, selected, false);
    }

    /** One row per damageable piece: the item icon and its remaining durability, colored by condition. */
    static HudBounds renderArmorHud(GuiGraphics context, Minecraft client, boolean editor, boolean selected, boolean preview) {
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
                context.renderItem(item.stack(), 3, rowY + (rowH - 16) / 2);
                int color = statusColor(durabilityColor(item.remaining(), item.max()));
                FlintFixFont.drawExact(context, Integer.toString(item.remaining()), textX,
                    FlintFixFont.centeredY(rowY, rowH, VALUE), VALUE, color, true, shadow);
            }
        }
        endWidget(context);
        return new HudBounds(p.x, p.y, p.width, p.height);
    }

    // ------------------------------------------------------------------
    // Potion effects, speed and compass
    // ------------------------------------------------------------------

    private static double smoothedSpeed;

    /** Called every client tick: smooths the player's speed in blocks per second. */
    static void tickSpeed(Minecraft client) {
        if (client.player == null) {
            smoothedSpeed = 0.0;
            return;
        }
        double dx = client.player.getX() - client.player.xo;
        double dy = client.player.getY() - client.player.yo;
        double dz = client.player.getZ() - client.player.zo;
        // Count vertical motion only while gliding, so jumping doesn't spike the meter.
        double perTick = FlintFixCompat.isGliding(client.player) ? Math.sqrt(dx * dx + dy * dy + dz * dz) : Math.sqrt(dx * dx + dz * dz);
        smoothedSpeed += (perTick * 20.0 - smoothedSpeed) * 0.35;
        if (smoothedSpeed < 0.01) smoothedSpeed = 0.0;
    }

    public static HudBounds renderSpeedHud(GuiGraphics context, Minecraft client, boolean editor, boolean selected) {
        return renderSpeedHud(context, client, editor, selected, false);
    }

    static HudBounds renderSpeedHud(GuiGraphics context, Minecraft client, boolean editor, boolean selected, boolean preview) {
        if (!CONFIG.speedEnabled && !editor && !preview) return emptyBounds();
        if (!editor && !preview && client.player == null) return emptyBounds();
        double speed = client.player == null ? 5.61 : smoothedSpeed;
        String value = CONFIG.speedUnit == 1
            ? String.format(Locale.ROOT, "%.1f km/h", speed * 3.6)
            : String.format(Locale.ROOT, "%.2f b/s", speed);
        return lineTile(context, client, editor, CONFIG.speedX, CONFIG.speedY, CONFIG.speedScale,
            CONFIG.speedBackground, CONFIG.speedBackgroundOpacity, true, "SPEED", value, FlintFixUi.text());
    }

    public static HudBounds renderPotionsHud(GuiGraphics context, Minecraft client, boolean editor, boolean selected) {
        return renderPotionsHud(context, client, editor, selected, false);
    }

    /**
     * Active effects with their icon, name and time left. The time blinks during
     * the last ten seconds so an expiring effect is easy to notice.
     */
    static HudBounds renderPotionsHud(GuiGraphics context, Minecraft client, boolean editor, boolean selected, boolean preview) {
        if (!CONFIG.potionsEnabled && !editor && !preview) return emptyBounds();
        List<MobEffectInstance> effects = new ArrayList<>();
        if (client.player != null) effects.addAll(client.player.getActiveEffects());
        if (effects.isEmpty() && (editor || preview)) {
            //? if >=1.21.5 {
            /*effects.add(new MobEffectInstance(MobEffects.SPEED, 1680, 1));
            effects.add(new MobEffectInstance(MobEffects.STRENGTH, 160, 0));
            *///?} else {
            effects.add(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1680, 1));
            effects.add(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 160, 0));
            //?}
            effects.add(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 3600, 0));
        }
        if (effects.isEmpty()) return emptyBounds();
        effects.sort((a, b) -> Integer.compare(b.getDuration(), a.getDuration()));

        int rowH = Math.max(20, FlintFixFont.lineHeight(7) + FlintFixFont.lineHeight(LABEL) + 3);
        int textX = 4 + 18 + 4;
        int textW = 0;
        String[] names = new String[effects.size()];
        String[] times = new String[effects.size()];
        for (int i = 0; i < effects.size(); i++) {
            MobEffectInstance effect = effects.get(i);
            String name = FlintFixCompat.effectName(effect);
            if (effect.getAmplifier() > 0) name += " " + roman(effect.getAmplifier() + 1);
            names[i] = name;
            times[i] = effectTime(effect);
            textW = Math.max(textW, Math.max(FlintFixFont.width(name, 7, true), FlintFixFont.width(times[i], LABEL, true)));
        }
        textW = Math.min(textW, 110);
        int rawWidth = textX + textW + PAD_X;
        int rawHeight = 2 + effects.size() * rowH + 1;
        float scale = CONFIG.potionsScale;
        HudPlacement p = placement(client, CONFIG.potionsX, CONFIG.potionsY, rawWidth, rawHeight, scale);

        beginWidget(context, p, scale);
        int opacity = panelOpacity(CONFIG.potionsBackground, CONFIG.potionsBackgroundOpacity, editor);
        boolean shadow = opacity < 110;
        drawHudPanel(context, rawWidth, rawHeight, opacity);
        boolean blinkOn = (System.currentTimeMillis() / 400L) % 2L == 0L;
        for (int i = 0; i < effects.size(); i++) {
            MobEffectInstance effect = effects.get(i);
            int rowY = 2 + i * rowH;
            TextureAtlasSprite sprite = client.getMobEffectTextures().get(effect.getEffect());
            FlintFixCompat.drawSprite(context, 4, rowY + (rowH - 18) / 2, 18, 18, sprite);
            int nameY = rowY + Math.max(0, (rowH - FlintFixFont.lineHeight(7) - FlintFixFont.lineHeight(LABEL)) / 2);
            FlintFixFont.drawExact(context, FlintFixFont.trim(names[i], textW, 7, true), textX, nameY, 7,
                FlintFixUi.text(), true, shadow);
            boolean expiring = !effect.isInfiniteDuration() && effect.getDuration() <= 200;
            int timeColor = expiring ? (blinkOn ? statusColor(BAD) : FlintFixUi.opacity(statusColor(BAD), 0.35f)) : FlintFixUi.muted();
            FlintFixFont.drawExact(context, times[i], textX, nameY + FlintFixFont.lineHeight(7) - 1, LABEL,
                timeColor, true, shadow);
        }
        endWidget(context);
        return new HudBounds(p.x, p.y, p.width, p.height);
    }

    private static String effectTime(MobEffectInstance effect) {
        if (effect.isInfiniteDuration()) return "--:--";
        int seconds = Math.max(0, effect.getDuration() / 20);
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }

    private static String roman(int value) {
        return switch (value) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> Integer.toString(value);
        };
    }

    public static HudBounds renderCompassHud(GuiGraphics context, Minecraft client, boolean editor, boolean selected) {
        return renderCompassHud(context, client, editor, selected, false);
    }

    /**
     * Heading strip: letters at the cardinal directions, smaller labels between
     * them and ticks every 15 degrees, fading toward the edges. A red marker
     * points at your last death in this dimension.
     */
    static HudBounds renderCompassHud(GuiGraphics context, Minecraft client, boolean editor, boolean selected, boolean preview) {
        if (!CONFIG.compassEnabled && !editor && !preview) return emptyBounds();
        if (!editor && !preview && client.player == null) return emptyBounds();

        int rawWidth = 180;
        int valueLine = FlintFixFont.lineHeight(VALUE);
        int rawHeight = valueLine + PAD_Y * 2 + 3;
        float scale = CONFIG.compassScale;
        HudPlacement p = placement(client, CONFIG.compassX, CONFIG.compassY, rawWidth, rawHeight, scale);
        float yaw = client.gameRenderer == null || client.player == null ? 180.0f : client.gameRenderer.getMainCamera().getYRot();
        // Minecraft yaw is 0 toward +Z (south); a compass bearing is 0 toward north.
        float bearing = Mth.wrapDegrees(yaw + 180.0f);
        float range = 75.0f;
        float center = rawWidth / 2.0f;
        float pxPerDegree = (rawWidth - 16) / (range * 2.0f);

        beginWidget(context, p, scale);
        int opacity = panelOpacity(CONFIG.compassBackground, CONFIG.compassBackgroundOpacity, editor);
        boolean shadow = opacity < 110;
        drawHudPanel(context, rawWidth, rawHeight, opacity);
        int textY = PAD_Y + 2;
        for (int degrees = 0; degrees < 360; degrees += 15) {
            float delta = Mth.wrapDegrees(degrees - bearing);
            if (Math.abs(delta) > range) continue;
            int x = Math.round(center + delta * pxPerDegree);
            float fade = 1.0f - Math.max(0.0f, (Math.abs(delta) - range * 0.55f) / (range * 0.45f));
            if (degrees % 90 == 0) {
                String letter = switch (degrees) {
                    case 0 -> "N";
                    case 90 -> "E";
                    case 180 -> "S";
                    default -> "W";
                };
                int color = degrees == 0 ? statusColor(BAD) : FlintFixUi.text();
                FlintFixFont.drawCenteredExact(context, letter, x, textY, VALUE, FlintFixUi.opacity(color, fade), true);
            } else if (degrees % 45 == 0) {
                String label = switch (degrees) {
                    case 45 -> "NE";
                    case 135 -> "SE";
                    case 225 -> "SW";
                    default -> "NW";
                };
                FlintFixFont.drawCenteredExact(context, label, x, textY + capBottom(VALUE) - capBottom(LABEL), LABEL,
                    FlintFixUi.opacity(FlintFixUi.muted(), fade), true);
            } else {
                context.fill(x, textY + 2, x + 1, textY + capBottom(VALUE), FlintFixUi.opacity(FlintFixUi.muted(), 0.6f * fade));
            }
        }
        if (CONFIG.compassDeathMarker && client.player != null && client.level != null) {
            client.player.getLastDeathLocation().ifPresent(death -> {
                if (!death.dimension().equals(client.level.dimension())) return;
                double dx = death.pos().getX() + 0.5 - client.player.getX();
                double dz = death.pos().getZ() + 0.5 - client.player.getZ();
                float target = (float) Math.toDegrees(Math.atan2(dx, -dz));
                float delta = Mth.wrapDegrees(target - bearing);
                if (Math.abs(delta) > range) return;
                int x = Math.round(center + delta * pxPerDegree);
                int y = rawHeight - 4;
                FlintFixUi.roundedRaw(context, x - 2, y - 2, 4, 4, 1, statusColor(BAD));
            });
        }
        if (CONFIG.waypointsEnabled && CONFIG.waypointsCompass && client.player != null && client.level != null) {
            for (FlintFixWaypoints.Waypoint waypoint : FlintFixWaypoints.visibleHere(client)) {
                if (waypoint.death) continue;
                double dx = waypoint.x + 0.5 - client.player.getX();
                double dz = waypoint.z + 0.5 - client.player.getZ();
                float delta = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(dx, -dz)) - bearing);
                if (Math.abs(delta) > range) continue;
                int x = Math.round(center + delta * pxPerDegree);
                FlintFixUi.roundedRaw(context, x - 2, rawHeight - 6, 4, 4, 1, waypoint.color);
            }
        }
        // Center caret.
        int caret = FlintFixUi.accent();
        context.fill(Math.round(center) - 2, 0, Math.round(center) + 3, 1, caret);
        context.fill(Math.round(center) - 1, 1, Math.round(center) + 2, 2, caret);
        context.fill(Math.round(center), 2, Math.round(center) + 1, 3, caret);
        endWidget(context);
        return new HudBounds(p.x, p.y, p.width, p.height);
    }

    private static List<ArmorHudItem> armorHudItems(Minecraft client, boolean editor) {
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

        addArmorItem(result, client.player.getItemBySlot(EquipmentSlot.HEAD));
        addArmorItem(result, client.player.getItemBySlot(EquipmentSlot.CHEST));
        addArmorItem(result, client.player.getItemBySlot(EquipmentSlot.LEGS));
        addArmorItem(result, client.player.getItemBySlot(EquipmentSlot.FEET));
        addArmorItem(result, client.player.getMainHandItem());
        addArmorItem(result, client.player.getOffhandItem());
        return result;
    }

    private static void addDemoArmorItem(List<ArmorHudItem> items, Item item, int remaining) {
        ItemStack stack = new ItemStack(item);
        int max = stack.getMaxDamage();
        int value = Math.max(0, Math.min(max, remaining));
        stack.setDamageValue(max - value);
        items.add(new ArmorHudItem(stack, value, max));
    }

    private static void addArmorItem(List<ArmorHudItem> items, ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.isDamageableItem()) return;
        int max = stack.getMaxDamage();
        int remaining = Math.max(0, max - stack.getDamageValue());
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

    private static void beginWidget(GuiGraphics context, HudPlacement p, float scale) {
        context.pose().pushPose();
        context.pose().translate(p.x, p.y, 0);
        context.pose().scale(scale, scale, 1.0f);
    }

    private static void endWidget(GuiGraphics context) {
        context.pose().popPose();
    }

    /** The editor always shows a panel so widgets stay visible while they are arranged. */
    private static int panelOpacity(boolean background, float opacity, boolean editor) {
        if (editor) return 200;
        return background ? Math.round(opacity * 255.0f) : 0;
    }

    /** A single flat, rounded, translucent panel. */
    private static void drawHudPanel(GuiGraphics context, int width, int height, int opacity) {
        if (opacity <= 0 || width <= 0 || height <= 0) return;
        float a = Math.max(0, Math.min(255, opacity)) / 255.0f;
        FlintFixUi.roundedRaw(context, 0, 0, width, height, 3, FlintFixUi.opacity(FlintFixUi.bg(), a));
    }

    /** Flat key tile that fills with the accent color while pressed. */
    private static void drawKey(GuiGraphics c, int x, int y, int w, int h, String label, boolean pressed, float fill) {
        float t = FlintFixUi.hoverProgress("hud-key:" + label, pressed);
        int idle = FlintFixUi.opacity(FlintFixUi.bg(), fill);
        FlintFixUi.roundedRaw(c, x, y, w, h, 3, FlintFixUi.blendColors(idle, FlintFixUi.accent(), t));
        int ink = FlintFixUi.blendColors(FlintFixUi.text(), FlintFixUi.onAccent(), t);
        FlintFixFont.drawCenteredExact(c, label, x + w / 2, FlintFixFont.centeredY(y, h, VALUE), VALUE, ink, true);
    }

    private static void drawMouseKey(GuiGraphics c, int x, int y, int w, int h, String label, int cps, boolean pressed,
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

    private static HudPlacement placement(Minecraft client, float nx, float ny, int rawWidth, int rawHeight, float scale) {
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
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
