package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Set;
import java.util.function.Predicate;

public final class AutoEnchantModule extends Module {

    private static final int ITEM_SLOT = 0;
    private static final int PLAYER_INVENTORY_START = 2;

    private final StringSetting items = this.setting(StringSetting.from(StringSetting.builder()
            .id("items")
            .name("Items")
            .defaultValue("book")
            .description("Comma-separated item ids. Empty enchants any enchantable item.")
            .build()));
    private final NumberSetting level = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("level")
            .name("Level")
            .defaultValue(3.0)
            .min(1.0)
            .max(3.0)
            .step(1.0)
            .build()));
    private final NumberSetting delayTicks = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay_ticks")
            .name("Delay")
            .defaultValue(20.0)
            .min(0.0)
            .max(100.0)
            .step(1.0)
            .build()));
    private final BooleanSetting dropOutput = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("drop_output")
            .name("Drop")
            .defaultValue(false)
            .build()));
    private int cooldown;

    public AutoEnchantModule() {
        super("auto_enchant", "Auto Enchant", ModuleCategory.PLAYER);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null || !(player.containerMenu instanceof EnchantmentMenu menu)) {
            this.cooldown = 0;
            return;
        }
        if (this.cooldown > 0) {
            this.cooldown--;
            return;
        }
        int targetLevel = targetLevel(this.level.value());
        if (menu.getSlot(ITEM_SLOT).hasItem()) {
            if (canUseChoice(menu.costs, targetLevel, player.experienceLevel, menu.getGoldCount(), player.hasInfiniteMaterials())) {
                client.gameMode.handleInventoryButtonClick(menu.containerId, targetLevel - 1);
                if (this.dropOutput.value()) {
                    ContainerActions.throwSlot(client, menu, ITEM_SLOT);
                } else {
                    ContainerActions.quickMove(client, menu, ITEM_SLOT);
                }
                this.cooldown = this.delayTicks.value().intValue();
            } else if (menu.getGoldCount() < targetLevel) {
                this.moveFirst(client, menu, stack -> stack.is(Items.LAPIS_LAZULI));
            }
            return;
        }
        if (menu.getGoldCount() < targetLevel && this.moveFirst(client, menu, stack -> stack.is(Items.LAPIS_LAZULI))) {
            return;
        }
        Set<Item> targets = ItemScan.parseItems(this.items.value());
        if (this.moveFirst(client, menu, stack -> shouldEnchantCandidate(stack, targets))) {
            return;
        }
    }

    private boolean moveFirst(final Minecraft client, final EnchantmentMenu menu, final Predicate<ItemStack> predicate) {
        int slot = firstMatchingSlot(menu, PLAYER_INVENTORY_START, predicate);
        if (slot < 0) {
            return false;
        }
        if (ContainerActions.quickMove(client, menu, slot)) {
            this.cooldown = this.delayTicks.value().intValue();
            return true;
        }
        return false;
    }

    static boolean shouldEnchantCandidate(final ItemStack stack, final Set<Item> targets) {
        return !stack.isEmpty() && stack.isEnchantable() && (targets.isEmpty() || targets.contains(stack.getItem()));
    }

    static boolean canUseChoice(final int[] costs, final int level, final int playerLevel,
                                final int lapisCount, final boolean infiniteMaterials) {
        int clampedLevel = targetLevel(level);
        int index = clampedLevel - 1;
        if (costs == null || index >= costs.length || costs[index] <= 0) {
            return false;
        }
        return infiniteMaterials || lapisCount >= clampedLevel && playerLevel >= clampedLevel && playerLevel >= costs[index];
    }

    static int targetLevel(final double level) {
        return Math.max(1, Math.min(3, (int) level));
    }

    private static int firstMatchingSlot(final EnchantmentMenu menu, final int start,
                                         final Predicate<ItemStack> predicate) {
        for (int slot = start; slot < menu.slots.size(); slot++) {
            ItemStack stack = menu.getSlot(slot).getItem();
            if (predicate.test(stack)) {
                return slot;
            }
        }
        return -1;
    }
}
