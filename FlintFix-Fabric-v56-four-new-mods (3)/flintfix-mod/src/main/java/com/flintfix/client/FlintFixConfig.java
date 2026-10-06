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
    public boolean skyEnabled = false;
    public String skyPreset = "AURORA";
    public boolean itemInspectEnabled = true;
    public boolean showHandEnabled = false;
    public boolean fullbrightEnabled = false;
    /** 0 follow world, 1 day, 2 sunset, 3 night. Only changes how the custom sky looks. */
    public int skyTimeMode = 0;
    public boolean skyStars = true;
    public boolean skyShootingStars = true;
    public boolean skyMilkyWay = true;
    public boolean skyAurora = true;
    public boolean skyNebula = true;
    public boolean skySunGlow = true;
    public boolean skySunRays = true;
    public boolean skyMoonPhases = true;
    public boolean skyHorizonGlow = true;
    public boolean skyFogTint = true;

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

    public boolean potionsEnabled = false;
    public boolean potionsBackground = true;
    public float potionsBackgroundOpacity = 0.45f;
    public float potionsScale = 1.0f;
    public float potionsX = 0.98f;
    public float potionsY = 0.08f;

    public boolean speedEnabled = false;
    public boolean speedBackground = true;
    public float speedBackgroundOpacity = 0.45f;
    public float speedScale = 1.0f;
    public float speedX = 0.018f;
    public float speedY = 0.42f;
    /** 0 blocks per second, 1 km/h. */
    public int speedUnit = 0;

    public boolean compassEnabled = false;
    public boolean compassBackground = true;
    public float compassBackgroundOpacity = 0.45f;
    public float compassScale = 1.0f;
    public float compassX = 0.5f;
    public float compassY = 0.0f;
    public boolean compassDeathMarker = true;

    public boolean blockOutlineEnabled = false;
    public int blockOutlineColor = 0xFFFFFFFF;
    public float blockOutlineWidth = 2.0f;
    public boolean blockOutlineFill = false;
    public float blockOutlineFillOpacity = 0.15f;
    public boolean blockOutlineRainbow = false;

    public boolean crosshairEnabled = false;
    /** 0 cross, 1 dot, 2 circle, 3 cross with dot, 4 x. */
    public int crosshairStyle = 0;
    public int crosshairColor = 0xFFFFFFFF;
    public float crosshairSize = 5.0f;
    public float crosshairGap = 2.0f;
    public float crosshairThickness = 1.0f;
    public boolean crosshairOutline = true;
    public boolean crosshairHitMarker = true;

    public boolean lowOverlaysEnabled = false;
    public boolean lowFire = true;
    public float lowFireAmount = 0.35f;
    public boolean lowShield = true;
    public float lowShieldAmount = 0.25f;
    public boolean lowTotem = true;
    public float totemSize = 0.5f;

    public boolean damageNumbersEnabled = false;
    public boolean damageNumbersHealing = true;

    public boolean hideWeatherEnabled = false;

    public boolean motionBlurEnabled = false;
    public float motionBlurStrength = 0.5f;

    public boolean teammateGlowEnabled = false;
    public int teammateGlowColor = 0xFF5CFFB0;
    public boolean teammateGlowFriends = true;
    public boolean teammateGlowTeam = true;

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
        potionsX = 0.98f;
        potionsY = 0.08f;
        speedX = 0.018f;
        speedY = 0.42f;
        compassX = 0.5f;
        compassY = 0.0f;
        sanitize();
    }

    void sanitize() {
        theme = FlintFixTheme.fromId(theme).id();
        skyPreset = FlintFixSky.Preset.fromId(skyPreset).name();
        skyTimeMode = Math.max(0, Math.min(3, skyTimeMode));
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

        potionsBackgroundOpacity = clamp(potionsBackgroundOpacity, 0.0f, 1.0f);
        potionsScale = clamp(potionsScale, 0.25f, 2.0f);
        potionsX = clamp(potionsX, 0.0f, 1.0f);
        potionsY = clamp(potionsY, 0.0f, 1.0f);
        speedBackgroundOpacity = clamp(speedBackgroundOpacity, 0.0f, 1.0f);
        speedScale = clamp(speedScale, 0.25f, 2.0f);
        speedX = clamp(speedX, 0.0f, 1.0f);
        speedY = clamp(speedY, 0.0f, 1.0f);
        speedUnit = Math.max(0, Math.min(1, speedUnit));
        compassBackgroundOpacity = clamp(compassBackgroundOpacity, 0.0f, 1.0f);
        compassScale = clamp(compassScale, 0.25f, 2.0f);
        compassX = clamp(compassX, 0.0f, 1.0f);
        compassY = clamp(compassY, 0.0f, 1.0f);

        blockOutlineColor |= 0xFF000000;
        blockOutlineWidth = clamp(blockOutlineWidth, 1.0f, 6.0f);
        blockOutlineFillOpacity = clamp(blockOutlineFillOpacity, 0.0f, 0.6f);
        crosshairStyle = Math.max(0, Math.min(4, crosshairStyle));
        crosshairColor |= 0xFF000000;
        crosshairSize = clamp(crosshairSize, 1.0f, 12.0f);
        crosshairGap = clamp(crosshairGap, 0.0f, 8.0f);
        crosshairThickness = clamp(crosshairThickness, 1.0f, 3.0f);
        lowFireAmount = clamp(lowFireAmount, 0.0f, 0.6f);
        lowShieldAmount = clamp(lowShieldAmount, 0.0f, 0.5f);
        totemSize = clamp(totemSize, 0.2f, 1.0f);
        motionBlurStrength = clamp(motionBlurStrength, 0.1f, 0.9f);
        teammateGlowColor |= 0xFF000000;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
