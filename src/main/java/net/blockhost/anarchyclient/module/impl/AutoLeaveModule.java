package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class AutoLeaveModule extends Module {

    private final NumberSetting health = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("health")
            .name("Health")
            .defaultValue(6.0)
            .min(0.0)
            .max(36.0)
            .step(0.5)
            .build()));
    private final BooleanSetting players = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("players")
            .name("Players")
            .defaultValue(false)
            .build()));
    private final NumberSetting playerRange = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("player_range")
            .name("Range")
            .defaultValue(12.0)
            .min(2.0)
            .max(96.0)
            .step(1.0)
            .build()));
    private boolean left;

    public AutoLeaveModule() {
        super("auto_leave", "Auto Leave", ModuleCategory.PLAYER);
    }

    @Override
    protected void onEnable() {
        this.left = false;
    }

    @Override
    public void tick(final Minecraft client) {
        if (this.left || client.player == null || client.level == null) {
            return;
        }
        if (client.player.getHealth() + client.player.getAbsorptionAmount() <= this.health.value()) {
            this.disconnect(client, "health reached " + String.format(java.util.Locale.ROOT, "%.1f",
                    client.player.getHealth() + client.player.getAbsorptionAmount()));
            return;
        }
        if (this.players.value() && untrustedPlayerNear(client, this.playerRange.value())) {
            this.disconnect(client, "untrusted player entered range");
        }
    }

    private void disconnect(final Minecraft client, final String reason) {
        ClientPacketListener listener = client.getConnection();
        Connection connection = listener == null ? null : listener.getConnection();
        if (connection != null) {
            this.left = true;
            connection.disconnect(Component.literal("Auto Leave: " + reason));
        }
    }

    private static boolean untrustedPlayerNear(final Minecraft client, final double range) {
        double rangeSqr = range * range;
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof Player player
                    && player != client.player
                    && player.isAlive()
                    && !player.isSpectator()
                    && client.player.distanceToSqr(player) <= rangeSqr) {
                return true;
            }
        }
        return false;
    }
}
