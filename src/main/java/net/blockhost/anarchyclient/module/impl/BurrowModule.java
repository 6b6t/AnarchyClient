package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public final class BurrowModule extends Module {

    public BurrowModule() {
        super("burrow", "Burrow", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }
        BlockPos base = client.player.blockPosition();
        if (BlockPlacement.place(client, this, base, true, 90.0F) != BlockPlacement.PlacementResult.WAITING) {
            this.enabled(false);
        }
    }
}
