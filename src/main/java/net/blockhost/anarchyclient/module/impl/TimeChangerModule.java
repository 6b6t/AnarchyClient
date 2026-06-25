package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;

public final class TimeChangerModule extends Module {

    private final NumberSetting time = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("time")
            .name("Time")
            .defaultValue(6000.0)
            .min(0.0)
            .max(24000.0)
            .step(500.0)
            .build()));

    public TimeChangerModule() {
        super("time_changer", "Time Changer", ModuleCategory.RENDER);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.level != null) {
            client.level.getLevelData().setGameTime(this.time.value().longValue());
        }
    }

    @Override
    public boolean receivePacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        return packet instanceof ClientboundSetTimePacket;
    }
}
