package net.blockhost.anarchyclient.profile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleManager;
import net.blockhost.anarchyclient.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class ProfileManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path path;
    private final Map<String, Profile> profiles = new LinkedHashMap<>();

    public ProfileManager(final Path path) {
        this.path = path;
    }

    public static Path defaultPath() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve("anarchyclient")
                .resolve("profiles.json");
    }

    public synchronized void load() {
        this.profiles.clear();
        if (!Files.exists(this.path)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(this.path)) {
            ProfileRoot root = GSON.fromJson(reader, ProfileRoot.class);
            if (root == null || root.profiles == null) {
                return;
            }
            for (Map.Entry<String, Profile> entry : root.profiles.entrySet()) {
                Profile profile = entry.getValue().normalized(entry.getKey());
                this.profiles.put(key(profile.name), profile);
            }
        } catch (IOException | RuntimeException exception) {
            AnarchyClient.LOGGER.warn("Failed to load AnarchyClient profiles from {}", this.path, exception);
        }
    }

    public synchronized void save() {
        try {
            Path parent = this.path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (Writer writer = Files.newBufferedWriter(this.path)) {
                GSON.toJson(new ProfileRoot(this.profiles), writer);
            }
        } catch (IOException exception) {
            AnarchyClient.LOGGER.warn("Failed to save AnarchyClient profiles to {}", this.path, exception);
        }
    }

    public synchronized Profile capture(final String name, final ModuleManager modules) {
        Profile profile = new Profile();
        profile.name = normalizeName(name);
        profile.updatedAt = Instant.now().toString();
        for (Module module : modules.all()) {
            profile.modules.put(module.id(), module.enabled());
            Map<String, JsonElement> settings = new LinkedHashMap<>();
            for (Setting<?> setting : module.settings()) {
                settings.put(setting.id(), setting.toJson().deepCopy());
            }
            profile.settings.put(module.id(), settings);
        }
        this.profiles.put(key(profile.name), profile);
        this.save();
        return profile.copy();
    }

    public synchronized Optional<Profile> find(final String name) {
        Profile profile = this.profiles.get(key(name));
        return profile == null ? Optional.empty() : Optional.of(profile.copy());
    }

    public synchronized List<ProfileSummary> summaries() {
        List<ProfileSummary> summaries = new ArrayList<>();
        for (Profile profile : this.profiles.values()) {
            summaries.add(new ProfileSummary(profile.name, profile.updatedAt, profile.modules.size()));
        }
        summaries.sort(Comparator.comparing(ProfileSummary::name, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(summaries);
    }

    public synchronized boolean delete(final String name) {
        boolean removed = this.profiles.remove(key(name)) != null;
        if (removed) {
            this.save();
        }
        return removed;
    }

    public synchronized boolean exportProfile(final String name, final Path output) throws IOException {
        Profile profile = this.profiles.get(key(name));
        if (profile == null) {
            return false;
        }
        Path parent = output.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (Writer writer = Files.newBufferedWriter(output)) {
            GSON.toJson(profile, writer);
        }
        return true;
    }

    public synchronized Profile importProfile(final Path input) throws IOException {
        try (Reader reader = Files.newBufferedReader(input)) {
            Profile profile = GSON.fromJson(reader, Profile.class);
            if (profile == null) {
                throw new IOException("Profile file is empty.");
            }
            String fallback = input.getFileName() == null ? "imported" : input.getFileName().toString().replaceFirst("\\.json$", "");
            profile = profile.normalized(fallback);
            profile.updatedAt = Instant.now().toString();
            this.profiles.put(key(profile.name), profile);
            this.save();
            return profile.copy();
        } catch (RuntimeException exception) {
            throw new IOException("Invalid profile file: " + input, exception);
        }
    }

    public synchronized int apply(final String name, final ModuleManager modules) {
        Profile profile = this.profiles.get(key(name));
        if (profile == null) {
            return -1;
        }
        int changed = 0;
        for (Module module : modules.all()) {
            Boolean enabled = profile.modules.get(module.id());
            if (enabled != null && module.enabled() != enabled) {
                module.enabled(enabled);
                changed++;
            }
            Map<String, JsonElement> settings = profile.settings.get(module.id());
            if (settings == null) {
                continue;
            }
            for (Setting<?> setting : module.settings()) {
                JsonElement value = settings.get(setting.id());
                if (value != null) {
                    setting.fromJson(value.deepCopy());
                    changed++;
                }
            }
        }
        return changed;
    }

    private static String normalizeName(final String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Profile name cannot be blank.");
        }
        return name.trim();
    }

    private static String key(final String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    private record ProfileRoot(Map<String, Profile> profiles) {
    }

    public record ProfileSummary(String name, String updatedAt, int modules) {
    }

    public static final class Profile {

        public String name = "";
        public String updatedAt = "";
        public Map<String, Boolean> modules = new LinkedHashMap<>();
        public Map<String, Map<String, JsonElement>> settings = new LinkedHashMap<>();

        private Profile normalized(final String fallbackName) {
            if (this.name == null || this.name.isBlank()) {
                this.name = fallbackName;
            }
            this.name = normalizeName(this.name);
            if (this.updatedAt == null) {
                this.updatedAt = "";
            }
            if (this.modules == null) {
                this.modules = new LinkedHashMap<>();
            }
            if (this.settings == null) {
                this.settings = new LinkedHashMap<>();
            }
            return this;
        }

        private Profile copy() {
            Profile copy = new Profile();
            copy.name = this.name;
            copy.updatedAt = this.updatedAt;
            copy.modules = new LinkedHashMap<>(this.modules);
            for (Map.Entry<String, Map<String, JsonElement>> entry : this.settings.entrySet()) {
                Map<String, JsonElement> settingsCopy = new LinkedHashMap<>();
                for (Map.Entry<String, JsonElement> setting : entry.getValue().entrySet()) {
                    settingsCopy.put(setting.getKey(), setting.getValue().deepCopy());
                }
                copy.settings.put(entry.getKey(), settingsCopy);
            }
            return copy;
        }
    }
}
