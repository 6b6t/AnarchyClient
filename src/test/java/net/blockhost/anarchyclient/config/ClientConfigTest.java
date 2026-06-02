package net.blockhost.anarchyclient.config;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.module.ModuleManager;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

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

        new ClientConfig(savedModules, path).save();

        ModuleManager loadedModules = new ModuleManager();
        ConfigModule loadedModule = new ConfigModule();
        loadedModules.register(loadedModule);
        new ClientConfig(loadedModules, path).load();

        assertTrue(loadedModule.enabled());
        assertEquals(false, loadedModule.enabledSetting.value());
        assertEquals(7.5, loadedModule.numberSetting.value());
        assertEquals("saved", loadedModule.stringSetting.value());
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

        private ConfigModule() {
            super("config_module", "Config Module", ModuleCategory.MISC);
        }
    }
}
