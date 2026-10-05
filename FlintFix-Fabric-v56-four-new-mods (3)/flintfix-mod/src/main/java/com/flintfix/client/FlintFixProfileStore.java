package com.flintfix.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Stores named copies of FlintFix settings and restores the active profile. */
public final class FlintFixProfileStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("flintfix-profiles.json");
    private static Store store;
    private static boolean loadingProfile;

    private FlintFixProfileStore() {}

    public static void initialize() {
        store = read();
        if (store.profiles == null) store.profiles = new ArrayList<>();
        normalizeProfiles();

        if (store.profiles.isEmpty()) {
            store.profiles.add(new Profile("Default", GSON.toJson(FlintFixClient.CONFIG)));
            store.selected = "Default";
        }
        Profile active = find(store.selected);
        if (active == null) {
            active = store.profiles.get(0);
            store.selected = active.name;
        }
        apply(active);
        write();
    }

    public static List<String> names() {
        List<String> names = new ArrayList<>();
        if (store != null) {
            for (Profile profile : store.profiles) names.add(profile.name);
        }
        return names;
    }

    public static String selectedName() {
        return store == null || store.selected == null ? "Default" : store.selected;
    }

    public static boolean select(String name) {
        Profile profile = find(name);
        if (profile == null) return false;
        store.selected = profile.name;
        apply(profile);
        write();
        return true;
    }

    public static boolean create(String name) {
        String clean = cleanName(name);
        if (store == null || clean == null || store.profiles.size() >= 32 || find(clean) != null) return false;
        Profile profile = new Profile(clean, GSON.toJson(FlintFixClient.CONFIG));
        store.profiles.add(profile);
        store.selected = clean;
        write();
        FlintFixClient.CONFIG.save();
        return true;
    }

    public static boolean renameSelected(String name) {
        String clean = cleanName(name);
        if (store == null || clean == null) return false;
        Profile current = find(store.selected);
        Profile duplicate = find(clean);
        if (current == null || (duplicate != null && duplicate != current)) return false;
        current.name = clean;
        store.selected = clean;
        write();
        FlintFixClient.CONFIG.save();
        return true;
    }

    public static boolean delete(String name) {
        if (store == null || store.profiles.size() <= 1) return false;
        Profile target = find(name);
        if (target == null) return false;
        boolean wasSelected = target.name.equalsIgnoreCase(store.selected);
        store.profiles.remove(target);
        if (wasSelected) {
            Profile fallback = store.profiles.get(0);
            store.selected = fallback.name;
            apply(fallback);
        }
        write();
        return true;
    }

    /** Called whenever config.save() runs so edits stay with the active profile. */
    static void syncActive(FlintFixConfig config) {
        if (store == null || loadingProfile || config == null) return;
        Profile active = find(store.selected);
        if (active == null) return;
        active.settingsJson = GSON.toJson(config);
        write();
    }

    private static void apply(Profile profile) {
        if (profile == null) return;
        loadingProfile = true;
        try {
            FlintFixConfig config = profile.settingsJson == null ? null
                : GSON.fromJson(profile.settingsJson, FlintFixConfig.class);
            if (config != null) {
                config.sanitize();
                FlintFixClient.CONFIG = config;
                FlintFixClient.CONFIG.save();
            }
        } catch (RuntimeException ignored) {
            // Keep the current valid config if a profile file has a broken snapshot.
        } finally {
            loadingProfile = false;
        }
        FlintFixUi.applyTheme();
    }

    private static void normalizeProfiles() {
        Set<String> seen = new HashSet<>();
        List<Profile> valid = new ArrayList<>();
        for (Profile profile : store.profiles) {
            if (profile == null) continue;
            String clean = cleanName(profile.name);
            if (clean == null || !seen.add(clean.toLowerCase(Locale.ROOT))) continue;
            profile.name = clean;
            if (profile.settingsJson == null) profile.settingsJson = GSON.toJson(FlintFixClient.CONFIG);
            valid.add(profile);
        }
        store.profiles = valid;
    }

    private static Profile find(String name) {
        if (store == null || name == null) return null;
        for (Profile profile : store.profiles) {
            if (profile.name.equalsIgnoreCase(name)) return profile;
        }
        return null;
    }

    private static String cleanName(String name) {
        if (name == null) return null;
        String clean = name.trim().replaceAll("[\\p{Cntrl}]", "");
        if (clean.isEmpty()) return null;
        return clean.length() > 24 ? clean.substring(0, 24).trim() : clean;
    }

    private static Store read() {
        if (!Files.exists(PATH)) return new Store();
        try (Reader reader = Files.newBufferedReader(PATH)) {
            Store loaded = GSON.fromJson(reader, Store.class);
            return loaded == null ? new Store() : loaded;
        } catch (Exception ignored) {
            return new Store();
        }
    }

    private static void write() {
        if (store == null) return;
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(store, writer);
            }
        } catch (Exception ignored) {
        }
    }

    private static final class Store {
        String selected = "Default";
        List<Profile> profiles = new ArrayList<>();
    }

    private static final class Profile {
        String name;
        String settingsJson;

        Profile() {}

        Profile(String name, String settingsJson) {
            this.name = name;
            this.settingsJson = settingsJson;
        }
    }
}
