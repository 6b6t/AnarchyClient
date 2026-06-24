package net.blockhost.anarchyclient.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.blockhost.anarchyclient.AnarchyClient;
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

    private final ModuleManager modules;
    private final Path path;
    private final Map<ModuleCategory, CategoryWindowState> categoryWindows = new EnumMap<>(ModuleCategory.class);
    private final List<ModuleCategory> categoryOrder = new ArrayList<>();
    private final Set<String> expandedModules = new HashSet<>();

    public ClientConfig(final ModuleManager modules) {
        this(modules, FabricLoader.getInstance().getConfigDir().resolve(AnarchyClient.MOD_ID + ".json"));
    }

    public ClientConfig(final ModuleManager modules, final Path path) {
        this.modules = modules;
        this.path = path;
    }

    public void load() {
        this.categoryWindows.clear();
        this.categoryOrder.clear();
        this.expandedModules.clear();
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
        } catch (RuntimeException | IOException exception) {
            LOGGER.warn("Failed to load AnarchyClient config from {}", this.path, exception);
            this.save();
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

        if (!this.categoryOrder.isEmpty()) {
            JsonArray categories = new JsonArray();
            this.categoryOrder.forEach(category -> categories.add(categoryKey(category)));
            ui.add("categoryOrder", categories);
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
