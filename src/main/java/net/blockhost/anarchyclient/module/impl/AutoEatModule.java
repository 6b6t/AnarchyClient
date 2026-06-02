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

    public AutoEatModule() {
        super("auto_eat", "Auto Eat", ModuleCategory.PLAYER);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.screen != null || client.gameMode == null) {
            return;
        }
        if (player.isUsingItem() || !shouldEat(player.getFoodData().getFoodLevel(), player.getHealth(), this.hungerThreshold.value(), this.healthThreshold.value())) {
            return;
        }
        if (!this.allowDuringCombat.value() && player.getAttackStrengthScale(0.0F) < 1.0F) {
            return;
        }
        InteractionHand hand = foodHand(player.getMainHandItem(), player.getOffhandItem());
        if (hand != null) {
            client.gameMode.useItem(player, hand);
        }
    }

    static boolean shouldEat(final int hunger, final float health, final double hungerThreshold, final double healthThreshold) {
        return hunger <= hungerThreshold || health <= healthThreshold;
    }

    private static InteractionHand foodHand(final ItemStack mainHand, final ItemStack offHand) {
        if (mainHand.has(DataComponents.FOOD)) {
            return InteractionHand.MAIN_HAND;
        }
        if (offHand.has(DataComponents.FOOD)) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }
}
