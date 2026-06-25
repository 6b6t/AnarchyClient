package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.AnvilBlock;

public final class AntiAnvilModule extends Module {

    public AntiAnvilModule() {
        super("anti_anvil", "Anti Anvil", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }
        for (int y = 1; y <= 4; y++) {
            BlockPos pos = client.player.blockPosition().above(y);
            if (client.level.getBlockState(pos).getBlock() instanceof AnvilBlock) {
                WorldInteraction.breakBlock(client, pos, Direction.UP, stack -> true);
                return;
            }
        }
    }
}
