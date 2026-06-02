package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class AutoRespawnModule extends Module {

    private final NumberSetting delayTicksSetting = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay_ticks")
            .name("Delay")
            .defaultValue(20.0)
            .min(0.0)
            .max(200.0)
            .step(5.0)
            .build()));
    private int deadTicks;

    public AutoRespawnModule() {
        super("auto_respawn", "Auto Respawn", ModuleCategory.PLAYER);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || !player.isDeadOrDying()) {
            this.deadTicks = 0;
            return;
        }
        this.deadTicks++;
        if (this.deadTicks >= this.delayTicksSetting.value()) {
            player.respawn();
            this.deadTicks = 0;
        }
    }
}
