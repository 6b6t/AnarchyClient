package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class BoatGlitchModule extends Module {

    private final NumberSetting interval = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("interval")
            .name("Interval")
            .defaultValue(10.0)
            .min(1.0)
            .max(100.0)
            .step(1.0)
            .build()));
    private final NumberSetting offset = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("offset")
            .name("Offset")
            .defaultValue(3.0)
            .min(0.1)
            .max(32.0)
            .step(0.1)
            .build()));
    private int ticks;

    public BoatGlitchModule() {
        super("boat_glitch", "Boat Glitch", ModuleCategory.MOVEMENT);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.getConnection() == null || client.player.getVehicle() == null) {
            this.ticks = 0;
            return;
        }
        if (++this.ticks < this.interval.value().intValue()) {
            return;
        }
        this.ticks = 0;
        Entity vehicle = client.player.getVehicle();
        Vec3 pos = vehicle.position();
        client.getConnection().send(new ServerboundMoveVehiclePacket(
                pos.add(0.0, this.offset.value(), 0.0),
                vehicle.getYRot(),
                vehicle.getXRot(),
                false
        ));
        client.getConnection().send(ServerboundMoveVehiclePacket.fromEntity(vehicle));
    }
}
