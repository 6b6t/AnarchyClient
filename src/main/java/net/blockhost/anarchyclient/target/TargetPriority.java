package net.blockhost.anarchyclient.target;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Comparator;

public enum TargetPriority {
    TYPE,
    DISTANCE,
    HEALTH,
    ARMOR,
    CROSSHAIR,
    AGE;

    public Comparator<LivingEntity> comparator(final Player player) {
        return switch (this) {
            case TYPE -> Comparator.<LivingEntity>comparingInt(entity -> typeWeight(TargetClassifier.kind(entity)))
                    .thenComparingDouble(player::distanceToSqr);
            case HEALTH -> Comparator.comparingDouble(LivingEntity::getHealth).thenComparingDouble(player::distanceToSqr);
            case ARMOR -> Comparator.comparingInt(LivingEntity::getArmorValue).thenComparingDouble(player::distanceToSqr);
            case CROSSHAIR -> Comparator.comparingDouble(entity -> crosshairAngle(player, entity));
            case AGE -> Comparator.<LivingEntity>comparingInt(entity -> -entity.tickCount).thenComparingDouble(player::distanceToSqr);
            case DISTANCE -> Comparator.comparingDouble(player::distanceToSqr);
        };
    }

    public static TargetPriority fromSetting(final String value) {
        return switch (value) {
            case "Type" -> TYPE;
            case "Lowest HP", "Health" -> HEALTH;
            case "Lowest Armor", "Armor" -> ARMOR;
            case "Crosshair" -> CROSSHAIR;
            case "Age" -> AGE;
            default -> DISTANCE;
        };
    }

    private static int typeWeight(final TargetKind kind) {
        return switch (kind) {
            case PLAYER -> 0;
            case HOSTILE -> 1;
            case NEUTRAL -> 2;
            case PASSIVE, WATER_CREATURE -> 3;
            case UNKNOWN -> 4;
        };
    }

    private static double crosshairAngle(final Player player, final LivingEntity entity) {
        double dot = player.getViewVector(0.0F).normalize()
                .dot(entity.getBoundingBox().getCenter().subtract(player.getEyePosition()).normalize());
        return Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dot))));
    }
}
