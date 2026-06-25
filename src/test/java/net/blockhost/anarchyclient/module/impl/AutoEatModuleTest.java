package net.blockhost.anarchyclient.module.impl;

import net.minecraft.world.food.FoodProperties;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoEatModuleTest {

    @Test
    void thresholdModeControlsWhenEatingStarts() {
        assertTrue(AutoEatModule.shouldEat(10, 20F, 14, 10, AutoEatModule.ThresholdMode.HUNGER));
        assertFalse(AutoEatModule.shouldEat(20, 8F, 14, 10, AutoEatModule.ThresholdMode.HUNGER));
        assertTrue(AutoEatModule.shouldEat(20, 8F, 14, 10, AutoEatModule.ThresholdMode.HEALTH));
        assertFalse(AutoEatModule.shouldEat(10, 20F, 14, 10, AutoEatModule.ThresholdMode.HEALTH));
        assertTrue(AutoEatModule.shouldEat(10, 8F, 14, 10, AutoEatModule.ThresholdMode.BOTH));
        assertFalse(AutoEatModule.shouldEat(10, 20F, 14, 10, AutoEatModule.ThresholdMode.BOTH));
        assertTrue(AutoEatModule.shouldEat(10, 20F, 14, 10, AutoEatModule.ThresholdMode.ANY));
    }

    @Test
    void parsesBlacklistTokens() {
        assertEquals(Set.of("pufferfish", "minecraft:chicken", "rotten_flesh"),
                AutoEatModule.parseBlacklist("pufferfish, minecraft:chicken | rotten_flesh"));
    }

    @Test
    void matchesBlacklistedFoodByPathOrIdentifier() {
        assertTrue(AutoEatModule.isBlacklisted("minecraft:pufferfish", Set.of("pufferfish")));
        assertTrue(AutoEatModule.isBlacklisted("minecraft:pufferfish", Set.of("minecraft:pufferfish")));
        assertFalse(AutoEatModule.isBlacklisted("minecraft:apple", Set.of("pufferfish")));
    }

    @Test
    void scoresFoodUsingSelectedPriority() {
        FoodProperties apple = new FoodProperties(4, 2.4F, false);

        assertEquals(4.0, AutoEatModule.foodScore(apple, AutoEatModule.FoodPriority.HUNGER));
        assertEquals(2.4, AutoEatModule.foodScore(apple, AutoEatModule.FoodPriority.SATURATION), 0.001);
        assertEquals(6.4, AutoEatModule.foodScore(apple, AutoEatModule.FoodPriority.COMBINED), 0.001);
    }
}
