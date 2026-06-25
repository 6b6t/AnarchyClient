package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.rotation.Rotation;
import net.blockhost.anarchyclient.rotation.RotationManager;
import net.blockhost.anarchyclient.rotation.RotationRequest;
import net.blockhost.anarchyclient.rotation.RotationTurnMode;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.blockhost.anarchyclient.target.RenderedEntityCache;
import net.blockhost.anarchyclient.target.TargetPolicy;
import net.blockhost.anarchyclient.target.TargetPriority;
import net.blockhost.anarchyclient.target.TargetQuery;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class AimAssistModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(5.0)
            .min(1.0)
            .max(8.0)
            .step(0.1)
            .build()));
    private final NumberSetting fov = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("fov")
            .name("FOV")
            .defaultValue(120.0)
            .min(15.0)
            .max(360.0)
            .step(5.0)
            .build()));
    private final SelectSetting priority = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("priority")
            .name("Priority")
            .defaultValue("Crosshair")
            .addAllOptions(List.of("Nearest", "Type", "Lowest HP", "Lowest Armor", "Crosshair", "Age"))
            .build()));
    private final SelectSetting targetPoint = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("target_point")
            .name("Point")
            .defaultValue("Body")
            .addAllOptions(List.of("Head", "Body", "Feet"))
            .build()));
    private final BooleanSetting instant = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("instant")
            .name("Instant")
            .defaultValue(false)
            .build()));
    private final NumberSetting maxTurnDegrees = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("max_turn_degrees")
            .name("Turn")
            .defaultValue(12.0)
            .min(1.0)
            .max(90.0)
            .step(1.0)
            .build()));
    private final SelectSetting rotationMode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("rotation_mode")
            .name("Turn Mode")
            .defaultValue("Linear")
            .addAllOptions(List.of("Stepped", "Linear"))
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

    public AimAssistModule() {
        super("aim_assist", "Aim Assist", ModuleCategory.COMBAT);
        this.maxTurnDegrees.visibleWhen(() -> !this.instant.value());
        this.rotationMode.visibleWhen(() -> !this.instant.value());
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            return;
        }
        if (this.pauseInGui.value() && client.gui.screen() != null) {
            RotationManager.clear(this.id());
            return;
        }
        if (this.pauseUsingItem.value() && player.isUsingItem()) {
            RotationManager.clear(this.id());
            return;
        }
        Optional<LivingEntity> target = this.findTarget(player);
        if (target.isEmpty()) {
            RotationManager.clear(this.id());
            return;
        }
        Rotation targetRotation = Rotation.lookingAt(aimPoint(target.orElseThrow(), this.targetPoint.value()), player.getEyePosition());
        RotationManager.request(new RotationRequest(
                this.id(),
                targetRotation,
                80,
                this.instant.value() ? 180.0F : this.maxTurnDegrees.value().floatValue(),
                2,
                0.5F,
                this.instant.value() ? RotationTurnMode.INSTANT : turnMode(this.rotationMode.value()),
                true
        ));
        RotationManager.apply(player);
    }

    private Optional<LivingEntity> findTarget(final LocalPlayer player) {
        TargetPolicy policy = TargetPolicy.of(
                this.players.value(),
                this.hostileMobs.value(),
                this.passiveMobs.value(),
                this.invisibles.value(),
                this.ignoreFriends.value(),
                this.ignoreTeams.value(),
                this.antiBot.value()
        );
        Comparator<LivingEntity> comparator = TargetPriority.fromSetting(this.priority.value()).comparator(player);
        double rangeSqr = this.range.value() * this.range.value();
        return TargetQuery.livingTargets(RenderedEntityCache.entities(), player, policy)
                .filter(entity -> player.distanceToSqr(entity) <= rangeSqr)
                .filter(entity -> !this.requireLineOfSight.value() || player.hasLineOfSight(entity))
                .filter(entity -> KillAuraTargetSelector.isInsideFov(player, entity, this.fov.value()))
                .min(comparator);
    }

    static Vec3 aimPoint(final LivingEntity entity, final String targetPoint) {
        return switch (targetPoint) {
            case "Head" -> new Vec3(entity.getX(), entity.getEyeY(), entity.getZ());
            case "Feet" -> entity.position();
            default -> entity.getBoundingBox().getCenter();
        };
    }

    private static RotationTurnMode turnMode(final String value) {
        return "Stepped".equals(value) ? RotationTurnMode.STEPPED : RotationTurnMode.LINEAR;
    }

    @Override
    protected void onEnable() {
        RenderedEntityCache.subscribe(this.id());
    }

    @Override
    protected void onDisable() {
        RenderedEntityCache.unsubscribe(this.id());
        RotationManager.clear(this.id());
    }
}
