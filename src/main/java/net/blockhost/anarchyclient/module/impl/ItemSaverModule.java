package net.blockhost.anarchyclient.module.impl;

import java.util.OptionalInt;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class ItemSaverModule extends Module {

    private final NumberSetting minDurability = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("min_durability")
            .name("Min Durability")
            .defaultValue(10.0)
            .min(1.0)
            .max(100.0)
            .step(1.0)
            .description("Stops using a tool that has this many uses or fewer left.")
            .build()));
    private final BooleanSetting attacks = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("attacks")
            .name("Attacks")
            .defaultValue(true)
            .description("Also protects weapons when you attack entities.")
            .build()));
    private final BooleanSetting switchSlot = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("switch_slot")
            .name("Switch Slot")
            .defaultValue(true)
            .description("Moves to a hotbar slot without a nearly broken item instead of only blocking the action.")
            .build()));

    public ItemSaverModule() {
        super(
                "item_saver",
                "Item Saver",
                ModuleCategory.PLAYER,
                "Stops you from mining or attacking with a tool or weapon that is about to break, so you do not lose it.");
    }

    @Override
    public boolean attackBlock(final Minecraft client, final BlockPos pos, final Direction direction) {
        return client.player != null && this.protect(client.player);
    }

    @Override
    public boolean attackEntity(final Minecraft client, final Player player, final Entity target) {
        return this.attacks.value() && client.player != null && player == client.player && this.protect(client.player);
    }

    private boolean protect(final LocalPlayer player) {
        int threshold = this.minDurability.value().intValue();
        if (!nearlyBroken(player.getMainHandItem(), threshold)) {
            return false;
        }
        if (this.switchSlot.value()) {
            OptionalInt safe =
                    safeSlot(player.getInventory(), player.getInventory().getSelectedSlot(), threshold);
            safe.ifPresent(slot -> InventoryActions.selectHotbarSlot(player, slot));
        }
        this.debugValue("blocked", player.getMainHandItem().getHoverName().getString());
        return true;
    }

    static boolean nearlyBroken(final ItemStack stack, final int minDurability) {
        return stack.isDamageableItem() && stack.getMaxDamage() - stack.getDamageValue() <= minDurability;
    }

    /** Returns the nearest hotbar slot to {@code selected} whose item is safe to use, preferring empty hands last. */
    static OptionalInt safeSlot(final Inventory inventory, final int selected, final int minDurability) {
        int emptySlot = -1;
        for (int offset = 1; offset < Inventory.getSelectionSize(); offset++) {
            int slot = (selected + offset) % Inventory.getSelectionSize();
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) {
                if (emptySlot < 0) {
                    emptySlot = slot;
                }
                continue;
            }
            if (!nearlyBroken(stack, minDurability)) {
                return OptionalInt.of(slot);
            }
        }
        return emptySlot < 0 ? OptionalInt.empty() : OptionalInt.of(emptySlot);
    }
}
