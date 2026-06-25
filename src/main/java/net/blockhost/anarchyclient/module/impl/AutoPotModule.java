package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

import java.util.OptionalInt;

public final class AutoPotModule extends Module {

    private final NumberSetting healthThreshold = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("health_threshold")
            .name("Health")
            .defaultValue(12.0)
            .min(1.0)
            .max(20.0)
            .step(0.5)
            .build()));
    private final BooleanSetting healing = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("healing")
            .name("Healing")
            .defaultValue(true)
            .build()));
    private final BooleanSetting strength = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("strength")
            .name("Strength")
            .defaultValue(true)
            .build()));
    private final BooleanSetting useSplash = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("use_splash")
            .name("Splash")
            .defaultValue(true)
            .build()));
    private final BooleanSetting rotateDown = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("rotate_down")
            .name("Rotate")
            .defaultValue(true)
            .build()));
    private final BooleanSetting restoreSlot = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("restore_slot")
            .name("Restore")
            .defaultValue(true)
            .build()));
    private final NumberSetting cooldownTicks = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("cooldown_ticks")
            .name("Cooldown")
            .defaultValue(20.0)
            .min(1.0)
            .max(100.0)
            .step(1.0)
            .build()));
    private int previousSlot = -1;
    private int cooldown;

    public AutoPotModule() {
        super("auto_pot", "Auto Pot", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }
        if (this.previousSlot >= 0 && !player.isUsingItem()) {
            if (this.restoreSlot.value()) {
                InventoryActions.selectHotbarSlot(player, this.previousSlot);
            }
            this.previousSlot = -1;
        }
        if (this.cooldown > 0) {
            this.cooldown--;
            return;
        }
        if (player.isUsingItem()) {
            return;
        }

        PotionTarget target = this.nextTarget(player);
        if (target == null) {
            return;
        }
        OptionalInt slot = findPotionSlot(player.getInventory(), target.effect(), this.useSplash.value());
        if (slot.isEmpty()) {
            return;
        }
        usePotion(client, player, slot.orElseThrow(), this.rotateDown.value());
        this.cooldown = Math.max(1, this.cooldownTicks.value().intValue());
    }

    @Override
    protected void onDisable() {
        this.previousSlot = -1;
        this.cooldown = 0;
    }

    private PotionTarget nextTarget(final LocalPlayer player) {
        if (this.healing.value() && shouldUseHealing(player.getHealth(), this.healthThreshold.value())) {
            return new PotionTarget(MobEffects.INSTANT_HEALTH);
        }
        if (this.strength.value() && !player.hasEffect(MobEffects.STRENGTH)) {
            return new PotionTarget(MobEffects.STRENGTH);
        }
        return null;
    }

    static boolean shouldUseHealing(final float health, final double threshold) {
        return health <= threshold;
    }

    private void usePotion(final Minecraft client, final LocalPlayer player, final int slot, final boolean rotateDown) {
        int selectedSlot = player.getInventory().getSelectedSlot();
        if (slot != selectedSlot) {
            this.previousSlot = selectedSlot;
            InventoryActions.selectHotbarSlot(player, slot);
        }
        ItemStack stack = player.getInventory().getItem(slot);
        if (stack.is(Items.SPLASH_POTION) && rotateDown) {
            player.setXRot(90.0F);
        }
        client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
    }

    static OptionalInt findPotionSlot(final Inventory inventory, final Holder<MobEffect> effect, final boolean allowSplash) {
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            if (matchesPotion(inventory.getItem(slot), effect, allowSplash)) {
                return OptionalInt.of(slot);
            }
        }
        return OptionalInt.empty();
    }

    static boolean matchesPotion(final ItemStack stack, final Holder<MobEffect> effect, final boolean allowSplash) {
        if (stack.isEmpty()) {
            return false;
        }
        boolean drinkable = stack.is(Items.POTION);
        boolean splash = stack.is(Items.SPLASH_POTION);
        if (!drinkable && !(allowSplash && splash)) {
            return false;
        }
        PotionContents contents = stack.getComponents().getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        for (MobEffectInstance instance : contents.getAllEffects()) {
            if (instance.getEffect().is(effect)) {
                return true;
            }
        }
        return false;
    }

    private record PotionTarget(Holder<MobEffect> effect) {
    }
}
