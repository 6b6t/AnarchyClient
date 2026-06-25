package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public final class AnimationsModule extends Module {

    private final NumberSetting sway = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("sway")
            .name("Sway")
            .defaultValue(0.025)
            .min(0.0)
            .max(0.15)
            .step(0.005)
            .build()));

    public AnimationsModule() {
        super("animations", "Animations", ModuleCategory.RENDER);
    }

    @Override
    public CameraTransform cameraTransform(final Minecraft client, final Vec3 position, final float yaw, final float pitch) {
        if (client.player == null || this.sway.value() <= 0.0) {
            return new CameraTransform(position, yaw, pitch);
        }
        double phase = (client.player.tickCount % 360) * 0.12;
        return new CameraTransform(position.add(Math.sin(phase) * this.sway.value(), Math.cos(phase) * this.sway.value() * 0.5, 0.0),
                yaw, pitch);
    }
}
