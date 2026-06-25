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
        assertNotNull(modules.find("anti_crash").orElseThrow());
        assertNotNull(modules.find("sound_locator").orElseThrow());
        assertNotNull(modules.find("auto_drop").orElseThrow());
        assertNotNull(modules.find("radar_hud").orElseThrow());
        assertNotNull(modules.find("auto_login").orElseThrow());
        assertNotNull(modules.find("auto_extinguish").orElseThrow());
        assertNotNull(modules.find("new_chunks").orElseThrow());
        assertNotNull(modules.find("chest_aura").orElseThrow());
        assertNotNull(modules.find("aim_assist").orElseThrow());
        assertNotNull(modules.find("anti_spawnpoint").orElseThrow());
        assertNotNull(modules.find("coord_logger").orElseThrow());
        assertNotNull(modules.find("gamemode_notifier").orElseThrow());
        assertNotNull(modules.find("auto_soup").orElseThrow());
        assertNotNull(modules.find("no_jump_delay").orElseThrow());
        assertNotNull(modules.find("vehicle_one_hit").orElseThrow());
        assertNotNull(modules.find("block_in").orElseThrow());
    }
}
