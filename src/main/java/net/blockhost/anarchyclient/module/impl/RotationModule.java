package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public final class RotationModule extends Module {

    private final NumberSetting yaw = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("yaw")
            .name("Yaw")
            .defaultValue(0.0)
            .min(-180.0)
            .max(180.0)
            .step(1.0)
            .build()));
    private final NumberSetting pitch = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("pitch")
            .name("Pitch")
            .defaultValue(0.0)
            .min(-90.0)
            .max(90.0)
            .step(1.0)
            .build()));

    public RotationModule() {
        super("rotation", "Rotation", ModuleCategory.PLAYER);
    }

    @Override
    public CameraTransform cameraTransform(final Minecraft client, final Vec3 position, final float yaw,
                                           final float pitch) {
        return new CameraTransform(position, this.yaw.value().floatValue(), this.pitch.value().floatValue());
    }
}
