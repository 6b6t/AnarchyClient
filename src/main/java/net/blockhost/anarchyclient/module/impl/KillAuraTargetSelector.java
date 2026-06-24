package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.target.TargetPolicy;
import net.blockhost.anarchyclient.target.TargetPriority;
import net.blockhost.anarchyclient.target.TargetQuery;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Comparator;
import java.util.Optional;

final class KillAuraTargetSelector {

    Optional<LivingEntity> findTarget(final Minecraft client, final LocalPlayer player, final TargetPolicy policy,
                                      final String prioritySetting, final double range, final double fov,
                                      final boolean requireLineOfSight) {
        Comparator<LivingEntity> comparator = TargetPriority.fromSetting(prioritySetting).comparator(player);
        double rangeSqr = range * range;
        return TargetQuery.livingTargets(client.level.entitiesForRendering(), player, policy)
                .filter(entity -> player.distanceToSqr(entity) <= rangeSqr)
                .filter(entity -> !requireLineOfSight || player.hasLineOfSight(entity))
                .filter(entity -> isInsideFov(player, entity, fov))
                .min(comparator);
    }

    static boolean isInsideFov(final LocalPlayer player, final LivingEntity target, final double fov) {
        if (fov >= 360.0) {
            return true;
        }
        double dot = player.getViewVector(0.0F).normalize()
                .dot(target.getBoundingBox().getCenter().subtract(player.getEyePosition()).normalize());
        double angle = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dot))));
        return angle <= fov / 2.0;
    }
}
