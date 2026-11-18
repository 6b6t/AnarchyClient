package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.EquipmentSlot;

public final class ElytraRecastModule extends Module {

    private final NumberSetting cooldown = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("cooldown")
            .name("Cooldown")
            .defaultValue(10.0)
            .min(1.0)
            .max(60.0)
            .step(1.0)
            .build()));
    private int cooldownTicks;
    private boolean wasGliding;

    public ElytraRecastModule() {
        super("elytra_recast", "Elytra Recast", ModuleCategory.MOVEMENT,
                "Resends the elytra start packet while airborne so gliding restarts after it stops.");
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || player.connection == null) {
            this.wasGliding = false;
            return;
        }
        if (player.isFallFlying()) {
            this.wasGliding = true;
            return;
        }
        // Only restart a glide that stopped mid-air; otherwise every jump or ladder climb would start gliding.
        if (!this.wasGliding || player.onGround() || player.onClimbable() || player.getAbilities().flying
                || player.isInWater() || !ExtraElytraModule.canGlide(player.getItemBySlot(EquipmentSlot.CHEST))) {
            this.wasGliding = false;
            return;
        }
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return;
        }
        player.connection.send(new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
        this.cooldownTicks = this.cooldown.value().intValue();
    }

    @Override
    protected void onDisable() {
        this.wasGliding = false;
        this.cooldownTicks = 0;
    }
}
