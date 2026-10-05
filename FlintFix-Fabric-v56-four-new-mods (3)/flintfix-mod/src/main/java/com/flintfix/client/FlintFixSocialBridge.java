package com.flintfix.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ServerInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Tiny file bridge between the Electron launcher and the Fabric client.
 * The launcher uses it for social presence and optional in-game message toasts.
 */
public final class FlintFixSocialBridge {
    private static final Gson GSON = new Gson();
    private static final Deque<SocialToast> TOASTS = new ArrayDeque<>();
    private static int ticks;
    private static long lastNotificationId;

    private FlintFixSocialBridge() {}

    public static void tick(MinecraftClient client) {
        ticks++;
        if (ticks % 40 == 0) writePresence(client);
        if (ticks % 10 == 0) readNotifications(client);
        pruneToasts();
    }

    private static Path statePath(MinecraftClient client) {
        return client.runDirectory.toPath().resolve("flintfix-social-state.json");
    }

    private static Path notificationsPath(MinecraftClient client) {
        return client.runDirectory.toPath().resolve("flintfix-social-notifications.json");
    }

    private static void writePresence(MinecraftClient client) {
        JsonObject root = new JsonObject();
        boolean inGame = client.player != null && client.world != null;
        root.addProperty("state", inGame ? "in_game" : "launcher");
        root.addProperty("updatedAt", System.currentTimeMillis());

        if (inGame) {
            String serverName = null;
            if (client.isInSingleplayer()) {
                serverName = "Singleplayer";
            } else {
                ServerInfo server = client.getCurrentServerEntry();
                if (server != null && server.address != null && !server.address.isBlank()) {
                    serverName = server.address;
                }
            }
            if (serverName != null) root.addProperty("server", serverName);
        }

        try {
            Files.writeString(statePath(client), GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // Presence is optional; never interrupt the game because the bridge file is unavailable.
        }
    }

    private static void readNotifications(MinecraftClient client) {
        Path path = notificationsPath(client);
        if (!Files.exists(path)) return;
        try {
            String text = Files.readString(path, StandardCharsets.UTF_8);
            JsonElement parsed = GSON.fromJson(text, JsonElement.class);
            if (parsed == null || !parsed.isJsonArray()) return;
            JsonArray array = parsed.getAsJsonArray();
            long now = System.currentTimeMillis();
            for (JsonElement element : array) {
                if (!element.isJsonObject()) continue;
                JsonObject object = element.getAsJsonObject();
                long id = object.has("id") ? object.get("id").getAsLong() : 0L;
                long createdAt = object.has("createdAt") ? object.get("createdAt").getAsLong() : now;
                if (id <= lastNotificationId) continue;
                lastNotificationId = Math.max(lastNotificationId, id);
                if (now - createdAt > 15000L) continue;
                String sender = object.has("sender") ? object.get("sender").getAsString() : "FlintFix Friend";
                String message = object.has("message") ? object.get("message").getAsString() : "";
                if (!message.isBlank()) addToast(sender, message);
            }
        } catch (Exception ignored) {
            // Invalid or partially-written bridge data is ignored and retried next tick.
        }
    }

    private static void addToast(String sender, String message) {
        String safeSender = clip(sender, 28);
        String safeMessage = clip(message.replace('\n', ' '), 72);
        TOASTS.addLast(new SocialToast(safeSender, safeMessage, System.currentTimeMillis() + 5200L));
        while (TOASTS.size() > 3) TOASTS.removeFirst();
    }

    private static void pruneToasts() {
        long now = System.currentTimeMillis();
        while (!TOASTS.isEmpty() && TOASTS.peekFirst().expiresAt < now) TOASTS.removeFirst();
    }

    public static void render(DrawContext context, MinecraftClient client) {
        if (TOASTS.isEmpty()) return;
        int width = client.getWindow().getScaledWidth();
        int y = 14;
        for (SocialToast toast : TOASTS) {
            int panelW = 220;
            int panelH = 43;
            int x = width - panelW - 14;
            context.fill(x, y, x + panelW, y + panelH, 0xC40A0D13);
            context.fill(x, y, x + 3, y + panelH, FlintFixClient.ACCENT);
            FlintFixFont.draw(context, toast.sender, x + 11, y + 8, 10, 0xFFF4F1F6, true);
            FlintFixFont.draw(context, toast.message, x + 11, y + 23, 8, 0xFFB5AFBA, false);
            y += panelH + 7;
        }
    }

    private static String clip(String value, int max) {
        if (value == null) return "";
        if (value.length() <= max) return value;
        return value.substring(0, Math.max(0, max - 1)) + "…";
    }

    private record SocialToast(String sender, String message, long expiresAt) {}
}
