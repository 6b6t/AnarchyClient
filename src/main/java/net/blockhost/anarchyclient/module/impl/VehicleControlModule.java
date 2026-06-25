package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class VehicleControlModule extends Module {

    private final NumberSetting speed = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("speed")
            .name("Speed")
            .defaultValue(0.45)
            .min(0.05)
            .max(2.5)
            .step(0.05)
            .build()));
    private final BooleanSetting vertical = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("vertical")
            .name("Vertical")
            .defaultValue(true)
            .build()));

    public VehicleControlModule() {
        super("vehicle_control", "Vehicle Control", ModuleCategory.MOVEMENT);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.player.getVehicle() == null) {
            return;
        }
        Entity vehicle = client.player.getVehicle();
        Vec3 horizontal = MovementVelocity.fromKeys(client, client.player.getYRot(), this.speed.value());
        double y = vehicle.getDeltaMovement().y;
        if (this.vertical.value()) {
            y = client.options.keyJump.isDown() ? this.speed.value() * 0.5
                    : client.options.keyShift.isDown() ? -this.speed.value() * 0.5 : y;
        }
        vehicle.setDeltaMovement(horizontal.x, y, horizontal.z);
        vehicle.setYRot(client.player.getYRot());
    }
}
