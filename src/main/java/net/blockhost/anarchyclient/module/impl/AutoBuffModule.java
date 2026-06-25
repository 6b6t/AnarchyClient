package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.inventory.SilentHotbar;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Items;

import java.util.OptionalInt;

public final class AutoBuffModule extends Module {

    private final BooleanSetting strength = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("strength")
            .name("Strength")
            .defaultValue(true)
            .build()));
    private final BooleanSetting speed = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("speed")
            .name("Speed")
            .defaultValue(true)
            .build()));
    private final BooleanSetting regeneration = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("regeneration")
            .name("Regen")
            .defaultValue(false)
            .build()));
    private final BooleanSetting splashOnly = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("splash_only")
            .name("Splash")
            .defaultValue(true)
            .build()));
    private final NumberSetting cooldown = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("cooldown")
            .name("Cooldown")
            .defaultValue(80.0)
            .min(10.0)
            .max(400.0)
            .step(5.0)
            .build()));
    private int cooldownTicks;

    public AutoBuffModule() {
        super("auto_buff", "Auto Buff", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null || client.gui.screen() != null || player.isUsingItem()) {
            return;
        }
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return;
        }
        Holder<MobEffect> effect = this.nextEffect(player);
        if (effect == null) {
            return;
        }
        OptionalInt slot = AutoPotModule.findPotionSlot(player.getInventory(), effect, true);
        if (slot.isEmpty()) {
            return;
        }
        if (!this.splashOnly.value() || player.getInventory().getItem(slot.orElseThrow()).is(Items.SPLASH_POTION)) {
            SilentHotbar.select(player, this.id(), slot.orElseThrow(), SilentHotbar.PRIORITY_COMBAT, 8, true);
            player.setXRot(90.0F);
            client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
            this.cooldownTicks = this.cooldown.value().intValue();
        }
    }

    private Holder<MobEffect> nextEffect(final LocalPlayer player) {
        if (this.strength.value() && !player.hasEffect(MobEffects.STRENGTH)) {
            return MobEffects.STRENGTH;
        }
        if (this.speed.value() && !player.hasEffect(MobEffects.SPEED)) {
            return MobEffects.SPEED;
        }
        if (this.regeneration.value() && !player.hasEffect(MobEffects.REGENERATION)) {
            return MobEffects.REGENERATION;
        }
        return null;
    }

    static boolean shouldBuff(final boolean enabled, final boolean hasEffect) {
        return enabled && !hasEffect;
    }
}
