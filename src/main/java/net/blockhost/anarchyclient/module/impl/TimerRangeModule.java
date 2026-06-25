package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.timer.TimerBalanceService;
import net.blockhost.anarchyclient.timer.TimerManager;
import net.minecraft.client.Minecraft;

public final class TimerRangeModule extends Module {

    private final NumberSetting startRange = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("start_range")
            .name("Start")
            .defaultValue(6.0)
            .min(1.0)
            .max(16.0)
            .step(0.25)
            .build()));
    private final NumberSetting pauseRange = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("pause_range")
            .name("Pause")
            .defaultValue(2.7)
            .min(0.5)
            .max(8.0)
            .step(0.25)
            .build()));
    private final NumberSetting boost = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("boost")
            .name("Boost")
            .defaultValue(1.35)
            .min(1.0)
            .max(4.0)
            .step(0.05)
            .build()));
    private final NumberSetting cruise = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("cruise")
            .name("Cruise")
            .defaultValue(0.95)
            .min(0.1)
            .max(1.0)
            .step(0.05)
            .build()));

    public TimerRangeModule() {
        super("timer_range", "Timer Range", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || CombatTargets.nearestEnemy(client, this.pauseRange.value()) != null) {
            TimerManager.clear(this.id());
            return;
        }
        boolean targetNearby = CombatTargets.nearestEnemy(client, this.startRange.value()) != null;
        double requested = targetNearby ? this.boost.value() : this.cruise.value();
        double balance = TimerBalanceService.tick(this.id(), 20.0, 0.4, requested);
        this.debugValue("balance", String.format("%.1f", balance));
        if (targetNearby && balance > 1.0) {
            TimerManager.request(this.id(), requested, TimerManager.PRIORITY_COMBAT, 2);
        } else if (!targetNearby && requested < 1.0) {
            TimerManager.request(this.id(), requested, TimerManager.PRIORITY_COMBAT, 2);
        } else {
            TimerManager.clear(this.id());
        }
    }

    @Override
    protected void onDisable() {
        TimerManager.clear(this.id());
        TimerBalanceService.clear(this.id());
        this.clearDebugValues();
    }
}
