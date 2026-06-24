package net.blockhost.anarchyclient.config;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.module.ModuleManager;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientConfigTest {

    @TempDir
    private Path tempDir;

    @Test
    void savesAndLoadsModuleState() {
        Path path = this.tempDir.resolve("anarchyclient.json");
        ModuleManager savedModules = new ModuleManager();
        ConfigModule savedModule = new ConfigModule();
        savedModules.register(savedModule);
        savedModule.enabled(true);
        savedModule.enabledSetting.value(false);
        savedModule.numberSetting.value(7.5);
        savedModule.stringSetting.value("saved");
        savedModule.selectSetting.value("Second");

        new ClientConfig(savedModules, path).save();

        ModuleManager loadedModules = new ModuleManager();
        ConfigModule loadedModule = new ConfigModule();
        loadedModules.register(loadedModule);
        new ClientConfig(loadedModules, path).load();

        assertTrue(loadedModule.enabled());
        assertEquals(false, loadedModule.enabledSetting.value());
        assertEquals(7.5, loadedModule.numberSetting.value());
        assertEquals("saved", loadedModule.stringSetting.value());
        assertEquals("Second", loadedModule.selectSetting.value());
    }

    @Test
    void savesAndLoadsUiState() {
        Path path = this.tempDir.resolve("anarchyclient.json");
        ClientConfig saved = new ClientConfig(new ModuleManager(), path);
        saved.categoryWindow(ModuleCategory.COMBAT, 24.5F, 48.25F);
        saved.categoryWindow(ModuleCategory.FUN, 96F, 128F);
        saved.categoryOrder(List.of(
                ModuleCategory.FUN,
                ModuleCategory.HUD,
                ModuleCategory.PLAYER,
                ModuleCategory.MOVEMENT,
                ModuleCategory.RENDER,
                ModuleCategory.COMBAT
        ));
        saved.expandedModules(Set.of("auto_gg", "nyan_cat_gif_spammer"));

        saved.save();

        ClientConfig loaded = new ClientConfig(new ModuleManager(), path);
        loaded.load();

        assertEquals(new ClientConfig.CategoryWindowState(24.5F, 48.25F), loaded.categoryWindow(ModuleCategory.COMBAT).orElseThrow());
        assertEquals(new ClientConfig.CategoryWindowState(96F, 128F), loaded.categoryWindow(ModuleCategory.FUN).orElseThrow());
        assertEquals(List.of(
                ModuleCategory.FUN,
                ModuleCategory.HUD,
                ModuleCategory.PLAYER,
                ModuleCategory.MOVEMENT,
                ModuleCategory.RENDER,
                ModuleCategory.COMBAT,
                ModuleCategory.WORLD,
                ModuleCategory.MISC
        ), loaded.categoryOrder().orElseThrow());
        assertEquals(Set.of("auto_gg", "nyan_cat_gif_spammer"), loaded.expandedModules().orElseThrow());
    }

    @Test
    void defaultsToNoExpandedModules() {
        Path path = this.tempDir.resolve("anarchyclient.json");
        ClientConfig config = new ClientConfig(new ModuleManager(), path);

        config.load();

        assertEquals(Set.of(), config.expandedModules().orElseThrow());

        ClientConfig reloaded = new ClientConfig(new ModuleManager(), path);
        reloaded.load();

        assertEquals(Set.of(), reloaded.expandedModules().orElseThrow());
    }

    @Test
    void loadsModuleAndSettingAliases() throws Exception {
        Path path = this.tempDir.resolve("anarchyclient.json");
        Files.writeString(path, """
                {
                  "modules": {
                    "old_alias_module": {
                      "enabled": true,
                      "settings": {
                        "old_enabled_setting": false
                      }
                    }
                  }
                }
                """);

        ModuleManager loadedModules = new ModuleManager();
        AliasModule loadedModule = new AliasModule();
        loadedModules.register(loadedModule);
        new ClientConfig(loadedModules, path).load();

        assertTrue(loadedModule.enabled());
        assertEquals(false, loadedModule.enabledSetting.value());
    }

    private static final class ConfigModule extends Module {

        private final BooleanSetting enabledSetting = this.setting(BooleanSetting.from(BooleanSetting.builder()
                .id("enabled_setting")
                .name("Enabled Setting")
                .defaultValue(true)
                .build()));
        private final NumberSetting numberSetting = this.setting(NumberSetting.from(NumberSetting.builder()
                .id("number_setting")
                .name("Number Setting")
                .defaultValue(1.0)
                .min(0.0)
                .max(10.0)
                .step(0.5)
                .build()));
        private final StringSetting stringSetting = this.setting(StringSetting.from(StringSetting.builder()
                .id("string_setting")
                .name("String Setting")
                .defaultValue("default")
                .build()));
        private final SelectSetting selectSetting = this.setting(SelectSetting.from(SelectSetting.builder()
                .id("select_setting")
                .name("Select Setting")
                .defaultValue("First")
                .addAllOptions(List.of("First", "Second"))
                .build()));

        private ConfigModule() {
            super("config_module", "Config Module", ModuleCategory.FUN);
        }
    }

    private static final class AliasModule extends Module {

        private final BooleanSetting enabledSetting = this.setting(BooleanSetting.from(BooleanSetting.builder()
                .id("enabled_setting")
                .name("Enabled Setting")
                .defaultValue(true)
                .aliases(List.of("old_enabled_setting"))
                .build()));

        private AliasModule() {
            super("alias_module", "Alias Module", ModuleCategory.FUN, List.of("old_alias_module"));
        }
    }
}
