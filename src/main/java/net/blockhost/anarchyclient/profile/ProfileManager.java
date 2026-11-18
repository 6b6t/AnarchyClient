package net.blockhost.anarchyclient.profile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.config.ConfigFiles;
import net.blockhost.anarchyclient.config.ModuleConfigMigration;
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
import java.util.Objects;
import java.util.Optional;

public final class ProfileManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path path;
    private final Map<String, Profile> profiles = new LinkedHashMap<>();
    // Set when an unreadable file could not be backed up. Saving would then destroy the user's profiles.
    private boolean saveBlocked;

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
        this.saveBlocked = false;
        if (!Files.exists(this.path)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(this.path)) {
            ProfileRoot root = GSON.fromJson(reader, ProfileRoot.class);
            if (root == null || root.profiles == null) {
                return;
            }
            for (Map.Entry<String, Profile> entry : root.profiles.entrySet()) {
                try {
                    Profile profile = entry.getValue().normalized(entry.getKey());
                    this.profiles.put(key(profile.name), profile);
                } catch (RuntimeException exception) {
                    AnarchyClient.LOGGER.warn("Skipping invalid AnarchyClient profile '{}'", entry.getKey(), exception);
                }
            }
        } catch (IOException | RuntimeException exception) {
            AnarchyClient.LOGGER.warn("Failed to load AnarchyClient profiles from {}", this.path, exception);
            this.profiles.clear();
            this.saveBlocked = !ConfigFiles.backUpUnreadable(this.path);
        }
    }

    public synchronized void save() {
        if (this.saveBlocked) {
            AnarchyClient.LOGGER.warn("Not saving AnarchyClient profiles: {} could not be read or backed up", this.path);
            return;
        }
        try {
            ConfigFiles.writeAtomically(this.path, GSON.toJson(new ProfileRoot(this.profiles)));
        } catch (IOException exception) {
            AnarchyClient.LOGGER.warn("Failed to save AnarchyClient profiles to {}", this.path, exception);
        }
    }

    public synchronized Profile capture(final String name, final ModuleManager modules) {
        Profile profile = new Profile();
        profile.name = normalizeName(name);
        profile.version = ModuleConfigMigration.CURRENT_VERSION;
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
            // Copy before storing: if anything in the file is still unusable, nothing is kept or saved.
            Profile result = profile.copy();
            this.profiles.put(key(profile.name), profile);
            this.save();
            return result;
        } catch (RuntimeException exception) {
            throw new IOException("Invalid profile file: " + input, exception);
        }
    }

    /**
     * Applies a profile. Settings load before modules are enabled, so {@code onEnable} sees the profile's
     * values. A setting that fails to load is logged and skipped.
     */
    public synchronized int apply(final String name, final ModuleManager modules) {
        Profile profile = this.profiles.get(key(name));
        if (profile == null) {
            return -1;
        }
        int changed = 0;
        for (Module module : modules.all()) {
            Map<String, JsonElement> settings = migratedSettings(module, profile.settings.get(module.id()),
                    profile.version);
            if (settings == null) {
                continue;
            }
            for (Setting<?> setting : module.settings()) {
                JsonElement value = settings.get(setting.id());
                if (value == null) {
                    continue;
                }
                try {
                    setting.fromJson(value.deepCopy());
                    changed++;
                } catch (RuntimeException exception) {
                    AnarchyClient.LOGGER.warn("Skipping invalid value for {}.{} in profile '{}'",
                            module.id(), setting.id(), profile.name, exception);
                }
            }
        }
        for (Module module : modules.all()) {
            Boolean enabled = profile.modules.get(module.id());
            if (enabled != null && module.enabled() != enabled) {
                try {
                    module.enabled(enabled);
                    changed++;
                } catch (RuntimeException exception) {
                    AnarchyClient.LOGGER.warn("Failed to toggle {} from profile '{}'", module.id(), profile.name,
                            exception);
                }
            }
        }
        return changed;
    }

    /**
     * Runs the module's config migration over settings from a profile saved by an older build, so applying it
     * does not bring back defaults the client config already moved past.
     */
    static Map<String, JsonElement> migratedSettings(final Module module, final Map<String, JsonElement> settings,
                                                     final int savedVersion) {
        if (settings == null || savedVersion >= ModuleConfigMigration.CURRENT_VERSION
                || !(module instanceof ModuleConfigMigration migration)) {
            return settings;
        }
        JsonObject settingsJson = new JsonObject();
        settings.forEach((id, value) -> {
            if (value != null) {
                settingsJson.add(id, value.deepCopy());
            }
        });
        JsonObject moduleJson = new JsonObject();
        moduleJson.add("settings", settingsJson);
        try {
            migration.migrateConfig(moduleJson, savedVersion);
        } catch (RuntimeException exception) {
            AnarchyClient.LOGGER.warn("Failed to migrate profile settings for module {}", module.id(), exception);
            return settings;
        }
        Map<String, JsonElement> migrated = new LinkedHashMap<>();
        settingsJson.entrySet().forEach(entry -> migrated.put(entry.getKey(), entry.getValue()));
        return migrated;
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
        /** Format version the profile was saved with; profiles from before versioning count as 1. */
        public int version = 1;
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
            // Null entries from a hand-edited or imported file would fail later, after the profile was saved.
            this.modules.values().removeIf(Objects::isNull);
            this.settings.values().removeIf(Objects::isNull);
            this.settings.values().forEach(values -> values.values().removeIf(Objects::isNull));
            return this;
        }

        private Profile copy() {
            Profile copy = new Profile();
            copy.name = this.name;
            copy.version = this.version;
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
