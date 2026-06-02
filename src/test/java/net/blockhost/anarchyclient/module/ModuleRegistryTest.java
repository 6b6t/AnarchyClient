package net.blockhost.anarchyclient.module;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleRegistryTest {

    @Test
    void registersExpandedDefaultModuleSet() {
        ModuleManager modules = new ModuleManager();

        ModuleRegistry.registerDefaults(modules);

        assertTrue(modules.all().size() >= 28);
        assertNotNull(modules.find("auto_weapon").orElseThrow());
        assertNotNull(modules.find("auto_armor").orElseThrow());
        assertNotNull(modules.find("block_esp").orElseThrow());
        assertNotNull(modules.find("storage_esp").orElseThrow());
        assertNotNull(modules.find("trajectories").orElseThrow());
        assertNotNull(modules.find("active_modules_hud").orElseThrow());
        assertNotNull(modules.find("middle_click_action").orElseThrow());
    }
}
