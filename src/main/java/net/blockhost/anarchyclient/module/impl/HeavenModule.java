package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class HeavenModule extends Module {

    private final NumberSetting height = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("height")
            .name("Height")
            .defaultValue(60.0)
            .min(5.0)
            .max(300.0)
            .step(5.0)
            .build()));
    private final NumberSetting seconds = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("seconds")
            .name("Seconds")
            .defaultValue(8.0)
            .min(1.0)
            .max(30.0)
            .step(1.0)
            .build()));
    private final BooleanSetting lookDown = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("look_down")
            .name("Look Down")
            .defaultValue(true)
            .description("Turns the camera toward the place you died while it rises.")
            .build()));
    private long deathMillis = -1L;

    public HeavenModule() {
        super("heaven", "Heaven", ModuleCategory.RENDER,
                "Slowly lifts your camera into the sky after you die, until you respawn.");
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || !player.isDeadOrDying()) {
            this.deathMillis = -1L;
        } else if (this.deathMillis < 0L) {
            this.deathMillis = System.currentTimeMillis();
        }
    }

    @Override
    public CameraTransform cameraTransform(final Minecraft client, final Vec3 position, final float yaw,
                                           final float pitch) {
        if (this.deathMillis < 0L || client.player == null || !client.player.isDeadOrDying()) {
            return new CameraTransform(position, yaw, pitch);
        }
        float progress = progress(System.currentTimeMillis() - this.deathMillis, this.seconds.value());
        double rise = this.height.value() * easeOut(progress);
        float newPitch = this.lookDown.value() ? Mth.lerp(progress, pitch, 90.0F) : pitch;
        return new CameraTransform(position.add(0.0, rise, 0.0), yaw, newPitch);
    }

    /** Returns how far the rise has gone, from 0 to 1. */
    static float progress(final long elapsedMillis, final double seconds) {
        if (seconds <= 0.0) {
            return 1.0F;
        }
        return (float) Mth.clamp(elapsedMillis / (seconds * 1000.0), 0.0, 1.0);
    }

    static float easeOut(final float progress) {
        float inverse = 1.0F - progress;
        return 1.0F - inverse * inverse;
    }

    @Override
    protected void onDisable() {
        this.deathMillis = -1L;
    }
}
