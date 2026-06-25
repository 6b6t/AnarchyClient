package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class AutoJumpModule extends Module {

    private final BooleanSetting onlyMoving = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("only_moving")
            .name("Moving Only")
            .defaultValue(true)
            .build()));
    private final BooleanSetting pauseInWater = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("pause_in_water")
            .name("Water Pause")
            .defaultValue(true)
            .build()));
    private final NumberSetting delay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay")
            .name("Delay")
            .defaultValue(0.0)
            .min(0.0)
            .max(20.0)
            .step(1.0)
            .build()));
    private int cooldownTicks;

    public AutoJumpModule() {
        super("auto_jump", "Auto Jump", ModuleCategory.MOVEMENT);
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
        boolean moving = player.input != null && InputStates.moving(player.input.keyPresses);
        if (!shouldJump(
                player.onGround(),
                moving,
                player.isInWater() || player.isInLava(),
                this.onlyMoving.value(),
                this.pauseInWater.value()
        )) {
            return;
        }
        player.jumpFromGround();
        this.cooldownTicks = this.delay.value().intValue();
    }

    static boolean shouldJump(final boolean onGround, final boolean moving, final boolean inFluid,
                              final boolean onlyMoving, final boolean pauseInWater) {
        return onGround && (!onlyMoving || moving) && (!pauseInWater || !inFluid);
    }
}
