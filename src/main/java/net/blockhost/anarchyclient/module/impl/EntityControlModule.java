package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class EntityControlModule extends Module {

    private final NumberSetting speed = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("speed")
            .name("Speed")
            .defaultValue(0.35)
            .min(0.05)
            .max(2.0)
            .step(0.05)
            .build()));

    public EntityControlModule() {
        super("entity_control", "Entity Control", ModuleCategory.MOVEMENT);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.player.getVehicle() == null) {
            return;
        }
        Entity vehicle = client.player.getVehicle();
        Vec3 horizontal = MovementVelocity.fromKeys(client, client.player.getYRot(), this.speed.value());
        double y = client.options.keyJump.isDown() ? this.speed.value() * 0.5 : client.options.keyShift.isDown() ? -this.speed.value() * 0.5 : vehicle.getDeltaMovement().y;
        vehicle.setDeltaMovement(horizontal.x, y, horizontal.z);
        vehicle.setYRot(client.player.getYRot());
    }
}
