package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.mixin.MinecraftAccessor;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;

public final class FastUseModule extends Module {

    private final NumberSetting delayTicks = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay")
            .name("Delay")
            .defaultValue(0.0)
            .min(0.0)
            .max(4.0)
            .step(1.0)
            .build()));

    public FastUseModule() {
        super("fast_use", "Fast Use", ModuleCategory.PLAYER);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.gui.screen() != null) {
            return;
        }
        MinecraftAccessor accessor = (MinecraftAccessor) client;
        int delay = this.delayTicks.value().intValue();
        if (accessor.anarchyclient$rightClickDelay() > delay) {
            accessor.anarchyclient$setRightClickDelay(delay);
        }
    }
}
