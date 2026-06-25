package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.Comparator;
import java.util.List;

public final class PlayerRadarHudModule extends HudElementModule {

    private final NumberSetting maxPlayers = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("max_players")
            .name("Max Players")
            .defaultValue(6.0)
            .min(1.0)
            .max(20.0)
            .step(1.0)
            .build()));
    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(256.0)
            .min(16.0)
            .max(1024.0)
            .step(16.0)
            .build()));

    public PlayerRadarHudModule() {
        super("player_radar_hud", "Player Radar", "Top Right");
    }

    @Override
    protected List<String> lines(final Minecraft client) {
        if (client.level == null) {
            return List.of();
        }
        double rangeSqr = this.range.value() * this.range.value();
        return client.level.players().stream()
                .filter(player -> player != client.player)
                .filter(player -> player.distanceToSqr(client.player) <= rangeSqr)
                .sorted(Comparator.comparingDouble(player -> player.distanceToSqr(client.player)))
                .limit(this.maxPlayers.value().intValue())
                .map(player -> line(client, player))
                .toList();
    }

    static String line(final Minecraft client, final Player player) {
        String prefix = AnarchyClient.FRIENDS.isFriend(player.getName().getString()) ? "F " : "";
        return prefix + player.getName().getString()
                + " " + Math.round(client.player.distanceTo(player)) + "m"
                + " " + Math.round(player.getHealth() + player.getAbsorptionAmount()) + "h";
    }
}
