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
import net.minecraft.world.item.BedItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class BedAuraModule extends Module {

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
    private final BooleanSetting render = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("render")
            .name("Render")
            .defaultValue(true)
            .build()));
    private int cooldown;

    public BedAuraModule() {
        super("bed_aura", "Bed Aura", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null || client.level.dimension() == Level.OVERWORLD) {
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
        double minDamage = minimumDamage(target, this.minDamage.value(), this.facePlaceHealth.value());
        ScoredBed bed = bestBed(client, target, this.range.value().intValue(), this.maxSelfDamage.value(), minDamage);
        if (bed != null) {
            client.gameMode.useItemOn(client.player, net.minecraft.world.InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(bed.pos()), Direction.UP, bed.pos(), false));
            client.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            this.cooldown = this.delay.value().intValue();
            this.mark(bed.pos());
            this.debugValue("damage", String.format("%.1f/%.1f", bed.targetDamage(), bed.selfDamage()));
            return;
        }
        if (!this.place.value()) {
            return;
        }
        ScoredBed placement = bestBedPlacement(client, target, this.range.value().intValue(),
                this.maxSelfDamage.value(), minDamage);
        if (placement != null) {
            WorldInteraction.ActionResult result = WorldInteraction.useOnBlock(client, this, placement.pos().below(), Direction.UP,
                    stack -> stack.getItem() instanceof BedItem, true);
            if (result == WorldInteraction.ActionResult.DONE) {
                this.cooldown = this.delay.value().intValue();
                this.mark(placement.pos());
            }
        }
    }

    static BlockPos findBed(final Minecraft client, final BlockPos center, final int radius) {
        if (client.level == null) {
            return null;
        }
        for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
            for (int y = center.getY() - 1; y <= center.getY() + 2; y++) {
                for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (client.level.getBlockState(pos).getBlock() instanceof BedBlock) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }

    private static ScoredBed bestBed(final Minecraft client, final Player target, final int radius,
                                     final double maxSelfDamage, final double minDamage) {
        if (client.level == null || client.player == null) {
            return null;
        }
        List<ScoredBed> scores = new ArrayList<>();
        BlockPos center = target.blockPosition();
        for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
            for (int y = center.getY() - 1; y <= center.getY() + 2; y++) {
                for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (client.level.getBlockState(pos).getBlock() instanceof BedBlock) {
                        score(client, target, pos, maxSelfDamage, minDamage).ifPresent(scores::add);
                    }
                }
            }
        }
        return best(scores);
    }

    private static ScoredBed bestBedPlacement(final Minecraft client, final Player target, final int radius,
                                              final double maxSelfDamage, final double minDamage) {
        if (client.level == null) {
            return null;
        }
        List<ScoredBed> scores = new ArrayList<>();
        BlockPos center = target.blockPosition();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos foot = center.relative(direction);
            BlockPos head = foot.relative(direction);
            if (center.distSqr(foot) > radius * radius
                    || !client.level.getBlockState(foot).canBeReplaced()
                    || !client.level.getBlockState(head).canBeReplaced()
                    || client.level.getBlockState(foot.below()).canBeReplaced()) {
                continue;
            }
            score(client, target, foot, maxSelfDamage, minDamage).ifPresent(scores::add);
        }
        return best(scores);
    }

    private static java.util.Optional<ScoredBed> score(final Minecraft client, final Player target, final BlockPos pos,
                                                       final double maxSelfDamage, final double minDamage) {
        Vec3 explosion = Vec3.atCenterOf(pos);
        double targetDamage = DamageEstimator.explosionDamage(target, explosion, 5.0);
        double selfDamage = DamageEstimator.explosionDamage(client.player, explosion, 5.0);
        if (targetDamage < minDamage || selfDamage > maxSelfDamage) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(new ScoredBed(pos.immutable(), targetDamage, selfDamage));
    }

    private static ScoredBed best(final List<ScoredBed> scores) {
        return scores.stream()
                .max(Comparator.comparingDouble(ScoredBed::value))
                .orElse(null);
    }

    private static double minimumDamage(final Player target, final double minDamage, final double facePlaceHealth) {
        return target.getHealth() + target.getAbsorptionAmount() <= facePlaceHealth ? 0.0 : minDamage;
    }

    private void mark(final BlockPos pos) {
        if (this.render.value()) {
            MarkerManager.put(new CuboidMarker(this.id() + ":bed", new AABB(pos), MarkerStyle.CYAN, 8));
        }
    }

    private record ScoredBed(BlockPos pos, double targetDamage, double selfDamage) {

        double value() {
            return this.targetDamage - this.selfDamage * 0.7;
        }
    }
}
