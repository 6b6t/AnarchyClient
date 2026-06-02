package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class KillAuraModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(3.0)
            .min(2.0)
            .max(6.0)
            .step(0.1)
            .build()));
    private final NumberSetting fov = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("fov")
            .name("FOV")
            .defaultValue(90.0)
            .min(15.0)
            .max(360.0)
            .step(5.0)
            .build()));
    private final SelectSetting priority = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("priority")
            .name("Priority")
            .defaultValue("Nearest")
            .addAllOptions(List.of("Nearest", "Lowest HP"))
            .build()));
    private final BooleanSetting players = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("players")
            .name("Players")
            .defaultValue(true)
            .build()));
    private final BooleanSetting hostileMobs = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("hostile_mobs")
            .name("Hostiles")
            .defaultValue(false)
            .build()));
    private final BooleanSetting passiveMobs = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("passive_mobs")
            .name("Passives")
            .defaultValue(false)
            .build()));
    private final BooleanSetting requireLineOfSight = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("require_line_of_sight")
            .name("Line Sight")
            .defaultValue(true)
            .build()));
    private final BooleanSetting pauseInGui = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("pause_in_gui")
            .name("Pause GUI")
            .defaultValue(true)
            .build()));

    public KillAuraModule() {
        super("kill_aura", "Kill Aura", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null) {
            return;
        }
        if (this.pauseInGui.value() && client.screen != null) {
            return;
        }
        if (player.getAttackStrengthScale(0.0F) < 1.0F) {
            return;
        }

        Optional<LivingEntity> target = this.findTarget(client, player);
        target.ifPresent(entity -> {
            client.gameMode.attack(player, entity);
            player.swing(InteractionHand.MAIN_HAND);
        });
    }

    Optional<LivingEntity> findTarget(final Minecraft client, final LocalPlayer player) {
        Comparator<LivingEntity> comparator = "Lowest HP".equals(this.priority.value())
                ? Comparator.comparingDouble(LivingEntity::getHealth).thenComparingDouble(player::distanceToSqr)
                : Comparator.comparingDouble(player::distanceToSqr);

        return findTarget(client.level.entitiesForRendering(), player, this.range.value(), this.fov.value(), this.requireLineOfSight.value(), comparator);
    }

    private Optional<LivingEntity> findTarget(final Iterable<Entity> entities, final LocalPlayer player, final double range,
                                             final double fov, final boolean requireLineOfSight,
                                             final Comparator<LivingEntity> comparator) {
        double rangeSqr = range * range;
        return toStream(entities)
                .filter(entity -> this.acceptsTargetType(entity) && EntityTargeting.isValidLivingTarget(entity, player))
                .map(LivingEntity.class::cast)
                .filter(entity -> player.distanceToSqr(entity) <= rangeSqr)
                .filter(entity -> !requireLineOfSight || player.hasLineOfSight(entity))
                .filter(entity -> isInsideFov(player, entity, fov))
                .min(comparator);
    }

    private boolean acceptsTargetType(final Entity entity) {
        return this.players.value() && EntityTargeting.isPlayer(entity)
                || this.hostileMobs.value() && EntityTargeting.isHostile(entity)
                || this.passiveMobs.value() && EntityTargeting.isPassive(entity);
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

    private static java.util.stream.Stream<Entity> toStream(final Iterable<Entity> entities) {
        return java.util.stream.StreamSupport.stream(entities.spliterator(), false);
    }
}
