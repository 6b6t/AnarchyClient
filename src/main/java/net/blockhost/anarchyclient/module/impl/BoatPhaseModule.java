package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class BoatPhaseModule extends Module {

    private final NumberSetting speed = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("speed")
            .name("Speed")
            .defaultValue(0.25)
            .min(0.02)
            .max(1.5)
            .step(0.01)
            .build()));
    private Entity lastVehicle;

    public BoatPhaseModule() {
        super("boat_phase", "Boat Phase", ModuleCategory.MOVEMENT,
                "Disables collision on your ridden vehicle and steers it with movement, jump, and sneak keys so it passes through blocks.");
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.player.getVehicle() == null) {
            clearVehicle();
            return;
        }
        Entity vehicle = client.player.getVehicle();
        this.lastVehicle = vehicle;
        vehicle.noPhysics = true;
        Vec3 horizontal = MovementVelocity.fromKeys(client, client.player.getYRot(), this.speed.value());
        vehicle.setDeltaMovement(horizontal.x,
                verticalSpeed(client.options.keyJump.isDown(), client.options.keyShift.isDown(), this.speed.value()),
                horizontal.z);
    }

    /** Without physics the vehicle would keep its gravity and sink through the floor, so it hovers when idle. */
    static double verticalSpeed(final boolean up, final boolean down, final double speed) {
        if (up == down) {
            return 0.0;
        }
        return up ? speed : -speed;
    }

    @Override
    protected void onDisable() {
        clearVehicle();
    }

    private void clearVehicle() {
        if (this.lastVehicle != null) {
            this.lastVehicle.noPhysics = false;
        }
        this.lastVehicle = null;
    }
}
