package net.blockhost.anarchyclient.module.impl;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdditionalModulesTest {

    @Test
    void emptyItemParserInputDoesNotTouchRegistry() {
        assertTrue(ItemScan.parseItems("  ").isEmpty());
    }

    @Test
    void parsesAutoLoginCommandMap() {
        assertEquals(Map.of(
                        "example.org", "/login hunter2",
                        "*", "/register hunter2 hunter2"
                ),
                AutoLoginModule.parseCommands("example.org=/login hunter2; *=/register hunter2 hunter2"));
    }

    @Test
    void classifiesOversizedVelocityPacketsAsSuspicious() {
        assertEquals(
                AntiCrashModule.SuspiciousPacket.ENTITY_MOTION,
                AntiCrashModule.suspiciousPacket(
                        new ClientboundSetEntityMotionPacket(1, new Vec3(0, 2000, 0)),
                        30_000_000,
                        1000,
                        100_000,
                        128,
                        4096
                )
        );
    }

    @Test
    void acceptsFiniteVectorsInsideLimit() {
        assertFalse(AntiCrashModule.outsideLimit(new Vec3(10, 20, -30), 100));
        assertTrue(AntiCrashModule.outsideLimit(new Vec3(Double.NaN, 20, -30), 100));
        assertTrue(AntiCrashModule.outsideLimit(new Vec3(10, 101, -30), 100));
    }

    @Test
    void nullFluidStatesAreNotFlowingFluidSignals() {
        assertFalse(NewChunksModule.isFlowingFluid(null));
    }

    @Test
    void hitChanceCanForceAttacks() {
        assertTrue(KillAuraModule.shouldAttack(new java.util.Random(0), 100));
    }

    @Test
    void antiSpawnpointCancelsOnlySpawnChangingUses() {
        assertTrue(AntiSpawnpointModule.shouldCancelUse(true, false, true, false));
        assertTrue(AntiSpawnpointModule.shouldCancelUse(false, true, false, true));
        assertFalse(AntiSpawnpointModule.shouldCancelUse(false, true, true, false));
        assertFalse(AntiSpawnpointModule.shouldCancelUse(true, false, false, true));
    }

    @Test
    void coordLoggerFormatsKnownEventsAndPositions() {
        assertEquals("Wither spawned", CoordLoggerModule.worldEventName(1023));
        assertEquals("End portal opened", CoordLoggerModule.worldEventName(1038));
        assertEquals("Global event 9", CoordLoggerModule.worldEventName(9));
        assertEquals("1.0, 65.0, -3.0", CoordLoggerModule.formatPosition(new BlockPos(1, 65, -3)));
    }

    @Test
    void autoSoupRecognizesSoupStacksAndHands() {
        assertTrue(AutoSoupModule.isSoupItemId("mushroom_stew"));
        assertTrue(AutoSoupModule.isSoupItemId("rabbit_stew"));
        assertTrue(AutoSoupModule.isSoupItemId("beetroot_soup"));
        assertFalse(AutoSoupModule.isSoupItemId("bowl"));
    }

    @Test
    void blockInTargetsExpectedShellAroundPlayer() {
        BlockPos base = new BlockPos(10, 64, -5);
        assertIterableEquals(List.of(
                new BlockPos(10, 63, -5),
                new BlockPos(11, 64, -5),
                new BlockPos(9, 64, -5),
                new BlockPos(10, 64, -4),
                new BlockPos(10, 64, -6),
                new BlockPos(11, 65, -5),
                new BlockPos(9, 65, -5),
                new BlockPos(10, 65, -4),
                new BlockPos(10, 65, -6),
                new BlockPos(10, 66, -5)
        ), BlockInModule.targetPositions(base));
    }

    @Test
    void boostUsesHorizontalLookDirectionOnly() {
        assertEquals(new Vec3(0.0, 0.0, 2.0), BoostModule.boostVector(new Vec3(0.0, -0.8, 1.0), 2.0));
        assertEquals(Vec3.ZERO, BoostModule.boostVector(new Vec3(0.0, 1.0, 0.0), 2.0));
    }

    @Test
    void glideCapsOnlyExcessiveDownwardVelocity() {
        assertEquals(-0.125, GlideModule.cappedFallVelocity(-0.5, 0.125));
        assertEquals(-0.05, GlideModule.cappedFallVelocity(-0.05, 0.125));
    }

    @Test
    void antiVanishIgnoresNormalLeaveMessages() {
        UUID vanished = UUID.randomUUID();
        Map<UUID, String> previous = new HashMap<>();
        previous.put(vanished, "AdminName");

        assertTrue(AntiVanishModule.vanishedPlayers(previous, Map.of(), List.of("AdminName left the game"), true).isEmpty());
        assertEquals(List.of("AdminName"), AntiVanishModule.vanishedPlayers(previous, Map.of(), List.of(), true));
    }

    @Test
    void autoPotUsesHealingOnlyAtOrBelowThreshold() {
        assertTrue(AutoPotModule.shouldUseHealing(10.0F, 10.0));
        assertTrue(AutoPotModule.shouldUseHealing(9.5F, 10.0));
        assertFalse(AutoPotModule.shouldUseHealing(10.5F, 10.0));
    }
}
