package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.inventory.SilentHotbar;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Optional;
import java.util.function.Predicate;

public final class SmartEatModule extends Module {

    private final NumberSetting healthThreshold = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("health_threshold")
            .name("Health")
            .defaultValue(12.0)
            .min(1.0)
            .max(20.0)
            .step(0.5)
            .build()));
    private final NumberSetting hungerThreshold = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("hunger_threshold")
            .name("Hunger")
            .defaultValue(14.0)
            .min(1.0)
            .max(19.0)
            .step(1.0)
            .build()));
    private final NumberSetting gappleHealth = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("gapple_health")
            .name("Gap HP")
            .defaultValue(8.0)
            .min(1.0)
            .max(20.0)
            .step(0.5)
            .build()));
    private final NumberSetting soupHealth = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("soup_health")
            .name("Soup HP")
            .defaultValue(10.0)
            .min(1.0)
            .max(20.0)
            .step(0.5)
            .build()));
    private final BooleanSetting allowGapples = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("gapples")
            .name("Gapples")
            .defaultValue(true)
            .build()));
    private final BooleanSetting allowSoup = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("soup")
            .name("Soup")
            .defaultValue(true)
            .build()));
    private final BooleanSetting allowDuringCombat = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("allow_combat")
            .name("Combat")
            .defaultValue(false)
            .build()));

    public SmartEatModule() {
        super("smart_eat", "Smart Eat", ModuleCategory.PLAYER);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null || client.gui.screen() != null || player.isUsingItem()) {
            return;
        }
        if (!this.allowDuringCombat.value() && player.getAttackStrengthScale(0.0F) < 1.0F) {
            return;
        }
        Predicate<ItemStack> predicate = this.pickPredicate(player);
        if (predicate == null) {
            return;
        }
        Optional<InteractionHand> hand = SilentHotbar.usableHand(player, this.id(), predicate,
                SilentHotbar.PRIORITY_LIFE, 8, true);
        hand.ifPresent(value -> client.gameMode.useItem(player, value));
    }

    private Predicate<ItemStack> pickPredicate(final LocalPlayer player) {
        float health = player.getHealth() + player.getAbsorptionAmount();
        if (this.allowSoup.value() && health <= this.soupHealth.value()) {
            return SmartEatModule::isSoup;
        }
        if (this.allowGapples.value() && health <= this.gappleHealth.value()) {
            return stack -> stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE);
        }
        if (health <= this.healthThreshold.value() || player.getFoodData().getFoodLevel() <= this.hungerThreshold.value()) {
            return stack -> stack.has(DataComponents.FOOD)
                    && !stack.is(Items.GOLDEN_APPLE)
                    && !stack.is(Items.ENCHANTED_GOLDEN_APPLE);
        }
        return null;
    }

    static boolean isSoup(final ItemStack stack) {
        return stack.is(Items.MUSHROOM_STEW)
                || stack.is(Items.RABBIT_STEW)
                || stack.is(Items.BEETROOT_SOUP)
                || stack.is(Items.SUSPICIOUS_STEW);
    }
}
