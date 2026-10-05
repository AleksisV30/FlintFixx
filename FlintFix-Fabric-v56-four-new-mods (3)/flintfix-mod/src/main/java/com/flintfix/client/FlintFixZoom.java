package com.flintfix.client;

/** Press-and-hold zoom state. Wheel steps adjust magnification until the key is released. */
public final class FlintFixZoom {
    private static boolean active;
    private static float distance;

    private FlintFixZoom() {}

    public static boolean isActive() { return active; }
    public static float distance() { return distance; }
    public static double magnification() { return active ? Math.min(8.0, 1.0 + (distance - 1.0) * 0.30) : 1.0; }

    public static void setActive(boolean value) {
        if (active == value) return;
        active = value;
        distance = value ? 4.0f : 0.0f;
    }

    public static void scroll(double vertical) {
        if (!active || vertical == 0.0) return;
        distance = Math.max(1.0f, Math.min(24.0f, distance + (vertical > 0 ? 1.5f : -1.5f)));
    }
}
