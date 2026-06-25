package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

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
        if (this.attackCrystals.value() && this.attackCrystal(client, target)) {
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
        if (!scores.isEmpty()) {
            WorldInteraction.useOnBlock(client, this, scores.getFirst().pos().below(), Direction.UP,
                    stack -> stack.is(Items.END_CRYSTAL), true);
        }
    }

    private boolean attackCrystal(final Minecraft client, final Player target) {
        Entity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof EndCrystal && entity.distanceToSqr(target) < bestDistance && entity.distanceTo(target) <= this.range.value()) {
                best = entity;
                bestDistance = entity.distanceToSqr(target);
            }
        }
        if (best == null) {
            return false;
        }
        client.gameMode.attack(client.player, best);
        client.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        return true;
    }
}
