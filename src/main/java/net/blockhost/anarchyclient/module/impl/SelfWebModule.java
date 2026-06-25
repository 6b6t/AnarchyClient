package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;

import java.util.List;

public final class SelfWebModule extends Module {

    public SelfWebModule() {
        super("self_web", "Self Web", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }
        for (BlockPos pos : List.of(client.player.blockPosition(), client.player.blockPosition().above())) {
            if (BlockPlacement.place(client, this, pos, true, 70.0F, stack -> stack.is(Items.COBWEB))
                    == BlockPlacement.PlacementResult.PLACED) {
                return;
            }
        }
    }
}
