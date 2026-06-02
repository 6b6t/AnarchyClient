package net.blockhost.anarchyclient.module.impl;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

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
                && !(entity instanceof ArmorStand)
                && living.isAlive()
                && !living.isDeadOrDying()
                && !living.isSpectator();
    }

    static boolean isAllowedTarget(final Entity entity, final Player player, final Options options) {
        if (!isValidLivingTarget(entity, player)) {
            return false;
        }
        if (!options.invisibles() && entity.isInvisible()) {
            return false;
        }
        if (options.ignoreFriends() && isFriend(entity, options.friends())) {
            return false;
        }
        if (options.ignoreTeams() && entity.getTeam() != null && player.getTeam() != null && entity.getTeam().isAlliedTo(player.getTeam())) {
            return false;
        }
        if (options.antiBot() && looksLikeBot(entity)) {
            return false;
        }
        return options.players() && isPlayer(entity)
                || options.hostiles() && isHostile(entity)
                || options.passives() && isPassive(entity);
    }

    static boolean isFriend(final Entity entity, final String value) {
        if (!(entity instanceof Player)) {
            return false;
        }
        Set<String> friends = parseNames(value);
        return friends.contains(entity.getScoreboardName().toLowerCase(Locale.ROOT));
    }

    static Set<String> parseNames(final String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(value.split("[,|\\s]+"))
                .map(name -> name.trim().toLowerCase(Locale.ROOT))
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    private static boolean looksLikeBot(final Entity entity) {
        String name = entity.getScoreboardName();
        return name.isBlank()
                || name.length() > 32
                || name.startsWith("CIT-")
                || name.startsWith("NPC")
                || entity.tickCount < 5;
    }

    record Options(boolean players, boolean hostiles, boolean passives, boolean invisibles,
                   boolean ignoreFriends, String friends, boolean ignoreTeams, boolean antiBot) {
    }
}
