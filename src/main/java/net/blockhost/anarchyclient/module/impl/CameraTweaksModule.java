package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public final class CameraTweaksModule extends Module {

    private final NumberSetting fovMultiplier = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("fov_multiplier")
            .name("FOV Multiplier")
            .defaultValue(1.0)
            .min(0.25)
            .max(2.5)
            .step(0.05)
            .build()));
    private final NumberSetting yOffset = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("y_offset")
            .name("Y Offset")
            .defaultValue(0.0)
            .min(-3.0)
            .max(3.0)
            .step(0.05)
            .build()));
    private final NumberSetting yawOffset = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("yaw_offset")
            .name("Yaw Offset")
            .defaultValue(0.0)
            .min(-180.0)
            .max(180.0)
            .step(5.0)
            .build()));
    private final NumberSetting pitchOffset = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("pitch_offset")
            .name("Pitch Offset")
            .defaultValue(0.0)
            .min(-89.0)
            .max(89.0)
            .step(1.0)
            .build()));

    public CameraTweaksModule() {
        super("camera_tweaks", "Camera Tweaks", ModuleCategory.RENDER);
    }

    @Override
    public float fov(final Minecraft client, final float fov) {
        return (float) (fov * this.fovMultiplier.value());
    }

    @Override
    public CameraTransform cameraTransform(final Minecraft client, final Vec3 position, final float yaw, final float pitch) {
        return new CameraTransform(
                position.add(0.0, this.yOffset.value(), 0.0),
                yaw + this.yawOffset.value().floatValue(),
                clampPitch(pitch + this.pitchOffset.value().floatValue())
        );
    }

    static float clampPitch(final float pitch) {
        return Math.max(-90.0F, Math.min(90.0F, pitch));
    }
}
