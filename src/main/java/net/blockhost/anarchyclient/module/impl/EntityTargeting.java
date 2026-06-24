package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.target.TargetClassifier;
import net.blockhost.anarchyclient.target.TargetPolicy;
import net.blockhost.anarchyclient.target.TargetQuery;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Set;

final class EntityTargeting {

    private EntityTargeting() {
    }

    static boolean isPlayer(final Entity entity) {
        return TargetClassifier.isPlayer(entity);
    }

    static boolean isHostile(final Entity entity) {
        return TargetClassifier.isHostile(entity);
    }

    static boolean isPassive(final Entity entity) {
        return TargetClassifier.isPassive(entity);
    }

    static boolean isValidLivingTarget(final Entity entity, final Player player) {
        return TargetClassifier.isValidLivingTarget(entity, player, false);
    }

    static boolean isAllowedTarget(final Entity entity, final Player player, final Options options) {
        return TargetQuery.allowed(entity, player, options.toPolicy());
    }

    static boolean isFriend(final Entity entity, final String value) {
        return TargetClassifier.isFriend(entity, value);
    }

    static Set<String> parseNames(final String value) {
        return TargetClassifier.parseNames(value);
    }

    static boolean looksLikeBot(final String name, final int tickCount) {
        return TargetClassifier.looksLikeBot(name, tickCount);
    }

    record Options(boolean players, boolean hostiles, boolean passives, boolean invisibles,
                   boolean ignoreFriends, String friends, boolean ignoreTeams, boolean antiBot) {

        TargetPolicy toPolicy() {
            return TargetPolicy.of(this.players, this.hostiles, this.passives, this.invisibles,
                    this.ignoreFriends, this.friends, this.ignoreTeams, this.antiBot);
        }
    }
}
