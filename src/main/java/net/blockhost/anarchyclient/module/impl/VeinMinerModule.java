package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayDeque;
import java.util.HashSet;
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

    public VeinMinerModule() {
        super("vein_miner", "Vein Miner", ModuleCategory.WORLD);
    }

    @Override
    public void tick(final Minecraft client) {
        if (!(client.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK
                || client.level == null
                || !client.options.keyAttack.isDown()) {
            return;
        }
        Block block = client.level.getBlockState(hit.getBlockPos()).getBlock();
        for (BlockPos pos : vein(client, hit.getBlockPos(), block, this.maxBlocks.value().intValue())) {
            WorldInteraction.breakBlock(client, pos, Direction.UP, stack -> true);
        }
    }

    static Set<BlockPos> vein(final Minecraft client, final BlockPos start, final Block block, final int maxBlocks) {
        Set<BlockPos> result = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start.immutable());
        while (!queue.isEmpty() && result.size() < maxBlocks) {
            BlockPos pos = queue.removeFirst();
            if (!result.add(pos) || client.level == null || client.level.getBlockState(pos).getBlock() != block) {
                continue;
            }
            for (Direction direction : Direction.values()) {
                queue.add(pos.relative(direction).immutable());
            }
        }
        return Set.copyOf(result);
    }
}
