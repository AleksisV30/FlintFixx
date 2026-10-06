package com.flintfix.client;

import com.flintfix.client.FlintFixOptionsScreen.Choice;
import com.flintfix.client.FlintFixOptionsScreen.ColorPick;
import com.flintfix.client.FlintFixOptionsScreen.Info;
import com.flintfix.client.FlintFixOptionsScreen.Option;
import com.flintfix.client.FlintFixOptionsScreen.Slider;
import com.flintfix.client.FlintFixOptionsScreen.Toggle;
import net.minecraft.client.gui.screen.Screen;

import java.util.List;
import java.util.Locale;

/** Settings pages for the modules that use {@link FlintFixOptionsScreen}. */
final class FlintFixModuleOptions {
    private FlintFixModuleOptions() {}

    private static FlintFixConfig c() {
        return FlintFixClient.CONFIG;
    }

    private static String percent(float value) {
        return Math.round(value * 100.0f) + "%";
    }

    private static String pixels(float value) {
        return Math.round(value) + " px";
    }

    /** Returns the settings screen for a module id, or null when the module has none here. */
    static Screen screenFor(String id, Screen parent) {
        return switch (id) {
            case "potions" -> new FlintFixOptionsScreen(parent, id, "Potion Effects",
                "Active effects with time left; blinks when one is about to end.", List.of(
                    new Toggle("Show potion effects", () -> c().potionsEnabled, v -> c().potionsEnabled = v),
                    new Toggle("Background", () -> c().potionsBackground, v -> c().potionsBackground = v),
                    opacity(() -> c().potionsBackgroundOpacity, v -> c().potionsBackgroundOpacity = v),
                    scale(() -> c().potionsScale, v -> c().potionsScale = v)),
                () -> {
                    c().potionsEnabled = true;
                    c().potionsBackground = true;
                    c().potionsBackgroundOpacity = 0.45f;
                    c().potionsScale = 1.0f;
                    c().potionsX = 0.98f;
                    c().potionsY = 0.08f;
                }, true);
            case "speed" -> new FlintFixOptionsScreen(parent, id, "Speed Meter",
                "How fast you are moving; counts height too while gliding with an elytra.", List.of(
                    new Toggle("Show speed", () -> c().speedEnabled, v -> c().speedEnabled = v),
                    new Choice("Unit", new String[] {"Blocks / sec", "km/h"}, () -> c().speedUnit, v -> c().speedUnit = v),
                    new Toggle("Background", () -> c().speedBackground, v -> c().speedBackground = v),
                    opacity(() -> c().speedBackgroundOpacity, v -> c().speedBackgroundOpacity = v),
                    scale(() -> c().speedScale, v -> c().speedScale = v)),
                () -> {
                    c().speedEnabled = true;
                    c().speedUnit = 0;
                    c().speedBackground = true;
                    c().speedBackgroundOpacity = 0.45f;
                    c().speedScale = 1.0f;
                    c().speedX = 0.018f;
                    c().speedY = 0.42f;
                }, true);
            case "compass" -> new FlintFixOptionsScreen(parent, id, "Compass Bar",
                "A heading strip at the top of the screen with a marker at your last death.", List.of(
                    new Toggle("Show compass bar", () -> c().compassEnabled, v -> c().compassEnabled = v),
                    new Toggle("Death marker", () -> c().compassDeathMarker, v -> c().compassDeathMarker = v),
                    new Toggle("Background", () -> c().compassBackground, v -> c().compassBackground = v),
                    opacity(() -> c().compassBackgroundOpacity, v -> c().compassBackgroundOpacity = v),
                    scale(() -> c().compassScale, v -> c().compassScale = v)),
                () -> {
                    c().compassEnabled = true;
                    c().compassDeathMarker = true;
                    c().compassBackground = true;
                    c().compassBackgroundOpacity = 0.45f;
                    c().compassScale = 1.0f;
                    c().compassX = 0.5f;
                    c().compassY = 0.0f;
                }, true);
            case "outline" -> new FlintFixOptionsScreen(parent, id, "Block Outline",
                "Replaces the thin black outline on the block you are looking at.", List.of(
                    new Toggle("Custom block outline", () -> c().blockOutlineEnabled, v -> c().blockOutlineEnabled = v),
                    new ColorPick("Color", () -> c().blockOutlineColor, v -> c().blockOutlineColor = v),
                    new Toggle("Rainbow", () -> c().blockOutlineRainbow, v -> c().blockOutlineRainbow = v),
                    new Slider("Thickness", 1.0f, 6.0f, 0.5f, () -> c().blockOutlineWidth, v -> c().blockOutlineWidth = v,
                        v -> String.format(Locale.ROOT, "%.1f", v)),
                    new Toggle("Fill the block", () -> c().blockOutlineFill, v -> c().blockOutlineFill = v),
                    new Slider("Fill opacity", 0.0f, 0.6f, 0.05f, () -> c().blockOutlineFillOpacity,
                        v -> c().blockOutlineFillOpacity = v, FlintFixModuleOptions::percent)),
                () -> {
                    c().blockOutlineColor = 0xFFFFFFFF;
                    c().blockOutlineWidth = 2.0f;
                    c().blockOutlineFill = false;
                    c().blockOutlineFillOpacity = 0.15f;
                    c().blockOutlineRainbow = false;
                }, false);
            case "crosshair" -> new FlintFixOptionsScreen(parent, id, "Crosshair",
                "Your own crosshair shape, size and color, with a hit marker.", List.of(
                    new Toggle("Custom crosshair", () -> c().crosshairEnabled, v -> c().crosshairEnabled = v),
                    new Choice("Style", FlintFixCrosshair.STYLES, () -> c().crosshairStyle, v -> c().crosshairStyle = v),
                    new ColorPick("Color", () -> c().crosshairColor, v -> c().crosshairColor = v),
                    new Slider("Size", 1.0f, 12.0f, 1.0f, () -> c().crosshairSize, v -> c().crosshairSize = v,
                        FlintFixModuleOptions::pixels),
                    new Slider("Gap", 0.0f, 8.0f, 1.0f, () -> c().crosshairGap, v -> c().crosshairGap = v,
                        FlintFixModuleOptions::pixels),
                    new Slider("Thickness", 1.0f, 3.0f, 1.0f, () -> c().crosshairThickness, v -> c().crosshairThickness = v,
                        FlintFixModuleOptions::pixels),
                    new Toggle("Dark outline", () -> c().crosshairOutline, v -> c().crosshairOutline = v),
                    new Toggle("Hit marker", () -> c().crosshairHitMarker, v -> c().crosshairHitMarker = v)),
                () -> {
                    c().crosshairStyle = 0;
                    c().crosshairColor = 0xFFFFFFFF;
                    c().crosshairSize = 5.0f;
                    c().crosshairGap = 2.0f;
                    c().crosshairThickness = 1.0f;
                    c().crosshairOutline = true;
                    c().crosshairHitMarker = true;
                }, false);
            case "lowoverlays" -> new FlintFixOptionsScreen(parent, id, "Low Overlays",
                "Lowers the fire overlay and shield, and shrinks the totem animation.", List.of(
                    new Toggle("Enable low overlays", () -> c().lowOverlaysEnabled, v -> c().lowOverlaysEnabled = v),
                    new Toggle("Low fire", () -> c().lowFire, v -> c().lowFire = v),
                    new Slider("Fire lowered by", 0.0f, 0.6f, 0.05f, () -> c().lowFireAmount, v -> c().lowFireAmount = v,
                        FlintFixModuleOptions::percent),
                    new Toggle("Low shield", () -> c().lowShield, v -> c().lowShield = v),
                    new Slider("Shield lowered by", 0.0f, 0.5f, 0.05f, () -> c().lowShieldAmount,
                        v -> c().lowShieldAmount = v, FlintFixModuleOptions::percent),
                    new Toggle("Small totem pop", () -> c().lowTotem, v -> c().lowTotem = v),
                    new Slider("Totem size", 0.2f, 1.0f, 0.05f, () -> c().totemSize, v -> c().totemSize = v,
                        FlintFixModuleOptions::percent)),
                () -> {
                    c().lowFire = true;
                    c().lowFireAmount = 0.35f;
                    c().lowShield = true;
                    c().lowShieldAmount = 0.25f;
                    c().lowTotem = true;
                    c().totemSize = 0.5f;
                }, false);
            case "damage" -> new FlintFixOptionsScreen(parent, id, "Damage Numbers",
                "Numbers float up from mobs and players when their health changes.", List.of(
                    new Toggle("Show damage numbers", () -> c().damageNumbersEnabled, v -> c().damageNumbersEnabled = v),
                    new Toggle("Show healing too", () -> c().damageNumbersHealing, v -> c().damageNumbersHealing = v),
                    new Info("Uses health the server already sends", "Some servers hide health; then nothing shows.")),
                () -> c().damageNumbersHealing = true, false);
            case "weather" -> new FlintFixOptionsScreen(parent, id, "No Weather",
                "Hides rain, snow and thunder for you only.", List.of(
                    new Toggle("Hide weather", () -> c().hideWeatherEnabled, v -> c().hideWeatherEnabled = v),
                    new Info("Only changes what you see", "The server and other players still have the real weather.")),
                () -> c().hideWeatherEnabled = false, false);
            case "motionblur" -> new FlintFixOptionsScreen(parent, id, "Motion Blur",
                "Blends frames together for smoother, cinematic camera movement.", List.of(
                    new Toggle("Enable motion blur", () -> c().motionBlurEnabled, v -> c().motionBlurEnabled = v),
                    new Slider("Strength", 0.1f, 0.9f, 0.05f, () -> c().motionBlurStrength, v -> c().motionBlurStrength = v,
                        FlintFixModuleOptions::percent),
                    new Info("Your hand and HUD stay sharp", "Higher strength leaves longer trails.")),
                () -> c().motionBlurStrength = 0.5f, false);
            case "teamglow" -> new FlintFixOptionsScreen(parent, id, "Team Glow",
                "Outlines your FlintFix friends and teammates so you can spot them.", List.of(
                    new Toggle("Enable team glow", () -> c().teammateGlowEnabled, v -> c().teammateGlowEnabled = v),
                    new Toggle("FlintFix friends", () -> c().teammateGlowFriends, v -> c().teammateGlowFriends = v),
                    new Toggle("Scoreboard teammates", () -> c().teammateGlowTeam, v -> c().teammateGlowTeam = v),
                    new ColorPick("Glow color", () -> c().teammateGlowColor, v -> c().teammateGlowColor = v),
                    new Info("Friends come from the launcher's Chat", "The glow is only shown on your screen.")),
                () -> {
                    c().teammateGlowFriends = true;
                    c().teammateGlowTeam = true;
                    c().teammateGlowColor = 0xFF5CFFB0;
                }, false);
            default -> null;
        };
    }

    private static Option opacity(java.util.function.DoubleSupplier get, java.util.function.Consumer<Float> set) {
        return new Slider("Background opacity", 0.0f, 1.0f, 0.05f, get, set, FlintFixModuleOptions::percent);
    }

    private static Option scale(java.util.function.DoubleSupplier get, java.util.function.Consumer<Float> set) {
        return new Slider("Scale", 0.25f, 2.0f, 0.05f, get, set, FlintFixModuleOptions::percent);
    }
}
