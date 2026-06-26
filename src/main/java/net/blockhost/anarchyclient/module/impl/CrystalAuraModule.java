package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.render.CuboidMarker;
import net.blockhost.anarchyclient.render.MarkerManager;
import net.blockhost.anarchyclient.render.MarkerStyle;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class CrystalAuraModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(6.0)
            .min(1.0)
            .max(8.0)
            .step(0.5)
            .build()));
    private final NumberSetting placeRadius = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("place_radius")
            .name("Place Radius")
            .defaultValue(3.0)
            .min(1.0)
            .max(5.0)
            .step(1.0)
            .build()));
    private final NumberSetting maxSelfDamage = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("max_self_damage")
            .name("Self Damage")
            .defaultValue(8.0)
            .min(0.0)
            .max(36.0)
            .step(1.0)
            .build()));
    private final BooleanSetting attackCrystals = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("attack")
            .name("Attack")
            .defaultValue(true)
            .build()));
    private final BooleanSetting placeCrystals = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("place")
            .name("Place")
            .defaultValue(true)
            .build()));
    private final NumberSetting minDamage = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("min_damage")
            .name("Min Damage")
            .defaultValue(4.0)
            .min(0.0)
            .max(20.0)
            .step(0.5)
            .build()));
    private final NumberSetting facePlaceHealth = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("face_place_health")
            .name("Face Place")
            .defaultValue(8.0)
            .min(0.0)
            .max(36.0)
            .step(1.0)
            .build()));
    private final NumberSetting breakDelay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("break_delay")
            .name("Break Delay")
            .defaultValue(2.0)
            .min(0.0)
            .max(20.0)
            .step(1.0)
            .build()));
    private final NumberSetting placeDelay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("place_delay")
            .name("Place Delay")
            .defaultValue(2.0)
            .min(0.0)
            .max(20.0)
            .step(1.0)
            .build()));
    private final BooleanSetting render = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("render")
            .name("Render")
            .defaultValue(true)
            .build()));
    private int breakCooldown;
    private int placeCooldown;

    public CrystalAuraModule() {
        super("crystal_aura", "Crystal Aura", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        Player target = CombatTargets.nearestEnemy(client, this.range.value());
        if (player == null || target == null || client.level == null || client.gameMode == null) {
            return;
        }
        if (this.breakCooldown > 0) {
            this.breakCooldown--;
        }
        if (this.placeCooldown > 0) {
            this.placeCooldown--;
        }
        if (this.attackCrystals.value() && this.breakCooldown <= 0 && this.attackCrystal(client, player, target)) {
            this.breakCooldown = this.breakDelay.value().intValue();
            return;
        }
        if (!this.placeCrystals.value() || this.placeCooldown > 0) {
            return;
        }
        SelfDamagePolicy policy = new SelfDamagePolicy(this.maxSelfDamage.value(), 3.0, 1.1);
        ExplosionPlacementScorer scorer = new ExplosionPlacementScorer(policy, 6.0);
        List<ExplosionPlacementScorer.PlacementScore> scores = TargetPlacementPlanner.explosionPlacements(
                client,
                player,
                target,
                this.placeRadius.value().intValue(),
                scorer
        );
        boolean facePlace = targetHealth(target) <= this.facePlaceHealth.value();
        List<ExplosionPlacementScorer.PlacementScore> allowed = scores.stream()
                .filter(score -> facePlace || score.targetDamage() >= this.minDamage.value())
                .toList();
        if (!allowed.isEmpty()) {
            ExplosionPlacementScorer.PlacementScore best = allowed.getFirst();
            WorldInteraction.ActionResult result = WorldInteraction.useOnBlock(client, this, best.pos().below(), Direction.UP,
                    stack -> stack.is(Items.END_CRYSTAL), true);
            if (result == WorldInteraction.ActionResult.DONE) {
                this.placeCooldown = this.placeDelay.value().intValue();
                this.debugValue("damage", String.format("%.1f/%.1f", best.targetDamage(), best.selfDamage()));
                this.mark(best.pos());
            }
        }
    }

    private boolean attackCrystal(final Minecraft client, final LocalPlayer player, final Player target) {
        Entity best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        boolean facePlace = targetHealth(target) <= this.facePlaceHealth.value();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof EndCrystal) || entity.distanceTo(target) > this.range.value()) {
                continue;
            }
            Vec3 explosion = entity.position();
            double targetDamage = DamageEstimator.explosionDamage(target, explosion, 6.0);
            double selfDamage = DamageEstimator.explosionDamage(player, explosion, 6.0);
            if (selfDamage > this.maxSelfDamage.value() || !facePlace && targetDamage < this.minDamage.value()) {
                continue;
            }
            double score = targetDamage - selfDamage * 0.65;
            if (score > bestScore) {
                best = entity;
                bestScore = score;
            }
        }
        if (best == null) {
            return false;
        }
        client.gameMode.attack(client.player, best);
        client.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        return true;
    }

    private void mark(final BlockPos pos) {
        if (this.render.value()) {
            MarkerManager.put(new CuboidMarker(this.id() + ":place", new AABB(pos), MarkerStyle.CYAN, 8));
        }
    }

    private static double targetHealth(final Player target) {
        return target.getHealth() + target.getAbsorptionAmount();
    }
}
