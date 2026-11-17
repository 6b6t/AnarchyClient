package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.inventory.ItemUseControl;
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
    private static final int SLOT_HOLD_TICKS = 2;
    private int cooldownTicks;
    private boolean drinking;

    public AutoBuffModule() {
        super("auto_buff", "Auto Buff", ModuleCategory.COMBAT,
                "Throws strength, speed, or regeneration potions at your feet, or drinks them when Splash is off, when the effect is missing.");
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null) {
            return;
        }
        if (this.drinking) {
            // Hold the potion slot and the use until the drink finishes; both would lapse otherwise.
            if (client.gui.screen() == null && player.isUsingItem()
                    && player.getUsedItemHand() == InteractionHand.MAIN_HAND
                    && SilentHotbar.select(player, this.id(), player.getInventory().getSelectedSlot(),
                    SilentHotbar.PRIORITY_COMBAT, SLOT_HOLD_TICKS, true)) {
                ItemUseControl.use(client, this.id(), InteractionHand.MAIN_HAND);
                return;
            }
            this.stopDrinking(client);
        }
        if (client.gui.screen() != null || player.isUsingItem()) {
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
        boolean splash = player.getInventory().getItem(slot.orElseThrow()).is(Items.SPLASH_POTION);
        if (this.splashOnly.value() && !splash) {
            return;
        }
        if (!SilentHotbar.select(player, this.id(), slot.orElseThrow(), SilentHotbar.PRIORITY_COMBAT,
                SLOT_HOLD_TICKS, true)) {
            return;
        }
        this.cooldownTicks = this.cooldown.value().intValue();
        if (splash) {
            // The use packet carries the rotation, so the pitch can be restored right after the throw.
            float pitch = player.getXRot();
            player.setXRot(90.0F);
            client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
            player.setXRot(pitch);
            SilentHotbar.clear(this.id());
            return;
        }
        this.drinking = ItemUseControl.use(client, this.id(), InteractionHand.MAIN_HAND);
        if (!this.drinking) {
            SilentHotbar.clear(this.id());
        }
    }

    @Override
    protected void onDisable() {
        this.stopDrinking(Minecraft.getInstance());
        this.cooldownTicks = 0;
    }

    private void stopDrinking(final Minecraft client) {
        ItemUseControl.release(client, this.id());
        SilentHotbar.clear(this.id());
        this.drinking = false;
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
