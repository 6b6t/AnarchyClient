package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.mixin.LivingEntityAccessor;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.minecraft.client.Minecraft;

public final class NoJumpDelayModule extends Module {

    public NoJumpDelayModule() {
        super("no_jump_delay", "No Jump Delay", ModuleCategory.MOVEMENT);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player instanceof LivingEntityAccessor accessor) {
            accessor.anarchyclient$setNoJumpDelay(0);
        }
    }
}
