package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.AnarchyClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;

final class CombatTargets {

    private CombatTargets() {
    }

    static Player nearestEnemy(final Minecraft client, final double range) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            return null;
        }
        double rangeSqr = range * range;
        Player best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Player target : client.level.players()) {
            if (target == player
                    || !target.isAlive()
                    || target.isSpectator()
                    || AnarchyClient.FRIENDS.isFriend(target.getScoreboardName())) {
                continue;
            }
            double distance = player.distanceToSqr(target);
            if (distance <= rangeSqr && distance < bestDistance) {
                best = target;
                bestDistance = distance;
            }
        }
        return best;
    }
}
