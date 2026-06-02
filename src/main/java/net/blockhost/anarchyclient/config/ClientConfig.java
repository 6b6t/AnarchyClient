package net.blockhost.anarchyclient.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.module.Module;
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

public final class ClientConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientConfig.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final ModuleManager modules;
    private final Path path;

    public ClientConfig(final ModuleManager modules) {
        this(modules, FabricLoader.getInstance().getConfigDir().resolve(AnarchyClient.MOD_ID + ".json"));
    }

    public ClientConfig(final ModuleManager modules, final Path path) {
        this.modules = modules;
        this.path = path;
    }

    public void load() {
        if (!Files.exists(this.path)) {
            this.save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(this.path)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null) {
                return;
            }
            JsonObject moduleRoot = root.getAsJsonObject("modules");
            if (moduleRoot == null) {
                return;
            }
            for (Module module : this.modules.all()) {
                JsonObject moduleJson = moduleRoot.getAsJsonObject(module.id());
                if (moduleJson == null) {
                    continue;
                }
                JsonElement enabled = moduleJson.get("enabled");
                if (enabled != null && enabled.isJsonPrimitive()) {
                    module.enabled(enabled.getAsBoolean());
                }
                JsonObject settings = moduleJson.getAsJsonObject("settings");
                if (settings == null) {
                    continue;
                }
                for (Setting<?> setting : module.settings()) {
                    setting.fromJson(settings.get(setting.id()));
                }
            }
        } catch (RuntimeException | IOException exception) {
            LOGGER.warn("Failed to load AnarchyClient config from {}", this.path, exception);
            this.save();
        }
    }

    public void save() {
        JsonObject root = new JsonObject();
        JsonObject moduleRoot = new JsonObject();
        root.add("modules", moduleRoot);

        for (Module module : this.modules.all()) {
            JsonObject moduleJson = new JsonObject();
            JsonObject settings = new JsonObject();
            moduleJson.addProperty("enabled", module.enabled());
            moduleJson.add("settings", settings);
            for (Setting<?> setting : module.settings()) {
                settings.add(setting.id(), setting.toJson());
            }
            moduleRoot.add(module.id(), moduleJson);
        }

        try {
            Files.createDirectories(this.path.getParent());
            try (Writer writer = Files.newBufferedWriter(this.path)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException exception) {
            LOGGER.warn("Failed to save AnarchyClient config to {}", this.path, exception);
        }
    }
}
