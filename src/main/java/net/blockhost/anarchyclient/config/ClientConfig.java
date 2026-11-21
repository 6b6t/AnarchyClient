package net.blockhost.anarchyclient.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.command.CommandPrefix;
import net.blockhost.anarchyclient.friends.FriendManager;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.module.ModuleManager;
import net.blockhost.anarchyclient.notification.ToggleNotifications;
import net.blockhost.anarchyclient.rivet.BackgroundDesign;
import net.blockhost.anarchyclient.setting.Setting;
import net.blockhost.anarchyclient.util.Enums;
import net.fabricmc.loader.api.FabricLoader;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClientConfig {

    /** Bump when a {@link ModuleConfigMigration} needs to know which files predate a change. */
    static final int CONFIG_VERSION = ModuleConfigMigration.CURRENT_VERSION;

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientConfig.class);
    private static final long SAVE_DELAY_NANOS = TimeUnit.MILLISECONDS.toNanos(500);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final ModuleManager modules;
    private final @Nullable FriendManager friends;
    private final Path path;
    private final Map<ModuleCategory, CategoryWindowState> categoryWindows = new EnumMap<>(ModuleCategory.class);
    private final List<ModuleCategory> categoryOrder = new ArrayList<>();
    private final Set<String> expandedModules = new HashSet<>();
    private final Set<String> favoriteModules = new HashSet<>();
    private final AtomicBoolean dirty = new AtomicBoolean();
    private volatile long lastChangeNanos;
    // Set when an unreadable config could not be backed up. Saving would then destroy the user's config.
    private boolean saveBlocked;
    private @Nullable ModuleCategory selectedCategory;
    private @Nullable String selectedModuleId;
    private boolean showDisabledModules = UiPreferences.DEFAULT.showDisabledModules();
    private boolean enabledModulesFirst = UiPreferences.DEFAULT.enabledModulesFirst();
    private boolean showModuleSummaries = UiPreferences.DEFAULT.showModuleSummaries();
    private boolean compactRows = UiPreferences.DEFAULT.compactRows();
    private boolean wideInspector = UiPreferences.DEFAULT.wideInspector();
    private GuiThemePreset guiTheme = UiPreferences.DEFAULT.guiTheme();
    private BackgroundDesign backgroundDesign = UiPreferences.DEFAULT.backgroundDesign();
    private float glassOpacity = UiPreferences.DEFAULT.glassOpacity();
    private float cornerRadius = UiPreferences.DEFAULT.cornerRadius();
    private float glassBlur = UiPreferences.DEFAULT.glassBlur();

    public ClientConfig(final ModuleManager modules) {
        this(modules, null, FabricLoader.getInstance().getConfigDir().resolve(AnarchyClient.MOD_ID + ".json"));
    }

    public ClientConfig(final ModuleManager modules, final Path path) {
        this(modules, null, path);
    }

    public ClientConfig(final ModuleManager modules, final FriendManager friends) {
        this(modules, friends, FabricLoader.getInstance().getConfigDir().resolve(AnarchyClient.MOD_ID + ".json"));
    }

    public ClientConfig(final ModuleManager modules, final @Nullable FriendManager friends, final Path path) {
        this.modules = modules;
        this.friends = friends;
        this.path = path;
    }

    public void load() {
        this.categoryWindows.clear();
        this.categoryOrder.clear();
        this.expandedModules.clear();
        this.favoriteModules.clear();
        this.selectedCategory = null;
        this.selectedModuleId = null;
        this.uiPreferences(UiPreferences.DEFAULT);
        this.saveBlocked = false;
        if (!Files.exists(this.path)) {
            this.save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(this.path)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null) {
                return;
            }
            try {
                this.loadUi(root);
            } catch (RuntimeException exception) {
                LOGGER.warn("Failed to load AnarchyClient UI settings from {}", this.path, exception);
            }
            JsonObject moduleRoot = root.getAsJsonObject("modules");
            if (moduleRoot == null) {
                return;
            }
            int savedVersion = savedVersion(root);
            boolean migratedFriends = this.migrateLegacyFriends(moduleRoot);
            for (Module module : this.modules.all()) {
                JsonObject moduleJson = findObject(moduleRoot, module.id(), module.aliases());
                if (moduleJson != null) {
                    this.loadModule(module, moduleJson, savedVersion);
                }
            }
            if (migratedFriends) {
                this.save();
            }
        } catch (RuntimeException | IOException exception) {
            LOGGER.warn("Failed to load AnarchyClient config from {}", this.path, exception);
            if (ConfigFiles.backUpUnreadable(this.path)) {
                this.save();
            } else {
                this.saveBlocked = true;
            }
        }
    }

    /** Format version of a saved config; files written before versioning count as version 1. */
    static int savedVersion(final JsonObject root) {
        JsonElement version = root.get("version");
        return version instanceof JsonPrimitive primitive && primitive.isNumber() ? primitive.getAsInt() : 1;
    }

    private void loadModule(final Module module, final JsonObject moduleJson, final int savedVersion) {
        if (module instanceof ModuleConfigMigration migration) {
            try {
                migration.migrateConfig(moduleJson, savedVersion);
            } catch (RuntimeException exception) {
                LOGGER.warn("Failed to migrate config for module {}", module.id(), exception);
            }
        }
        try {
            JsonElement keybind = moduleJson.get("keybind");
            if (keybind != null && keybind.isJsonObject()) {
                module.keybind().fromJson(keybind.getAsJsonObject());
            }
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to load keybind for module {}", module.id(), exception);
        }
        JsonElement settingsJson = moduleJson.get("settings");
        if (settingsJson != null && settingsJson.isJsonObject()) {
            JsonObject settings = settingsJson.getAsJsonObject();
            for (Setting<?> setting : module.settings()) {
                try {
                    setting.fromJson(findElement(settings, setting.id(), setting.aliases()));
                } catch (RuntimeException exception) {
                    LOGGER.warn("Failed to load setting {} for module {}", setting.id(), module.id(), exception);
                }
            }
        }
        try {
            JsonElement enabled = moduleJson.get("enabled");
            if (enabled != null && enabled.isJsonPrimitive()) {
                module.enabled(enabled.getAsBoolean());
            }
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to restore enabled state for module {}", module.id(), exception);
        }
    }

    public Optional<CategoryWindowState> categoryWindow(final ModuleCategory category) {
        return Optional.ofNullable(this.categoryWindows.get(category));
    }

    public void categoryWindow(final ModuleCategory category, final float x, final float y) {
        this.categoryWindows.put(category, new CategoryWindowState(x, y));
    }

    public Optional<List<ModuleCategory>> categoryOrder() {
        if (this.categoryOrder.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(List.copyOf(this.categoryOrder));
    }

    public void categoryOrder(final List<ModuleCategory> categories) {
        this.categoryOrder.clear();
        this.categoryOrder.addAll(normalizeCategoryOrder(categories));
    }

    public Optional<Set<String>> expandedModules() {
        return Optional.of(Set.copyOf(this.expandedModules));
    }

    public void expandedModules(final Set<String> moduleIds) {
        this.expandedModules.clear();
        this.expandedModules.addAll(moduleIds);
    }

    public Optional<Set<String>> favoriteModules() {
        return Optional.of(Set.copyOf(this.favoriteModules));
    }

    public void favoriteModules(final Set<String> moduleIds) {
        this.favoriteModules.clear();
        for (String moduleId : moduleIds) {
            if (moduleId != null && !moduleId.isBlank()) {
                this.favoriteModules.add(moduleId);
            }
        }
    }

    public boolean moduleFavorite(final String moduleId) {
        return moduleId != null && this.favoriteModules.contains(moduleId);
    }

    public void moduleFavorite(final String moduleId, final boolean favorite) {
        if (moduleId == null || moduleId.isBlank()) {
            return;
        }
        if (favorite) {
            this.favoriteModules.add(moduleId);
        } else {
            this.favoriteModules.remove(moduleId);
        }
    }

    public Optional<ModuleCategory> selectedCategory() {
        return Optional.ofNullable(this.selectedCategory);
    }

    public void selectedCategory(final ModuleCategory category) {
        this.selectedCategory = category;
    }

    public Optional<String> selectedModuleId() {
        return Optional.ofNullable(this.selectedModuleId);
    }

    public void selectedModuleId(final String moduleId) {
        this.selectedModuleId = moduleId;
    }

    public UiPreferences uiPreferences() {
        return new UiPreferences(
                this.showDisabledModules,
                this.enabledModulesFirst,
                this.showModuleSummaries,
                this.compactRows,
                this.wideInspector,
                this.guiTheme,
                this.backgroundDesign,
                this.glassOpacity,
                this.cornerRadius,
                this.glassBlur);
    }

    public void uiPreferences(final UiPreferences preferences) {
        UiPreferences value = preferences == null ? UiPreferences.DEFAULT : preferences;
        this.showDisabledModules = value.showDisabledModules();
        this.enabledModulesFirst = value.enabledModulesFirst();
        this.showModuleSummaries = value.showModuleSummaries();
        this.compactRows = value.compactRows();
        this.wideInspector = value.wideInspector();
        this.guiTheme = value.guiTheme();
        this.backgroundDesign = value.backgroundDesign();
        this.glassOpacity = value.glassOpacity();
        this.cornerRadius = value.cornerRadius();
        this.glassBlur = value.glassBlur();
    }

    public void resetUiPreferences() {
        this.uiPreferences(UiPreferences.DEFAULT);
    }

    /**
     * Marks the config as changed. Safe to call from any thread; {@link #saveIfDirty()} writes it once
     * changes have settled.
     */
    public void markDirty() {
        this.lastChangeNanos = System.nanoTime();
        this.dirty.set(true);
    }

    /**
     * Saves pending changes once no change arrived for {@link #SAVE_DELAY_NANOS}, so a slider drag or a
     * burst of toggles writes the file once.
     */
    public void saveIfDirty() {
        if (this.dirty.get() && System.nanoTime() - this.lastChangeNanos >= SAVE_DELAY_NANOS) {
            this.save();
        }
    }

    public void save() {
        this.dirty.set(false);
        if (this.saveBlocked) {
            LOGGER.warn("Not saving AnarchyClient config: {} could not be read or backed up", this.path);
            return;
        }
        JsonObject root = new JsonObject();
        root.addProperty("version", CONFIG_VERSION);
        JsonObject moduleRoot = new JsonObject();
        root.add("modules", moduleRoot);
        this.saveUi(root);

        for (Module module : this.modules.all()) {
            JsonObject moduleJson = new JsonObject();
            JsonObject settings = new JsonObject();
            moduleJson.addProperty("enabled", module.enabled());
            moduleJson.add("keybind", module.keybind().toJson());
            moduleJson.add("settings", settings);
            for (Setting<?> setting : module.settings()) {
                settings.add(setting.id(), setting.toJson());
            }
            moduleRoot.add(module.id(), moduleJson);
        }

        try {
            ConfigFiles.writeAtomically(this.path, GSON.toJson(root));
        } catch (IOException exception) {
            LOGGER.warn("Failed to save AnarchyClient config to {}", this.path, exception);
        }
    }

    private void loadUi(final JsonObject root) {
        JsonObject ui = root.getAsJsonObject("ui");
        if (ui == null) {
            return;
        }

        JsonObject categoryWindows = ui.getAsJsonObject("categories");
        if (categoryWindows != null) {
            for (ModuleCategory category : ModuleCategory.values()) {
                JsonObject categoryJson = categoryWindows.getAsJsonObject(categoryKey(category));
                if (categoryJson == null) {
                    continue;
                }
                JsonElement x = categoryJson.get("x");
                JsonElement y = categoryJson.get("y");
                if (x != null && x.isJsonPrimitive() && y != null && y.isJsonPrimitive()) {
                    this.categoryWindow(category, x.getAsFloat(), y.getAsFloat());
                }
            }
        }

        JsonElement expanded = ui.get("expandedModules");
        if (expanded != null && expanded.isJsonArray()) {
            Set<String> moduleIds = new HashSet<>();
            for (JsonElement element : expanded.getAsJsonArray()) {
                if (element != null && element.isJsonPrimitive()) {
                    moduleIds.add(element.getAsString());
                }
            }
            this.expandedModules(moduleIds);
        }

        JsonElement favorites = ui.get("favoriteModules");
        if (favorites != null && favorites.isJsonArray()) {
            Set<String> moduleIds = new HashSet<>();
            for (JsonElement element : favorites.getAsJsonArray()) {
                if (element != null && element.isJsonPrimitive()) {
                    moduleIds.add(element.getAsString());
                }
            }
            this.favoriteModules(moduleIds);
        }

        JsonElement categoryOrderJson = ui.get("categoryOrder");
        if (categoryOrderJson != null && categoryOrderJson.isJsonArray()) {
            List<ModuleCategory> categories = new ArrayList<>();
            for (JsonElement element : categoryOrderJson.getAsJsonArray()) {
                if (element != null && element.isJsonPrimitive()) {
                    parseCategory(element.getAsString()).ifPresent(categories::add);
                }
            }
            if (!categories.isEmpty()) {
                this.categoryOrder(categories);
            }
        }

        JsonElement selectedCategoryJson = ui.get("selectedCategory");
        if (selectedCategoryJson != null && selectedCategoryJson.isJsonPrimitive()) {
            parseCategory(selectedCategoryJson.getAsString()).ifPresent(this::selectedCategory);
        }

        JsonElement selectedModuleJson = ui.get("selectedModule");
        if (selectedModuleJson != null && selectedModuleJson.isJsonPrimitive()) {
            this.selectedModuleId(selectedModuleJson.getAsString());
        }

        this.showDisabledModules = readBoolean(ui, "showDisabledModules", this.showDisabledModules);
        this.enabledModulesFirst = readBoolean(ui, "enabledModulesFirst", this.enabledModulesFirst);
        this.showModuleSummaries = readBoolean(ui, "showModuleSummaries", this.showModuleSummaries);
        this.compactRows = readBoolean(ui, "compactRows", this.compactRows);
        this.wideInspector = readBoolean(ui, "wideInspector", this.wideInspector);
        JsonElement themeJson = ui.get("guiTheme");
        if (themeJson != null && themeJson.isJsonPrimitive()) {
            GuiThemePreset.fromKey(themeJson.getAsString()).ifPresent(theme -> this.guiTheme = theme);
        }
        JsonElement backgroundJson = ui.get("backgroundDesign");
        if (backgroundJson != null && backgroundJson.isJsonPrimitive()) {
            this.backgroundDesign = parseBackgroundDesign(backgroundJson.getAsString());
        }
        this.glassOpacity = readFloat(ui, "glassOpacity", this.glassOpacity);
        this.cornerRadius = readFloat(ui, "cornerRadius", this.cornerRadius);
        this.glassBlur = readFloat(ui, "glassBlur", this.glassBlur);
        JsonElement prefixJson = ui.get("commandPrefix");
        if (prefixJson != null && prefixJson.isJsonPrimitive()) {
            CommandPrefix.set(prefixJson.getAsString());
        }
        ToggleNotifications.enabled(readBoolean(ui, "toggleNotifications", ToggleNotifications.enabled()));
        JsonElement modeJson = ui.get("toggleNotificationsMode");
        if (modeJson != null && modeJson.isJsonPrimitive()) {
            ToggleNotifications.mode(ToggleNotifications.Mode.fromName(modeJson.getAsString()));
        }
        JsonElement cornerJson = ui.get("toggleNotificationsCorner");
        if (cornerJson != null && cornerJson.isJsonPrimitive()) {
            ToggleNotifications.corner(ToggleNotifications.Corner.fromName(cornerJson.getAsString()));
        }
        JsonElement durationJson = ui.get("toggleNotificationsDuration");
        if (durationJson != null && durationJson.isJsonPrimitive()) {
            ToggleNotifications.durationMs(durationJson.getAsInt());
        }
    }

    private void saveUi(final JsonObject root) {
        JsonObject ui = new JsonObject();
        if (!this.categoryWindows.isEmpty()) {
            JsonObject categories = new JsonObject();
            for (Map.Entry<ModuleCategory, CategoryWindowState> entry : this.categoryWindows.entrySet()) {
                CategoryWindowState state = entry.getValue();
                JsonObject category = new JsonObject();
                category.addProperty("x", state.x());
                category.addProperty("y", state.y());
                categories.add(categoryKey(entry.getKey()), category);
            }
            ui.add("categories", categories);
        }

        JsonArray expanded = new JsonArray();
        this.expandedModules.stream().sorted().forEach(expanded::add);
        ui.add("expandedModules", expanded);

        JsonArray favorites = new JsonArray();
        this.favoriteModules.stream().sorted().forEach(favorites::add);
        ui.add("favoriteModules", favorites);

        if (!this.categoryOrder.isEmpty()) {
            JsonArray categories = new JsonArray();
            this.categoryOrder.forEach(category -> categories.add(categoryKey(category)));
            ui.add("categoryOrder", categories);
        }

        if (this.selectedCategory != null) {
            ui.addProperty("selectedCategory", categoryKey(this.selectedCategory));
        }
        if (this.selectedModuleId != null && !this.selectedModuleId.isBlank()) {
            ui.addProperty("selectedModule", this.selectedModuleId);
        }
        UiPreferences preferences = this.uiPreferences();
        ui.addProperty("showDisabledModules", preferences.showDisabledModules());
        ui.addProperty("enabledModulesFirst", preferences.enabledModulesFirst());
        ui.addProperty("showModuleSummaries", preferences.showModuleSummaries());
        ui.addProperty("compactRows", preferences.compactRows());
        ui.addProperty("wideInspector", preferences.wideInspector());
        ui.addProperty("guiTheme", preferences.guiTheme().key());
        ui.addProperty("backgroundDesign", preferences.backgroundDesign().name().toLowerCase(Locale.ROOT));
        ui.addProperty("glassOpacity", preferences.glassOpacity());
        ui.addProperty("cornerRadius", preferences.cornerRadius());
        ui.addProperty("glassBlur", preferences.glassBlur());
        ui.addProperty("commandPrefix", CommandPrefix.get());
        ui.addProperty("toggleNotifications", ToggleNotifications.enabled());
        ui.addProperty(
                "toggleNotificationsMode", ToggleNotifications.mode().name().toLowerCase(Locale.ROOT));
        ui.addProperty(
                "toggleNotificationsCorner", ToggleNotifications.corner().name().toLowerCase(Locale.ROOT));
        ui.addProperty("toggleNotificationsDuration", ToggleNotifications.durationMs());

        if (ui.size() > 0) {
            root.add("ui", ui);
        }
    }

    private static float readFloat(final JsonObject json, final String key, final float fallback) {
        JsonElement element = json.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsFloat() : fallback;
    }

    private static boolean readBoolean(final JsonObject json, final String key, final boolean fallback) {
        JsonElement element = json.get(key);
        return (element != null && element.isJsonPrimitive()) ? element.getAsBoolean() : fallback;
    }

    private static BackgroundDesign parseBackgroundDesign(final String value) {
        for (BackgroundDesign design : BackgroundDesign.values()) {
            if (design.name().equalsIgnoreCase(value) || design.displayName().equalsIgnoreCase(value)) {
                return design;
            }
        }
        return BackgroundDesign.NONE;
    }

    private static String categoryKey(final ModuleCategory category) {
        return category.name().toLowerCase(Locale.ROOT);
    }

    private static Optional<ModuleCategory> parseCategory(final String key) {
        for (ModuleCategory category : ModuleCategory.values()) {
            if (categoryKey(category).equals(key)) {
                return Optional.of(category);
            }
        }
        return Optional.empty();
    }

    private static @Nullable JsonObject findObject(
            final JsonObject json, final String key, final List<String> aliases) {
        JsonElement element = findElement(json, key, aliases);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static @Nullable JsonElement findElement(
            final JsonObject json, final String key, final List<String> aliases) {
        JsonElement element = json.get(key);
        if (element != null) {
            return element;
        }
        for (String alias : aliases) {
            element = json.get(alias);
            if (element != null) {
                return element;
            }
        }
        return null;
    }

    private boolean migrateLegacyFriends(final JsonObject moduleRoot) {
        if (this.friends == null) {
            return false;
        }
        Set<String> names = new HashSet<>();
        boolean foundLegacyField = false;
        for (String moduleId : List.of("esp", "tracers", "nametags", "kill_aura")) {
            JsonObject moduleJson = moduleRoot.getAsJsonObject(moduleId);
            if (moduleJson == null) {
                continue;
            }
            JsonObject settings = moduleJson.getAsJsonObject("settings");
            if (settings == null) {
                continue;
            }
            JsonElement friendsJson = settings.get("friends");
            if (friendsJson != null && friendsJson.isJsonPrimitive()) {
                foundLegacyField = true;
                names.addAll(FriendManager.parseNames(friendsJson.getAsString()));
            }
        }
        this.friends.addAll(names);
        return foundLegacyField;
    }

    private static List<ModuleCategory> normalizeCategoryOrder(final List<ModuleCategory> categories) {
        List<ModuleCategory> normalized = new ArrayList<>();
        for (ModuleCategory category : categories) {
            if (!normalized.contains(category)) {
                normalized.add(category);
            }
        }
        for (ModuleCategory category : ModuleCategory.values()) {
            if (!normalized.contains(category)) {
                normalized.add(category);
            }
        }
        return normalized;
    }

    public record CategoryWindowState(float x, float y) {}

    public record UiPreferences(
            boolean showDisabledModules,
            boolean enabledModulesFirst,
            boolean showModuleSummaries,
            boolean compactRows,
            boolean wideInspector,
            GuiThemePreset guiTheme,
            BackgroundDesign backgroundDesign,
            float glassOpacity,
            float cornerRadius,
            float glassBlur) {

        public static final UiPreferences DEFAULT = new UiPreferences(
                true, false, true, true, false, GuiThemePreset.EMERALD, BackgroundDesign.NONE, 0.55F, 14F, 5F);

        public UiPreferences {
            if (guiTheme == null) {
                guiTheme = GuiThemePreset.EMERALD;
            }
            if (backgroundDesign == null) {
                backgroundDesign = BackgroundDesign.NONE;
            }
            glassOpacity = Math.min(0.95F, Math.max(0.15F, glassOpacity));
            cornerRadius = Math.min(24F, Math.max(0F, cornerRadius));
            glassBlur = Math.min(10F, Math.max(0F, glassBlur));
        }
    }

    public enum GuiThemePreset {
        EMERALD("emerald", "Emerald"),
        AMBER("amber", "Amber"),
        ROSE("rose", "Rose"),
        CYAN("cyan", "Cyan");

        private final String key;
        private final String displayName;

        GuiThemePreset(final String key, final String displayName) {
            this.key = key;
            this.displayName = displayName;
        }

        public String key() {
            return this.key;
        }

        public String displayName() {
            return this.displayName;
        }

        public GuiThemePreset next() {
            return Enums.next(this);
        }

        public static Optional<GuiThemePreset> fromKey(final String key) {
            for (GuiThemePreset preset : values()) {
                if (preset.key.equalsIgnoreCase(key) || preset.name().equalsIgnoreCase(key)) {
                    return Optional.of(preset);
                }
            }
            return Optional.empty();
        }
    }
}
