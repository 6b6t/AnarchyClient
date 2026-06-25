package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.timer.TimerManager;
import net.minecraft.client.Minecraft;

public final class TimerModule extends Module {

    private final NumberSetting multiplier = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("multiplier")
            .name("Multiplier")
            .defaultValue(1.25)
            .min(0.1)
            .max(10.0)
            .step(0.05)
            .build()));
    private final BooleanSetting movingOnly = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("moving_only")
            .name("Moving Only")
            .defaultValue(false)
            .build()));

    public TimerModule() {
        super("timer", "Timer", ModuleCategory.MOVEMENT);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || this.movingOnly.value() && !MovementVelocity.moving(client)) {
            TimerManager.clear(this.id());
            return;
        }
        TimerManager.request(this.id(), this.multiplier.value(), TimerManager.PRIORITY_MOVEMENT, 2);
    }

    @Override
    protected void onDisable() {
        TimerManager.clear(this.id());
    }
}
