package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.timer.TimerBalanceService;
import net.blockhost.anarchyclient.timer.TimerManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public final class TickBaseModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(4.0)
            .min(1.0)
            .max(8.0)
            .step(0.25)
            .build()));
    private final NumberSetting multiplier = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("multiplier")
            .name("Timer")
            .defaultValue(1.6)
            .min(1.0)
            .max(4.0)
            .step(0.05)
            .build()));
    private final NumberSetting maxBalance = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("max_balance")
            .name("Balance")
            .defaultValue(18.0)
            .min(1.0)
            .max(100.0)
            .step(1.0)
            .build()));
    private final NumberSetting recovery = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("recovery")
            .name("Recovery")
            .defaultValue(0.35)
            .min(0.0)
            .max(5.0)
            .step(0.05)
            .build()));
    private final BooleanSetting groundOnly = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("ground_only")
            .name("Ground")
            .defaultValue(false)
            .build()));

    public TickBaseModule() {
        super("tick_base", "Tick Base", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || this.groundOnly.value() && !client.player.onGround()) {
            TimerManager.clear(this.id());
            return;
        }
        Player target = CombatTargets.nearestEnemy(client, this.range.value());
        double balance = TimerBalanceService.tick(this.id(), this.maxBalance.value(), this.recovery.value(),
                target == null ? 1.0 : this.multiplier.value());
        this.debugValue("balance", String.format("%.1f", balance));
        if (target != null && balance > 1.0) {
            TimerManager.request(this.id(), this.multiplier.value(), TimerManager.PRIORITY_COMBAT, 2);
            this.debugValue("target", target.getScoreboardName());
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
