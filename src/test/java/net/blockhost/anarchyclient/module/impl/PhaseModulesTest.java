package net.blockhost.anarchyclient.module.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhaseModulesTest {

    @Test
    void hudAndRenderUtilitiesFormatStableValues() {
        assertEquals("[N] E S W", CompassHudModule.compassLine(Direction.NORTH));
        assertEquals(270.0F, CompassHudModule.wrapYaw(-90.0F));
        assertEquals(90.0F, RenderingModule.clampPitch(120.0F));
        assertEquals("1:05", PotionTimersHudModule.formatTicks(20 * 65));
        assertEquals("empty", ArmorHudModule.describe(ItemStack.EMPTY));
    }

    @Test
    void packetKickRejectsInvalidMovementPackets() {
        assertTrue(AntiPacketKickModule.isInvalidMovement(
                new ServerboundMovePlayerPacket.Pos(Double.NaN, 64.0, 0.0, true, false),
                30_000_000.0
        ));
        assertTrue(AntiPacketKickModule.isInvalidMovement(
                new ServerboundMovePlayerPacket.Rot(0.0F, 120.0F, true, false),
                30_000_000.0
        ));
        assertFalse(AntiPacketKickModule.isInvalidMovement(
                new ServerboundMovePlayerPacket.Pos(0.0, 64.0, 0.0, true, false),
                30_000_000.0
        ));
    }

    @Test
    void packetCancellerRequiresEnabledNonBlankFilter() {
        assertFalse(PacketCancellerModule.shouldCancel(null, true, ""));
        assertTrue(PacketCancellerModule.shouldCancel(
                new ServerboundMovePlayerPacket.StatusOnly(true, false),
                true,
                "move_player"
        ));
    }

    @Test
    void highwayPlannerBuildsFloorAndRailsAhead() {
        assertEquals(List.of(
                new BlockPos(0, 63, 1),
                new BlockPos(-1, 63, 1),
                new BlockPos(1, 63, 1)
        ), HighwayBuilderModule.plan(new BlockPos(0, 64, 0), Direction.SOUTH, 1, true));
    }

    @Test
    void combatScoringPolicyRejectsBadTrades() {
        SelfDamagePolicy policy = new SelfDamagePolicy(8.0, 4.0, 1.2);

        assertTrue(policy.allows(2.0, 5.0));
        assertFalse(policy.allows(9.0, 12.0));
        assertFalse(policy.allows(4.0, 3.0));
        assertFalse(policy.allows(5.0, 5.5));
    }

    @Test
    void autoSignTrimsPacketLines() {
        assertEquals("", AutoSignModule.trim(null));
        assertEquals(384, AutoSignModule.trim("x".repeat(400)).length());
    }

    @Test
    void arrowDodgeDetectsApproachingProjectiles() {
        assertTrue(ArrowDodgeModule.approaching(new Vec3(0.0, 0.0, 0.0), new Vec3(1.0, 0.0, 0.0), new Vec3(4.0, 0.0, 0.0)));
        assertFalse(ArrowDodgeModule.approaching(new Vec3(0.0, 0.0, 0.0), new Vec3(-1.0, 0.0, 0.0), new Vec3(4.0, 0.0, 0.0)));
    }
}
