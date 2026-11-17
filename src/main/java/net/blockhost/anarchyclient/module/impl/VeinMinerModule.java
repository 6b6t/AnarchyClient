package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class VeinMinerModule extends Module {

    private final NumberSetting maxBlocks = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("max_blocks")
            .name("Max Blocks")
            .defaultValue(12.0)
            .min(1.0)
            .max(64.0)
            .step(1.0)
            .build()));
    private BlockPos minedPos;
    private Block minedBlock;
    private Direction minedFace;
    private boolean breakingVein;

    public VeinMinerModule() {
        super("vein_miner", "Vein Miner", ModuleCategory.WORLD,
                "When a block you mine breaks, also breaks connected blocks of the same type that you can break instantly, up to a set limit.");
    }

    @Override
    public boolean attackBlock(final Minecraft client, final BlockPos pos, final Direction direction) {
        if (this.breakingVein || client.level == null) {
            return false;
        }
        BlockState state = client.level.getBlockState(pos);
        if (state.isAir()) {
            this.clearTarget();
            return false;
        }
        this.minedPos = pos.immutable();
        this.minedBlock = state.getBlock();
        this.minedFace = direction;
        return false;
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (this.minedPos == null || player == null || client.level == null || client.gameMode == null) {
            return;
        }
        if (client.level.getBlockState(this.minedPos).is(this.minedBlock)) {
            if (!client.options.keyAttack.isDown()) {
                this.clearTarget();
            }
            return;
        }
        BlockPos origin = this.minedPos;
        Block block = this.minedBlock;
        Direction face = this.minedFace;
        this.clearTarget();
        int extraBlocks = this.maxBlocks.value().intValue() - 1;
        this.breakingVein = true;
        try {
            for (BlockPos pos : vein(client.level, origin, block, extraBlocks)) {
                BlockState state = client.level.getBlockState(pos);
                boolean instant = player.getAbilities().instabuild
                        || state.getDestroyProgress(player, client.level, pos) >= 1.0F;
                if (instant && player.isWithinBlockInteractionRange(pos, 1.0)) {
                    client.gameMode.startDestroyBlock(pos, face);
                }
            }
        } finally {
            this.breakingVein = false;
        }
    }

    @Override
    protected void onDisable() {
        this.clearTarget();
    }

    private void clearTarget() {
        this.minedPos = null;
        this.minedBlock = null;
        this.minedFace = null;
    }

    /**
     * Returns up to {@code limit} blocks of {@code block} connected to {@code start}, nearest first. The start
     * position is not included and does not need to match, because it is usually already broken.
     */
    static List<BlockPos> vein(final BlockGetter level, final BlockPos start, final Block block, final int limit) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        visited.add(start.immutable());
        queue.add(start.immutable());
        while (!queue.isEmpty() && result.size() < limit) {
            BlockPos pos = queue.removeFirst();
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (result.size() < limit && visited.add(next) && level.getBlockState(next).is(block)) {
                    result.add(next);
                    queue.add(next);
                }
            }
        }
        return List.copyOf(result);
    }
}
