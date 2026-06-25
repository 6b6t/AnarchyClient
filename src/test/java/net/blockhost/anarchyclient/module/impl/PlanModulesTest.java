package net.blockhost.anarchyclient.module.impl;

import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanModulesTest {

    @Test
    void textFieldProtectMasksConfiguredFieldsOnly() {
        TextFieldProtectModule module = new TextFieldProtectModule();
        module.enabled(true);
        try {
            assertTrue(TextFieldProtectModule.shouldMask(Component.literal("Password"), "hunter2"));
            assertEquals("*******", TextFieldProtectModule.mask("hunter2"));
            assertFalse(TextFieldProtectModule.shouldMask(Component.literal("Server Name"), "example.org"));
        } finally {
            module.enabled(false);
        }
    }

    @Test
    void strongholdFinderIntersectsEyeThrowRays() {
        Optional<Vec3> estimate = StrongholdFinderModule.estimate(
                new Vec3(0.0, 64.0, 0.0),
                new Vec3(1.0, 0.0, 1.0),
                new Vec3(10.0, 70.0, 0.0),
                new Vec3(-1.0, 0.0, 1.0)
        );

        assertTrue(estimate.isPresent());
        assertEquals(5.0, estimate.orElseThrow().x, 1.0E-9);
        assertEquals(5.0, estimate.orElseThrow().z, 1.0E-9);
    }

    @Test
    void autoTntKeepsIgniterDurabilityReserve() {
        assertEquals(64, AutoTntModule.remainingDurability(64, 0));
        assertEquals(4, AutoTntModule.remainingDurability(64, 60));
        assertTrue(AutoTntModule.hasEnoughDurability(64, 53, 10));
        assertFalse(AutoTntModule.hasEnoughDurability(64, 54, 10));
        assertTrue(AutoTntModule.acceptsIgniter(false, true, true, true, 0, 0, 10));
        assertFalse(AutoTntModule.acceptsIgniter(false, true, false, true, 0, 0, 10));
        assertTrue(AutoTntModule.acceptsIgniter(true, false, false, true, 64, 53, 10));
        assertFalse(AutoTntModule.acceptsIgniter(true, false, false, true, 64, 54, 10));
    }

    @Test
    void painterRespectsEnabledSurfaceFamiliesAndOneHighMode() {
        assertTrue(PainterModule.shouldPaintSurface(true, true, true, false, false,
                true, false, false, false));
        assertFalse(PainterModule.shouldPaintSurface(true, true, false, false, true,
                true, false, false, false));
        assertTrue(PainterModule.shouldPaintSurface(true, true, false, true, false,
                false, true, false, false));
        assertFalse(PainterModule.shouldPaintSurface(true, true, true, true, true,
                true, true, true, true));
        assertTrue(PainterModule.shouldPaintSurface(true, false, true, true, true,
                true, true, true, true));
    }

    @Test
    void obsidianFarmCandidatePositionsStartNearCenter() {
        BlockPos center = new BlockPos(10, 64, -5);
        List<BlockPos> positions = ObsidianFarmModule.candidatePositions(center, 1, 0);

        assertEquals(9, positions.size());
        assertEquals(center, positions.getFirst());
        assertTrue(positions.contains(center.north()));
        assertFalse(ObsidianFarmModule.isConsuming(ItemStack.EMPTY, true));
    }

    @Test
    void autoBedTrapBuildsShellAroundBothBedHalves() {
        BlockPos foot = new BlockPos(0, 64, 0);
        BlockPos head = foot.east();
        List<BlockPos> positions = AutoBedTrapModule.trapPositions(foot, head);

        assertEquals(10, positions.size());
        assertFalse(positions.contains(foot));
        assertFalse(positions.contains(head));
        assertTrue(positions.contains(foot.above()));
        assertTrue(positions.contains(head.above()));
        assertTrue(positions.contains(foot.below()));
        assertTrue(positions.contains(head.east()));
    }

    @Test
    void autoWitherPlansSoulSandAndSkullOrder() {
        BlockPos foot = new BlockPos(0, 64, 0);

        assertEquals(List.of(
                foot,
                foot.above(),
                foot.above().west(),
                foot.above().east(),
                foot.above(2).west(),
                foot.above(2),
                foot.above(2).east()
        ), AutoWitherModule.planPositions(foot, Direction.Axis.X));
    }
}
