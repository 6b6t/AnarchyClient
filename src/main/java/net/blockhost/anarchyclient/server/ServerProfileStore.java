package net.blockhost.anarchyclient.server;

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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ServerProfileStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve(AnarchyClient.MOD_ID + "-server-profiles.json");
    private static final Map<String, ServerProfile> PROFILES = new LinkedHashMap<>();
    private static boolean loaded;

    private ServerProfileStore() {
    }

    public static synchronized void load() {
        PROFILES.clear();
        loaded = true;
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
        }
    }

    public static synchronized void save() {
        ensureLoaded();
        try {
            Path parent = PATH.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(new ProfileRoot(PROFILES), writer);
            }
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
        getOrCreate(domain).knownPlugins = new LinkedHashSet<>(plugins);
        save();
    }

    public static synchronized int apply(final ModuleManager modules, final String domain) {
        Optional<ServerProfile> maybeProfile = find(domain);
        if (maybeProfile.isEmpty()) {
            return 0;
        }
        ServerProfile profile = maybeProfile.get();
        int changed = 0;
        if (profile.moduleStates != null) {
            for (Map.Entry<String, Boolean> entry : profile.moduleStates.entrySet()) {
                Module module = modules.find(entry.getKey()).orElse(null);
                if (module != null && module.enabled() != entry.getValue()) {
                    module.enabled(entry.getValue());
                    changed++;
                }
            }
        }
        if (profile.settings != null) {
            for (Map.Entry<String, Map<String, JsonElement>> moduleEntry : profile.settings.entrySet()) {
                Module module = modules.find(moduleEntry.getKey()).orElse(null);
                if (module == null) {
                    continue;
                }
                for (Map.Entry<String, JsonElement> settingEntry : moduleEntry.getValue().entrySet()) {
                    Setting<?> setting = module.settings().stream()
                            .filter(candidate -> candidate.id().equals(settingEntry.getKey()))
                            .findFirst()
                            .orElse(null);
                    if (setting != null) {
                        setting.fromJson(settingEntry.getValue());
                        changed++;
                    }
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

    private record ProfileRoot(Map<String, ServerProfile> profiles) {
    }

    public static final class ServerProfile {

        public String antiCheat = "";
        public Set<String> knownPlugins = new LinkedHashSet<>();
        public Set<String> staffNames = new LinkedHashSet<>();
        public Map<String, Boolean> moduleStates = new LinkedHashMap<>();
        public Map<String, Map<String, JsonElement>> settings = new LinkedHashMap<>();
        public String notes = "";

        private ServerProfile normalized() {
            if (this.knownPlugins == null) {
                this.knownPlugins = new LinkedHashSet<>();
            }
            if (this.staffNames == null) {
                this.staffNames = new LinkedHashSet<>();
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
