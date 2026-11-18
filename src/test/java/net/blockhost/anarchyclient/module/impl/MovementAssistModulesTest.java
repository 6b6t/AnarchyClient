package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.test.MapBlockGetter;
import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MinecraftBootstrapExtension.class)
class MovementAssistModulesTest {

    @Test
    void spiralLegsGrowEveryTwoTurns() {
        assertArrayEquals(new double[] {10.0, 0.0}, SpiralModule.corner(0.0, 0.0, 0, 10.0));
        assertArrayEquals(new double[] {10.0, 10.0}, SpiralModule.corner(0.0, 0.0, 1, 10.0));
        assertArrayEquals(new double[] {-10.0, 10.0}, SpiralModule.corner(0.0, 0.0, 2, 10.0));
        assertArrayEquals(new double[] {-10.0, -10.0}, SpiralModule.corner(0.0, 0.0, 3, 10.0));
        assertArrayEquals(new double[] {20.0, -10.0}, SpiralModule.corner(0.0, 0.0, 4, 10.0));
        assertArrayEquals(new double[] {105.0, 50.0}, SpiralModule.corner(100.0, 50.0, 0, 5.0));
    }

    @Test
    void holeSnapOnlyTargetsHolesAheadOfMovement() {
        Vec3 position = new Vec3(0.5, 64.0, 0.5);
        Vec3 hole = new Vec3(2.5, 64.0, 0.5);

        assertTrue(HoleSnapModule.isAhead(position, hole, new Vec3(0.2, 0.0, 0.0)));
        assertFalse(HoleSnapModule.isAhead(position, hole, new Vec3(-0.2, 0.0, 0.0)));
        assertFalse(HoleSnapModule.isAhead(position, hole, Vec3.ZERO));
    }

    @Test
    void holeSnapPullNeverOvershootsTheCenter() {
        Vec3 far = HoleSnapModule.pull(new Vec3(0.0, 64.0, 0.0), new Vec3(3.0, 64.0, 4.0), 0.5);
        Vec3 near = HoleSnapModule.pull(new Vec3(0.0, 64.0, 0.0), new Vec3(0.1, 64.0, 0.0), 0.5);

        assertEquals(0.5, far.horizontalDistance(), 1.0E-9);
        assertEquals(0.1, near.x, 1.0E-9);
        assertEquals(Vec3.ZERO, HoleSnapModule.pull(Vec3.ZERO, Vec3.ZERO, 0.5));
    }

    @Test
    void autoFireworkFiresBelowTheSpeedAndProtectsWornElytra() {
        assertTrue(AutoFireworkModule.shouldFire(AutoFireworkModule.blocksPerSecond(new Vec3(0.5, 0.0, 0.0)), 20.0));
        assertFalse(AutoFireworkModule.shouldFire(AutoFireworkModule.blocksPerSecond(new Vec3(1.5, 0.0, 0.0)), 20.0));

        ItemStack elytra = new ItemStack(Items.ELYTRA);
        assertFalse(AutoFireworkModule.nearlyBroken(elytra));
        elytra.setDamageValue(elytra.getMaxDamage() - 5);
        assertTrue(AutoFireworkModule.nearlyBroken(elytra));
    }

    @Test
    void voidColumnHasNothingBelowToTheBottomOfTheWorld() {
        BlockPos edge = new BlockPos(0, 64, 0);
        MapBlockGetter level = new MapBlockGetter();

        assertTrue(AvoidHazardsModule.isVoidColumn(level, edge));

        level.set(new BlockPos(0, -60, 0), Blocks.STONE);
        assertFalse(AvoidHazardsModule.isVoidColumn(level, edge));

        assertFalse(AvoidHazardsModule.isVoidColumn(new MapBlockGetter().set(new BlockPos(0, 10, 0), Blocks.WATER), edge));
    }
}
