package net.blockhost.anarchyclient.module.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MinecraftBootstrapExtension.class)
class AutoFarmModuleTest {

    @Test
    void boneMealsGrowingCropsOnly() {
        assertTrue(AutoFarmModule.isBoneMealCrop(Blocks.WHEAT.defaultBlockState()));
        assertFalse(AutoFarmModule.isBoneMealCrop(
                Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, CropBlock.MAX_AGE)));
        assertFalse(AutoFarmModule.isBoneMealCrop(Blocks.GRASS_BLOCK.defaultBlockState()));
        assertFalse(AutoFarmModule.isBoneMealCrop(Blocks.MOSS_BLOCK.defaultBlockState()));
    }
}
