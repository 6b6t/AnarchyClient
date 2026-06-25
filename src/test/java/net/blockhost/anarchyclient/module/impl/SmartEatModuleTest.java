package net.blockhost.anarchyclient.module.impl;

import net.minecraft.world.food.FoodProperties;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmartEatModuleTest {

    @Test
    void thresholdModeControlsWhenEatingStarts() {
        assertTrue(SmartEatModule.shouldEat(10, 20F, 14, 10, SmartEatModule.ThresholdMode.HUNGER));
        assertFalse(SmartEatModule.shouldEat(20, 8F, 14, 10, SmartEatModule.ThresholdMode.HUNGER));
        assertTrue(SmartEatModule.shouldEat(20, 8F, 14, 10, SmartEatModule.ThresholdMode.HEALTH));
        assertFalse(SmartEatModule.shouldEat(10, 20F, 14, 10, SmartEatModule.ThresholdMode.HEALTH));
        assertTrue(SmartEatModule.shouldEat(10, 8F, 14, 10, SmartEatModule.ThresholdMode.BOTH));
        assertFalse(SmartEatModule.shouldEat(10, 20F, 14, 10, SmartEatModule.ThresholdMode.BOTH));
        assertTrue(SmartEatModule.shouldEat(10, 20F, 14, 10, SmartEatModule.ThresholdMode.ANY));
    }

    @Test
    void parsesBlacklistTokens() {
        assertEquals(Set.of("pufferfish", "minecraft:chicken", "rotten_flesh"),
                SmartEatModule.parseBlacklist("pufferfish, minecraft:chicken | rotten_flesh"));
    }

    @Test
    void matchesBlacklistedFoodByPathOrIdentifier() {
        assertTrue(SmartEatModule.isBlacklisted("minecraft:pufferfish", Set.of("pufferfish")));
        assertTrue(SmartEatModule.isBlacklisted("minecraft:pufferfish", Set.of("minecraft:pufferfish")));
        assertFalse(SmartEatModule.isBlacklisted("minecraft:apple", Set.of("pufferfish")));
    }

    @Test
    void scoresFoodUsingSelectedPriority() {
        FoodProperties apple = new FoodProperties(4, 2.4F, false);

        assertEquals(4.0, SmartEatModule.foodScore(apple, SmartEatModule.FoodPriority.HUNGER));
        assertEquals(2.4, SmartEatModule.foodScore(apple, SmartEatModule.FoodPriority.SATURATION), 0.001);
        assertEquals(6.4, SmartEatModule.foodScore(apple, SmartEatModule.FoodPriority.COMBINED), 0.001);
    }
}
