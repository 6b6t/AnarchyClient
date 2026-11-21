package net.blockhost.anarchyclient.module.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MinecraftBootstrapExtension.class)
class BedPlatesModuleTest {

    @Test
    void matchesBothHalvesOfColoredBeds() {
        assertTrue(BedPlatesModule.isBed(
                Blocks.BED.pick(DyeColor.RED).defaultBlockState().setValue(AbstractBedBlock.PART, BedPart.HEAD)));
        assertTrue(BedPlatesModule.isBed(
                Blocks.BED.pick(DyeColor.WHITE).defaultBlockState().setValue(AbstractBedBlock.PART, BedPart.FOOT)));
    }

    @Test
    void matchesStrawBeds() {
        assertTrue(BedPlatesModule.isBed(Blocks.STRAW_BED.defaultBlockState()));
    }

    @Test
    void ignoresOtherBlocks() {
        assertFalse(BedPlatesModule.isBed(Blocks.STONE.defaultBlockState()));
        assertFalse(BedPlatesModule.isBed(Blocks.WOOL.pick(DyeColor.RED).defaultBlockState()));
    }
}
