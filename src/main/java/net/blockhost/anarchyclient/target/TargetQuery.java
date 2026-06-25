package net.blockhost.anarchyclient.target;

import net.blockhost.anarchyclient.AnarchyClient;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class TargetQuery {

    private TargetQuery() {
    }

    public static boolean allowed(final Entity entity, final Player player, final TargetPolicy policy) {
        if (!TargetClassifier.isValidLivingTarget(entity, player, policy.dead())) {
            return false;
        }
        if (!policy.invisibles() && entity.isInvisible()) {
            return false;
        }
        if (policy.ignoreFriends() && TargetClassifier.isPlayer(entity)
                && AnarchyClient.FRIENDS.isFriend(entity.getScoreboardName())) {
            return false;
        }
        if (policy.ignoreTeams() && entity.getTeam() != null && player.getTeam() != null
                && entity.getTeam().isAlliedTo(player.getTeam())) {
            return false;
        }
        if (policy.antiBot() && TargetClassifier.isPlayer(entity)
                && TargetClassifier.looksLikeBot(entity.getScoreboardName(), entity.tickCount)) {
            return false;
        }
        return policy.allows(TargetClassifier.kind(entity));
    }

    public static Stream<LivingEntity> livingTargets(final Iterable<? extends Entity> entities, final Player player, final TargetPolicy policy) {
        return StreamSupport.stream(entities.spliterator(), false)
                .filter(entity -> allowed(entity, player, policy))
                .map(LivingEntity.class::cast);
    }

    public static Optional<LivingEntity> closest(final Iterable<? extends Entity> entities, final Player player,
                                                final TargetPolicy policy, final double range,
                                                final Comparator<LivingEntity> comparator) {
        double rangeSqr = range * range;
        return livingTargets(entities, player, policy)
                .filter(entity -> player.distanceToSqr(entity) <= rangeSqr)
                .min(comparator);
    }
}
