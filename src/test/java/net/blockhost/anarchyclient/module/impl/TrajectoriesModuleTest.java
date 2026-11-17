package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MinecraftBootstrapExtension.class)
class TrajectoriesModuleTest {

    private static final double EPSILON = 1.0E-6;

    @Test
    void usesVanillaLaunchValuesForThrownItems() {
        assertEquals(new TrajectoriesModule.Ballistics(1.5, 0.03, 0.0F, true),
                TrajectoriesModule.ballistics(null, new ItemStack(Items.ENDER_PEARL)));
        assertEquals(new TrajectoriesModule.Ballistics(0.5, 0.05, -20.0F, true),
                TrajectoriesModule.ballistics(null, new ItemStack(Items.SPLASH_POTION)));
        assertEquals(new TrajectoriesModule.Ballistics(0.7, 0.07, -20.0F, true),
                TrajectoriesModule.ballistics(null, new ItemStack(Items.EXPERIENCE_BOTTLE)));
        assertEquals(new TrajectoriesModule.Ballistics(3.0, 0.05, 0.0F, false),
                TrajectoriesModule.ballistics(null, new ItemStack(Items.BOW)));
    }

    @Test
    void showsNothingForAnUnloadedCrossbowOrOtherItems() {
        assertNull(TrajectoriesModule.ballistics(null, new ItemStack(Items.CROSSBOW)));
        assertNull(TrajectoriesModule.ballistics(null, new ItemStack(Items.STONE)));
    }

    @Test
    void launchesStraightAheadAtTheGivenSpeed() {
        Vec3 motion = TrajectoriesModule.launchMotion(0.0F, 0.0F, 0.0F, 1.5);

        assertEquals(0.0, motion.x, EPSILON);
        assertEquals(0.0, motion.y, EPSILON);
        assertEquals(1.5, motion.z, EPSILON);
    }

    @Test
    void pitchOffsetThrowsPotionsUpward() {
        Vec3 motion = TrajectoriesModule.launchMotion(0.0F, 0.0F, -20.0F, 0.5);

        assertTrue(motion.y > 0.0);
        assertEquals(0.5, motion.length(), EPSILON);
    }

    @Test
    void appliesGravityAndDragInVanillaOrder() {
        Vec3 motion = new Vec3(1.0, 0.0, 0.0);

        Vec3 throwable = TrajectoriesModule.throwableStep(motion, 0.03);
        Vec3 arrow = TrajectoriesModule.arrowStep(motion, 0.05);

        assertEquals(-0.03 * 0.99, throwable.y, EPSILON);
        assertEquals(0.99, throwable.x, EPSILON);
        assertEquals(-0.05, arrow.y, EPSILON);
        assertEquals(0.99, arrow.x, EPSILON);
    }
}
