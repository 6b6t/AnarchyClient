package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.mixin.MinecraftAccessor;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;

public final class BreakDelayModule extends Module {

    private final NumberSetting delay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay")
            .name("Delay")
            .defaultValue(0.0)
            .min(0.0)
            .max(6.0)
            .step(1.0)
            .build()));

    public BreakDelayModule() {
        super(
                "break_delay",
                "Break Delay",
                ModuleCategory.WORLD,
                "Sets the vanilla right-click repeat delay, so held use actions such as placing blocks repeat faster.");
    }

    @Override
    public void tick(final Minecraft client) {
        // Only shorten the delay vanilla set after a use. Writing the value every tick kept it from ever counting
        // down to 0, which is when vanilla repeats a held use.
        MinecraftAccessor accessor = (MinecraftAccessor) client;
        accessor.anarchyclient$setRightClickDelay(shortenedDelay(
                accessor.anarchyclient$rightClickDelay(), this.delay.value().intValue()));
    }

    static int shortenedDelay(final int current, final int configured) {
        return Math.min(current, configured);
    }
}
