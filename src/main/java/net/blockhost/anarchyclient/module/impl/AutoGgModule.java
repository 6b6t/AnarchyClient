package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.client.Minecraft;

public final class AutoGgModule extends Module {

    private final StringSetting message = this.setting(StringSetting.from(StringSetting.builder()
            .id("message")
            .name("Message")
            .defaultValue("gg")
            .build()));
    private final NumberSetting cooldownSeconds = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("cooldown_seconds")
            .name("Cooldown")
            .defaultValue(15.0)
            .min(5.0)
            .max(120.0)
            .step(5.0)
            .build()));
    private boolean wasDead;
    private int cooldownTicks;

    public AutoGgModule() {
        super("auto_gg", "Auto GG", ModuleCategory.FUN);
    }

    @Override
    public void tick(final Minecraft client) {
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
        }
        if (client.player == null || client.getConnection() == null) {
            this.wasDead = false;
            return;
        }

        boolean dead = client.player.isDeadOrDying();
        if (dead && !this.wasDead && this.cooldownTicks <= 0) {
            client.getConnection().sendChat(this.message.value());
            this.cooldownTicks = (int) Math.round(this.cooldownSeconds.value() * 20);
        }
        this.wasDead = dead;
    }
}
