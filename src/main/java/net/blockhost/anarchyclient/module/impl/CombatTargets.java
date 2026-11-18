package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.target.TargetPolicy;
import net.blockhost.anarchyclient.target.TargetQuery;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

final class CombatTargets {

    private CombatTargets() {
    }

    /**
     * Players only, friends skipped. Teammates are left to the Teams module (checked in {@link #nearestEnemy}), so a
     * server that puts everyone in one scoreboard team still has targets while Teams is off.
     */
    private static final TargetPolicy ENEMY_PLAYERS = TargetPolicy.of(true, false, false, true, true, false, false);

    /** The locked target when Target Lock holds a player in range, else the nearest allowed player. */
    static Player nearestEnemy(final Minecraft client, final double range) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            return null;
        }
        Optional<LivingEntity> locked = TargetLockModule.preferred(client.level.players(), player, ENEMY_PLAYERS, range);
        if (locked.isPresent() && locked.get() instanceof Player lockedPlayer && TeamsModule.allows(lockedPlayer, player)) {
            return lockedPlayer;
        }
        double rangeSqr = range * range;
        Player best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Player target : client.level.players()) {
            if (!TargetQuery.allowed(target, player, ENEMY_PLAYERS) || !TeamsModule.allows(target, player)) {
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

    /**
     * Returns whether the block is within {@code range} of the player's eyes, measured to the
     * nearest point of the block like the server's block interaction check.
     */
    static boolean canReachBlock(final LocalPlayer player, final BlockPos pos, final double range) {
        return range > 0.0 && new AABB(pos).distanceToSqr(player.getEyePosition()) <= range * range;
    }

    /** Same as {@link #canReachBlock(LocalPlayer, BlockPos, double)} with the player's block interaction range. */
    static boolean canReachBlock(final LocalPlayer player, final BlockPos pos) {
        return canReachBlock(player, pos, player.blockInteractionRange());
    }

    /** Returns whether a point is within {@code range} of the player's eyes. */
    static boolean canReachPoint(final LocalPlayer player, final Vec3 point, final double range) {
        return range > 0.0 && player.getEyePosition().distanceToSqr(point) <= range * range;
    }

    /**
     * Returns whether the entity's hitbox is within {@code range} of the player's eyes, like the
     * server's entity interaction check.
     */
    static boolean canReachEntity(final LocalPlayer player, final Entity entity, final double range) {
        return range > 0.0 && entity.getBoundingBox().distanceToSqr(player.getEyePosition()) <= range * range;
    }

    /** Same as {@link #canReachEntity(LocalPlayer, Entity, double)} with the player's entity interaction range. */
    static boolean canReachEntity(final LocalPlayer player, final Entity entity) {
        return canReachEntity(player, entity, player.entityInteractionRange());
    }
}
