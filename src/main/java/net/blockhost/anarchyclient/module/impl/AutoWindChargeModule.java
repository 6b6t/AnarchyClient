package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.inventory.SilentHotbar;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

import java.util.Optional;

public final class AutoWindChargeModule extends Module {

    private final NumberSetting fallDistance = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("fall_distance")
            .name("Fall")
            .defaultValue(3.0)
            .min(0.0)
            .max(30.0)
            .step(0.5)
            .build()));
    private final NumberSetting cooldown = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("cooldown")
            .name("Cooldown")
            .defaultValue(10.0)
            .min(1.0)
            .max(80.0)
            .step(1.0)
            .build()));
    private int cooldownTicks;

    public AutoWindChargeModule() {
        super("auto_wind_charge", "Auto Wind Charge", ModuleCategory.MOVEMENT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null || client.gui.screen() != null || player.onGround()) {
            return;
        }
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return;
        }
        if (player.fallDistance < this.fallDistance.value()) {
            return;
        }
        Optional<InteractionHand> hand = SilentHotbar.usableHand(player, this.id(), stack -> stack.is(Items.WIND_CHARGE),
                SilentHotbar.PRIORITY_LIFE, 6, true);
        if (hand.isPresent()) {
            player.setXRot(90.0F);
            client.gameMode.useItem(player, hand.orElseThrow());
            this.cooldownTicks = this.cooldown.value().intValue();
        }
    }
}
