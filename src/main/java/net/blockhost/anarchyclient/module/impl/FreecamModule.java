package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.mixin.ClientInputAccessor;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public final class FreecamModule extends Module {

    private final NumberSetting speed = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("speed")
            .name("Speed")
            .defaultValue(0.6)
            .min(0.1)
            .max(3.0)
            .step(0.1)
            .build()));
    private Vec3 position;

    public FreecamModule() {
        super("freecam", "Freecam", ModuleCategory.RENDER);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) {
            this.position = null;
            return;
        }
        if (this.position == null) {
            this.position = player.getEyePosition();
        }
        ClientInput input = player.input;
        if (input == null) {
            return;
        }
        this.position = move(this.position, player.getYRot(), input.keyPresses, this.speed.value());
    }

    @Override
    public void updateInput(final Minecraft client, final ClientInput input) {
        if (input != null) {
            ((ClientInputAccessor) input).anarchyclient$setMoveVector(Vec2.ZERO);
        }
    }

    @Override
    public CameraTransform cameraTransform(final Minecraft client, final Vec3 position, final float yaw,
                                           final float pitch) {
        return this.position == null ? new CameraTransform(position, yaw, pitch) : new CameraTransform(this.position, yaw, pitch);
    }

    @Override
    protected void onDisable() {
        this.position = null;
    }

    static Vec3 move(final Vec3 position, final float yaw, final Input input, final double speed) {
        if (input == null) {
            return position;
        }
        double radians = Math.toRadians(yaw);
        Vec3 forward = new Vec3(-Math.sin(radians), 0.0, Math.cos(radians));
        Vec3 right = new Vec3(forward.z, 0.0, -forward.x);
        Vec3 delta = Vec3.ZERO;
        if (input.forward()) {
            delta = delta.add(forward);
        }
        if (input.backward()) {
            delta = delta.subtract(forward);
        }
        if (input.right()) {
            delta = delta.add(right);
        }
        if (input.left()) {
            delta = delta.subtract(right);
        }
        if (input.jump()) {
            delta = delta.add(0.0, 1.0, 0.0);
        }
        if (input.shift()) {
            delta = delta.add(0.0, -1.0, 0.0);
        }
        return delta.lengthSqr() == 0.0 ? position : position.add(delta.normalize().scale(speed));
    }
}
