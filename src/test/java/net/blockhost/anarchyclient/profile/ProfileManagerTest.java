package net.blockhost.anarchyclient.profile;

import com.google.gson.JsonObject;
import net.blockhost.anarchyclient.config.ModuleConfigMigration;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.module.ModuleManager;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void capturesPersistsAndAppliesProfiles() {
        ModuleManager modules = new ModuleManager();
        TestModule module = new TestModule();
        modules.register(module);
        module.enabled(true);
        module.flag.value(false);

        ProfileManager manager = new ProfileManager(this.tempDir.resolve("profiles.json"));
        manager.capture("main", modules);
        module.enabled(false);
        module.flag.value(true);

        assertEquals(3, manager.apply("main", modules));
        assertTrue(module.enabled());
        assertEquals(false, module.flag.value());

        ProfileManager reloaded = new ProfileManager(this.tempDir.resolve("profiles.json"));
        reloaded.load();
        assertTrue(reloaded.find("main").isPresent());
        assertEquals(1, reloaded.summaries().size());
    }

    @Test
    void applySkipsBadValuesAndLoadsSettingsBeforeEnabling() throws Exception {
        Path path = this.tempDir.resolve("profiles.json");
        Files.writeString(path, """
                {
                  "profiles": {
                    "pvp": {
                      "name": "pvp",
                      "modules": {"test_module": true},
                      "settings": {"test_module": {"range": "far", "flag": false}}
                    }
                  }
                }
                """);
        ModuleManager modules = new ModuleManager();
        TestModule module = new TestModule();
        modules.register(module);
        ProfileManager manager = new ProfileManager(path);
        manager.load();

        assertEquals(2, manager.apply("pvp", modules));
        assertTrue(module.enabled());
        assertEquals(false, module.flag.value());
        assertEquals(false, module.flagOnEnable);
        assertEquals(3.0, module.range.value());
    }

    @Test
    void doesNotOverwriteUnreadableProfiles() throws Exception {
        Path path = this.tempDir.resolve("profiles.json");
        Files.writeString(path, "{ broken");
        ModuleManager modules = new ModuleManager();
        modules.register(new TestModule());

        ProfileManager manager = new ProfileManager(path);
        manager.load();
        manager.capture("fresh", modules);

        try (Stream<Path> files = Files.list(this.tempDir)) {
            Path backup = files.filter(file -> file.getFileName().toString().endsWith(".backup.json"))
                    .findFirst()
                    .orElseThrow();
            assertEquals("{ broken", Files.readString(backup));
        }
        ProfileManager reloaded = new ProfileManager(path);
        reloaded.load();
        assertTrue(reloaded.find("fresh").isPresent());
    }

    @Test
    void migratesOldDefaultsOnlyInProfilesFromOlderBuilds() throws Exception {
        Path path = this.tempDir.resolve("profiles.json");
        Files.writeString(path, """
                {
                  "profiles": {
                    "old": {"name": "old", "settings": {"migrating_module": {"range": 2.0}}},
                    "new": {"name": "new", "version": 2, "settings": {"migrating_module": {"range": 2.0}}},
                    "dev": {"name": "dev", "updatedAt": "2026-11-18T10:00:00Z",
                            "settings": {"migrating_module": {"range": 2.0}}}
                  }
                }
                """);
        ModuleManager modules = new ModuleManager();
        MigratingModule module = new MigratingModule();
        modules.register(module);
        ProfileManager manager = new ProfileManager(path);
        manager.load();

        manager.apply("old", modules);
        assertEquals(4.0, module.range.value());

        manager.apply("new", modules);
        assertEquals(2.0, module.range.value());

        // Saved without a version, but after the defaults changed, so it already holds the new values.
        module.range.value(5.0);
        manager.apply("dev", modules);
        assertEquals(2.0, module.range.value());
    }

    private static final class MigratingModule extends Module implements ModuleConfigMigration {

        private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
                .id("range")
                .name("Range")
                .defaultValue(4.0)
                .min(0.0)
                .max(6.0)
                .step(0.5)
                .build()));

        private MigratingModule() {
            super("migrating_module", "Migrating Module", ModuleCategory.MISC, "Test module.");
        }

        @Override
        public void migrateConfig(final JsonObject moduleJson, final int savedVersion) {
            if (savedVersion < VANILLA_DAMAGE_DEFAULTS) {
                ModuleConfigMigration.upgradeDefault(moduleJson, "range", 2.0, 4.0);
            }
        }
    }

    private static final class TestModule extends Module {

        private final BooleanSetting flag = this.setting(BooleanSetting.from(BooleanSetting.builder()
                .id("flag")
                .name("Flag")
                .defaultValue(true)
                .build()));
        private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
                .id("range")
                .name("Range")
                .defaultValue(3.0)
                .min(0.0)
                .max(6.0)
                .step(0.5)
                .build()));
        private Boolean flagOnEnable;

        private TestModule() {
            super("test_module", "Test Module", ModuleCategory.MISC, "Test module.");
        }

        @Override
        protected void onEnable() {
            this.flagOnEnable = this.flag.value();
        }
    }
}
