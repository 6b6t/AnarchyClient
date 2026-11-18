package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.ItemListSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.List;
import java.util.Set;

public final class AutoTradeModule extends Module {

    private static final int RESULT_SLOT = 2;

    private final ItemListSetting results = this.setting(ItemListSetting.from(ItemListSetting.builder()
            .id("results")
            .name("Buy")
            .description("Trades whose result is one of these items are made automatically.")
            .addAllDefaultValue(List.of(Items.EMERALD))
            .build()));
    private final NumberSetting delay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay")
            .name("Delay")
            .defaultValue(2.0)
            .min(1.0)
            .max(20.0)
            .step(1.0)
            .description("Ticks between two trade clicks.")
            .build()));
    private int cooldown;

    public AutoTradeModule() {
        super("auto_trade", "Auto Trade", ModuleCategory.WORLD,
                "While a villager's trade screen is open, repeats every trade that gives one of your chosen items "
                        + "for as long as the villager has stock and you can pay.");
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null || client.getConnection() == null
                || !(player.containerMenu instanceof MerchantMenu menu)) {
            this.cooldown = 0;
            return;
        }
        if (this.cooldown > 0) {
            this.cooldown--;
            return;
        }
        this.cooldown = this.delay.value().intValue();
        ItemStack result = menu.getSlot(RESULT_SLOT).getItem();
        if (!result.isEmpty()) {
            if (!AutoSmelterModule.hasRoomFor(menu, result)) {
                this.debugValue("state", "inventory full");
                return;
            }
            // Shift-clicking the result repeats the trade until stock or payment runs out.
            client.gameMode.handleContainerInput(menu.containerId, RESULT_SLOT, 0, ContainerInput.QUICK_MOVE, player);
            return;
        }
        int index = nextTrade(menu.getOffers(), Set.copyOf(this.results.value()), player.getInventory());
        if (index < 0) {
            this.debugValue("state", "nothing to trade");
            return;
        }
        menu.setSelectionHint(index);
        menu.tryMoveItems(index);
        client.getConnection().send(new ServerboundSelectTradePacket(index));
        this.debugValue("trade", index);
    }

    /** Returns the first in-stock offer with a wanted result that the inventory can pay for, or -1. */
    static int nextTrade(final MerchantOffers offers, final Set<Item> wanted, final Inventory inventory) {
        for (int index = 0; index < offers.size(); index++) {
            MerchantOffer offer = offers.get(index);
            if (!offer.isOutOfStock() && wanted.contains(offer.getResult().getItem())
                    && canPay(inventory, offer.getCostA()) && canPay(inventory, offer.getCostB())) {
                return index;
            }
        }
        return -1;
    }

    static boolean canPay(final Inventory inventory, final ItemStack cost) {
        if (cost.isEmpty()) {
            return true;
        }
        int count = 0;
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (ItemStack.isSameItem(stack, cost)) {
                count += stack.getCount();
                if (count >= cost.getCount()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void onDisable() {
        this.cooldown = 0;
    }
}
