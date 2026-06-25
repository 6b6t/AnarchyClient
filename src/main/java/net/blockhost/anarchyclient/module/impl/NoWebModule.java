package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class NoWebModule extends Module {

    private static boolean active;

    public NoWebModule() {
        super("no_web", "No Web", ModuleCategory.MOVEMENT);
    }

    @Override
    protected void onEnable() {
        active = true;
    }

    @Override
    protected void onDisable() {
        active = false;
    }

    public static boolean shouldIgnore(final BlockState state) {
        return active && state != null && state.is(Blocks.COBWEB);
    }
}
