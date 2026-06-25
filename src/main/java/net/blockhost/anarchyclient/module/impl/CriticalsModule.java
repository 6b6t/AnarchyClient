package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.event.PacketEventSilencer;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class CriticalsModule extends Module {

    public CriticalsModule() {
        super("criticals", "Criticals", ModuleCategory.COMBAT);
    }

    @Override
    public boolean attackEntity(final Minecraft client, final Player player, final Entity target) {
        if (client.getConnection() == null || !shouldCrit(player)) {
            return false;
        }
        PacketEventSilencer.runSilently(() -> {
            client.getConnection().send(new ServerboundMovePlayerPacket.Pos(
                    player.getX(),
                    player.getY() + 0.0625,
                    player.getZ(),
                    false,
                    player.horizontalCollision
            ));
            client.getConnection().send(new ServerboundMovePlayerPacket.Pos(
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    false,
                    player.horizontalCollision
            ));
        });
        return false;
    }

    static boolean shouldCrit(final Player player) {
        return player.onGround()
                && !player.isInWater()
                && !player.isInLava()
                && !player.isFallFlying()
                && !player.isPassenger();
    }
}
