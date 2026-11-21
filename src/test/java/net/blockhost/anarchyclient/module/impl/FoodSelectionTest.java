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
class FoodSelectionTest {

    @Test
    void thresholdModeControlsWhenEatingStarts() {
        assertTrue(FoodSelection.shouldEat(10, 20F, 14, 10, FoodSelection.ThresholdMode.HUNGER));
        assertFalse(FoodSelection.shouldEat(20, 8F, 14, 10, FoodSelection.ThresholdMode.HUNGER));
        assertTrue(FoodSelection.shouldEat(20, 8F, 14, 10, FoodSelection.ThresholdMode.HEALTH));
        assertFalse(FoodSelection.shouldEat(10, 20F, 14, 10, FoodSelection.ThresholdMode.HEALTH));
        assertTrue(FoodSelection.shouldEat(10, 8F, 14, 10, FoodSelection.ThresholdMode.BOTH));
        assertFalse(FoodSelection.shouldEat(10, 20F, 14, 10, FoodSelection.ThresholdMode.BOTH));
        assertTrue(FoodSelection.shouldEat(10, 20F, 14, 10, FoodSelection.ThresholdMode.ANY));
    }

    @Test
    void parsesBlacklistTokens() {
        assertEquals(
                Set.of("pufferfish", "minecraft:chicken", "rotten_flesh"),
                FoodSelection.parseBlacklist("pufferfish, minecraft:chicken | rotten_flesh"));
    }

    @Test
    void matchesBlacklistedFoodByPathOrIdentifier() {
        assertTrue(FoodSelection.isBlacklisted("minecraft:pufferfish", Set.of("pufferfish")));
        assertTrue(FoodSelection.isBlacklisted("minecraft:pufferfish", Set.of("minecraft:pufferfish")));
        assertFalse(FoodSelection.isBlacklisted("minecraft:apple", Set.of("pufferfish")));
    }

    @Test
    void scoresFoodUsingSelectedPriority() {
        FoodProperties apple = new FoodProperties(4, 2.4F, false);

        assertEquals(4.0, FoodSelection.foodScore(apple, FoodSelection.FoodPriority.HUNGER));
        assertEquals(2.4, FoodSelection.foodScore(apple, FoodSelection.FoodPriority.SATURATION), 0.001);
        assertEquals(6.4, FoodSelection.foodScore(apple, FoodSelection.FoodPriority.COMBINED), 0.001);
    }

    @Test
    void choosesGappleTypeByPreference() {
        Inventory inventory = TestInventories.withSlots(
                new ItemStack(Items.BREAD),
                new ItemStack(Items.GOLDEN_APPLE),
                new ItemStack(Items.ENCHANTED_GOLDEN_APPLE));

        assertEquals(OptionalInt.of(2), FoodSelection.bestGappleSlot(inventory, true));
        assertEquals(OptionalInt.of(1), FoodSelection.bestGappleSlot(inventory, false));
    }

    @Test
    void fallsBackToTheOtherGappleType() {
        Inventory enchantedOnly = TestInventories.withSlots(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE));
        Inventory goldenOnly = TestInventories.withSlots(new ItemStack(Items.GOLDEN_APPLE));

        assertEquals(OptionalInt.of(0), FoodSelection.bestGappleSlot(enchantedOnly, false));
        assertEquals(OptionalInt.of(0), FoodSelection.bestGappleSlot(goldenOnly, true));
        assertEquals(OptionalInt.empty(), FoodSelection.bestGappleSlot(TestInventories.withSlots(), true));
    }

    @Test
    void findsSoupButNotEmptyBowls() {
        Inventory inventory = TestInventories.withSlots(
                new ItemStack(Items.BOWL), new ItemStack(Items.BREAD), new ItemStack(Items.RABBIT_STEW));

        assertEquals(OptionalInt.of(2), FoodSelection.findSoupSlot(inventory, 0, 8));
        assertEquals(OptionalInt.empty(), FoodSelection.findSoupSlot(inventory, 0, 1));
    }

    @Test
    void usesTheHandHoldingSoup() {
        ItemStack stew = new ItemStack(Items.MUSHROOM_STEW);
        ItemStack sword = new ItemStack(Items.IRON_SWORD);

        assertEquals(InteractionHand.MAIN_HAND, FoodSelection.soupHand(stew, ItemStack.EMPTY));
        assertEquals(InteractionHand.OFF_HAND, FoodSelection.soupHand(sword, stew));
        assertNull(FoodSelection.soupHand(sword, ItemStack.EMPTY));
    }

    @Test
    void onlyAlwaysEdibleFoodWorksAtFullHunger() {
        assertFalse(FoodSelection.canEatNow(false, new ItemStack(Items.BREAD)));
        assertTrue(FoodSelection.canEatNow(false, new ItemStack(Items.GOLDEN_APPLE)));
        assertTrue(FoodSelection.canEatNow(true, new ItemStack(Items.BREAD)));
        assertFalse(FoodSelection.canEatNow(true, new ItemStack(Items.STONE)));
    }
}
