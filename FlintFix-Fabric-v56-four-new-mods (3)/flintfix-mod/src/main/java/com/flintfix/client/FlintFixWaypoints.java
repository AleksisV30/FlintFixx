package com.flintfix.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
//? if >=1.21.9 {
/*import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
*///?} else {
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
//?}
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Waypoints saved per world (singleplayer save or server address) and per
 * dimension. Visible waypoints get a light beam and a label with the distance
 * that shows through walls; they also appear on the compass bar.
 */
public final class FlintFixWaypoints {
    public static final int[] COLORS = {0xFF5CC8FF, 0xFF6FDC9A, 0xFFFFE15C, 0xFFFFA14A, 0xFFFF5C5C, 0xFFB57BFF, 0xFFFF7AD9, 0xFFFFFFFF};
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("flintfix-waypoints.json");
    private static final int MAX_PER_WORLD = 100;
    private static Store store;
    private static boolean wasDead;

    public static final class Waypoint {
        public String name;
        public int x;
        public int y;
        public int z;
        public String dimension;
        public int color;
        public boolean visible = true;
        public boolean death;

        Waypoint() {}

        Waypoint(String name, int x, int y, int z, String dimension, int color) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimension = dimension;
            this.color = color;
        }
    }

    private static final class Store {
        Map<String, List<Waypoint>> worlds = new LinkedHashMap<>();
    }

    private FlintFixWaypoints() {}

    private static Store store() {
        if (store == null) {
            store = new Store();
            if (Files.exists(PATH)) {
                try (Reader reader = Files.newBufferedReader(PATH)) {
                    Store loaded = GSON.fromJson(reader, Store.class);
                    if (loaded != null && loaded.worlds != null) store = loaded;
                } catch (Exception ignored) {
                    // A broken file starts an empty list rather than breaking the game.
                }
            }
        }
        return store;
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(store(), writer);
            }
        } catch (Exception ignored) {
        }
    }

    /** "sp:<save name>" in singleplayer, "mp:<address>" on a server. */
    public static String worldKey(Minecraft client) {
        if (client.isLocalServer() && client.getSingleplayerServer() != null) {
            return "sp:" + client.getSingleplayerServer().getWorldData().getLevelName();
        }
        ServerData server = client.getCurrentServer();
        if (server != null && server.ip != null) return "mp:" + server.ip.toLowerCase(Locale.ROOT);
        return "unknown";
    }

    public static String dimension(Minecraft client) {
        return client.level == null ? "" : FlintFixCompat.keyId(client.level.dimension()).toString();
    }

    /** All waypoints of the current world (every dimension). Never null. */
    public static List<Waypoint> currentWorld(Minecraft client) {
        return store().worlds.computeIfAbsent(worldKey(client), key -> new ArrayList<>());
    }

    /** Visible waypoints in the dimension the player is in. */
    public static List<Waypoint> visibleHere(Minecraft client) {
        List<Waypoint> result = new ArrayList<>();
        if (client.level == null) return result;
        String dimension = dimension(client);
        for (Waypoint waypoint : currentWorld(client)) {
            if (waypoint.visible && dimension.equals(waypoint.dimension)) result.add(waypoint);
        }
        return result;
    }

    public static Waypoint addHere(Minecraft client, String name) {
        if (client.player == null || client.level == null) return null;
        List<Waypoint> list = currentWorld(client);
        if (list.size() >= MAX_PER_WORLD) return null;
        String clean = name == null || name.isBlank() ? "Waypoint " + (list.size() + 1) : name.trim();
        if (clean.length() > 32) clean = clean.substring(0, 32);
        Waypoint waypoint = new Waypoint(clean, (int) Math.floor(client.player.getX()), (int) Math.floor(client.player.getY()),
            (int) Math.floor(client.player.getZ()), dimension(client), COLORS[list.size() % COLORS.length]);
        list.add(waypoint);
        save();
        return waypoint;
    }

    public static void remove(Minecraft client, Waypoint waypoint) {
        currentWorld(client).remove(waypoint);
        save();
    }

    /** Drops a "Death" waypoint where the player died, replacing the previous one. */
    public static void tick(Minecraft client) {
        if (client.player == null || client.level == null) {
            wasDead = false;
            return;
        }
        boolean dead = client.player.isDeadOrDying();
        if (dead && !wasDead && FlintFixClient.CONFIG.waypointsEnabled && FlintFixClient.CONFIG.waypointsDeath) {
            List<Waypoint> list = currentWorld(client);
            list.removeIf(waypoint -> waypoint.death);
            Waypoint death = new Waypoint("Death", (int) Math.floor(client.player.getX()),
                (int) Math.floor(client.player.getY()), (int) Math.floor(client.player.getZ()), dimension(client), 0xFFFF5C5C);
            death.death = true;
            list.add(death);
            save();
        }
        wasDead = dead;
    }

    public static void render(WorldRenderContext context) {
        //? if >=26.2 {
        /*// In-world rendering is not ported to 26.2 yet (no MultiBufferSource/Tesselator).
        *///?} else {
        FlintFixConfig config = FlintFixClient.CONFIG;
        if (config == null || !config.waypointsEnabled || FlintFixCompat.matrices(context) == null || context.consumers() == null) return;
        Minecraft client = Minecraft.getInstance();
        List<Waypoint> waypoints = visibleHere(client);
        if (waypoints.isEmpty()) return;
        Vec3 camera = FlintFixCompat.cameraPos(context);
        PoseStack matrices = FlintFixCompat.matrices(context);

        if (config.waypointsBeams) {
            matrices.pushPose();
            matrices.translate(-camera.x, -camera.y, -camera.z);
            Matrix4f matrix = matrices.last().pose();
            VertexConsumer quads = context.consumers().getBuffer(FlintFixCompat.debugQuadsType());
            for (Waypoint waypoint : waypoints) {
                Vec3 base = new Vec3(waypoint.x + 0.5, waypoint.y, waypoint.z + 0.5);
                double distance = base.distanceTo(camera);
                if (distance > 600.0) continue;
                double width = 0.25 + distance * 0.004;
                Vec3 top = base.add(0.0, 220.0, 0.0);
                FlintFixWorldDraw.line(quads, matrix, camera, base, base.add(0.0, 6.0, 0.0), width, waypoint.color, 0.55f);
                FlintFixWorldDraw.line(quads, matrix, camera, base.add(0.0, 6.0, 0.0), top, width * 0.6, waypoint.color, 0.22f);
            }
            matrices.popPose();
        }

        Font text = client.font;
        for (Waypoint waypoint : waypoints) {
            Vec3 target = new Vec3(waypoint.x + 0.5, waypoint.y + 1.6, waypoint.z + 0.5);
            Vec3 toTarget = target.subtract(camera);
            double distance = toTarget.length();
            if (distance < 0.5) continue;
            // Labels far away are drawn closer along the same line so they never fall
            // outside the view distance, but keep the same size on screen.
            double shown = Math.min(distance, 48.0);
            Vec3 at = camera.add(toTarget.scale(shown / distance));
            float scale = 0.025f * (float) Math.max(1.0, shown / 7.0);
            String label = waypoint.name + (config.waypointsDistance ? "  " + Math.round(distance) + "m" : "");

            matrices.pushPose();
            matrices.translate(at.x - camera.x, at.y - camera.y, at.z - camera.z);
            matrices.mulPose(FlintFixCompat.cameraRotation(context));
            matrices.scale(scale, -scale, scale);
            float x = -text.width(label) / 2.0f;
            int background = 0x66000000;
            text.drawInBatch(label, x, 0.0f, 0xFFFFFFFF, false, matrices.last().pose(), context.consumers(),
                Font.DisplayMode.SEE_THROUGH, background, LightTexture.FULL_BRIGHT);
            text.drawInBatch("◆", -text.width("◆") / 2.0f, -10.0f, waypoint.color, false, matrices.last().pose(),
                context.consumers(), Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);
            matrices.popPose();
        }
        //?}
    }
}
