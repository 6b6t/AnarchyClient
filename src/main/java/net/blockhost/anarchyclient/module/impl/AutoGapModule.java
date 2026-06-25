package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.OptionalInt;

public final class AutoGapModule extends Module {

    private final NumberSetting healthThreshold = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("health_threshold")
            .name("Health")
            .defaultValue(10.0)
            .min(1.0)
            .max(20.0)
            .step(0.5)
            .build()));
    private final BooleanSetting preferEnchanted = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("prefer_enchanted")
            .name("Enchanted")
            .defaultValue(true)
            .build()));
    private final BooleanSetting allowDuringCombat = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("allow_during_combat")
            .name("In Combat")
            .defaultValue(true)
            .build()));
    private final BooleanSetting restoreSlot = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("restore_slot")
            .name("Restore")
            .defaultValue(true)
            .build()));
    private final NumberSetting delay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay")
            .name("Delay")
            .defaultValue(5.0)
            .min(0.0)
            .max(40.0)
            .step(1.0)
            .build()));
    private int cooldownTicks;
    private int previousSlot = -1;

    public AutoGapModule() {
        super("auto_gap", "Auto Gap", ModuleCategory.PLAYER);
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
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return;
        }
        if (player.getHealth() > this.healthThreshold.value()
                || player.isUsingItem()
                || !this.allowDuringCombat.value() && player.getAttackStrengthScale(0.0F) < 1.0F) {
            return;
        }
        InteractionHand hand = gappleHand(player.getMainHandItem(), player.getOffhandItem());
        if (hand != null) {
            client.gameMode.useItem(player, hand);
            this.cooldownTicks = this.delay.value().intValue();
            return;
        }
        OptionalInt slot = bestGappleSlot(player.getInventory(), this.preferEnchanted.value());
        if (slot.isEmpty()) {
            return;
        }
        int selectedSlot = player.getInventory().getSelectedSlot();
        if (slot.orElseThrow() != selectedSlot) {
            this.previousSlot = selectedSlot;
            InventoryActions.selectHotbarSlot(player, slot.orElseThrow());
        }
        client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        this.cooldownTicks = this.delay.value().intValue();
    }

    static OptionalInt bestGappleSlot(final Inventory inventory, final boolean preferEnchanted) {
        OptionalInt enchanted = InventoryActions.findHotbarSlot(inventory, stack -> stack.is(Items.ENCHANTED_GOLDEN_APPLE));
        OptionalInt golden = InventoryActions.findHotbarSlot(inventory, stack -> stack.is(Items.GOLDEN_APPLE));
        if (preferEnchanted && enchanted.isPresent()) {
            return enchanted;
        }
        return golden.isPresent() ? golden : enchanted;
    }

    private static InteractionHand gappleHand(final ItemStack mainHand, final ItemStack offHand) {
        if (isGapple(mainHand)) {
            return InteractionHand.MAIN_HAND;
        }
        if (isGapple(offHand)) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    private static boolean isGapple(final ItemStack stack) {
        return stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE);
    }
}
