package net.blockhost.anarchyclient.module.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalInt;
import java.util.Set;
import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.blockhost.anarchyclient.test.TestInventories;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MinecraftBootstrapExtension.class)
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
        assertEquals(
                Set.of("pufferfish", "minecraft:chicken", "rotten_flesh"),
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

    @Test
    void choosesGappleTypeByPreference() {
        Inventory inventory = TestInventories.withSlots(
                new ItemStack(Items.BREAD),
                new ItemStack(Items.GOLDEN_APPLE),
                new ItemStack(Items.ENCHANTED_GOLDEN_APPLE));

        assertEquals(OptionalInt.of(2), SmartEatModule.bestGappleSlot(inventory, true));
        assertEquals(OptionalInt.of(1), SmartEatModule.bestGappleSlot(inventory, false));
    }

    @Test
    void fallsBackToTheOtherGappleType() {
        Inventory enchantedOnly = TestInventories.withSlots(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE));
        Inventory goldenOnly = TestInventories.withSlots(new ItemStack(Items.GOLDEN_APPLE));

        assertEquals(OptionalInt.of(0), SmartEatModule.bestGappleSlot(enchantedOnly, false));
        assertEquals(OptionalInt.of(0), SmartEatModule.bestGappleSlot(goldenOnly, true));
        assertEquals(OptionalInt.empty(), SmartEatModule.bestGappleSlot(TestInventories.withSlots(), true));
    }

    @Test
    void findsSoupButNotEmptyBowls() {
        Inventory inventory = TestInventories.withSlots(
                new ItemStack(Items.BOWL), new ItemStack(Items.BREAD), new ItemStack(Items.RABBIT_STEW));

        assertEquals(OptionalInt.of(2), SmartEatModule.findSoupSlot(inventory, 0, 8));
        assertEquals(OptionalInt.empty(), SmartEatModule.findSoupSlot(inventory, 0, 1));
    }

    @Test
    void usesTheHandHoldingSoup() {
        ItemStack stew = new ItemStack(Items.MUSHROOM_STEW);
        ItemStack sword = new ItemStack(Items.IRON_SWORD);

        assertEquals(InteractionHand.MAIN_HAND, SmartEatModule.soupHand(stew, ItemStack.EMPTY));
        assertEquals(InteractionHand.OFF_HAND, SmartEatModule.soupHand(sword, stew));
        assertNull(SmartEatModule.soupHand(sword, ItemStack.EMPTY));
    }

    @Test
    void onlyAlwaysEdibleFoodWorksAtFullHunger() {
        assertFalse(SmartEatModule.canEatNow(false, new ItemStack(Items.BREAD)));
        assertTrue(SmartEatModule.canEatNow(false, new ItemStack(Items.GOLDEN_APPLE)));
        assertTrue(SmartEatModule.canEatNow(true, new ItemStack(Items.BREAD)));
        assertFalse(SmartEatModule.canEatNow(true, new ItemStack(Items.STONE)));
    }
}
