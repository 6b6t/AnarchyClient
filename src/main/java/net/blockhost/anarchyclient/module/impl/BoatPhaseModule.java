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
        super("boat_phase", "Boat Phase", ModuleCategory.MOVEMENT);
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
        double y = client.options.keyJump.isDown() ? this.speed.value()
                : client.options.keyShift.isDown() ? -this.speed.value() : vehicle.getDeltaMovement().y;
        vehicle.setDeltaMovement(horizontal.x, y, horizontal.z);
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
