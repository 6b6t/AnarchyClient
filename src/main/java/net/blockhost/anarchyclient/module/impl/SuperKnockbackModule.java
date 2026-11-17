package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class SuperKnockbackModule extends Module {

    private final BooleanSetting resetSprint = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("reset_sprint")
            .name("Reset")
            .defaultValue(true)
            .build()));

    public SuperKnockbackModule() {
        super("super_knockback", "Super Knockback", ModuleCategory.COMBAT,
                "Sends a stop and start sprint packet before each hit while you sprint, so the server applies sprint knockback.");
    }

    @Override
    public boolean attackEntity(final Minecraft client, final Player player, final Entity target) {
        ClientPacketListener connection = client.getConnection();
        if (this.resetSprint.value() && player != null && connection != null && player.isSprinting()) {
            // Toggling the client flag sends nothing, because LocalPlayer only syncs sprint state once per tick.
            connection.send(new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
            connection.send(new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
        }
        return false;
    }
}
