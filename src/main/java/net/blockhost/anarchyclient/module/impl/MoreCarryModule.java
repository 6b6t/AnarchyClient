package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;

public final class MoreCarryModule extends Module {

    private final BooleanSetting playerInventoryOnly = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("player_inventory_only")
            .name("Inventory")
            .defaultValue(true)
            .build()));

    public MoreCarryModule() {
        super("more_carry", "More Carry", ModuleCategory.PLAYER);
    }

    @Override
    public boolean sendPacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (client.player == null || client.hasSingleplayerServer() || !(packet instanceof ServerboundContainerClosePacket close)) {
            return false;
        }
        return !this.playerInventoryOnly.value() || close.getContainerId() == client.player.inventoryMenu.containerId;
    }
}
