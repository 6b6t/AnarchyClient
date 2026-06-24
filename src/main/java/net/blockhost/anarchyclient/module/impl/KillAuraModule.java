package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.rotation.Rotation;
import net.blockhost.anarchyclient.rotation.RotationManager;
import net.blockhost.anarchyclient.rotation.RotationRequest;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.blockhost.anarchyclient.target.TargetPolicy;
import net.blockhost.anarchyclient.target.TargetPriority;
import net.blockhost.anarchyclient.target.TargetQuery;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

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
            .addAllOptions(List.of("Nearest", "Type", "Lowest HP", "Lowest Armor", "Crosshair", "Age"))
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
    private final AttackCadence attackCadence = new AttackCadence(new Random());

    public KillAuraModule() {
        super("kill_aura", "Kill Aura", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null) {
            return;
        }
        if (this.pauseInGui.value() && client.gui.screen() != null) {
            return;
        }
        if (this.pauseUsingItem.value() && player.isUsingItem()) {
            return;
        }

        Optional<LivingEntity> target = this.findTarget(client, player);
        if (target.isEmpty()) {
            RotationManager.clear(this.id());
            return;
        }
        LivingEntity entity = target.orElseThrow();
        if (this.rotate.value()) {
            Rotation targetRotation = Rotation.lookingAt(entity.getBoundingBox().getCenter(), player.getEyePosition());
            RotationManager.request(new RotationRequest(this.id(), targetRotation, 100,
                    this.maxTurnDegrees.value().floatValue(), 2));
            RotationManager.apply(player);
        }
        if (!this.attackCadence.ready()) {
            return;
        }
        if (player.getAttackStrengthScale(0.0F) < this.minCharge.value()) {
            return;
        }

        client.gameMode.attack(player, entity);
        player.swing(InteractionHand.MAIN_HAND);
        this.attackCadence.reset(this.minCps.value(), this.maxCps.value());
    }

    Optional<LivingEntity> findTarget(final Minecraft client, final LocalPlayer player) {
        Comparator<LivingEntity> comparator = TargetPriority.fromSetting(this.priority.value()).comparator(player);
        double rangeValue = this.range.value();
        double rangeSqr = rangeValue * rangeValue;
        return TargetQuery.livingTargets(client.level.entitiesForRendering(), player, this.targetPolicy())
                .filter(entity -> player.distanceToSqr(entity) <= rangeSqr)
                .filter(entity -> !this.requireLineOfSight.value() || player.hasLineOfSight(entity))
                .filter(entity -> isInsideFov(player, entity, this.fov.value()))
                .min(comparator);
    }

    private TargetPolicy targetPolicy() {
        return TargetPolicy.of(
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

    @Override
    protected void onDisable() {
        this.attackCadence.clear();
        RotationManager.clear(this.id());
    }
}
