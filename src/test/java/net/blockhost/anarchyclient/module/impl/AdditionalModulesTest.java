package net.blockhost.anarchyclient.module.impl;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
