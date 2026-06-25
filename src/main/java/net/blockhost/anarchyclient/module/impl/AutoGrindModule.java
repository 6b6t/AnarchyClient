package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.Set;

public final class AutoGrindModule extends Module {

    private final StringSetting itemBlacklist = this.setting(StringSetting.from(StringSetting.builder()
            .id("item_blacklist")
            .name("Items")
            .defaultValue("")
            .description("Comma-separated item ids to ignore.")
            .build()));
    private final StringSetting enchantmentBlacklist = this.setting(StringSetting.from(StringSetting.builder()
            .id("enchantment_blacklist")
            .name("Enchants")
            .defaultValue("binding_curse, vanishing_curse")
            .description("Comma-separated enchantment ids to ignore.")
            .build()));
    private final NumberSetting delayTicks = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay_ticks")
            .name("Delay")
            .defaultValue(10.0)
            .min(0.0)
            .max(100.0)
            .step(1.0)
            .build()));
    private int cooldown;

    public AutoGrindModule() {
        super("auto_grind", "Auto Grind", ModuleCategory.PLAYER);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null || !(player.containerMenu instanceof GrindstoneMenu menu)) {
            this.cooldown = 0;
            return;
        }
        if (this.cooldown > 0) {
            this.cooldown--;
            return;
        }
        if (menu.getSlot(GrindstoneMenu.RESULT_SLOT).hasItem()) {
            ContainerActions.quickMove(client, menu, GrindstoneMenu.RESULT_SLOT);
            this.cooldown = this.delayTicks.value().intValue();
            return;
        }
        if (menu.getSlot(GrindstoneMenu.INPUT_SLOT).hasItem()) {
            return;
        }
        Set<Item> ignoredItems = ItemScan.parseItems(this.itemBlacklist.value());
        Set<String> ignoredEnchantments = EquipmentScorer.parseIdentifiers(this.enchantmentBlacklist.value());
        for (int slot = 3; slot < menu.slots.size(); slot++) {
            ItemStack stack = menu.getSlot(slot).getItem();
            if (canGrind(stack, ignoredItems, ignoredEnchantments)) {
                ContainerActions.quickMove(client, menu, slot);
                this.cooldown = this.delayTicks.value().intValue();
                return;
            }
        }
    }

    static boolean canGrind(final ItemStack stack, final Set<Item> ignoredItems, final Set<String> ignoredEnchantments) {
        if (stack.isEmpty() || ignoredItems.contains(stack.getItem())) {
            return false;
        }
        ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        int removable = 0;
        for (Holder<Enchantment> enchantment : enchantments.keySet()) {
            if (enchantment.is(EnchantmentTags.CURSE)) {
                continue;
            }
            ResourceKey<Enchantment> key = enchantment.unwrapKey().orElse(null);
            if (key != null && EquipmentScorer.matchesAny(key.identifier().toString(), ignoredEnchantments)) {
                return false;
            }
            removable++;
        }
        return removable > 0;
    }
}
