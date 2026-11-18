package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class InventorySorterModule extends Module {

    static final Comparator<ItemStack> ORDER = Comparator
            .comparing(ItemStack::isEmpty)
            .thenComparing(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())
            .thenComparing(stack -> stack.getComponentsPatch().toString());

    private final BooleanSetting hotbar = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("hotbar")
            .name("Hotbar")
            .defaultValue(false)
            .description("Also sorts the hotbar. Off keeps your hotbar layout.")
            .build()));
    private final NumberSetting delay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay")
            .name("Delay")
            .defaultValue(1.0)
            .min(0.0)
            .max(20.0)
            .step(1.0)
            .description("Ticks between two swaps.")
            .build()));
    private int cooldown;

    public InventorySorterModule() {
        super("inventory_sorter", "Inventory Sorter", ModuleCategory.PLAYER,
                "Sorts your inventory by item, one swap at a time while no screen is open, then turns itself off.");
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null) {
            return;
        }
        if (this.cooldown > 0) {
            this.cooldown--;
            return;
        }
        if (!InventoryActions.canUseInventoryMenu(client, player)) {
            return;
        }
        int first = this.hotbar.value() ? 0 : Inventory.getSelectionSize();
        List<ItemStack> stacks = new ArrayList<>();
        for (int slot = first; slot < Inventory.INVENTORY_SIZE; slot++) {
            stacks.add(player.getInventory().getItem(slot));
        }
        int[] swap = nextSwap(stacks);
        if (swap == null) {
            this.enabled(false);
            return;
        }
        InventoryActions.pickupSwap(client, player, InventoryActions.toInventoryMenuSlot(first + swap[0]),
                InventoryActions.toInventoryMenuSlot(first + swap[1]));
        this.cooldown = this.delay.value().intValue();
    }

    /**
     * Returns the next swap of a selection sort, as {@code {from, to}} indexes into {@code stacks},
     * or {@code null} when the stacks are already in order. Each swap puts one slot in its final
     * place, and equal items never swap with each other, so vanilla never merges them by accident.
     */
    static int[] nextSwap(final List<ItemStack> stacks) {
        List<ItemStack> sorted = new ArrayList<>(stacks);
        sorted.sort(ORDER);
        for (int target = 0; target < stacks.size(); target++) {
            ItemStack wanted = sorted.get(target);
            if (sameKind(stacks.get(target), wanted)) {
                continue;
            }
            for (int from = target + 1; from < stacks.size(); from++) {
                if (sameKind(stacks.get(from), wanted) && !sameKind(stacks.get(from), sorted.get(from))) {
                    return new int[] {from, target};
                }
            }
            for (int from = target + 1; from < stacks.size(); from++) {
                if (sameKind(stacks.get(from), wanted)) {
                    return new int[] {from, target};
                }
            }
        }
        return null;
    }

    private static boolean sameKind(final ItemStack first, final ItemStack second) {
        if (first.isEmpty() || second.isEmpty()) {
            return first.isEmpty() && second.isEmpty();
        }
        return ItemStack.isSameItemSameComponents(first, second);
    }

    @Override
    protected void onDisable() {
        this.cooldown = 0;
    }
}
