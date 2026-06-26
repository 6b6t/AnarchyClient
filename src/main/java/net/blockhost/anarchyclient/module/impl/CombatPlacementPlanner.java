package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.placement.BlockPlacer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

final class CombatPlacementPlanner {

    static final Predicate<ItemStack> COMBAT_BLOCKS = stack -> stack.is(Items.OBSIDIAN)
            || stack.is(Items.CRYING_OBSIDIAN)
            || stack.is(Items.ENDER_CHEST)
            || stack.is(Items.RESPAWN_ANCHOR);
    static final Predicate<ItemStack> HARD_BLOCKS = stack -> stack.is(Items.OBSIDIAN)
            || stack.is(Items.CRYING_OBSIDIAN)
            || stack.is(Items.ENDER_CHEST);

    private CombatPlacementPlanner() {
    }

    static int placeBatch(final Minecraft client, final Module owner, final Iterable<BlockPos> positions,
                          final Predicate<ItemStack> itemPredicate, final Options options) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null) {
            return 0;
        }

        List<BlockPos> ordered = orderedTargets(player.position(), unique(positions));
        List<BlockPlacer.PlacementRequest> requests = new ArrayList<>();
        for (BlockPos pos : ordered) {
            if (!shouldTry(client, player, pos, options)) {
                continue;
            }
            if (options.support() && BlockPlacer.findPlacementTarget(client.level, pos).isEmpty()) {
                for (BlockPos support : supportPositions(client.level, pos)) {
                    if (shouldTry(client, player, support, options.allowEntitySupport())) {
                        requests.add(new BlockPlacer.PlacementRequest(support, itemPredicate));
                    }
                }
            }
            requests.add(new BlockPlacer.PlacementRequest(pos, itemPredicate));
        }
        int placed = BlockPlacer.placeBatch(client, owner.id(), requests, options.blocksPerTick(),
                options.rotate(), options.maxTurnDegrees());
        return placed;
    }

    static List<BlockPos> unique(final Iterable<BlockPos> positions) {
        Set<BlockPos> seen = new LinkedHashSet<>();
        for (BlockPos pos : positions) {
            if (pos != null) {
                seen.add(pos.immutable());
            }
        }
        return List.copyOf(seen);
    }

    static List<BlockPos> orderedTargets(final Vec3 origin, final Iterable<BlockPos> positions) {
        List<BlockPos> ordered = new ArrayList<>();
        for (BlockPos pos : positions) {
            ordered.add(pos.immutable());
        }
        ordered.sort(Comparator.comparingDouble(pos -> Vec3.atCenterOf(pos).distanceToSqr(origin)));
        return ordered;
    }

    static List<BlockPos> supportPositions(final ClientLevel level, final BlockPos target) {
        List<BlockPos> positions = new ArrayList<>();
        for (Direction direction : supportOrder()) {
            BlockPos support = target.relative(direction);
            Direction face = direction.getOpposite();
            if (level.getBlockState(support).isFaceSturdy(level, support, face)) {
                continue;
            }
            positions.add(support);
        }
        return positions;
    }

    static boolean isReplaceable(final ClientLevel level, final BlockPos pos) {
        return level != null && BlockPlacer.needsPlacement(level, pos);
    }

    static boolean isCombatBlock(final ClientLevel level, final BlockPos pos) {
        if (level == null) {
            return false;
        }
        return level.getBlockState(pos).is(Blocks.OBSIDIAN)
                || level.getBlockState(pos).is(Blocks.CRYING_OBSIDIAN)
                || level.getBlockState(pos).is(Blocks.ENDER_CHEST)
                || level.getBlockState(pos).is(Blocks.RESPAWN_ANCHOR)
                || level.getBlockState(pos).is(Blocks.BEDROCK);
    }

    static boolean entityBlocks(final ClientLevel level, final BlockPos pos, final boolean includeLiving) {
        if (level == null) {
            return true;
        }
        AABB box = new AABB(pos).deflate(0.001);
        for (Entity entity : level.getEntities(null, box)) {
            if (!entity.isAlive() || entity.isSpectator()) {
                continue;
            }
            if (!includeLiving && entity instanceof LivingEntity) {
                continue;
            }
            return true;
        }
        return false;
    }

    static boolean playerInside(final Player player, final BlockPos pos) {
        return player != null && player.getBoundingBox().intersects(new AABB(pos).deflate(0.001));
    }

    static boolean withinRange(final LocalPlayer player, final BlockPos pos, final double range) {
        return range <= 0.0 || player.distanceToSqr(Vec3.atCenterOf(pos)) <= range * range;
    }

    private static boolean shouldTry(final Minecraft client, final LocalPlayer player, final BlockPos pos,
                                     final Options options) {
        return shouldTry(client, player, pos, options.placeRange(), options.avoidSelf(), options.avoidEntities());
    }

    private static boolean shouldTry(final Minecraft client, final LocalPlayer player, final BlockPos pos,
                                     final boolean allowLivingSupport) {
        return shouldTry(client, player, pos, 0.0, false, !allowLivingSupport);
    }

    private static boolean shouldTry(final Minecraft client, final LocalPlayer player, final BlockPos pos,
                                     final double placeRange, final boolean avoidSelf, final boolean avoidEntities) {
        return client.level != null
                && isReplaceable(client.level, pos)
                && withinRange(player, pos, placeRange)
                && (!avoidSelf || !playerInside(player, pos))
                && (!avoidEntities || !entityBlocks(client.level, pos, true));
    }

    private static List<Direction> supportOrder() {
        return List.of(Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP);
    }

    record Options(int blocksPerTick, boolean rotate, float maxTurnDegrees, double placeRange,
                   boolean support, boolean allowEntitySupport, boolean avoidSelf, boolean avoidEntities) {

        static Options of(final int blocksPerTick, final boolean rotate, final double placeRange) {
            return new Options(blocksPerTick, rotate, 70.0F, placeRange, true, true, true, true);
        }

        Options withSupport(final boolean enabled) {
            return new Options(this.blocksPerTick, this.rotate, this.maxTurnDegrees, this.placeRange, enabled,
                    this.allowEntitySupport, this.avoidSelf, this.avoidEntities);
        }

        Options withAvoidSelf(final boolean enabled) {
            return new Options(this.blocksPerTick, this.rotate, this.maxTurnDegrees, this.placeRange, this.support,
                    this.allowEntitySupport, enabled, this.avoidEntities);
        }

        Options withAvoidEntities(final boolean enabled) {
            return new Options(this.blocksPerTick, this.rotate, this.maxTurnDegrees, this.placeRange, this.support,
                    this.allowEntitySupport, this.avoidSelf, enabled);
        }
    }
}
