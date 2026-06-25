package net.blockhost.anarchyclient.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.friends.FriendManager;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.module.ModuleManager;
import net.blockhost.anarchyclient.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ClientConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientConfig.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter BACKUP_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT);

    private final ModuleManager modules;
    private final FriendManager friends;
    private final Path path;
    private final Map<ModuleCategory, CategoryWindowState> categoryWindows = new EnumMap<>(ModuleCategory.class);
    private final List<ModuleCategory> categoryOrder = new ArrayList<>();
    private final Set<String> expandedModules = new HashSet<>();
    private final Set<String> favoriteModules = new HashSet<>();
    private ModuleCategory selectedCategory;
    private String selectedModuleId;

    public ClientConfig(final ModuleManager modules) {
        this(modules, null, FabricLoader.getInstance().getConfigDir().resolve(AnarchyClient.MOD_ID + ".json"));
    }

    public ClientConfig(final ModuleManager modules, final Path path) {
        this(modules, null, path);
    }

    public ClientConfig(final ModuleManager modules, final FriendManager friends) {
        this(modules, friends, FabricLoader.getInstance().getConfigDir().resolve(AnarchyClient.MOD_ID + ".json"));
    }

    public ClientConfig(final ModuleManager modules, final FriendManager friends, final Path path) {
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
        if (!Files.exists(this.path)) {
            this.save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(this.path)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null) {
                return;
            }
            this.loadUi(root);
            JsonObject moduleRoot = root.getAsJsonObject("modules");
            if (moduleRoot == null) {
                return;
            }
            boolean migratedFriends = this.migrateLegacyFriends(moduleRoot);
            for (Module module : this.modules.all()) {
                JsonObject moduleJson = findObject(moduleRoot, module.id(), module.aliases());
                if (moduleJson == null) {
                    continue;
                }
                if (module instanceof ModuleConfigMigration migration) {
                    migration.migrateConfig(moduleJson);
                }
                JsonElement enabled = moduleJson.get("enabled");
                if (enabled != null && enabled.isJsonPrimitive()) {
                    module.enabled(enabled.getAsBoolean());
                }
                module.keybind().fromJson(moduleJson.getAsJsonObject("keybind"));
                JsonObject settings = moduleJson.getAsJsonObject("settings");
                if (settings == null) {
                    continue;
                }
                for (Setting<?> setting : module.settings()) {
                    setting.fromJson(findElement(settings, setting.id(), setting.aliases()));
                }
            }
            if (migratedFriends) {
                this.save();
            }
        } catch (RuntimeException | IOException exception) {
            LOGGER.warn("Failed to load AnarchyClient config from {}", this.path, exception);
            if (this.backupFailedConfig()) {
                this.save();
            }
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

    public void save() {
        JsonObject root = new JsonObject();
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
            Path parent = this.path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path tempPath = this.path.resolveSibling(this.path.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(tempPath)) {
                GSON.toJson(root, writer);
            }
            try {
                Files.move(tempPath, this.path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException exception) {
                Files.move(tempPath, this.path, StandardCopyOption.REPLACE_EXISTING);
            }
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

        if (ui.size() > 0) {
            root.add("ui", ui);
        }
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

    private static JsonObject findObject(final JsonObject json, final String key, final List<String> aliases) {
        JsonObject object = json.getAsJsonObject(key);
        if (object != null) {
            return object;
        }
        for (String alias : aliases) {
            object = json.getAsJsonObject(alias);
            if (object != null) {
                return object;
            }
        }
        return null;
    }

    private static JsonElement findElement(final JsonObject json, final String key, final List<String> aliases) {
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

    private boolean backupFailedConfig() {
        if (!Files.exists(this.path)) {
            return true;
        }
        Path backup = this.backupPath();
        try {
            Files.move(this.path, backup, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            LOGGER.info("Backed up unreadable AnarchyClient config to {}", backup);
            return true;
        } catch (IOException atomicException) {
            try {
                Files.move(this.path, backup, StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info("Backed up unreadable AnarchyClient config to {}", backup);
                return true;
            } catch (IOException moveException) {
                LOGGER.warn("Failed to back up unreadable AnarchyClient config at {}", this.path, moveException);
                return false;
            }
        }
    }

    private Path backupPath() {
        Path fileName = this.path.getFileName();
        String baseName = fileName == null ? AnarchyClient.MOD_ID : fileName.toString();
        String timestamp = LocalDateTime.now().format(BACKUP_TIMESTAMP_FORMAT);
        Path backup = this.path.resolveSibling(baseName + "." + timestamp + ".backup.json");
        int copy = 1;
        while (Files.exists(backup)) {
            backup = this.path.resolveSibling(baseName + "." + timestamp + "." + copy + ".backup.json");
            copy++;
        }
        return backup;
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

    public record CategoryWindowState(float x, float y) {
    }
}
