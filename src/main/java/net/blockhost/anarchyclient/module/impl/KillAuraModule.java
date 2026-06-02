package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;

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
            .addAllOptions(List.of("Nearest", "Lowest HP", "Lowest Armor"))
            .build()));
    private final NumberSetting minCps = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("min_cps")
            .name("Min CPS")
            .defaultValue(8.0)
            .min(1.0)
            .max(20.0)
            .step(1.0)
            .build()));
    private final NumberSetting maxCps = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("max_cps")
            .name("Max CPS")
            .defaultValue(12.0)
            .min(1.0)
            .max(20.0)
            .step(1.0)
            .build()));
    private final NumberSetting minCharge = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("min_charge")
            .name("Charge")
            .defaultValue(0.85)
            .min(0.0)
            .max(1.0)
            .step(0.05)
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
    private final BooleanSetting invisibles = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("invisibles")
            .name("Invisibles")
            .defaultValue(false)
            .build()));
    private final BooleanSetting ignoreFriends = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("ignore_friends")
            .name("Friends")
            .defaultValue(true)
            .build()));
    private final StringSetting friends = this.setting(StringSetting.from(StringSetting.builder()
            .id("friends")
            .name("Friend List")
            .defaultValue("")
            .build()));
    private final BooleanSetting ignoreTeams = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("ignore_teams")
            .name("Teams")
            .defaultValue(true)
            .build()));
    private final BooleanSetting antiBot = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("anti_bot")
            .name("Anti Bot")
            .defaultValue(true)
            .build()));
    private final BooleanSetting rotate = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("rotate")
            .name("Rotate")
            .defaultValue(true)
            .build()));
    private final NumberSetting maxTurnDegrees = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("max_turn_degrees")
            .name("Turn")
            .defaultValue(45.0)
            .min(5.0)
            .max(180.0)
            .step(5.0)
            .build()));
    private final BooleanSetting pauseUsingItem = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("pause_using_item")
            .name("Use Pause")
            .defaultValue(true)
            .build()));
    private final BooleanSetting pauseInGui = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("pause_in_gui")
            .name("Pause GUI")
            .defaultValue(true)
            .build()));
    private final Random random = new Random();
    private int attackDelayTicks;

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
        if (this.pauseUsingItem.value() && player.isUsingItem()) {
            return;
        }

        Optional<LivingEntity> target = this.findTarget(client, player);
        if (target.isEmpty()) {
            return;
        }
        LivingEntity entity = target.orElseThrow();
        if (this.rotate.value()) {
            rotateToward(player, entity, this.maxTurnDegrees.value().floatValue());
        }
        if (this.attackDelayTicks > 0) {
            this.attackDelayTicks--;
            return;
        }
        if (player.getAttackStrengthScale(0.0F) < this.minCharge.value()) {
            return;
        }

        client.gameMode.attack(player, entity);
        player.swing(InteractionHand.MAIN_HAND);
        this.attackDelayTicks = this.randomAttackDelay();
    }

    Optional<LivingEntity> findTarget(final Minecraft client, final LocalPlayer player) {
        Comparator<LivingEntity> comparator = switch (this.priority.value()) {
            case "Lowest HP" -> Comparator.comparingDouble(LivingEntity::getHealth).thenComparingDouble(player::distanceToSqr);
            case "Lowest Armor" -> Comparator.comparingInt(LivingEntity::getArmorValue).thenComparingDouble(player::distanceToSqr);
            default -> Comparator.comparingDouble(player::distanceToSqr);
        };

        return findTarget(client.level.entitiesForRendering(), player, this.range.value(), this.fov.value(), this.requireLineOfSight.value(), comparator);
    }

    private Optional<LivingEntity> findTarget(final Iterable<Entity> entities, final LocalPlayer player, final double range,
                                             final double fov, final boolean requireLineOfSight,
                                             final Comparator<LivingEntity> comparator) {
        double rangeSqr = range * range;
        EntityTargeting.Options options = this.targetOptions();
        return toStream(entities)
                .filter(entity -> EntityTargeting.isAllowedTarget(entity, player, options))
                .map(LivingEntity.class::cast)
                .filter(entity -> player.distanceToSqr(entity) <= rangeSqr)
                .filter(entity -> !requireLineOfSight || player.hasLineOfSight(entity))
                .filter(entity -> isInsideFov(player, entity, fov))
                .min(comparator);
    }

    private EntityTargeting.Options targetOptions() {
        return new EntityTargeting.Options(
                this.players.value(),
                this.hostileMobs.value(),
                this.passiveMobs.value(),
                this.invisibles.value(),
                this.ignoreFriends.value(),
                this.friends.value(),
                this.ignoreTeams.value(),
                this.antiBot.value()
        );
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

    private int randomAttackDelay() {
        double lower = Math.min(this.minCps.value(), this.maxCps.value());
        double upper = Math.max(this.minCps.value(), this.maxCps.value());
        double cps = lower + this.random.nextDouble() * (upper - lower);
        return Math.max(1, (int) Math.round(20.0 / cps));
    }

    private static void rotateToward(final LocalPlayer player, final LivingEntity target, final float maxTurnDegrees) {
        Vec3 delta = target.getBoundingBox().getCenter().subtract(player.getEyePosition());
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float targetYRot = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0F;
        float targetXRot = (float) -Math.toDegrees(Math.atan2(delta.y, horizontal));
        player.setYRot(stepAngle(player.getYRot(), targetYRot, maxTurnDegrees));
        player.setXRot(stepAngle(player.getXRot(), targetXRot, maxTurnDegrees));
    }

    private static float stepAngle(final float current, final float target, final float maxStep) {
        float delta = wrapDegrees(target - current);
        float clamped = Math.max(-maxStep, Math.min(maxStep, delta));
        return current + clamped;
    }

    private static float wrapDegrees(final float value) {
        float wrapped = value % 360.0F;
        if (wrapped >= 180.0F) {
            wrapped -= 360.0F;
        }
        if (wrapped < -180.0F) {
            wrapped += 360.0F;
        }
        return wrapped;
    }
}
