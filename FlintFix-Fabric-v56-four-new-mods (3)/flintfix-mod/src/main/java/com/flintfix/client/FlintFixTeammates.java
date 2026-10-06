package com.flintfix.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Team Glow: outlines your FlintFix friends (the launcher writes their
 * Minecraft UUIDs to flintfix-social-friends.json) and scoreboard teammates.
 */
public final class FlintFixTeammates {
    private static final Set<UUID> FRIENDS = ConcurrentHashMap.newKeySet();
    private static int ticks;
    private static long lastModified;

    private FlintFixTeammates() {}

    public static void tick(MinecraftClient client) {
        if (++ticks % 100 != 0) return;
        Path path = client.runDirectory.toPath().resolve("flintfix-social-friends.json");
        try {
            if (!Files.exists(path)) {
                FRIENDS.clear();
                return;
            }
            long modified = Files.getLastModifiedTime(path).toMillis();
            if (modified == lastModified) return;
            lastModified = modified;
            JsonElement root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
            JsonArray friends = root.isJsonObject() && root.getAsJsonObject().has("friends")
                ? root.getAsJsonObject().getAsJsonArray("friends") : new JsonArray();
            Set<UUID> next = ConcurrentHashMap.newKeySet();
            for (JsonElement element : friends) {
                if (!element.isJsonObject() || !element.getAsJsonObject().has("uuid")) continue;
                UUID uuid = parseUuid(element.getAsJsonObject().get("uuid").getAsString());
                if (uuid != null) next.add(uuid);
            }
            FRIENDS.clear();
            FRIENDS.addAll(next);
        } catch (Exception ignored) {
            // A half-written file is simply read again on the next pass.
        }
    }

    public static boolean shouldGlow(Entity entity) {
        FlintFixConfig config = FlintFixClient.CONFIG;
        if (config == null || !config.teammateGlowEnabled) return false;
        MinecraftClient client = MinecraftClient.getInstance();
        if (!(entity instanceof PlayerEntity player) || client.player == null || player == client.player) return false;
        if (config.teammateGlowFriends && FRIENDS.contains(player.getUuid())) return true;
        return config.teammateGlowTeam && client.player.getScoreboardTeam() != null && player.isTeammate(client.player);
    }

    public static int glowColor() {
        return FlintFixClient.CONFIG.teammateGlowColor & 0x00FFFFFF;
    }

    private static UUID parseUuid(String raw) {
        if (raw == null) return null;
        String clean = raw.replace("-", "").trim();
        if (clean.length() != 32) return null;
        try {
            return UUID.fromString(clean.substring(0, 8) + "-" + clean.substring(8, 12) + "-" + clean.substring(12, 16)
                + "-" + clean.substring(16, 20) + "-" + clean.substring(20));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
