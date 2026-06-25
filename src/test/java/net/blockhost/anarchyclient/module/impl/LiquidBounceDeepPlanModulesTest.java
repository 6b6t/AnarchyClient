package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.DebugValueRegistry;
import net.blockhost.anarchyclient.module.ModuleManager;
import net.blockhost.anarchyclient.module.ModuleMode;
import net.blockhost.anarchyclient.module.ModuleRegistry;
import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.blockhost.anarchyclient.timer.TimerBalanceService;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MinecraftBootstrapExtension.class)
class LiquidBounceDeepPlanModulesTest {

    @Test
    void registryIncludesDeepPlanModules() {
        ModuleManager modules = new ModuleManager();

        ModuleRegistry.registerDefaults(modules);

        assertNotNull(modules.find("payload_inspector").orElseThrow());
        assertNotNull(modules.find("server_fingerprint_hud").orElseThrow());
        assertNotNull(modules.find("packet_debug_hud").orElseThrow());
        assertNotNull(modules.find("backtrack").orElseThrow());
        assertNotNull(modules.find("tick_base").orElseThrow());
        assertNotNull(modules.find("timer_range").orElseThrow());
        assertNotNull(modules.find("target_strafe").orElseThrow());
        assertNotNull(modules.find("auto_queue").orElseThrow());
        assertNotNull(modules.find("auto_shop").orElseThrow());
        assertNotNull(modules.find("chest_cleaner").orElseThrow());
        assertNotNull(modules.find("crosshair").orElseThrow());
        assertNotNull(modules.find("silent_hotbar").orElseThrow());
    }

    @Test
    void helperPrimitivesStayStable() {
        ModuleMode mode = new ModuleMode("low_hop", "Low Hop");
        assertEquals("low_hop", mode.id());
        assertEquals(0.35, SpeedModule.modeMultiplier("Collision Safe", true, true));
        assertTrue(AutoQueueModule.matches("You died! Click to play again", "you died", false));
        assertFalse(AutoQueueModule.matches("You died! Click to play again", "you died", true));
    }

    @Test
    void debugValuesAndTimerBalanceAreScopedByOwner() {
        DebugValueRegistry.clearAll();
        TimerBalanceService.clearAll();

        DebugValueRegistry.put("tick_base", "balance", "10");
        double balance = TimerBalanceService.tick("tick_base", 20.0, 1.0, 1.5);

        assertEquals("10", DebugValueRegistry.value("tick_base", "balance").orElseThrow());
        assertEquals(19.5, balance, 1.0E-9);
        assertEquals(Map.of("balance", "10"), DebugValueRegistry.snapshot().get("tick_base"));
    }

    @Test
    void inventoryFacetHelpersClassifyCommonItems() {
        assertFalse(InventoryCleanerModule.isRanged(ItemStack.EMPTY));
        assertFalse(InventoryCleanerModule.isPotion(ItemStack.EMPTY));
        assertFalse(InventoryCleanerModule.isThrowable(ItemStack.EMPTY));
    }

    @Test
    void backtrackPacketClassifierIsConservative() {
        assertFalse(BacktrackModule.isEntityMovementPacket(new ServerboundSwingPacket(InteractionHand.MAIN_HAND)));
    }

    @Test
    void chestCleanerFindsOnlyConfiguredJunk() {
        assertEquals(-1, ChestCleanerModule.firstJunkSlot(java.util.List.of(ItemStack.EMPTY), 1, Set.of()));
    }

    @Test
    void swordBlockAcceptsWeaponsAndShields() {
        assertFalse(SwordBlockModule.canBlock(ItemStack.EMPTY));
    }
}
