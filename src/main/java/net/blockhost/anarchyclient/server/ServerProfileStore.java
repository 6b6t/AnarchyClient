package net.blockhost.anarchyclient.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.config.ConfigFiles;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleManager;
import net.blockhost.anarchyclient.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;

public final class ServerProfileStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH =
            FabricLoader.getInstance().getConfigDir().resolve(AnarchyClient.MOD_ID + "-server-profiles.json");
    private static final Map<String, ServerProfile> PROFILES = new LinkedHashMap<>();
    private static boolean loaded;
    // Set when an unreadable file could not be backed up. Saving would then destroy the stored profiles.
    private static boolean saveBlocked;

    private ServerProfileStore() {}

    public static synchronized void load() {
        PROFILES.clear();
        loaded = true;
        saveBlocked = false;
        if (!Files.exists(PATH)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(PATH)) {
            ProfileRoot root = GSON.fromJson(reader, ProfileRoot.class);
            if (root == null || root.profiles == null) {
                return;
            }
            for (Map.Entry<String, ServerProfile> entry : root.profiles.entrySet()) {
                PROFILES.put(normalizeDomain(entry.getKey()), entry.getValue().normalized());
            }
        } catch (IOException | RuntimeException exception) {
            AnarchyClient.LOGGER.warn("Failed to load AnarchyClient server profiles from {}", PATH, exception);
            PROFILES.clear();
            saveBlocked = !ConfigFiles.backUpUnreadable(PATH);
        }
    }

    public static synchronized void save() {
        ensureLoaded();
        if (saveBlocked) {
            AnarchyClient.LOGGER.warn(
                    "Not saving AnarchyClient server profiles: {} could not be read or backed up", PATH);
            return;
        }
        try {
            ConfigFiles.writeAtomically(PATH, GSON.toJson(new ProfileRoot(PROFILES)));
        } catch (IOException exception) {
            AnarchyClient.LOGGER.warn("Failed to save AnarchyClient server profiles to {}", PATH, exception);
        }
    }

    public static synchronized Optional<ServerProfile> find(final String domain) {
        ensureLoaded();
        return Optional.ofNullable(PROFILES.get(normalizeDomain(domain)));
    }

    public static synchronized ServerProfile getOrCreate(final String domain) {
        ensureLoaded();
        String key = normalizeDomain(domain);
        return PROFILES.computeIfAbsent(key, ignored -> new ServerProfile());
    }

    public static synchronized void recordAntiCheat(final String domain, final String antiCheat) {
        if (domain == null || domain.isBlank() || antiCheat == null || antiCheat.isBlank()) {
            return;
        }
        getOrCreate(domain).antiCheat = antiCheat;
        save();
    }

    public static synchronized void recordPlugins(final String domain, final Set<String> plugins) {
        if (domain == null || domain.isBlank() || plugins == null || plugins.isEmpty()) {
            return;
        }
        if (getOrCreate(domain).knownPlugins.addAll(plugins)) {
            save();
        }
    }

    public static synchronized void recordFingerprint(final ServerObserver.Fingerprint fingerprint) {
        if (fingerprint == null || fingerprint.rootDomain().isBlank()) {
            return;
        }
        getOrCreate(fingerprint.rootDomain()).merge(fingerprint);
        save();
    }

    public static synchronized int apply(final ModuleManager modules, final String domain) {
        Optional<ServerProfile> maybeProfile = find(domain);
        if (maybeProfile.isEmpty()) {
            return 0;
        }
        ServerProfile profile = maybeProfile.get();
        int changed = 0;
        // Settings first, so modules enabled below start with the profile's values.
        if (profile.settings != null) {
            for (Map.Entry<String, Map<String, JsonElement>> moduleEntry : profile.settings.entrySet()) {
                Module module = modules.find(moduleEntry.getKey()).orElse(null);
                if (module == null || moduleEntry.getValue() == null) {
                    continue;
                }
                for (Map.Entry<String, JsonElement> settingEntry :
                        moduleEntry.getValue().entrySet()) {
                    Setting<?> setting = module.settings().stream()
                            .filter(candidate -> candidate.id().equals(settingEntry.getKey()))
                            .findFirst()
                            .orElse(null);
                    if (setting == null || settingEntry.getValue() == null) {
                        continue;
                    }
                    try {
                        setting.fromJson(settingEntry.getValue());
                        changed++;
                    } catch (RuntimeException exception) {
                        AnarchyClient.LOGGER.warn(
                                "Skipping invalid server profile value {}.{}",
                                moduleEntry.getKey(),
                                settingEntry.getKey(),
                                exception);
                    }
                }
            }
        }
        if (profile.moduleStates != null) {
            for (Map.Entry<String, Boolean> entry : profile.moduleStates.entrySet()) {
                Module module = modules.find(entry.getKey()).orElse(null);
                if (module != null && entry.getValue() != null && module.enabled() != entry.getValue()) {
                    module.enabled(entry.getValue());
                    changed++;
                }
            }
        }
        return changed;
    }

    public static Set<String> staffNames(final String domain) {
        return find(domain)
                .map(profile -> profile.staffNames == null ? Set.<String>of() : Set.copyOf(profile.staffNames))
                .orElse(Set.of());
    }

    private static void ensureLoaded() {
        if (!loaded) {
            load();
        }
    }

    private static String normalizeDomain(final String domain) {
        return domain == null ? "" : domain.trim().toLowerCase(Locale.ROOT);
    }

    private record ProfileRoot(Map<String, ServerProfile> profiles) {}

    public static final class ServerProfile {

        public String antiCheat = "";
        public Set<String> knownPlugins = new LinkedHashSet<>();
        public Set<String> payloadChannels = new LinkedHashSet<>();
        public Set<String> environmentLabels = new LinkedHashSet<>();
        public Set<String> staffNames = new LinkedHashSet<>();
        public Map<String, Boolean> moduleStates = new LinkedHashMap<>();
        public Map<String, Map<String, JsonElement>> settings = new LinkedHashMap<>();
        public int lastSafetyScore;
        public String notes = "";

        /**
         * Adds what this session observed. A new session starts with an empty fingerprint, so blank or empty
         * values never replace what earlier sessions found.
         */
        void merge(final ServerObserver.Fingerprint fingerprint) {
            if (!fingerprint.antiCheat().isBlank()) {
                this.antiCheat = fingerprint.antiCheat();
            }
            this.knownPlugins.addAll(fingerprint.plugins());
            this.payloadChannels.addAll(fingerprint.payloadChannels());
            this.environmentLabels.addAll(fingerprint.labels());
            this.lastSafetyScore = fingerprint.safetyScore();
        }

        private ServerProfile normalized() {
            if (this.knownPlugins == null) {
                this.knownPlugins = new LinkedHashSet<>();
            }
            if (this.staffNames == null) {
                this.staffNames = new LinkedHashSet<>();
            }
            if (this.payloadChannels == null) {
                this.payloadChannels = new LinkedHashSet<>();
            }
            if (this.environmentLabels == null) {
                this.environmentLabels = new LinkedHashSet<>();
            }
            if (this.moduleStates == null) {
                this.moduleStates = new LinkedHashMap<>();
            }
            if (this.settings == null) {
                this.settings = new LinkedHashMap<>();
            }
            if (this.antiCheat == null) {
                this.antiCheat = "";
            }
            if (this.notes == null) {
                this.notes = "";
            }
            return this;
        }
    }
}
