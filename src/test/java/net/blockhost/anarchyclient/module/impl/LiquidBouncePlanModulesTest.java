package net.blockhost.anarchyclient.module.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.configuration.ClientboundRegistryDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.blockhost.anarchyclient.render.RenderSuppression;
import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        assertNull(AntiExploitModule.exploitReason(ServerboundPunchPacket.INSTANCE,
                1024, 64));
        assertEquals(List.of(new BlockPos(1, 64, -2), new BlockPos(5, 70, 5)),
                ProtectionZonesModule.parseZones("1,64,-2; nope; 5,70,5"));
    }

    @Test
    void exploitCheckNeverDropsRegistrySync() {
        // Dropping registry or tag sync leaves the client without biomes, and the next world join dies
        // with "Missing element minecraft:plains" instead of loading.
        var registryPacket = new ClientboundRegistryDataPacket(
                Registries.BIOME, List.of());
        var tagsPacket = new ClientboundUpdateTagsPacket(Map.of());
        var otherPacket = ServerboundPunchPacket.INSTANCE;

        // Only displayed text is measured, so large non-text packets survive even a limit of one.
        assertNull(AntiExploitModule.exploitReason(registryPacket, 1, 64));
        assertNull(AntiExploitModule.exploitReason(tagsPacket, 1, 64));
        assertNull(AntiExploitModule.exploitReason(otherPacket, 1, 64));
        assertNotNull(AntiExploitModule.exploitReason(
                new ClientboundSystemChatPacket(Component.literal("too long"), false), 1, 64));
    }

    @Test
    void autoBuffOnlyRequestsMissingEnabledEffects() {
        assertTrue(AutoBuffModule.shouldBuff(true, false));
        assertFalse(AutoBuffModule.shouldBuff(false, false));
        assertFalse(AutoBuffModule.shouldBuff(true, true));
    }
}
