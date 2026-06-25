package net.blockhost.anarchyclient.module.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

final class BlockTargetScanner {

    private BlockTargetScanner() {
    }

    static List<BlockTarget> scan(final Minecraft client, final int horizontalRadius, final int verticalRadius,
                                  final SortMode sortMode, final int maxResults, final Predicate<BlockTarget> predicate) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || maxResults <= 0) {
            return List.of();
        }
        BlockPos center = player.blockPosition();
        Vec3 eye = player.getEyePosition();
        int horizontal = Math.max(0, horizontalRadius);
        int vertical = Math.max(0, verticalRadius);
        double maxDistanceSqr = Math.max(1.0, horizontal * horizontal + vertical * vertical);
        List<BlockTarget> targets = new ArrayList<>();
        int minY = Math.max(client.level.getMinY(), center.getY() - vertical);
        int maxY = Math.min(client.level.getMinY() + client.level.getHeight() - 1, center.getY() + vertical);
        for (int y = minY; y <= maxY; y++) {
            for (int x = center.getX() - horizontal; x <= center.getX() + horizontal; x++) {
                for (int z = center.getZ() - horizontal; z <= center.getZ() + horizontal; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!client.level.isLoaded(pos)) {
                        continue;
                    }
                    double distanceSqr = eye.distanceToSqr(Vec3.atCenterOf(pos));
                    if (distanceSqr > maxDistanceSqr) {
                        continue;
                    }
                    BlockTarget target = new BlockTarget(pos.immutable(), client.level.getBlockState(pos), distanceSqr);
                    if (predicate.test(target)) {
                        targets.add(target);
                    }
                }
            }
        }
        sort(targets, sortMode);
        return List.copyOf(targets.subList(0, Math.min(targets.size(), maxResults)));
    }

    static void sort(final List<BlockTarget> targets, final SortMode sortMode) {
        switch (sortMode) {
            case FARTHEST -> targets.sort(Comparator.comparingDouble(BlockTarget::distanceSqr).reversed());
            case RANDOM -> shuffle(targets);
            case CLOSEST -> targets.sort(Comparator.comparingDouble(BlockTarget::distanceSqr));
        }
    }

    private static void shuffle(final List<BlockTarget> targets) {
        RandomSource random = RandomSource.create();
        for (int index = targets.size() - 1; index > 0; index--) {
            int swapIndex = random.nextInt(index + 1);
            BlockTarget current = targets.get(index);
            targets.set(index, targets.get(swapIndex));
            targets.set(swapIndex, current);
        }
    }

    enum SortMode {
        CLOSEST("Closest"),
        FARTHEST("Farthest"),
        RANDOM("Random");

        private final String label;

        SortMode(final String label) {
            this.label = label;
        }

        String label() {
            return this.label;
        }

        static SortMode fromSetting(final String value) {
            for (SortMode mode : values()) {
                if (mode.label.equals(value)) {
                    return mode;
                }
            }
            return CLOSEST;
        }
    }

    record BlockTarget(BlockPos pos, BlockState state, double distanceSqr) {
    }
}
