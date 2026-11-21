package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
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
        super(
                "rotation",
                "Rotation",
                ModuleCategory.PLAYER,
                "Locks your player to a fixed yaw and pitch, so you look, walk, and hit in that direction.");
    }

    @Override
    public void tick(final Minecraft client) {
        this.lockPlayer(client.player);
    }

    @Override
    public CameraTransform cameraTransform(
            final Minecraft client, final Vec3 position, final float yaw, final float pitch) {
        // The camera updates every frame before the crosshair target is picked, so undo mouse turns here too.
        this.lockPlayer(client.player);
        return new CameraTransform(
                position, this.yaw.value().floatValue(), this.pitch.value().floatValue());
    }

    private void lockPlayer(final LocalPlayer player) {
        if (player == null) {
            return;
        }
        float lockedYaw = this.yaw.value().floatValue();
        float lockedPitch = this.pitch.value().floatValue();
        player.setYRot(lockedYaw);
        player.setXRot(lockedPitch);
        player.yRotO = lockedYaw;
        player.xRotO = lockedPitch;
    }
}
