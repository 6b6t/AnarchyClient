package net.blockhost.anarchyclient.gametest;

import java.util.LinkedHashMap;
import java.util.Map;

/** Every client game test scenario, in run order. */
final class GameScenarios {

    private GameScenarios() {
    }

    static Map<String, GameScenario> all() {
        Map<String, GameScenario> scenarios = new LinkedHashMap<>();
        scenarios.put("piston_crystal", new PistonCrystalGameTest());
        scenarios.put("piston_push", new PistonPushGameTest());
        scenarios.put("packet_mine", new PacketMineGameTest());
        scenarios.put("auto_smith", new AutoSmithGameTest());
        scenarios.put("anti_quit", new AntiQuitGameTest());
        scenarios.put("hole_snap", new HoleSnapGameTest());
        scenarios.put("mainhand_totem", new MainhandTotemGameTest());
        scenarios.put("crystal_blocker", new CrystalBlockerGameTest());
        scenarios.put("chat_filter", MiscModuleScenarios::chatFilter);
        scenarios.put("anti_cyrillic", MiscModuleScenarios::antiCyrillic);
        scenarios.put("anti_aim", MiscModuleScenarios::antiAim);
        scenarios.put("kill_effects", MiscModuleScenarios::killEffects);
        scenarios.put("heaven", MiscModuleScenarios::heaven);
        scenarios.put("portal_god_mode", MiscModuleScenarios::portalGodMode);
        scenarios.put("auto_tame", WorldModuleScenarios::autoTame);
        scenarios.put("auto_trade", WorldModuleScenarios::autoTrade);
        scenarios.put("inventory_sorter", WorldModuleScenarios::inventorySorter);
        scenarios.put("item_saver", WorldModuleScenarios::itemSaver);
        scenarios.put("arrow_swap", WorldModuleScenarios::arrowSwap);
        scenarios.put("avoid_hazards_void", WorldModuleScenarios::avoidHazardsVoid);
        scenarios.put("avoid_hazards_lava", WorldModuleScenarios::avoidHazardsLava);
        scenarios.put("packet_fly_stays_connected", PacketFlyScenario::run);
        scenarios.put("block_in", MovementModuleScenarios::blockIn);
        scenarios.put("burrow", MovementModuleScenarios::burrow);
        scenarios.put("crystal_aura", CombatModuleScenarios::crystalAura);
        scenarios.put("anchor_aura", CombatModuleScenarios::anchorAura);
        scenarios.put("kill_aura", CombatModuleScenarios::killAura);
        scenarios.put("auto_totem", CombatModuleScenarios::autoTotem);
        scenarios.put("surround", CombatModuleScenarios::surround);
        scenarios.put("auto_trap", CombatModuleScenarios::autoTrap);
        scenarios.put("auto_web", CombatModuleScenarios::autoWeb);
        scenarios.put("hole_filler", CombatModuleScenarios::holeFiller);
        scenarios.put("auto_city", CombatModuleScenarios::autoCity);
        scenarios.put("auto_armor", PlayerModuleScenarios::autoArmor);
        scenarios.put("no_fall", PlayerModuleScenarios::noFall);
        scenarios.put("smart_eat", PlayerModuleScenarios::smartEat);
        scenarios.put("chest_stealer", PlayerModuleScenarios::chestStealer);
        scenarios.put("auto_tool", PlayerModuleScenarios::autoTool);
        scenarios.put("scaffold", PlayerModuleScenarios::scaffold);
        scenarios.put("jesus", PlayerModuleScenarios::jesus);
        scenarios.put("nuker", PlayerModuleScenarios::nuker);
        scenarios.put("piston_crystal_repeats", EdgeCaseScenarios::pistonCrystalRepeats);
        scenarios.put("anti_aim_with_surround", EdgeCaseScenarios::antiAimWithSurround);
        scenarios.put("anti_aim_with_kill_aura", EdgeCaseScenarios::antiAimWithKillAura);
        scenarios.put("arrow_swap_skips_active", EdgeCaseScenarios::arrowSwapSkipsActiveEffects);
        scenarios.put("item_saver_blocks_attacks", EdgeCaseScenarios::itemSaverBlocksAttacks);
        scenarios.put("crawl_trap", EdgeCaseScenarios::crawlTrap);
        scenarios.put("crystal_aura_id_predict", EdgeCaseScenarios::crystalAuraIdPredict);
        scenarios.put("skin_flicker_restores", EdgeCaseScenarios::skinFlickerRestores);
        scenarios.put("hole_snap_directional", EdgeCaseScenarios::holeSnapDirectional);
        scenarios.put("announcer", EdgeCaseScenarios::announcer);
        scenarios.put("spiral", EdgeCaseScenarios::spiral);
        scenarios.put("auto_firework", EdgeCaseScenarios::autoFirework);
        scenarios.put("skybox", SmokeScenarios::skybox);
        scenarios.put("smoke_render_modules", SmokeScenarios::renderModules);
        scenarios.put("smoke_other_modules", SmokeScenarios::otherModules);
        return scenarios;
    }
}
