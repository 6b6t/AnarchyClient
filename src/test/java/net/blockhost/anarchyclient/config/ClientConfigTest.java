package net.blockhost.anarchyclient.config;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleBindAction;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.module.ModuleManager;
import net.blockhost.anarchyclient.friends.FriendManager;
import net.blockhost.anarchyclient.rivet.BackgroundDesign;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
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
        savedModule.keybind().key(75);
        savedModule.keybind().action(ModuleBindAction.HOLD);

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
        assertEquals(75, loadedModule.keybind().key());
        assertEquals(ModuleBindAction.HOLD, loadedModule.keybind().action());
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
        saved.favoriteModules(Set.of("auto_gg", "esp"));
        saved.selectedCategory(ModuleCategory.COMBAT);
        saved.selectedModuleId("auto_gg");
        saved.uiPreferences(new ClientConfig.UiPreferences(
                false,
                true,
                false,
                true,
                true,
                ClientConfig.GuiThemePreset.ROSE,
                BackgroundDesign.DEEP
        ));

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
        assertEquals(Set.of("auto_gg", "esp"), loaded.favoriteModules().orElseThrow());
        assertTrue(loaded.moduleFavorite("auto_gg"));
        assertEquals(ModuleCategory.COMBAT, loaded.selectedCategory().orElseThrow());
        assertEquals("auto_gg", loaded.selectedModuleId().orElseThrow());
        assertEquals(new ClientConfig.UiPreferences(
                false,
                true,
                false,
                true,
                true,
                ClientConfig.GuiThemePreset.ROSE,
                BackgroundDesign.DEEP
        ), loaded.uiPreferences());
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
                      "keybind": {
                        "key": 82,
                        "action": "smart"
                      },
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
        assertEquals(82, loadedModule.keybind().key());
        assertEquals(ModuleBindAction.SMART, loadedModule.keybind().action());
    }

    @Test
    void migratesLegacyModuleFriendSettings() throws Exception {
        Path configPath = this.tempDir.resolve("anarchyclient.json");
        Path friendsPath = this.tempDir.resolve("friends.txt");
        Files.writeString(configPath, """
                {
                  "modules": {
                    "esp": {
                      "settings": {
                        "friends": "Alex, Steve"
                      }
                    },
                    "kill_aura": {
                      "settings": {
                        "friends": "alex | EnderDash"
                      }
                    }
                  }
                }
                """);
        FriendManager friends = new FriendManager(friendsPath);

        new ClientConfig(new ModuleManager(), friends, configPath).load();

        assertTrue(friends.isFriend("alex"));
        assertTrue(friends.isFriend("steve"));
        assertTrue(friends.isFriend("enderdash"));
        assertEquals(List.of("Alex", "Steve", "EnderDash"), friends.friends());
    }

    @Test
    void backsUpMalformedConfigBeforeSavingFreshConfig() throws Exception {
        Path path = this.tempDir.resolve("anarchyclient.json");
        Files.writeString(path, "{ definitely not json");

        new ClientConfig(new ModuleManager(), path).load();

        assertTrue(Files.exists(path));
        assertTrue(Files.readString(path).contains("\"modules\""));
        Path backup = backupFiles(path).getFirst();
        assertEquals("{ definitely not json", Files.readString(backup));
    }

    @Test
    void backsUpInvalidConfigShapeBeforeSavingFreshConfig() throws Exception {
        Path path = this.tempDir.resolve("anarchyclient.json");
        Files.writeString(path, """
                {
                  "modules": []
                }
                """);

        new ClientConfig(new ModuleManager(), path).load();

        assertTrue(Files.exists(path));
        assertTrue(Files.readString(path).contains("\"modules\""));
        Path backup = backupFiles(path).getFirst();
        assertTrue(Files.readString(backup).contains("\"modules\": []"));
    }

    private static List<Path> backupFiles(final Path configPath) throws Exception {
        try (var files = Files.list(configPath.getParent())) {
            return files
                    .filter(path -> path.getFileName().toString().startsWith(configPath.getFileName() + "."))
                    .filter(path -> path.getFileName().toString().endsWith(".backup.json"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        }
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
                .addAllAliases(List.of("old_enabled_setting"))
                .build()));

        private AliasModule() {
            super("alias_module", "Alias Module", ModuleCategory.FUN, List.of("old_alias_module"));
        }
    }
}
