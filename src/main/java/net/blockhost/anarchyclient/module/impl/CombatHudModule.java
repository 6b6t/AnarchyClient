package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.AnarchyClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

import java.util.Comparator;
import java.util.List;

public final class CombatHudModule extends HudElementModule {

    public CombatHudModule() {
        super("combat_hud", "Combat HUD", "Top Right");
    }

    @Override
    protected int color() {
        return 0xFFE56A6A;
    }

    @Override
    protected List<String> lines(final Minecraft client) {
        Player target = nearestEnemy(client);
        String targetLine = target == null
                ? "Target none"
                : "Target " + target.getName().getString() + " "
                + String.format("%.1fm %.1fh", client.player.distanceTo(target), target.getHealth() + target.getAbsorptionAmount());
        return List.of(
                targetLine,
                "Health " + String.format("%.1f", client.player.getHealth() + client.player.getAbsorptionAmount()),
                "Totems " + InventoryHudModule.count(client.player.getInventory(), Items.TOTEM_OF_UNDYING),
                "Crystals " + InventoryHudModule.count(client.player.getInventory(), Items.END_CRYSTAL),
                "XP " + InventoryHudModule.count(client.player.getInventory(), Items.EXPERIENCE_BOTTLE)
        );
    }

    static Player nearestEnemy(final Minecraft client) {
        if (client.level == null || client.player == null) {
            return null;
        }
        return client.level.players().stream()
                .filter(player -> player != client.player)
                .filter(player -> !player.isRemoved() && player.isAlive())
                .filter(player -> !AnarchyClient.FRIENDS.isFriend(player.getName().getString()))
                .min(Comparator.comparingDouble(player -> player.distanceToSqr(client.player)))
                .orElse(null);
    }
}
