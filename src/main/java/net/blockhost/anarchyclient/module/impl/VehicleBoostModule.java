package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class VehicleBoostModule extends Module {

    private final NumberSetting strength = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("strength")
            .name("Strength")
            .defaultValue(0.12)
            .min(0.01)
            .max(0.8)
            .step(0.01)
            .build()));

    public VehicleBoostModule() {
        super("vehicle_boost", "Vehicle Boost", ModuleCategory.MOVEMENT);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.player.getVehicle() == null || !client.options.keyUp.isDown()) {
            return;
        }
        Entity vehicle = client.player.getVehicle();
        Vec3 boost = client.player.getViewVector(0.0F).multiply(1.0, 0.0, 1.0);
        if (boost.horizontalDistanceSqr() > 1.0E-6) {
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().add(boost.normalize().scale(this.strength.value())));
        }
    }
}
