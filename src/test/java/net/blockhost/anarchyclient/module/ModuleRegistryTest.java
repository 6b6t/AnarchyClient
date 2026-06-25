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
        assertNotNull(modules.find("auto_log").orElseThrow());
        assertNotNull(modules.find("auto_mend").orElseThrow());
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
        assertNotNull(modules.find("auto_craft").orElseThrow());
        assertNotNull(modules.find("auto_rename").orElseThrow());
        assertNotNull(modules.find("auto_grind").orElseThrow());
        assertNotNull(modules.find("auto_enchant").orElseThrow());
        assertNotNull(modules.find("new_chunks").orElseThrow());
        assertNotNull(modules.find("chest_aura").orElseThrow());
        assertNotNull(modules.find("auto_farm").orElseThrow());
        assertNotNull(modules.find("tree_aura").orElseThrow());
        assertNotNull(modules.find("moss_bot").orElseThrow());
        assertNotNull(modules.find("lawn_bot").orElseThrow());
        assertNotNull(modules.find("aim_assist").orElseThrow());
        assertNotNull(modules.find("anti_bot").orElseThrow());
        assertNotNull(modules.find("anti_spawnpoint").orElseThrow());
        assertNotNull(modules.find("coord_logger").orElseThrow());
        assertNotNull(modules.find("gamemode_notifier").orElseThrow());
        assertNotNull(modules.find("notifier").orElseThrow());
        assertNotNull(modules.find("auto_soup").orElseThrow());
        assertNotNull(modules.find("no_jump_delay").orElseThrow());
        assertNotNull(modules.find("vehicle_one_hit").orElseThrow());
        assertNotNull(modules.find("block_in").orElseThrow());
        assertNotNull(modules.find("boost").orElseThrow());
        assertNotNull(modules.find("glide").orElseThrow());
        assertNotNull(modules.find("jetpack").orElseThrow());
        assertNotNull(modules.find("extra_elytra").orElseThrow());
        assertNotNull(modules.find("anti_vanish").orElseThrow());
        assertNotNull(modules.find("silent_disconnect").orElseThrow());
        assertNotNull(modules.find("skeleton_esp").orElseThrow());
        assertNotNull(modules.find("knockback_plus").orElseThrow());
        assertNotNull(modules.find("shield_bypass").orElseThrow());
        assertNotNull(modules.find("auto_pot").orElseThrow());
        assertNotNull(modules.find("auto_walk").orElseThrow());
        assertNotNull(modules.find("auto_jump").orElseThrow());
        assertNotNull(modules.find("sneak").orElseThrow());
        assertNotNull(modules.find("sound_blocker").orElseThrow());
        assertNotNull(modules.find("packet_logger").orElseThrow());
        assertNotNull(modules.find("auto_gap").orElseThrow());
        assertNotNull(modules.find("exp_thrower").orElseThrow());
        assertNotNull(modules.find("auto_replenish").orElseThrow());
        assertNotNull(modules.find("anti_void").orElseThrow());
        assertNotNull(modules.find("velocity").orElseThrow());
        assertNotNull(modules.find("void_esp").orElseThrow());
        assertNotNull(modules.find("breadcrumbs").orElseThrow());
        assertNotNull(modules.find("auto_clicker").orElseThrow());
        assertNotNull(modules.find("no_rotate_set").orElseThrow());
        assertNotNull(modules.find("no_slot_set").orElseThrow());
        assertNotNull(modules.find("no_swing").orElseThrow());
        assertNotNull(modules.find("chest_stealer").orElseThrow());
        assertNotNull(modules.find("chest_swap").orElseThrow());
        assertNotNull(modules.find("inventory_cleaner").orElseThrow());
        assertNotNull(modules.find("fast_place").orElseThrow());
        assertNotNull(modules.find("tnt_timer").orElseThrow());
        assertNotNull(modules.find("zoom").orElseThrow());
        assertNotNull(modules.find("auto_disable").orElseThrow());
        assertNotNull(modules.find("hole_esp").orElseThrow());
        assertNotNull(modules.find("light_overlay").orElseThrow());
        assertNotNull(modules.find("logout_spots").orElseThrow());
        assertNotNull(modules.find("color_signs").orElseThrow());
        assertNotNull(modules.find("server_observer").orElseThrow());
        assertNotNull(modules.find("auto_config").orElseThrow());
        assertNotNull(modules.find("anti_cheat_detect").orElseThrow());
        assertNotNull(modules.find("plugin_scanner").orElseThrow());
        assertNotNull(modules.find("flag_check").orElseThrow());
        assertNotNull(modules.find("staff_alert").orElseThrow());
        assertNotNull(modules.find("debug_recorder").orElseThrow());
        assertNotNull(modules.find("more_carry").orElseThrow());
        assertNotNull(modules.find("anti_hunger").orElseThrow());
        assertNotNull(modules.find("anti_reduced_debug_info").orElseThrow());
        assertNotNull(modules.find("ping_spoof").orElseThrow());
        assertNotNull(modules.find("fake_lag").orElseThrow());
        assertNotNull(modules.find("portal_menu").orElseThrow());
        assertNotNull(modules.find("multi_actions").orElseThrow());
        assertNotNull(modules.find("name_collector").orElseThrow());
    }
}
