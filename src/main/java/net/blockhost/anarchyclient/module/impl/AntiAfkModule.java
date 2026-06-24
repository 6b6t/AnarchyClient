package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class AntiAfkModule extends Module {

    private final NumberSetting intervalSeconds = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("interval_seconds")
            .name("Interval")
            .defaultValue(30.0)
            .min(5.0)
            .max(300.0)
            .step(5.0)
            .build()));
    private final BooleanSetting jump = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("jump")
            .name("Jump")
            .defaultValue(true)
            .build()));
    private final BooleanSetting rotate = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("rotate")
            .name("Rotate")
            .defaultValue(true)
            .build()));
    private int cooldownTicks;

    public AntiAfkModule() {
        super("anti_afk", "Anti AFK", ModuleCategory.PLAYER);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gui.screen() != null) {
            return;
        }
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return;
        }

        if (this.rotate.value()) {
            player.setYRot(player.getYRot() + 7.5F);
        }
        if (this.jump.value() && player.onGround()) {
            player.jumpFromGround();
            player.setDeltaMovement(player.getDeltaMovement().add(new Vec3(0.0, 0.02, 0.0)));
        }
        this.cooldownTicks = Math.max(1, (int) Math.round(this.intervalSeconds.value() * 20.0));
    }
}
