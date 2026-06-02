package net.blockhost.anarchyclient.module.impl;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;

final class EntityTargeting {

    private EntityTargeting() {
    }

    static boolean isPlayer(final Entity entity) {
        return entity instanceof Player;
    }

    static boolean isHostile(final Entity entity) {
        return entity instanceof Monster
                || entity instanceof Mob mob && mob.getType().getCategory() == MobCategory.MONSTER;
    }

    static boolean isPassive(final Entity entity) {
        return entity instanceof Animal
                || entity instanceof Mob mob && mob.getType().getCategory() == MobCategory.CREATURE;
    }

    static boolean isValidLivingTarget(final Entity entity, final Player player) {
        return entity instanceof LivingEntity living
                && entity != player
                && living.isAlive()
                && !living.isDeadOrDying()
                && !living.isSpectator();
    }
}
