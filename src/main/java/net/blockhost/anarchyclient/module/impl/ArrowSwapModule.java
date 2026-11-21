package net.blockhost.anarchyclient.module.impl;

import java.util.List;
import java.util.OptionalInt;
import java.util.function.Predicate;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.StatusEffectListSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.alchemy.PotionContents;

public final class ArrowSwapModule extends Module {

    private final StatusEffectListSetting effects =
            this.setting(StatusEffectListSetting.from(StatusEffectListSetting.builder()
                    .id("effects")
                    .name("Effects")
                    .description("Tipped arrow effects to fire first, in order.")
                    .addAllDefaultValue(List.of(MobEffects.STRENGTH.value(), MobEffects.SPEED.value()))
                    .build()));
    private final BooleanSetting skipActive = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("skip_active")
            .name("Skip Active")
            .defaultValue(true)
            .description("Skips arrows whose effect you already have, so shooting yourself cycles through buffs.")
            .build()));

    public ArrowSwapModule() {
        super(
                "arrow_swap",
                "Arrow Swap",
                ModuleCategory.COMBAT,
                "Moves the tipped arrow you want next to the front of your inventory, so your bow or crossbow fires "
                        + "it. Works through the effect list in order.");
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null || !InventoryActions.canUseInventoryMenu(client, player)) {
            return;
        }
        ItemStack weapon = heldWeapon(player);
        if (weapon.isEmpty()) {
            return;
        }
        ProjectileWeaponItem weaponItem = (ProjectileWeaponItem) weapon.getItem();
        // Arrows held in a hand always fire first; there is nothing to reorder.
        if (!ProjectileWeaponItem.getHeldProjectile(player, weaponItem.getSupportedHeldProjectiles())
                .isEmpty()) {
            return;
        }
        Inventory inventory = player.getInventory();
        Predicate<ItemStack> ammo = weaponItem.getAllSupportedProjectiles();
        OptionalInt first = firstMatching(inventory, ammo);
        OptionalInt wanted = this.wantedSlot(player, inventory, ammo);
        if (first.isEmpty() || wanted.isEmpty() || first.getAsInt() == wanted.getAsInt()) {
            return;
        }
        InventoryActions.pickupSwap(
                client,
                player,
                InventoryActions.toInventoryMenuSlot(wanted.getAsInt()),
                InventoryActions.toInventoryMenuSlot(first.getAsInt()));
        this.debugValue("swapped", wanted.getAsInt() + " -> " + first.getAsInt());
    }

    private OptionalInt wantedSlot(
            final LocalPlayer player, final Inventory inventory, final Predicate<ItemStack> ammo) {
        for (MobEffect effect : this.effects.value()) {
            if (this.skipActive.value() && hasEffect(player.getActiveEffects(), effect)) {
                continue;
            }
            OptionalInt slot = firstMatching(inventory, stack -> ammo.test(stack) && hasArrowEffect(stack, effect));
            if (slot.isPresent()) {
                return slot;
            }
        }
        return OptionalInt.empty();
    }

    private static ItemStack heldWeapon(final LocalPlayer player) {
        if (player.getMainHandItem().getItem() instanceof ProjectileWeaponItem) {
            return player.getMainHandItem();
        }
        if (player.getOffhandItem().getItem() instanceof ProjectileWeaponItem) {
            return player.getOffhandItem();
        }
        return ItemStack.EMPTY;
    }

    /** Finds the lowest main inventory slot (hotbar first) that matches, in the order vanilla picks ammo. */
    static OptionalInt firstMatching(final Inventory inventory, final Predicate<ItemStack> predicate) {
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && predicate.test(stack)) {
                return OptionalInt.of(slot);
            }
        }
        return OptionalInt.empty();
    }

    static boolean hasArrowEffect(final ItemStack stack, final MobEffect effect) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        return contents != null && hasEffect(contents.getAllEffects(), effect);
    }

    private static boolean hasEffect(final Iterable<MobEffectInstance> instances, final MobEffect effect) {
        for (MobEffectInstance instance : instances) {
            if (instance.getEffect().value() == effect) {
                return true;
            }
        }
        return false;
    }
}
