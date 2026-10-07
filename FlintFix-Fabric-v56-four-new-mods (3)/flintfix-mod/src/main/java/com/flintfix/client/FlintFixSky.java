package com.flintfix.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import org.joml.Matrix4f;

import java.util.Locale;
import java.util.Random;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;

/**
 * Procedural replacement for the overworld sky: a gradient dome with
 * atmospheric haze, stars, a Milky Way band, shooting stars, sun rays,
 * phased moon, aurora ribbons and nebula clouds. Every effect can be
 * toggled from the Custom Sky screen.
 */
public final class FlintFixSky {
    private static final float RADIUS = 100.0f;
    private static final float[] LATITUDES = {-90, -40, -20, -8, -3, 0, 1, 2.5f, 4, 6, 9, 13, 18, 24, 31, 40, 50, 62, 76, 90};
    private static final int LONGITUDES = 72;
    private static final int STAR_COUNT = 1400;
    private static final int GALAXY_STAR_COUNT = 2200;
    private static final int STAR_STRIDE = 11;
    /** Per star: x, y, z, ux, uy, uz, vx, vy, vz, phase, tint. */
    private static final float[] STARS = buildStars(STAR_COUNT, 10842L, false);
    private static final float[] GALAXY_STARS = buildStars(GALAXY_STAR_COUNT, 77031L, true);
    private static final float[] GALAXY_NORMAL = normalize(0.32f, 0.55f, 0.77f);
    private static final float[][] GALAXY_BASIS = basis(GALAXY_NORMAL);
    private static final float SHOOTING_PERIOD = 5.5f;
    private static final float SHOOTING_LIFE = 0.85f;
    private static final float[][] NEBULAE = {
        // direction x, y, z, radius, color
        {0.55f, 0.62f, -0.56f, 46, 0xFFB04AD8},
        {-0.40f, 0.78f, 0.48f, 38, 0xFF3D6BFF},
        {-0.70f, 0.20f, -0.68f, 34, 0xFFFF5C9A},
        {0.20f, -0.30f, 0.93f, 40, 0xFF2EC5CE},
        {0.85f, -0.10f, 0.50f, 30, 0xFF8A5CFF}
    };

    public enum Preset {
        AURORA("Aurora", "Northern lights over a deep night sky",
            new Palette(0xFF3D7FC4, 0xFFA8D8E8, 0xFF5A7E96, 0xFFFFD9A0),
            new Palette(0xFF040A1C, 0xFF0D2E40, 0xFF02060E, 0xFF2E8C8C),
            1.0f, 0.0f, -1.0f, 1.0f, 0.25f, 0xFF5CFFB0, 0xFFA070FF, 0xFFFFE8B8, 0xFFD8E8FF),
        GOLDEN_HOUR("Golden Hour", "A warm sunset that never ends",
            new Palette(0xFF3A4C9A, 0xFFFF9A5C, 0xFF5A3048, 0xFFFFB070),
            new Palette(0xFF0C0E2A, 0xFF6A2E52, 0xFF14081A, 0xFFFF7A6A),
            0.6f, 0.15f, 0.8f, 0.3f, 0.2f, 0xFFFFC27A, 0xFFFF7AB0, 0xFFFFC27A, 0xFFFFE2D0),
        COSMIC("Cosmic", "Nebula clouds and a dense star field",
            new Palette(0xFF2A1E6A, 0xFF8A5AC0, 0xFF1A1030, 0xFFE0A0FF),
            new Palette(0xFF06031A, 0xFF2C1258, 0xFF04020C, 0xFF9A5CFF),
            1.0f, 0.5f, -1.0f, 0.4f, 1.0f, 0xFF7A9CFF, 0xFFE06AFF, 0xFFFFE0FF, 0xFFE8D8FF),
        CRYSTAL("Crystal Day", "Deep blue skies and a clean horizon",
            new Palette(0xFF2878E0, 0xFFC4E6FF, 0xFF7090B0, 0xFFFFF2D0),
            new Palette(0xFF030814, 0xFF14243E, 0xFF02040A, 0xFF4060A0),
            0.7f, 0.0f, -1.0f, 0.45f, 0.2f, 0xFF6AE8FF, 0xFF6A8CFF, 0xFFFFF6DC, 0xFFE0ECFF),
        PASTEL("Pastel Dream", "Soft pink and lavender all day",
            new Palette(0xFF86A8FF, 0xFFFFC6E2, 0xFFB090C0, 0xFFFFE0F0),
            new Palette(0xFF1A1640, 0xFF51306A, 0xFF100C20, 0xFFC080E0),
            0.8f, 0.1f, -1.0f, 0.5f, 0.45f, 0xFFFFA8E0, 0xFFA8C8FF, 0xFFFFF0F6, 0xFFF4E8FF),
        SYNTHWAVE("Synthwave", "Neon magenta horizon over deep purple",
            new Palette(0xFF2A1A6A, 0xFFFF6AC8, 0xFF3A1050, 0xFFFFB0E0),
            new Palette(0xFF0A0420, 0xFF8A1A8A, 0xFF100418, 0xFFFF4AD0),
            0.9f, 0.2f, -1.0f, 0.6f, 0.6f, 0xFFFF4AD0, 0xFF4AE0FF, 0xFFFFD06A, 0xFFB0E8FF),
        BLOOD_MOON("Blood Moon", "A crimson night under a red moon",
            new Palette(0xFF5A2A3A, 0xFFE07A5A, 0xFF3A1A20, 0xFFFF8A60),
            new Palette(0xFF120408, 0xFF4A0E16, 0xFF080204, 0xFFC03030),
            0.7f, 0.0f, -1.0f, 0.3f, 0.5f, 0xFFFF5A5A, 0xFFFFA05A, 0xFFFFC8A0, 0xFFFF6A5A),
        ETERNAL_NIGHT("Eternal Night", "Always midnight, full of stars",
            new Palette(0xFF02030C, 0xFF0B1836, 0xFF010208, 0xFF3050A0),
            new Palette(0xFF02030C, 0xFF0B1836, 0xFF010208, 0xFF3050A0),
            1.0f, 1.0f, 0.0f, 0.8f, 0.7f, 0xFF5CFFB0, 0xFFA070FF, 0xFFFFE8B8, 0xFFE4EEFF);

        private final String label;
        private final String description;
        final Palette day;
        final Palette night;
        final float stars;
        final float starsInDay;
        final float forcedDaylight;
        final float aurora;
        final float nebula;
        final int auroraA;
        final int auroraB;
        final int sun;
        final int moon;

        Preset(String label, String description, Palette day, Palette night, float stars, float starsInDay,
               float forcedDaylight, float aurora, float nebula, int auroraA, int auroraB, int sun, int moon) {
            this.label = label;
            this.description = description;
            this.day = day;
            this.night = night;
            this.stars = stars;
            this.starsInDay = starsInDay;
            this.forcedDaylight = forcedDaylight;
            this.aurora = aurora;
            this.nebula = nebula;
            this.auroraA = auroraA;
            this.auroraB = auroraB;
            this.sun = sun;
            this.moon = moon;
        }

        public String label() { return label; }
        public String description() { return description; }
        public Palette day() { return day; }
        public Palette night() { return night; }

        public static Preset fromId(String id) {
            if (id == null) return AURORA;
            try {
                return valueOf(id.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return AURORA;
            }
        }
    }

    public record Palette(int zenith, int horizon, int ground, int glow) {}

    private FlintFixSky() {}

    public static Preset preset() {
        return FlintFixClient.CONFIG == null ? Preset.AURORA : Preset.fromId(FlintFixClient.CONFIG.skyPreset);
    }

    /** True when the custom sky should replace the vanilla one for this frame. */
    public static boolean shouldRender(Camera camera, boolean thickFog) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel world = client.level;
        if (!FlintFixCompat.SKY_SUPPORTED) return false;
        if (FlintFixClient.CONFIG == null || !FlintFixClient.CONFIG.skyEnabled || world == null || thickFog) return false;
        if (!FlintFixCompat.hasOverworldSky(world)) return false;
        if (camera.getFluidInCamera() != FogType.NONE) return false;
        if (FlintFixCompat.cameraEntity(camera) instanceof LivingEntity living
            && (living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS))) {
            return false;
        }
        return true;
    }

    /** Fog tint matched to the dome's horizon (rgb + strength), or null to keep vanilla fog. */
    public static float[] fogColor(Camera camera, ClientLevel world, float tickDelta) {
        if (world == null || !FlintFixClient.CONFIG.skyFogTint || !shouldRender(camera, false)) return null;
        // Fade the effect out underground so caves keep their dark fog.
        double depth = world.getSeaLevel() - 8 - FlintFixCompat.cameraPos(camera).y;
        float strength = 0.8f * (float) Math.max(0.0, Math.min(1.0, 1.0 - depth / 24.0));
        if (strength <= 0.0f) return null;
        Preset preset = preset();
        float daylight = daylight(preset, world, tickDelta);
        int horizon = weather(lerpColor(preset.night.horizon, preset.day.horizon, daylight), world.getRainLevel(tickDelta));
        return new float[] {
            ((horizon >>> 16) & 0xFF) / 255.0f,
            ((horizon >>> 8) & 0xFF) / 255.0f,
            (horizon & 0xFF) / 255.0f,
            strength
        };
    }

    public static void render(Matrix4f modelView, float tickDelta) {
        ClientLevel world = Minecraft.getInstance().level;
        if (world == null) return;
        FlintFixConfig config = FlintFixClient.CONFIG;
        Preset preset = preset();
        float skyAngle = skyAngle(world, tickDelta);
        float daylight = daylight(preset, world, tickDelta);
        float night = 1.0f - daylight;
        float rain = world.getRainLevel(tickDelta);
        float clear = 1.0f - rain;
        float time = (System.currentTimeMillis() % 3_600_000L) / 1000.0f;

        PoseStack matrices = new PoseStack();
        matrices.last().pose().mul(modelView);

        FlintFixCompat.beginSky();

        // Sun direction in world space; matches the celestial rotation below.
        float theta = skyAngle * (float) (Math.PI * 2.0);
        float[] sunDir = {-(float) Math.sin(theta), (float) Math.cos(theta), 0.0f};
        Matrix4f world0 = matrices.last().pose();
        draw(dome(world0, preset, daylight, rain, sunDir, config.skyHorizonGlow));

        // Everything below glows, so add it on top of the dome.
        additive();
        if (config.skyAurora && night * clear * preset.aurora > 0.02f) {
            draw(aurora(world0, preset, night * clear * preset.aurora, time));
        }

        matrices.pushPose();
        matrices.mulPose(Axis.YP.rotationDegrees(-90.0f));
        matrices.mulPose(Axis.XP.rotationDegrees(skyAngle * 360.0f));
        Matrix4f celestial = matrices.last().pose();
        float starAlpha = preset.stars * Math.max(night, preset.starsInDay) * clear;
        if (config.skyMilkyWay && starAlpha > 0.02f) {
            draw(milkyWay(celestial, starAlpha, time, theta));
        }
        if (config.skyNebula && preset.nebula > 0.0f) {
            draw(nebulae(celestial, (night + 0.4f * daylight) * clear * preset.nebula));
        }
        if (config.skyStars && starAlpha > 0.02f) {
            draw(stars(celestial, STARS, Math.round(STAR_COUNT * preset.stars), starAlpha, time, 1.0f, theta));
        }

        // The sun fades as it sinks below the horizon so it never shows through the ground.
        float sunUp = smooth((sunDir[1] + 0.07f) / 0.09f);
        if (sunUp > 0.0f && clear > 0.0f) {
            float sunAlpha = clear * sunUp;
            draw(sunGlow(celestial, preset, sunAlpha, sunDir[1], time, config.skySunGlow, config.skySunRays));
            // Solid core with normal blending, so it stays visible against bright skies.
            FlintFixCompat.skyBlend(false);
            draw(sunCore(celestial, preset, sunAlpha, sunDir[1]));
            additive();
        }
        float moonUp = smooth((-sunDir[1] + 0.07f) / 0.09f);
        if (moonUp > 0.0f && clear > 0.0f) {
            draw(moon(celestial, preset, clear * moonUp, FlintFixCompat.moonPhase(world), config.skySunGlow, config.skyMoonPhases));
        }
        matrices.popPose();

        if (config.skyShootingStars && night * clear > 0.3f) {
            BufferBuilder meteors = shootingStars(world0, night * clear, time);
            if (meteors != null) draw(meteors);
        }

        FlintFixCompat.endSky();
    }

    private static void additive() {
        FlintFixCompat.skyBlend(true);
    }

    public static final String[] TIME_MODES = {"Follow world", "Day", "Sunset", "Night"};

    /** Sky angle used for drawing; the Sky time option can pin it without touching the world clock. */
    private static float skyAngle(ClientLevel world, float tickDelta) {
        return switch (FlintFixClient.CONFIG.skyTimeMode) {
            case 1 -> 0.0f;
            case 2 -> 0.22f;
            case 3 -> 0.5f;
            default -> FlintFixCompat.timeOfDay(world, tickDelta);
        };
    }

    private static float daylight(Preset preset, ClientLevel world, float tickDelta) {
        if (FlintFixClient.CONFIG.skyTimeMode == 0 && preset.forcedDaylight >= 0.0f) return preset.forcedDaylight;
        float angle = skyAngle(world, tickDelta);
        float light = (float) Math.cos(angle * Math.PI * 2.0) * 2.0f + 0.5f;
        return Math.max(0.0f, Math.min(1.0f, light));
    }

    private static BufferBuilder begin() {
        return FlintFixCompat.beginSkyBuffer();
    }

    private static void draw(BufferBuilder buffer) {
        FlintFixCompat.drawBuffer(buffer);
    }

    // ------------------------------------------------------------------
    // Dome
    // ------------------------------------------------------------------

    /** Colors shared by every vertex of the dome for one frame. */
    private record DomeLight(int zenith, int horizon, int ground, int glow, float twilight, float haze,
                             float day, float[] sun) {}

    private static BufferBuilder dome(Matrix4f m, Preset preset, float daylight, float rain, float[] sunDir,
                                      boolean horizonGlow) {
        Palette day = preset.day;
        Palette night = preset.night;
        // Twilight peaks while the sun sits on the horizon and fades out by ~25 degrees.
        float twilight = horizonGlow ? smooth(1.0f - Math.abs(sunDir[1]) * 2.4f) * (1.0f - rain * 0.8f) : 0.0f;
        DomeLight light = new DomeLight(
            weather(lerpColor(night.zenith, day.zenith, daylight), rain),
            weather(lerpColor(night.horizon, day.horizon, daylight), rain),
            weather(lerpColor(night.ground, day.ground, daylight), rain),
            lerpColor(night.glow, day.glow, daylight),
            twilight,
            horizonGlow ? 1.0f - rain * 0.5f : 0.0f,
            daylight,
            sunDir);

        BufferBuilder b = begin();
        for (int ring = 0; ring < LATITUDES.length - 1; ring++) {
            float lat0 = LATITUDES[ring];
            float lat1 = LATITUDES[ring + 1];
            for (int seg = 0; seg < LONGITUDES; seg++) {
                float lon0 = seg * 360.0f / LONGITUDES;
                float lon1 = (seg + 1) * 360.0f / LONGITUDES;
                float[] a = domePoint(lat0, lon0);
                float[] c = domePoint(lat1, lon0);
                float[] d = domePoint(lat1, lon1);
                float[] e = domePoint(lat0, lon1);
                int ca = domeColor(a, lat0, light);
                int cc = domeColor(c, lat1, light);
                int cd = domeColor(d, lat1, light);
                int ce = domeColor(e, lat0, light);
                vertex(b, m, a, ca); vertex(b, m, c, cc); vertex(b, m, d, cd);
                vertex(b, m, a, ca); vertex(b, m, d, cd); vertex(b, m, e, ce);
            }
        }
        return b;
    }

    private static float[] domePoint(float latDeg, float lonDeg) {
        double lat = Math.toRadians(latDeg);
        double lon = Math.toRadians(lonDeg);
        return new float[] {
            (float) (Math.cos(lat) * Math.cos(lon)) * RADIUS,
            (float) Math.sin(lat) * RADIUS,
            (float) (Math.cos(lat) * Math.sin(lon)) * RADIUS
        };
    }

    /**
     * Gradient from horizon to zenith, plus a scattering halo around the sun,
     * a warm twilight band on the sun's side, and the pink "belt" with the
     * darker Earth shadow on the opposite side during sunrise and sunset.
     */
    private static int domeColor(float[] p, float lat, DomeLight light) {
        int color;
        if (lat < 0.0f) {
            color = lerpColor(light.horizon, light.ground, smooth(Math.min(1.0f, -lat / 20.0f)));
        } else {
            color = lerpColor(light.horizon, light.zenith, (float) Math.pow(lat / 90.0f, 0.45));
            if (light.haze > 0.0f && lat < 8.0f) {
                color = lerpColor(color, lighten(light.horizon, 0.25f), light.haze * 0.35f * (1.0f - lat / 8.0f));
            }
        }
        float[] sun = light.sun;
        float mu = (p[0] * sun[0] + p[1] * sun[1] + p[2] * sun[2]) / RADIUS;
        float toward = Math.max(0.0f, mu);
        float low = 1.0f - Math.min(1.0f, Math.abs(lat) / 38.0f);
        if (light.twilight > 0.0f) {
            // Warm glow hugging the horizon under the sun.
            float band = light.twilight * (toward * toward) * low * 0.9f;
            if (band > 0.0f) color = lerpColor(color, light.glow, Math.min(0.9f, band));
            // Opposite the sun: pink belt above a cool, darker shadow band.
            float away = Math.max(0.0f, -mu);
            if (away > 0.0f && lat >= 0.0f) {
                float belt = light.twilight * away * smooth(1.0f - Math.abs(lat - 9.0f) / 9.0f) * 0.32f;
                color = lerpColor(color, 0xFFD898B8, belt);
                float shadow = light.twilight * away * smooth(1.0f - lat / 4.0f) * 0.30f;
                color = lerpColor(color, 0xFF1C2440, shadow);
            }
        }
        // Daytime forward scattering: a soft brightening around the sun that never
        // reaches white, so the sun disc keeps its contrast.
        float halo = (float) Math.pow(toward, 6.0) * 0.28f * light.day * (0.5f + 0.5f * low);
        if (halo > 0.0f) color = lerpColor(color, lighten(light.glow, 0.35f), halo);
        return capBrightness(color, 0.86f);
    }

    /** Keeps sky colors below a luminance ceiling so the sun always reads brighter. */
    private static int capBrightness(int color, float ceiling) {
        int r = (color >>> 16) & 0xFF, g = (color >>> 8) & 0xFF, b = color & 0xFF;
        float lum = (r * 0.2126f + g * 0.7152f + b * 0.0722f) / 255.0f;
        if (lum <= ceiling) return color;
        float k = ceiling / lum;
        return (color & 0xFF000000) | (Math.round(r * k) << 16) | (Math.round(g * k) << 8) | Math.round(b * k);
    }

    // ------------------------------------------------------------------
    // Effects
    // ------------------------------------------------------------------

    /**
     * Aurora curtains: folded ribbons with fine vertical rays, a bright lower
     * edge in the preset's first color fading upward into the second.
     */
    private static BufferBuilder aurora(Matrix4f m, Preset preset, float strength, float time) {
        BufferBuilder b = begin();
        int segments = 140;
        for (int ribbon = 0; ribbon < 3; ribbon++) {
            float offset = ribbon * 1.7f;
            float[][] bottom = new float[segments + 1][];
            float[][] edge = new float[segments + 1][];
            float[][] top = new float[segments + 1][];
            int[] low = new int[segments + 1];
            int[] high = new int[segments + 1];
            for (int k = 0; k <= segments; k++) {
                float u = k / (float) segments;
                // Slow folds travel along the ribbon.
                float lon = 190.0f + u * 160.0f + 7.0f * (float) Math.sin(time * 0.17f + u * 8.0f + offset)
                    + 2.5f * (float) Math.sin(time * 0.41f + u * 23.0f + offset * 3.0f);
                float rad = (float) Math.toRadians(lon);
                float base = 12.0f + ribbon * 5.5f + 4.0f * (float) Math.sin(rad * 3.0f + time * 0.21f + offset);
                float height = 20.0f + 10.0f * (float) Math.sin(rad * 2.0f - time * 0.15f + offset * 2.0f);
                bottom[k] = domePoint(base - 1.0f, lon);
                edge[k] = domePoint(base + 2.2f, lon);
                top[k] = domePoint(base + height, lon);
                float fade = (float) Math.sin(Math.PI * u);
                fade *= fade;
                // Vertical rays: fine, slowly drifting striations.
                float rays = 0.55f + 0.45f * (float) Math.sin(u * 190.0f + time * 0.9f + offset * 5.0f)
                    * (float) Math.sin(u * 47.0f - time * 0.35f + offset);
                float pulse = 0.75f + 0.25f * (float) Math.sin(time * 0.6f + u * 6.0f + offset * 2.0f);
                float alpha = strength * fade * pulse * (0.55f + 0.45f * rays) / (1.0f + ribbon * 0.35f);
                low[k] = withAlpha(lighten(preset.auroraA, 0.25f), 0.55f * alpha);
                high[k] = withAlpha(lerpColor(preset.auroraA, preset.auroraB, 0.75f), 0.30f * alpha);
            }
            for (int k = 0; k < segments; k++) {
                // Sharp lower edge, long soft fade upward.
                quad(b, m, bottom[k], bottom[k + 1], edge[k + 1], edge[k],
                    low[k] & 0x00FFFFFF, low[k + 1] & 0x00FFFFFF, low[k + 1], low[k]);
                quad(b, m, edge[k], edge[k + 1], top[k + 1], top[k],
                    low[k], low[k + 1], high[k + 1] & 0x00FFFFFF, high[k] & 0x00FFFFFF);
            }
        }
        return b;
    }

    /** A soft glowing band with a dense cluster of faint stars along one great circle. */
    private static BufferBuilder milkyWay(Matrix4f m, float alpha, float time, float theta) {
        BufferBuilder b = begin();
        float[] u = GALAXY_BASIS[0];
        float[] v = GALAXY_BASIS[1];
        float[] n = GALAXY_NORMAL;
        int segments = 72;
        for (int s = 0; s < segments; s++) {
            double a0 = s * Math.PI * 2.0 / segments;
            double a1 = (s + 1) * Math.PI * 2.0 / segments;
            float core0 = 0.55f + 0.45f * (float) Math.cos(a0 - 1.0);
            float core1 = 0.55f + 0.45f * (float) Math.cos(a1 - 1.0);
            float[] c0 = galaxyPoint(u, v, n, a0, 0.0f);
            float[] c1 = galaxyPoint(u, v, n, a1, 0.0f);
            for (int side = -1; side <= 1; side += 2) {
                float[] e0 = galaxyPoint(u, v, n, a0, side * 0.16f);
                float[] e1 = galaxyPoint(u, v, n, a1, side * 0.16f);
                float fade0 = smooth((worldHeight(c0[1] / RADIUS, c0[2] / RADIUS, theta) + 0.05f) / 0.35f);
                float fade1 = smooth((worldHeight(c1[1] / RADIUS, c1[2] / RADIUS, theta) + 0.05f) / 0.35f);
                int center0 = withAlpha(lerpColor(0xFFB8C6FF, 0xFFE8D2B8, core0 * 0.35f), 0.15f * alpha * core0 * fade0);
                int center1 = withAlpha(lerpColor(0xFFB8C6FF, 0xFFE8D2B8, core1 * 0.35f), 0.15f * alpha * core1 * fade1);
                quad(b, m, c0, c1, e1, e0, center0, center1, 0x00B8C6FF, 0x00B8C6FF);
            }
        }
        appendStars(b, m, GALAXY_STARS, GALAXY_STAR_COUNT, alpha * 0.55f, time, 0.8f, theta);
        return b;
    }

    private static float[] galaxyPoint(float[] u, float[] v, float[] n, double angle, float lift) {
        float c = (float) Math.cos(angle);
        float s = (float) Math.sin(angle);
        float[] dir = normalize(u[0] * c + v[0] * s + n[0] * lift, u[1] * c + v[1] * s + n[1] * lift,
            u[2] * c + v[2] * s + n[2] * lift);
        return new float[] {dir[0] * RADIUS, dir[1] * RADIUS, dir[2] * RADIUS};
    }

    private static BufferBuilder nebulae(Matrix4f m, float strength) {
        BufferBuilder b = begin();
        for (float[] cloud : NEBULAE) {
            float[] dir = normalize(cloud[0], cloud[1], cloud[2]);
            float[][] basis = basis(dir);
            float[] center = {dir[0] * RADIUS, dir[1] * RADIUS, dir[2] * RADIUS};
            int color = (int) cloud[4];
            disc(b, m, center, basis, 0.0f, cloud[3] * 0.45f, withAlpha(color, 0.20f * strength), withAlpha(color, 0.12f * strength));
            disc(b, m, center, basis, cloud[3] * 0.45f, cloud[3], withAlpha(color, 0.12f * strength), color & 0x00FFFFFF);
        }
        return b;
    }

    private static BufferBuilder stars(Matrix4f m, float[] data, int count, float alpha, float time, float sizeScale,
                                       float theta) {
        BufferBuilder b = begin();
        appendStars(b, m, data, count, alpha, time, sizeScale, theta);
        return b;
    }

    /**
     * World-space height (-1..1) of a celestial direction. The celestial frame is
     * rotated by theta around X, then -90 degrees around Y, which keeps height.
     */
    private static float worldHeight(float y, float z, float theta) {
        return y * (float) Math.cos(theta) - z * (float) Math.sin(theta);
    }

    private static void appendStars(BufferBuilder b, Matrix4f m, float[] data, int count, float alpha, float time,
                                    float sizeScale, float theta) {
        for (int i = 0; i < count; i++) {
            int o = i * STAR_STRIDE;
            // Atmospheric extinction: stars dim and stop twinkling-out near the horizon.
            float height = worldHeight(data[o + 1], data[o + 2], theta);
            float extinction = smooth((height + 0.02f) / 0.32f);
            if (extinction <= 0.0f) continue;
            float scintillation = 0.35f + 0.25f * (1.0f - extinction);
            float twinkle = (1.0f - scintillation) + scintillation * (float) Math.sin(time * (1.3f + (i % 7) * 0.35f) + data[o + 9]);
            float tint = data[o + 10];
            int base = tint < 0.25f ? 0xFFBFD4FF : (tint < 0.8f ? 0xFFFFFFFF : 0xFFFFE2B8);
            int color = withAlpha(base, alpha * twinkle * extinction);
            float x = data[o] * RADIUS, y = data[o + 1] * RADIUS, z = data[o + 2] * RADIUS;
            float ux = data[o + 3] * sizeScale, uy = data[o + 4] * sizeScale, uz = data[o + 5] * sizeScale;
            float vx = data[o + 6] * sizeScale, vy = data[o + 7] * sizeScale, vz = data[o + 8] * sizeScale;
            float[] p0 = {x - ux - vx, y - uy - vy, z - uz - vz};
            float[] p1 = {x + ux - vx, y + uy - vy, z + uz - vz};
            float[] p2 = {x + ux + vx, y + uy + vy, z + uz + vz};
            float[] p3 = {x - ux + vx, y - uy + vy, z - uz + vz};
            quad(b, m, p0, p1, p2, p3, color, color, color, color);
        }
    }

    /** Sun color: the preset's tint, warming toward orange-red near the horizon. */
    private static int sunColor(Preset preset, float elevation) {
        float low = smooth(1.0f - elevation / 0.32f);
        return lerpColor(preset.sun, 0xFFFF7A38, low * 0.75f);
    }

    /** Soft bloom, corona and slow light shafts around the sun (additive). */
    private static BufferBuilder sunGlow(Matrix4f m, Preset preset, float alpha, float elevation, float time,
                                         boolean glow, boolean rays) {
        BufferBuilder b = begin();
        float[][] flat = {{1, 0, 0}, {0, 0, 1}};
        float[] center = {0, RADIUS, 0};
        int sun = sunColor(preset, elevation);
        float low = smooth(1.0f - elevation / 0.32f);
        if (rays) {
            int count = 18;
            for (int i = 0; i < count; i++) {
                double angle = i * Math.PI * 2.0 / count + time * 0.012 + Math.sin(i * 12.9898) * 0.12;
                float pulse = 0.5f + 0.5f * (float) Math.sin(i * 1.7 + time * 0.23);
                float length = 34.0f + 26.0f * pulse + 14.0f * low;
                float spread = 0.035f + 0.02f * (float) Math.sin(i * 3.1);
                float[] tip0 = offset(center, flat[0], flat[1], (float) Math.cos(angle - spread) * length, (float) Math.sin(angle - spread) * length);
                float[] tip1 = offset(center, flat[0], flat[1], (float) Math.cos(angle + spread) * length, (float) Math.sin(angle + spread) * length);
                vertex(b, m, center, withAlpha(sun, (0.07f + 0.05f * pulse) * alpha));
                vertex(b, m, tip0, sun & 0x00FFFFFF);
                vertex(b, m, tip1, sun & 0x00FFFFFF);
            }
        }
        if (glow) {
            // Several rings approximate an exponential falloff instead of a hard cone.
            float bloom = 1.0f + 0.6f * low;
            softDisc(b, m, center, flat, 46.0f * bloom, sun, 0.22f * alpha);
            softDisc(b, m, center, flat, 20.0f * bloom, sun, 0.30f * alpha);
            softDisc(b, m, center, flat, 10.5f, lighten(sun, 0.4f), 0.45f * alpha);
        }
        return b;
    }

    /** The disc itself, drawn with normal blending: bright center, warmer limb, crisp edge. */
    private static BufferBuilder sunCore(Matrix4f m, Preset preset, float alpha, float elevation) {
        BufferBuilder b = begin();
        float[][] flat = {{1, 0, 0}, {0, 0, 1}};
        float[] center = {0, RADIUS, 0};
        int sun = sunColor(preset, elevation);
        int hot = lerpColor(lighten(sun, 0.82f), 0xFFFFFFFF, 0.3f);
        disc(b, m, center, flat, 0.0f, 4.2f, withAlpha(hot, alpha), withAlpha(lighten(sun, 0.6f), alpha));
        disc(b, m, center, flat, 4.2f, 5.6f, withAlpha(lighten(sun, 0.6f), alpha), withAlpha(lighten(sun, 0.3f), alpha * 0.9f));
        disc(b, m, center, flat, 5.6f, 6.6f, withAlpha(lighten(sun, 0.3f), alpha * 0.9f), sun & 0x00FFFFFF);
        return b;
    }

    /** Disc that fades from the center outward over a few rings, like a blurred spot. */
    private static void softDisc(BufferBuilder b, Matrix4f m, float[] center, float[][] basis, float radius,
                                 int color, float alpha) {
        float[] stops = {0.0f, 0.18f, 0.42f, 0.7f, 1.0f};
        float[] weights = {1.0f, 0.62f, 0.3f, 0.1f, 0.0f};
        for (int i = 0; i < stops.length - 1; i++) {
            disc(b, m, center, basis, stops[i] * radius, stops[i + 1] * radius,
                withAlpha(color, alpha * weights[i]), withAlpha(color, alpha * weights[i + 1]));
        }
    }

    /** Moon with a lit crescent/gibbous shape that follows the world's moon phase. */
    private static BufferBuilder moon(Matrix4f m, Preset preset, float clear, int phase, boolean glow, boolean phases) {
        BufferBuilder b = begin();
        float[][] flat = {{1, 0, 0}, {0, 0, 1}};
        float[] center = {0, -RADIUS, 0};
        int moon = preset.moon;
        float lit = phases ? (1.0f + (float) Math.cos(phase * Math.PI / 4.0)) / 2.0f : 1.0f;
        if (glow) {
            softDisc(b, m, center, flat, 26.0f, moon, 0.16f * clear * (0.3f + 0.7f * lit));
            softDisc(b, m, center, flat, 10.0f, moon, 0.20f * clear * (0.3f + 0.7f * lit));
        }
        float radius = 5.0f;
        // Faint full disc so the dark part of the moon still reads against the stars.
        disc(b, m, center, flat, 0.0f, radius, withAlpha(moon, 0.07f * clear), withAlpha(moon, 0.07f * clear));
        float k = 1.0f - 2.0f * lit;
        float side = phase <= 4 ? -1.0f : 1.0f;
        int color = withAlpha(moon, 0.95f * clear);
        int rows = 20;
        for (int i = 0; i < rows; i++) {
            double a0 = -Math.PI / 2.0 + i * Math.PI / rows;
            double a1 = -Math.PI / 2.0 + (i + 1) * Math.PI / rows;
            float y0 = (float) Math.sin(a0) * radius, w0 = (float) Math.cos(a0) * radius;
            float y1 = (float) Math.sin(a1) * radius, w1 = (float) Math.cos(a1) * radius;
            float[] p0 = offset(center, flat[0], flat[1], side * k * w0, y0);
            float[] p1 = offset(center, flat[0], flat[1], side * w0, y0);
            float[] p2 = offset(center, flat[0], flat[1], side * w1, y1);
            float[] p3 = offset(center, flat[0], flat[1], side * k * w1, y1);
            quad(b, m, p0, p1, p2, p3, color, color, color, color);
        }
        return b;
    }

    /** Occasional bright streaks across the night sky, timed deterministically by wall clock. */
    private static BufferBuilder shootingStars(Matrix4f m, float alpha, float time) {
        int slot = (int) Math.floor(time / SHOOTING_PERIOD);
        Random random = new Random(slot * 7919L + 13L);
        if (random.nextFloat() > 0.8f) return null;
        float start = random.nextFloat() * (SHOOTING_PERIOD - SHOOTING_LIFE);
        float progress = (time - slot * SHOOTING_PERIOD - start) / SHOOTING_LIFE;
        if (progress < 0.0f || progress > 1.0f) return null;

        float lon0 = random.nextFloat() * 360.0f;
        float lat0 = 35.0f + random.nextFloat() * 35.0f;
        float dLon = (18.0f + random.nextFloat() * 14.0f) * (random.nextBoolean() ? 1 : -1);
        float dLat = -(12.0f + random.nextFloat() * 10.0f);
        float head = progress;
        float tail = Math.max(0.0f, progress - 0.35f);
        float[] headPos = domePoint(lat0 + dLat * head, lon0 + dLon * head);
        float[] tailPos = domePoint(lat0 + dLat * tail, lon0 + dLon * tail);
        float[] side = normalize(
            headPos[1] * tailPos[2] - headPos[2] * tailPos[1],
            headPos[2] * tailPos[0] - headPos[0] * tailPos[2],
            headPos[0] * tailPos[1] - headPos[1] * tailPos[0]);
        float width = 0.45f;
        float fade = (float) Math.sin(Math.PI * progress);
        int headColor = withAlpha(0xFFFFFFFF, alpha * fade);
        BufferBuilder b = begin();
        float[] h0 = {headPos[0] + side[0] * width, headPos[1] + side[1] * width, headPos[2] + side[2] * width};
        float[] h1 = {headPos[0] - side[0] * width, headPos[1] - side[1] * width, headPos[2] - side[2] * width};
        vertex(b, m, tailPos, 0x00FFFFFF);
        vertex(b, m, h0, headColor);
        vertex(b, m, h1, headColor);
        float[][] headBasis = basis(normalize(headPos[0], headPos[1], headPos[2]));
        disc(b, m, headPos, headBasis, 0.0f, 1.4f, headColor, 0x00FFFFFF);
        return b;
    }

    // ------------------------------------------------------------------
    // Geometry helpers
    // ------------------------------------------------------------------

    /** Annulus between radii r0 and r1 around center, in the plane spanned by basis. */
    private static void disc(BufferBuilder b, Matrix4f m, float[] center, float[][] basis,
                             float r0, float r1, int innerColor, int outerColor) {
        int segments = 32;
        float[] u = basis[0];
        float[] v = basis[1];
        for (int s = 0; s < segments; s++) {
            double a0 = s * Math.PI * 2.0 / segments;
            double a1 = (s + 1) * Math.PI * 2.0 / segments;
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            float[] i0 = offset(center, u, v, c0 * r0, s0 * r0);
            float[] i1 = offset(center, u, v, c1 * r0, s1 * r0);
            float[] o0 = offset(center, u, v, c0 * r1, s0 * r1);
            float[] o1 = offset(center, u, v, c1 * r1, s1 * r1);
            quad(b, m, i0, o0, o1, i1, innerColor, outerColor, outerColor, innerColor);
        }
    }

    private static float[] offset(float[] center, float[] u, float[] v, float a, float c) {
        return new float[] {
            center[0] + u[0] * a + v[0] * c,
            center[1] + u[1] * a + v[1] * c,
            center[2] + u[2] * a + v[2] * c
        };
    }

    private static void quad(BufferBuilder b, Matrix4f m, float[] p0, float[] p1, float[] p2, float[] p3,
                             int c0, int c1, int c2, int c3) {
        vertex(b, m, p0, c0); vertex(b, m, p1, c1); vertex(b, m, p2, c2);
        vertex(b, m, p0, c0); vertex(b, m, p2, c2); vertex(b, m, p3, c3);
    }

    private static void vertex(BufferBuilder b, Matrix4f m, float[] p, int argb) {
        FlintFixCompat.colorVertex(b, m, p[0], p[1], p[2], argb);
    }

    /** Random stars; galaxy stars cluster tightly around the Milky Way's great circle. */
    private static float[] buildStars(int count, long seed, boolean galaxy) {
        Random random = new Random(seed);
        float[] data = new float[count * STAR_STRIDE];
        float[] n = galaxy ? normalize(0.32f, 0.55f, 0.77f) : null;
        float[][] nb = galaxy ? basis(n) : null;
        for (int i = 0; i < count; i++) {
            float[] dir;
            float size;
            if (galaxy) {
                double angle = random.nextDouble() * Math.PI * 2.0;
                float lift = (float) random.nextGaussian() * 0.06f;
                float c = (float) Math.cos(angle), s = (float) Math.sin(angle);
                dir = normalize(nb[0][0] * c + nb[1][0] * s + n[0] * lift,
                    nb[0][1] * c + nb[1][1] * s + n[1] * lift,
                    nb[0][2] * c + nb[1][2] * s + n[2] * lift);
                size = 0.06f + random.nextFloat() * 0.12f;
            } else {
                dir = normalize((float) random.nextGaussian(), (float) random.nextGaussian(), (float) random.nextGaussian());
                size = 0.12f + random.nextFloat() * random.nextFloat() * 0.40f;
            }
            float[][] basis = basis(dir);
            int o = i * STAR_STRIDE;
            data[o] = dir[0];
            data[o + 1] = dir[1];
            data[o + 2] = dir[2];
            for (int k = 0; k < 3; k++) {
                data[o + 3 + k] = basis[0][k] * size;
                data[o + 6 + k] = basis[1][k] * size;
            }
            data[o + 9] = random.nextFloat() * 6.283f;
            data[o + 10] = random.nextFloat();
        }
        return data;
    }

    /** Two unit vectors perpendicular to dir and to each other. */
    private static float[][] basis(float[] dir) {
        float[] helper = Math.abs(dir[1]) < 0.9f ? new float[] {0, 1, 0} : new float[] {1, 0, 0};
        float[] u = normalize(
            helper[1] * dir[2] - helper[2] * dir[1],
            helper[2] * dir[0] - helper[0] * dir[2],
            helper[0] * dir[1] - helper[1] * dir[0]);
        float[] v = {
            dir[1] * u[2] - dir[2] * u[1],
            dir[2] * u[0] - dir[0] * u[2],
            dir[0] * u[1] - dir[1] * u[0]
        };
        return new float[][] {u, v};
    }

    private static float[] normalize(float x, float y, float z) {
        float length = (float) Math.sqrt(x * x + y * y + z * z);
        if (length < 1.0e-5f) return new float[] {0, 1, 0};
        return new float[] {x / length, y / length, z / length};
    }

    /** Desaturates and darkens toward an overcast gray as rain sets in. */
    private static int weather(int color, float rain) {
        if (rain <= 0.0f) return color;
        int r = (color >>> 16) & 0xFF, g = (color >>> 8) & 0xFF, b = color & 0xFF;
        int gray = Math.round((r * 30 + g * 59 + b * 11) / 100.0f * 0.75f);
        int overcast = 0xFF000000 | (gray << 16) | (gray << 8) | gray;
        return lerpColor(color, overcast, rain * 0.7f);
    }

    private static int lighten(int color, float amount) {
        return lerpColor(color, 0xFFFFFFFF, amount);
    }

    static int lerpColor(int from, int to, float t) {
        return FlintFixUi.blendColors(from, to, t);
    }

    private static int withAlpha(int color, float alpha) {
        int a = Math.round(Math.max(0.0f, Math.min(1.0f, alpha)) * 255.0f);
        return (a << 24) | (color & 0x00FFFFFF);
    }

    private static float smooth(float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));
        return t * t * (3.0f - 2.0f * t);
    }
}
