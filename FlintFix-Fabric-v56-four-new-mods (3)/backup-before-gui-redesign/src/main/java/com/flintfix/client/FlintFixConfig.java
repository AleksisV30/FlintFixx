package com.flintfix.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FlintFixConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("flintfix.json");

    public String theme = FlintFixTheme.GRAPHITE.id();
    public boolean freecamRiskAccepted = false;
    public float freecamSpeed = 0.36f;

    public boolean fpsEnabled = true;
    public boolean cpsEnabled = false;
    public boolean coordinatesEnabled = false;
    public boolean pingEnabled = false;
    public boolean keystrokesEnabled = false;
    public boolean armorEnabled = true;
    public boolean chunksEnabled = false;
    public boolean trajectoryEnabled = false;
    public boolean hitboxesEnabled = false;
    public boolean zoomEnabled = false;
    public boolean lookAroundEnabled = false;
    public boolean shulkerPreviewEnabled = false;

    public boolean fpsTextShadow = true;
    public boolean fpsBackground = true;
    public float fpsBackgroundOpacity = 0.45f;
    public float fpsScale = 1.0f;
    public float fpsX = 0.018f;
    public float fpsY = 0.025f;

    public boolean cpsBackground = true;
    public float cpsBackgroundOpacity = 0.45f;
    public float cpsScale = 1.0f;
    public float cpsX = 0.018f;
    public float cpsY = 0.10f;

    public boolean coordinatesBackground = true;
    public float coordinatesBackgroundOpacity = 0.45f;
    public float coordinatesScale = 1.0f;
    public float coordinatesX = 0.018f;
    public float coordinatesY = 0.17f;

    public boolean pingBackground = true;
    public float pingBackgroundOpacity = 0.45f;
    public float pingScale = 1.0f;
    public float pingX = 0.86f;
    public float pingY = 0.84f;

    public boolean keystrokesBackground = true;
    public boolean keystrokesShowCps = true;
    public float keystrokesBackgroundOpacity = 0.45f;
    public float keystrokesScale = 1.0f;
    public float keystrokesX = 0.86f;
    public float keystrokesY = 0.70f;

    public boolean armorBackground = true;
    public float armorBackgroundOpacity = 0.45f;
    public float armorScale = 1.0f;
    public float armorX = 0.018f;
    public float armorY = 0.25f;

    public static FlintFixConfig load() {
        if (!Files.exists(PATH)) {
            FlintFixConfig config = new FlintFixConfig();
            config.save();
            return config;
        }

        try (Reader reader = Files.newBufferedReader(PATH)) {
            FlintFixConfig config = GSON.fromJson(reader, FlintFixConfig.class);
            if (config == null) config = new FlintFixConfig();
            config.sanitize();
            return config;
        } catch (Exception ignored) {
            FlintFixConfig config = new FlintFixConfig();
            config.save();
            return config;
        }
    }

    public void save() {
        sanitize();
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException ignored) {
        }
        FlintFixProfileStore.syncActive(this);
    }

    public void resetFps() {
        fpsEnabled = true;
        fpsTextShadow = true;
        fpsBackground = true;
        fpsBackgroundOpacity = 0.45f;
        fpsScale = 1.0f;
        fpsX = 0.018f;
        fpsY = 0.025f;
        save();
    }

    public void resetCps() {
        cpsEnabled = true;
        cpsBackground = true;
        cpsBackgroundOpacity = 0.45f;
        cpsScale = 1.0f;
        cpsX = 0.018f;
        cpsY = 0.10f;
        save();
    }

    public void resetCoordinates() {
        coordinatesEnabled = true;
        coordinatesBackground = true;
        coordinatesBackgroundOpacity = 0.45f;
        coordinatesScale = 1.0f;
        coordinatesX = 0.018f;
        coordinatesY = 0.17f;
        save();
    }

    public void resetPing() {
        pingEnabled = true;
        pingBackground = true;
        pingBackgroundOpacity = 0.45f;
        pingScale = 1.0f;
        pingX = 0.86f;
        pingY = 0.84f;
        save();
    }

    public void resetKeystrokes() {
        keystrokesEnabled = true;
        keystrokesBackground = true;
        keystrokesShowCps = true;
        keystrokesBackgroundOpacity = 0.45f;
        keystrokesScale = 1.0f;
        keystrokesX = 0.86f;
        keystrokesY = 0.70f;
        save();
    }

    public void resetArmor() {
        armorEnabled = true;
        armorBackground = true;
        armorBackgroundOpacity = 0.45f;
        armorScale = 1.0f;
        armorX = 0.018f;
        armorY = 0.25f;
        save();
    }

    public void resetChunks() {
        chunksEnabled = false;
        save();
    }

    public void resetUtilityModules() {
        hitboxesEnabled = false;
        zoomEnabled = false;
        lookAroundEnabled = false;
        shulkerPreviewEnabled = false;
        save();
    }

    /** Reset only positions. The HUD editor decides when to persist them. */
    public void resetHudPositions() {
        fpsX = 0.018f;
        fpsY = 0.025f;
        cpsX = 0.018f;
        cpsY = 0.10f;
        coordinatesX = 0.018f;
        coordinatesY = 0.17f;
        pingX = 0.86f;
        pingY = 0.84f;
        keystrokesX = 0.86f;
        keystrokesY = 0.70f;
        armorX = 0.018f;
        armorY = 0.25f;
        sanitize();
    }

    void sanitize() {
        theme = FlintFixTheme.fromId(theme).id();
        freecamSpeed = clamp(freecamSpeed, 0.10f, 2.00f);
        fpsBackgroundOpacity = clamp(fpsBackgroundOpacity, 0.0f, 1.0f);
        fpsScale = clamp(fpsScale, 0.25f, 2.0f);
        fpsX = clamp(fpsX, 0.0f, 1.0f);
        fpsY = clamp(fpsY, 0.0f, 1.0f);

        cpsBackgroundOpacity = clamp(cpsBackgroundOpacity, 0.0f, 1.0f);
        cpsScale = clamp(cpsScale, 0.25f, 2.0f);
        cpsX = clamp(cpsX, 0.0f, 1.0f);
        cpsY = clamp(cpsY, 0.0f, 1.0f);

        coordinatesBackgroundOpacity = clamp(coordinatesBackgroundOpacity, 0.0f, 1.0f);
        coordinatesScale = clamp(coordinatesScale, 0.25f, 2.0f);
        coordinatesX = clamp(coordinatesX, 0.0f, 1.0f);
        coordinatesY = clamp(coordinatesY, 0.0f, 1.0f);

        pingBackgroundOpacity = clamp(pingBackgroundOpacity, 0.0f, 1.0f);
        pingScale = clamp(pingScale, 0.25f, 2.0f);
        pingX = clamp(pingX, 0.0f, 1.0f);
        pingY = clamp(pingY, 0.0f, 1.0f);

        keystrokesBackgroundOpacity = clamp(keystrokesBackgroundOpacity, 0.0f, 1.0f);
        keystrokesScale = clamp(keystrokesScale, 0.25f, 2.0f);
        keystrokesX = clamp(keystrokesX, 0.0f, 1.0f);
        keystrokesY = clamp(keystrokesY, 0.0f, 1.0f);

        armorBackgroundOpacity = clamp(armorBackgroundOpacity, 0.0f, 1.0f);
        armorScale = clamp(armorScale, 0.25f, 2.0f);
        armorX = clamp(armorX, 0.0f, 1.0f);
        armorY = clamp(armorY, 0.0f, 1.0f);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
