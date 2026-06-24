package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.blockhost.anarchyclient.rotation.RotationTurnMode;
import net.blockhost.anarchyclient.target.TargetPolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

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
    private final SelectSetting rotationMode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("rotation_mode")
            .name("Turn Mode")
            .defaultValue("Stepped")
            .addAllOptions(List.of("Stepped", "Linear", "Instant"))
            .build()));
    private final NumberSetting rotationResetThreshold = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("rotation_reset_threshold")
            .name("Aim Reset")
            .defaultValue(1.0)
            .min(0.0)
            .max(15.0)
            .step(0.5)
            .build()));
    private final BooleanSetting pauseRotationsInInventory = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("pause_rotations_in_inventory")
            .name("Inv Pause")
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
    private final KillAuraTargetSelector targetSelector = new KillAuraTargetSelector();
    private final KillAuraRotationPlanner rotationPlanner = new KillAuraRotationPlanner();
    private final KillAuraTiming timing = new KillAuraTiming();

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
            this.rotationPlanner.clear(this);
            return;
        }
        LivingEntity entity = target.orElseThrow();
        if (this.rotate.value()) {
            RotationTurnMode turnMode = KillAuraRotationPlanner.turnMode(this.rotationMode.value());
            this.rotationPlanner.request(
                    this,
                    player,
                    entity,
                    this.maxTurnDegrees.value().floatValue(),
                    this.rotationResetThreshold.value().floatValue(),
                    turnMode,
                    this.pauseRotationsInInventory.value()
            );
        }
        if (!this.timing.readyToAttack(player, this.minCharge.value())) {
            return;
        }

        client.gameMode.attack(player, entity);
        player.swing(InteractionHand.MAIN_HAND);
        this.timing.markAttack(this.minCps.value(), this.maxCps.value());
    }

    Optional<LivingEntity> findTarget(final Minecraft client, final LocalPlayer player) {
        return this.targetSelector.findTarget(
                client,
                player,
                this.targetPolicy(),
                this.priority.value(),
                this.range.value(),
                this.fov.value(),
                this.requireLineOfSight.value()
        );
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
        return KillAuraTargetSelector.isInsideFov(player, target, fov);
    }

    @Override
    protected void onDisable() {
        this.timing.clear();
        this.rotationPlanner.clear(this);
    }
}
