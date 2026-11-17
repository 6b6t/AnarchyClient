package net.blockhost.anarchyclient.module.impl;

import com.google.gson.JsonObject;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.blockhost.anarchyclient.config.ModuleConfigMigration;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.render.CuboidMarker;
import net.blockhost.anarchyclient.render.MarkerManager;
import net.blockhost.anarchyclient.render.MarkerStyle;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.rotation.Rotation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class BedAuraModule extends Module implements ModuleConfigMigration {

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
            .defaultValue(6.0)
            .min(0.0)
            .max(36.0)
            .step(0.5)
            .build()));
    private final NumberSetting maxSelfDamage = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("max_self_damage")
            .name("Self Damage")
            .defaultValue(12.0)
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
    private static final int PLACE_RADIUS = 2;
    private static final float MAX_FACING_ERROR = 35.0F;
    private int cooldown;

    public BedAuraModule() {
        super("bed_aura", "Bed Aura", ModuleCategory.COMBAT, "Places and detonates beds within reach near targets outside the Overworld. Damage values are in HP.");
    }

    /** Moves configs saved with the old defaults to the current ones. */
    @Override
    public void migrateConfig(final JsonObject moduleJson, final int savedVersion) {
        if (savedVersion >= VANILLA_DAMAGE_DEFAULTS) {
            return;
        }
        ModuleConfigMigration.upgradeDefault(moduleJson, "min_damage", 5.0, 6.0);
        ModuleConfigMigration.upgradeDefault(moduleJson, "max_self_damage", 8.0, 12.0);
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
            InteractionResult result = client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(bed.clickPos()), Direction.UP, bed.clickPos(), false));
            if (result.consumesAction()) {
                client.player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
                this.cooldown = this.delay.value().intValue();
                this.mark(bed.head());
                this.debugValue("damage", String.format("%.1f/%.1f", bed.targetDamage(), bed.selfDamage()));
            }
            return;
        }
        if (!this.place.value()) {
            return;
        }
        ScoredBed placement = bestBedPlacement(client, target, this.maxSelfDamage.value(), minDamage);
        if (placement != null) {
            WorldInteraction.ActionResult result = WorldInteraction.useOnBlock(client, this, placement.clickPos().below(),
                    Direction.UP, stack -> stack.is(ItemTags.BEDS), true);
            if (result == WorldInteraction.ActionResult.DONE) {
                this.cooldown = this.delay.value().intValue();
                this.mark(placement.head());
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
                    BlockState state = client.level.getBlockState(pos);
                    // The explosion happens at the head, whichever half is clicked.
                    if (state.getBlock() instanceof BedBlock && state.getValue(BedBlock.PART) == BedPart.HEAD
                            && CombatTargets.canReachBlock(client.player, pos)) {
                        BlockPos foot = pos.relative(state.getValue(BedBlock.FACING).getOpposite());
                        score(client, target, pos, pos, foot, maxSelfDamage, minDamage).ifPresent(scores::add);
                    }
                }
            }
        }
        return best(scores);
    }

    /**
     * Scores bed placements next to the target. Vanilla puts the foot on the clicked block and the
     * head one block further in the direction the player faces, so the head position follows from
     * the rotation the placement will use.
     */
    private static ScoredBed bestBedPlacement(final Minecraft client, final Player target,
                                              final double maxSelfDamage, final double minDamage) {
        LocalPlayer player = client.player;
        if (client.level == null || player == null) {
            return null;
        }
        List<ScoredBed> scores = new ArrayList<>();
        BlockPos center = target.blockPosition();
        Vec3 eye = player.getEyePosition();
        for (int x = -PLACE_RADIUS; x <= PLACE_RADIUS; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = -PLACE_RADIUS; z <= PLACE_RADIUS; z++) {
                    BlockPos foot = center.offset(x, y, z);
                    Optional<Direction> facing = placementFacing(eye, foot);
                    if (facing.isEmpty() || !CombatTargets.canReachBlock(player, foot.below())) {
                        continue;
                    }
                    BlockPos head = foot.relative(facing.orElseThrow());
                    if (!client.level.getBlockState(foot).canBeReplaced()
                            || !client.level.getBlockState(head).canBeReplaced()
                            || client.level.getBlockState(foot.below()).canBeReplaced()
                            || !client.level.isUnobstructed(null, Shapes.block().move(foot))) {
                        continue;
                    }
                    score(client, target, foot, head, foot, maxSelfDamage, minDamage).ifPresent(scores::add);
                }
            }
        }
        return best(scores);
    }

    /**
     * Returns the direction the player faces after rotating to click the top of the block under
     * {@code foot}, which is the direction vanilla points the bed. Empty when the yaw is too close
     * to a diagonal for the facing to be reliable.
     */
    static Optional<Direction> placementFacing(final Vec3 eye, final BlockPos foot) {
        Vec3 hit = Vec3.atBottomCenterOf(foot);
        if (Math.abs(hit.x - eye.x) < 1.0E-3 && Math.abs(hit.z - eye.z) < 1.0E-3) {
            return Optional.empty();
        }
        float yaw = Rotation.lookingAt(hit, eye).yaw();
        Direction facing = Direction.fromYRot(yaw);
        if (Math.abs(Mth.wrapDegrees(yaw - facing.toYRot())) > MAX_FACING_ERROR) {
            return Optional.empty();
        }
        return Optional.of(facing);
    }

    private static Optional<ScoredBed> score(final Minecraft client, final Player target, final BlockPos clickPos,
                                             final BlockPos head, final BlockPos foot, final double maxSelfDamage,
                                             final double minDamage) {
        Vec3 explosion = DamageEstimator.blockCenter(head);
        // Vanilla removes both bed halves before the explosion, so the bed does not shield anyone.
        Set<BlockPos> removed = Set.of(head.immutable(), foot.immutable());
        double targetDamage = DamageEstimator.explosionDamage(target, explosion, DamageEstimator.BED_POWER, removed);
        double selfDamage = DamageEstimator.explosionDamage(client.player, explosion, DamageEstimator.BED_POWER, removed);
        double selfHealth = client.player.getHealth() + client.player.getAbsorptionAmount();
        if (targetDamage < minDamage || selfDamage > maxSelfDamage || selfDamage >= selfHealth) {
            return Optional.empty();
        }
        return Optional.of(new ScoredBed(clickPos.immutable(), head.immutable(), targetDamage, selfDamage));
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

    private record ScoredBed(BlockPos clickPos, BlockPos head, double targetDamage, double selfDamage) {

        double value() {
            return this.targetDamage - this.selfDamage * 0.7;
        }
    }
}
