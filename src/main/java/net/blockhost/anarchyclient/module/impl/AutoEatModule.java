package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public final class AutoEatModule extends Module {

    private final NumberSetting hungerThreshold = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("hunger_threshold")
            .name("Hunger")
            .defaultValue(14.0)
            .min(1.0)
            .max(19.0)
            .step(1.0)
            .build()));
    private final NumberSetting healthThreshold = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("health_threshold")
            .name("Health")
            .defaultValue(10.0)
            .min(1.0)
            .max(20.0)
            .step(0.5)
            .build()));
    private final BooleanSetting allowDuringCombat = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("allow_during_combat")
            .name("In Combat")
            .defaultValue(false)
            .build()));
    private final BooleanSetting selectHotbarFood = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("select_hotbar_food")
            .name("Hotbar")
            .defaultValue(true)
            .build()));
    private final BooleanSetting restoreSlot = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("restore_slot")
            .name("Restore")
            .defaultValue(true)
            .build()));
    private final BooleanSetting useGoldenApples = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("use_golden_apples")
            .name("Gapples")
            .defaultValue(false)
            .build()));
    private int previousSlot = -1;

    public AutoEatModule() {
        super("auto_eat", "Auto Eat", ModuleCategory.PLAYER);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.screen != null || client.gameMode == null) {
            return;
        }
        if (this.previousSlot >= 0 && !player.isUsingItem()) {
            if (this.restoreSlot.value()) {
                InventoryActions.selectHotbarSlot(player, this.previousSlot);
            }
            this.previousSlot = -1;
        }
        if (player.isUsingItem() || !shouldEat(player.getFoodData().getFoodLevel(), player.getHealth(), this.hungerThreshold.value(), this.healthThreshold.value())) {
            return;
        }
        if (!this.allowDuringCombat.value() && player.getAttackStrengthScale(0.0F) < 1.0F) {
            return;
        }
        InteractionHand hand = foodHand(player.getMainHandItem(), player.getOffhandItem(), this.useGoldenApples.value());
        if (hand == null && this.selectHotbarFood.value()) {
            int selectedSlot = player.getInventory().getSelectedSlot();
            java.util.OptionalInt foodSlot = InventoryActions.findHotbarSlot(player.getInventory(), stack -> isFood(stack, this.useGoldenApples.value()));
            if (foodSlot.isPresent() && foodSlot.orElseThrow() != selectedSlot) {
                this.previousSlot = selectedSlot;
                InventoryActions.selectHotbarSlot(player, foodSlot.orElseThrow());
                hand = InteractionHand.MAIN_HAND;
            }
        }
        if (hand != null) {
            client.gameMode.useItem(player, hand);
        }
    }

    static boolean shouldEat(final int hunger, final float health, final double hungerThreshold, final double healthThreshold) {
        return hunger <= hungerThreshold || health <= healthThreshold;
    }

    private static InteractionHand foodHand(final ItemStack mainHand, final ItemStack offHand, final boolean allowGoldenApples) {
        if (isFood(mainHand, allowGoldenApples)) {
            return InteractionHand.MAIN_HAND;
        }
        if (isFood(offHand, allowGoldenApples)) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    private static boolean isFood(final ItemStack stack, final boolean allowGoldenApples) {
        if (!stack.has(DataComponents.FOOD)) {
            return false;
        }
        return allowGoldenApples
                || !stack.is(net.minecraft.world.item.Items.GOLDEN_APPLE)
                && !stack.is(net.minecraft.world.item.Items.ENCHANTED_GOLDEN_APPLE);
    }
}
