package net.blockhost.anarchyclient.profile;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.module.ModuleManager;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

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

        assertEquals(2, manager.apply("main", modules));
        assertTrue(module.enabled());
        assertEquals(false, module.flag.value());

        ProfileManager reloaded = new ProfileManager(this.tempDir.resolve("profiles.json"));
        reloaded.load();
        assertTrue(reloaded.find("main").isPresent());
        assertEquals(1, reloaded.summaries().size());
    }

    private static final class TestModule extends Module {

        private final BooleanSetting flag = this.setting(BooleanSetting.from(BooleanSetting.builder()
                .id("flag")
                .name("Flag")
                .defaultValue(true)
                .build()));

        private TestModule() {
            super("test_module", "Test Module", ModuleCategory.MISC);
        }
    }
}
