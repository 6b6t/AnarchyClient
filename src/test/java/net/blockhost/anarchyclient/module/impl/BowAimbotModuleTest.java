package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MinecraftBootstrapExtension.class)
class BowAimbotModuleTest {

    @Test
    void fullyDrawnBowUsesFullArrowSpeed() {
        assertEquals(3.0, BowAimbotModule.arrowVelocity(new ItemStack(Items.BOW), 20), 1.0E-6);
        assertEquals(3.0, BowAimbotModule.arrowVelocity(new ItemStack(Items.BOW), 200), 1.0E-6);
    }

    @Test
    void partiallyDrawnBowShootsSlower() {
        double half = BowAimbotModule.arrowVelocity(new ItemStack(Items.BOW), 10);

        assertTrue(half > 0.0 && half < 3.0, "half draw speed was " + half);
    }

    @Test
    void crossbowUsesVanillaArrowSpeed() {
        assertEquals(3.15, BowAimbotModule.arrowVelocity(new ItemStack(Items.CROSSBOW), 0), 1.0E-6);
    }
}
