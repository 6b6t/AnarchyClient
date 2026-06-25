package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.event.PacketEventSilencer;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.phys.EntityHitResult;

public final class VehicleOneHitModule extends Module {

    private final NumberSetting packetCount = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("packet_count")
            .name("Packets")
            .defaultValue(8.0)
            .min(2.0)
            .max(32.0)
            .step(1.0)
            .build()));

    public VehicleOneHitModule() {
        super("vehicle_one_hit", "Vehicle One Hit", ModuleCategory.COMBAT);
    }

    @Override
    public boolean sendPacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (!(packet instanceof ServerboundInteractPacket interact)
                || !(client.hitResult instanceof EntityHitResult hit)
                || !client.options.keyAttack.isDown()
                || !isVehicle(hit.getEntity())
                || hit.getEntity().getId() != interact.entityId()) {
            return false;
        }
        int duplicates = Math.max(1, this.packetCount.value().intValue() - 1);
        PacketEventSilencer.runSilently(() -> {
            for (int i = 0; i < duplicates; i++) {
                connection.send(packet, null, true);
            }
        });
        return false;
    }

    static boolean isVehicle(final Entity entity) {
        return entity instanceof AbstractBoat || entity instanceof AbstractMinecart;
    }
}
