package net.blockhost.anarchyclient.module.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.blockhost.anarchyclient.test.TestInventories;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MinecraftBootstrapExtension.class)
class WorldAutomationModulesTest {

    @Test
    void inventorySorterGroupsItemsAndFinishes() {
        List<ItemStack> stacks = new ArrayList<>(List.of(
                new ItemStack(Items.STONE, 3),
                ItemStack.EMPTY,
                new ItemStack(Items.APPLE),
                new ItemStack(Items.STONE, 64),
                new ItemStack(Items.APPLE, 5)));

        int swaps = 0;
        for (int[] swap = InventorySorterModule.nextSwap(stacks);
                swap != null;
                swap = InventorySorterModule.nextSwap(stacks)) {
            ItemStack moved = stacks.get(swap[0]);
            stacks.set(swap[0], stacks.get(swap[1]));
            stacks.set(swap[1], moved);
            assertTrue(++swaps <= stacks.size(), "sorting must finish");
        }

        assertTrue(stacks.get(0).is(Items.APPLE) && stacks.get(1).is(Items.APPLE));
        assertTrue(stacks.get(2).is(Items.STONE) && stacks.get(3).is(Items.STONE));
        assertTrue(stacks.get(4).isEmpty());
    }

    @Test
    void inventorySorterLeavesSortedInventoriesAlone() {
        assertNull(InventorySorterModule.nextSwap(
                List.of(new ItemStack(Items.APPLE), new ItemStack(Items.STONE), ItemStack.EMPTY)));
    }

    @Test
    void autoTradePicksTheFirstAffordableWantedOffer() {
        MerchantOffers offers = new MerchantOffers();
        offers.add(new MerchantOffer(new ItemCost(Items.WHEAT, 20), new ItemStack(Items.EMERALD), 12, 1, 0.05F));
        offers.add(new MerchantOffer(new ItemCost(Items.PAPER, 24), new ItemStack(Items.EMERALD), 12, 1, 0.05F));
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 1), new ItemStack(Items.BREAD, 6), 12, 1, 0.05F));
        Inventory inventory = TestInventories.withSlots(new ItemStack(Items.PAPER, 16), new ItemStack(Items.PAPER, 16));

        assertEquals(1, AutoTradeModule.nextTrade(offers, Set.of(Items.EMERALD), inventory));
        assertEquals(-1, AutoTradeModule.nextTrade(offers, Set.of(Items.BREAD), inventory));
    }

    @Test
    void autoTradeCountsPaymentAcrossStacks() {
        Inventory inventory = TestInventories.withSlots(new ItemStack(Items.PAPER, 10), new ItemStack(Items.PAPER, 10));

        assertTrue(AutoTradeModule.canPay(inventory, new ItemStack(Items.PAPER, 20)));
        assertFalse(AutoTradeModule.canPay(inventory, new ItemStack(Items.PAPER, 21)));
        assertTrue(AutoTradeModule.canPay(inventory, ItemStack.EMPTY));
    }

    @Test
    void autoSmithUpgradesOnlyDiamondGear() {
        assertTrue(AutoSmithModule.isDiamondGear(new ItemStack(Items.DIAMOND_CHESTPLATE)));
        assertTrue(AutoSmithModule.isDiamondGear(new ItemStack(Items.DIAMOND_SWORD)));
        assertFalse(AutoSmithModule.isDiamondGear(new ItemStack(Items.DIAMOND)));
        assertFalse(AutoSmithModule.isDiamondGear(new ItemStack(Items.NETHERITE_SWORD)));
    }

    @Test
    void itemSaverMovesAwayFromNearlyBrokenTools() {
        ItemStack worn = new ItemStack(Items.DIAMOND_PICKAXE);
        worn.setDamageValue(worn.getMaxDamage() - 3);
        Inventory inventory = TestInventories.withSlots(worn, ItemStack.EMPTY, new ItemStack(Items.IRON_PICKAXE));

        assertTrue(ItemSaverModule.nearlyBroken(worn, 10));
        assertFalse(ItemSaverModule.nearlyBroken(new ItemStack(Items.STONE), 10));
        assertEquals(OptionalInt.of(2), ItemSaverModule.safeSlot(inventory, 0, 10));
    }
}
