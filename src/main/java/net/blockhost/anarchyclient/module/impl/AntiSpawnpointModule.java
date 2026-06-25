package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class AntiSpawnpointModule extends Module {

    private final BooleanSetting fakeSwing = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("fake_swing")
            .name("Swing")
            .defaultValue(true)
            .build()));

    public AntiSpawnpointModule() {
        super("anti_spawnpoint", "Anti Spawnpoint", ModuleCategory.PLAYER);
    }

    @Override
    public boolean sendPacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (!(packet instanceof ServerboundUseItemOnPacket useItemOn) || client.level == null || client.player == null) {
            return false;
        }
        BlockPos pos = useItemOn.getHitResult().getBlockPos();
        BlockState state = client.level.getBlockState(pos);
        if (!shouldCancelUse(client.level.dimension(), state.getBlock())) {
            return false;
        }
        if (this.fakeSwing.value()) {
            InteractionHand hand = useItemOn.getHand();
            client.player.swing(hand);
        }
        return true;
    }

    static boolean shouldCancelUse(final net.minecraft.resources.ResourceKey<Level> dimension, final Block block) {
        return shouldCancelUse(dimension, block instanceof BedBlock, block == Blocks.RESPAWN_ANCHOR);
    }

    static boolean shouldCancelUse(final net.minecraft.resources.ResourceKey<Level> dimension, final boolean bed,
                                   final boolean respawnAnchor) {
        return shouldCancelUse(dimension == Level.OVERWORLD, dimension == Level.NETHER, bed, respawnAnchor);
    }

    static boolean shouldCancelUse(final boolean overworld, final boolean nether, final boolean bed,
                                   final boolean respawnAnchor) {
        return bed && overworld || respawnAnchor && nether;
    }
}
