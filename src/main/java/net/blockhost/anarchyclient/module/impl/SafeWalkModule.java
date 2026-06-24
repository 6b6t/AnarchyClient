package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public final class SafeWalkModule extends Module {

    public SafeWalkModule() {
        super("safe_walk", "Safe Walk", ModuleCategory.MOVEMENT);
    }

    @Override
    public boolean preventEdgeFall(final Minecraft client, final Player player) {
        return client.player == player && client.gui.screen() == null;
    }
}
