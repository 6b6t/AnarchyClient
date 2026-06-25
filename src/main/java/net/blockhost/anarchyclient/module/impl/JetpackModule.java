package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class JetpackModule extends Module {

    private final NumberSetting speed = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("speed")
            .name("Speed")
            .defaultValue(0.42)
            .min(0.05)
            .max(2.0)
            .step(0.01)
            .build()));
    private final BooleanSetting requireJump = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("require_jump")
            .name("Jump")
            .defaultValue(true)
            .build()));

    public JetpackModule() {
        super("jetpack", "Jetpack", ModuleCategory.MOVEMENT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gui.screen() != null) {
            return;
        }
        boolean active = !this.requireJump.value() || client.options.keyJump.isDown();
        if (active) {
            player.setDeltaMovement(jetpackVelocity(player.getDeltaMovement(), this.speed.value(), true));
            player.resetFallDistance();
        }
    }

    static Vec3 jetpackVelocity(final Vec3 current, final double speed, final boolean active) {
        if (!active) {
            return current;
        }
        return new Vec3(current.x, speed, current.z);
    }
}
