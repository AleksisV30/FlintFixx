package com.flintfix.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
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
    public static String worldKey(MinecraftClient client) {
        if (client.isInSingleplayer() && client.getServer() != null) {
            return "sp:" + client.getServer().getSaveProperties().getLevelName();
        }
        ServerInfo server = client.getCurrentServerEntry();
        if (server != null && server.address != null) return "mp:" + server.address.toLowerCase(Locale.ROOT);
        return "unknown";
    }

    public static String dimension(MinecraftClient client) {
        return client.world == null ? "" : client.world.getRegistryKey().getValue().toString();
    }

    /** All waypoints of the current world (every dimension). Never null. */
    public static List<Waypoint> currentWorld(MinecraftClient client) {
        return store().worlds.computeIfAbsent(worldKey(client), key -> new ArrayList<>());
    }

    /** Visible waypoints in the dimension the player is in. */
    public static List<Waypoint> visibleHere(MinecraftClient client) {
        List<Waypoint> result = new ArrayList<>();
        if (client.world == null) return result;
        String dimension = dimension(client);
        for (Waypoint waypoint : currentWorld(client)) {
            if (waypoint.visible && dimension.equals(waypoint.dimension)) result.add(waypoint);
        }
        return result;
    }

    public static Waypoint addHere(MinecraftClient client, String name) {
        if (client.player == null || client.world == null) return null;
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

    public static void remove(MinecraftClient client, Waypoint waypoint) {
        currentWorld(client).remove(waypoint);
        save();
    }

    /** Drops a "Death" waypoint where the player died, replacing the previous one. */
    public static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            wasDead = false;
            return;
        }
        boolean dead = client.player.isDead();
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
        FlintFixConfig config = FlintFixClient.CONFIG;
        if (config == null || !config.waypointsEnabled || context.matrixStack() == null || context.consumers() == null) return;
        MinecraftClient client = MinecraftClient.getInstance();
        List<Waypoint> waypoints = visibleHere(client);
        if (waypoints.isEmpty()) return;
        Vec3d camera = context.camera().getPos();
        MatrixStack matrices = context.matrixStack();

        if (config.waypointsBeams) {
            matrices.push();
            matrices.translate(-camera.x, -camera.y, -camera.z);
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            VertexConsumer quads = context.consumers().getBuffer(RenderLayer.getDebugQuads());
            for (Waypoint waypoint : waypoints) {
                Vec3d base = new Vec3d(waypoint.x + 0.5, waypoint.y, waypoint.z + 0.5);
                double distance = base.distanceTo(camera);
                if (distance > 600.0) continue;
                double width = 0.25 + distance * 0.004;
                Vec3d top = base.add(0.0, 220.0, 0.0);
                FlintFixWorldDraw.line(quads, matrix, camera, base, base.add(0.0, 6.0, 0.0), width, waypoint.color, 0.55f);
                FlintFixWorldDraw.line(quads, matrix, camera, base.add(0.0, 6.0, 0.0), top, width * 0.6, waypoint.color, 0.22f);
            }
            matrices.pop();
        }

        TextRenderer text = client.textRenderer;
        for (Waypoint waypoint : waypoints) {
            Vec3d target = new Vec3d(waypoint.x + 0.5, waypoint.y + 1.6, waypoint.z + 0.5);
            Vec3d toTarget = target.subtract(camera);
            double distance = toTarget.length();
            if (distance < 0.5) continue;
            // Labels far away are drawn closer along the same line so they never fall
            // outside the view distance, but keep the same size on screen.
            double shown = Math.min(distance, 48.0);
            Vec3d at = camera.add(toTarget.multiply(shown / distance));
            float scale = 0.025f * (float) Math.max(1.0, shown / 7.0);
            String label = waypoint.name + (config.waypointsDistance ? "  " + Math.round(distance) + "m" : "");

            matrices.push();
            matrices.translate(at.x - camera.x, at.y - camera.y, at.z - camera.z);
            matrices.multiply(context.camera().getRotation());
            matrices.scale(scale, -scale, scale);
            float x = -text.getWidth(label) / 2.0f;
            int background = 0x66000000;
            text.draw(label, x, 0.0f, 0xFFFFFFFF, false, matrices.peek().getPositionMatrix(), context.consumers(),
                TextRenderer.TextLayerType.SEE_THROUGH, background, LightmapTextureManager.MAX_LIGHT_COORDINATE);
            text.draw("◆", -text.getWidth("◆") / 2.0f, -10.0f, waypoint.color, false, matrices.peek().getPositionMatrix(),
                context.consumers(), TextRenderer.TextLayerType.SEE_THROUGH, 0, LightmapTextureManager.MAX_LIGHT_COORDINATE);
            matrices.pop();
        }
    }
}
