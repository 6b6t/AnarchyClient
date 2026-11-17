package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.world.entity.Entity;

public final class NoSwingModule extends Module {

    private static NoSwingModule active;

    private final BooleanSetting serverSide = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("server_side")
            .name("Server Side")
            .defaultValue(false)
            .description("Stops swing packets for missed attacks from reaching the server.")
            .build()));

    public NoSwingModule() {
        super("no_swing", "No Swing", ModuleCategory.RENDER,
                "Hides your own hand swing animation. With Server Side on, also stops the swing packet for missed attacks from reaching the server.");
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
    }

    @Override
    public boolean sendPacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        return client.player != null && shouldCancel(packet, this.serverSide.value());
    }

    public static boolean shouldHideSwing(final Minecraft client, final Entity entity) {
        return active != null && client != null && entity != null && entity == client.player;
    }

    static boolean shouldCancel(final Packet<?> packet, final boolean serverSide) {
        return serverSide && packet instanceof ServerboundPunchPacket;
    }
}
