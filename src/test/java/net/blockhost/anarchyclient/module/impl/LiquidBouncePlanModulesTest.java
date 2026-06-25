package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.render.RenderSuppression;
import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MinecraftBootstrapExtension.class)
class LiquidBouncePlanModulesTest {

    @Test
    void renderSuppressionTracksOwnersIndependently() {
        RenderSuppression.clear();

        RenderSuppression.enable("a", RenderSuppression.Kind.HURT_CAMERA);
        RenderSuppression.enable("b", RenderSuppression.Kind.VIEW_BOB);

        assertTrue(RenderSuppression.suppresses(RenderSuppression.Kind.HURT_CAMERA));
        assertTrue(RenderSuppression.suppresses(RenderSuppression.Kind.VIEW_BOB));
        RenderSuppression.disable("a");
        assertFalse(RenderSuppression.suppresses(RenderSuppression.Kind.HURT_CAMERA));
        assertTrue(RenderSuppression.suppresses(RenderSuppression.Kind.VIEW_BOB));
        RenderSuppression.clear();
    }

    @Test
    void visualAndInventoryHelpersReturnStableValues() {
        assertEquals(75, BetterInventoryModule.durabilityPercent(100, 25));
        assertEquals(100, BetterInventoryModule.durabilityPercent(0, 25));
        assertEquals(90.0F, FreeLookModule.clampedPitch(120.0F));
    }

    @Test
    void hazardAndTeamParsersStayConservative() {
        assertTrue(AvoidHazardsModule.isHazard(Blocks.LAVA));
        assertFalse(AvoidHazardsModule.isHazard(Blocks.COBWEB));
        assertTrue(TeamsModule.samePrefix("[team]Alex", "[team]Steve"));
        assertFalse(TeamsModule.samePrefix("Alex", "Steve"));
    }

    @Test
    void exploitAndZoneHelpersHandleInvalidInput() {
        assertEquals("null packet", AntiExploitModule.exploitReason(null, 1024, 64));
        assertNull(AntiExploitModule.exploitReason(new net.minecraft.network.protocol.game.ServerboundSwingPacket(net.minecraft.world.InteractionHand.MAIN_HAND),
                1024, 64));
        assertEquals(List.of(new net.minecraft.core.BlockPos(1, 64, -2), new net.minecraft.core.BlockPos(5, 70, 5)),
                ProtectionZonesModule.parseZones("1,64,-2; nope; 5,70,5"));
    }

    @Test
    void autoBuffOnlyRequestsMissingEnabledEffects() {
        assertTrue(AutoBuffModule.shouldBuff(true, false));
        assertFalse(AutoBuffModule.shouldBuff(false, false));
        assertFalse(AutoBuffModule.shouldBuff(true, true));
    }
}
