package net.blockhost.anarchyclient.target;

import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.server.ServerObserver;
import net.blockhost.anarchyclient.server.ServerProfileStore;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;
import java.util.regex.Pattern;

public final class TargetClassifier {

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
    );
    private static final Pattern STAFF_HINT_PATTERN = Pattern.compile("(admin|mod|staff|helper|owner|srmod)", Pattern.CASE_INSENSITIVE);

    private TargetClassifier() {
    }

    public static TargetKind kind(final Entity entity) {
        if (entity instanceof Player) {
            return TargetKind.PLAYER;
        }
        if (entity instanceof WaterAnimal) {
            return TargetKind.WATER_CREATURE;
        }
        if (isHostile(entity)) {
            return TargetKind.HOSTILE;
        }
        if (entity instanceof NeutralMob) {
            return TargetKind.NEUTRAL;
        }
        if (isPassive(entity)) {
            return TargetKind.PASSIVE;
        }
        return TargetKind.UNKNOWN;
    }

    public static boolean isPlayer(final Entity entity) {
        return entity instanceof Player;
    }

    public static boolean isHostile(final Entity entity) {
        return entity instanceof Enemy
                || entity instanceof Mob mob && mob.getType().getCategory() == MobCategory.MONSTER;
    }

    public static boolean isPassive(final Entity entity) {
        return entity instanceof Bat
                || entity instanceof Allay
                || entity instanceof Mob mob && mob.getType().getCategory().isFriendly();
    }

    public static boolean isValidLivingTarget(final Entity entity, final Player player, final boolean allowDead) {
        return entity instanceof LivingEntity living
                && entity != player
                && !(entity instanceof ArmorStand)
                && (allowDead || living.isAlive() && !living.isDeadOrDying())
                && !living.isSpectator();
    }

    public static boolean looksLikeBot(final String name, final int tickCount) {
        return name.isBlank()
                || name.length() > 32
                || name.startsWith("CIT-")
                || name.startsWith("NPC")
                || name.startsWith("[NPC]")
                || UUID_PATTERN.matcher(name).matches()
                || tickCount < 5;
    }

    public static boolean isFriend(final Entity entity) {
        return entity != null && entity instanceof Player && AnarchyClient.FRIENDS.isFriend(entity.getScoreboardName());
    }

    public static boolean isKnownStaffName(final String name) {
        String normalized = normalize(name);
        if (normalized.isBlank()) {
            return false;
        }
        return ServerProfileStore.staffNames(ServerObserver.snapshot().rootDomain()).stream()
                .map(TargetClassifier::normalize)
                .anyMatch(normalized::equals);
    }

    public static boolean looksLikeStaffName(final String name) {
        return name != null && STAFF_HINT_PATTERN.matcher(name).find();
    }

    private static String normalize(final String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
