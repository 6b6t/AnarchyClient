package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.render.CuboidMarker;
import net.blockhost.anarchyclient.render.MarkerManager;
import net.blockhost.anarchyclient.render.MarkerStyle;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class AnchorAuraModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(5.0)
            .min(1.0)
            .max(8.0)
            .step(0.5)
            .build()));
    private final NumberSetting minDamage = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("min_damage")
            .name("Min Damage")
            .defaultValue(5.0)
            .min(0.0)
            .max(20.0)
            .step(0.5)
            .build()));
    private final NumberSetting maxSelfDamage = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("max_self_damage")
            .name("Self Damage")
            .defaultValue(8.0)
            .min(0.0)
            .max(36.0)
            .step(1.0)
            .build()));
    private final NumberSetting facePlaceHealth = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("face_place_health")
            .name("Face Place")
            .defaultValue(8.0)
            .min(0.0)
            .max(36.0)
            .step(1.0)
            .build()));
    private final NumberSetting delay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay")
            .name("Delay")
            .defaultValue(3.0)
            .min(0.0)
            .max(20.0)
            .step(1.0)
            .build()));
    private final BooleanSetting place = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("place")
            .name("Place")
            .defaultValue(true)
            .build()));
    private final BooleanSetting charge = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("charge")
            .name("Charge")
            .defaultValue(true)
            .build()));
    private final BooleanSetting render = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("render")
            .name("Render")
            .defaultValue(true)
            .build()));
    private int cooldown;

    public AnchorAuraModule() {
        super("anchor_aura", "Anchor Aura", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null) {
            return;
        }
        if (this.cooldown > 0) {
            this.cooldown--;
            return;
        }
        Player target = CombatTargets.nearestEnemy(client, this.range.value());
        if (target == null) {
            return;
        }
        boolean canExplode = client.level.dimension() != Level.NETHER;
        ScoredAnchor anchor = bestAnchor(client, target, this.range.value().intValue(), this.maxSelfDamage.value(),
                minimumDamage(target, this.minDamage.value(), this.facePlaceHealth.value()));
        if (anchor != null) {
            BlockState state = client.level.getBlockState(anchor.pos());
            int charges = state.getValue(RespawnAnchorBlock.CHARGE);
            if (canExplode && charges > 0) {
                client.gameMode.useItemOn(client.player, net.minecraft.world.InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(anchor.pos()), Direction.UP, anchor.pos(), false));
                client.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                this.mark(anchor.pos());
                this.cooldown = this.delay.value().intValue();
                this.debugValue("damage", String.format("%.1f/%.1f", anchor.targetDamage(), anchor.selfDamage()));
                return;
            }
            if (this.charge.value() && charges < RespawnAnchorBlock.MAX_CHARGES) {
                WorldInteraction.ActionResult result = WorldInteraction.useOnBlock(client, this, anchor.pos(), Direction.UP,
                        stack -> stack.is(Items.GLOWSTONE), true);
                if (result == WorldInteraction.ActionResult.DONE) {
                    this.cooldown = this.delay.value().intValue();
                }
                return;
            }
            return;
        }
        if (!this.place.value()) {
            return;
        }
        ScoredAnchor placement = bestAnchorPlacement(client, target, this.range.value().intValue(),
                this.maxSelfDamage.value(), minimumDamage(target, this.minDamage.value(), this.facePlaceHealth.value()));
        if (placement != null) {
            BlockPlacement.PlacementResult result = BlockPlacement.place(client, this, placement.pos(), true, 70.0F,
                    stack -> stack.is(Items.RESPAWN_ANCHOR));
            if (result == BlockPlacement.PlacementResult.PLACED) {
                this.cooldown = this.delay.value().intValue();
                this.mark(placement.pos());
            }
        }
    }

    static BlockPos findAnchor(final Minecraft client, final BlockPos center, final int radius) {
        if (client.level == null) {
            return null;
        }
        for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
            for (int y = center.getY() - 1; y <= center.getY() + 2; y++) {
                for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (client.level.getBlockState(pos).getBlock() instanceof RespawnAnchorBlock) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }

    private static ScoredAnchor bestAnchor(final Minecraft client, final Player target, final int radius,
                                           final double maxSelfDamage, final double minDamage) {
        if (client.level == null || client.player == null) {
            return null;
        }
        List<ScoredAnchor> scores = new ArrayList<>();
        for (int x = target.blockPosition().getX() - radius; x <= target.blockPosition().getX() + radius; x++) {
            for (int y = target.blockPosition().getY() - 1; y <= target.blockPosition().getY() + 2; y++) {
                for (int z = target.blockPosition().getZ() - radius; z <= target.blockPosition().getZ() + radius; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (client.level.getBlockState(pos).getBlock() instanceof RespawnAnchorBlock) {
                        score(client, target, pos, maxSelfDamage, minDamage).ifPresent(scores::add);
                    }
                }
            }
        }
        return best(scores);
    }

    private static ScoredAnchor bestAnchorPlacement(final Minecraft client, final Player target, final int radius,
                                                    final double maxSelfDamage, final double minDamage) {
        if (client.level == null) {
            return null;
        }
        List<ScoredAnchor> scores = new ArrayList<>();
        BlockPos center = target.blockPosition();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            for (int y = 0; y <= 1; y++) {
                BlockPos pos = center.relative(direction).above(y);
                if (center.distSqr(pos) <= radius * radius && CombatPlacementPlanner.isReplaceable(client.level, pos)) {
                    score(client, target, pos, maxSelfDamage, minDamage).ifPresent(scores::add);
                }
            }
        }
        return best(scores);
    }

    private static java.util.Optional<ScoredAnchor> score(final Minecraft client, final Player target, final BlockPos pos,
                                                         final double maxSelfDamage, final double minDamage) {
        Vec3 explosion = Vec3.atCenterOf(pos);
        double targetDamage = DamageEstimator.explosionDamage(target, explosion, 5.0);
        double selfDamage = DamageEstimator.explosionDamage(client.player, explosion, 5.0);
        if (targetDamage < minDamage || selfDamage > maxSelfDamage) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(new ScoredAnchor(pos.immutable(), targetDamage, selfDamage));
    }

    private static ScoredAnchor best(final List<ScoredAnchor> scores) {
        return scores.stream()
                .max(Comparator.comparingDouble(ScoredAnchor::value))
                .orElse(null);
    }

    private static double minimumDamage(final Player target, final double minDamage, final double facePlaceHealth) {
        return target.getHealth() + target.getAbsorptionAmount() <= facePlaceHealth ? 0.0 : minDamage;
    }

    private void mark(final BlockPos pos) {
        if (this.render.value()) {
            MarkerManager.put(new CuboidMarker(this.id() + ":anchor", new AABB(pos), MarkerStyle.CYAN, 8));
        }
    }

    private record ScoredAnchor(BlockPos pos, double targetDamage, double selfDamage) {

        double value() {
            return this.targetDamage - this.selfDamage * 0.7;
        }
    }
}
