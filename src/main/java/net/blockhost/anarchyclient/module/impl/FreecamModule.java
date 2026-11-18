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
    private static FreecamModule active;

    private Vec3 position;
    private Vec3 previousPosition;
    private Input cameraInput = Input.EMPTY;

    public FreecamModule() {
        super("freecam", "Freecam", ModuleCategory.RENDER, "Detaches the camera from the player so areas can be inspected without moving the body.");
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) {
            this.position = null;
            this.previousPosition = null;
            return;
        }
        if (this.position == null) {
            this.position = player.getEyePosition();
        }
        this.previousPosition = this.position;
        this.position = move(this.position, player.getYRot(), this.cameraInput, this.speed.value());
    }

    /** Whether the camera is away from the player, so vanilla draws the player's body and hides the held item. */
    public static boolean detachesCamera() {
        FreecamModule module = active;
        return module != null && module.position != null;
    }

    @Override
    public void updateInput(final Minecraft client, final ClientInput input) {
        if (input == null) {
            return;
        }
        // Keep the keys for the camera and give the body no input at all, including jump and sneak.
        this.cameraInput = input.keyPresses;
        input.keyPresses = Input.EMPTY;
        ((ClientInputAccessor) input).anarchyclient$setMoveVector(Vec2.ZERO);
    }

    @Override
    public CameraTransform cameraTransform(final Minecraft client, final Vec3 position, final float yaw,
                                           final float pitch) {
        if (this.position == null) {
            return new CameraTransform(position, yaw, pitch);
        }
        // The camera moves once per tick; interpolate so it stays smooth at high frame rates.
        Vec3 previous = this.previousPosition == null ? this.position : this.previousPosition;
        float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        return new CameraTransform(previous.lerp(this.position, partialTick), yaw, pitch);
    }

    @Override
    protected void onEnable() {
        active = this;
    }

    @Override
    protected void onDisable() {
        if (active == this) {
            active = null;
        }
        this.position = null;
        this.previousPosition = null;
        this.cameraInput = Input.EMPTY;
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
