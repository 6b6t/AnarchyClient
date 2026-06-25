package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;

public final class AntiHungerModule extends Module {

    private final BooleanSetting noSprint = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("no_sprint")
            .name("Sprint")
            .defaultValue(true)
            .description("Hides sprinting from the server. This can flag anti-cheats.")
            .build()));
    private final BooleanSetting whileSwimming = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("while_swimming")
            .name("Swim")
            .defaultValue(false)
            .build()));
    private final BooleanSetting cancelBreakStart = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("cancel_break_start")
            .name("Break")
            .defaultValue(false)
            .build()));
    private final BooleanSetting keepFalling = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("keep_falling")
            .name("Falling")
            .defaultValue(false)
            .description("Sends grounded movement as airborne. This can flag anti-cheats.")
            .build()));

    public AntiHungerModule() {
        super("anti_hunger", "Anti Hunger", ModuleCategory.PLAYER);
    }

    @Override
    public Packet<?> replaceSendPacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (client.player == null || !this.keepFalling.value() || !(packet instanceof ServerboundMovePlayerPacket move)) {
            return packet;
        }
        if (!move.isOnGround() || client.player.isPassenger() || client.player.isInWater() || client.player.isSwimming()
                || client.player.isUnderWater() || client.player.fallDistance > 0.0F) {
            return packet;
        }
        return withGround(move, client, false);
    }

    @Override
    public boolean sendPacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (client.player == null) {
            return false;
        }
        if (packet instanceof ServerboundPlayerCommandPacket command
                && command.getAction() == ServerboundPlayerCommandPacket.Action.START_SPRINTING
                && this.noSprint.value()
                && (this.whileSwimming.value() || !client.player.isSwimming())) {
            return true;
        }
        return packet instanceof ServerboundPlayerActionPacket action
                && action.getAction() == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK
                && this.cancelBreakStart.value();
    }

    private static Packet<?> withGround(final ServerboundMovePlayerPacket packet, final Minecraft client, final boolean onGround) {
        boolean horizontalCollision = packet.horizontalCollision();
        if (packet instanceof ServerboundMovePlayerPacket.PosRot) {
            return new ServerboundMovePlayerPacket.PosRot(
                    packet.getX(client.player.getX()),
                    packet.getY(client.player.getY()),
                    packet.getZ(client.player.getZ()),
                    packet.getYRot(client.player.getYRot()),
                    packet.getXRot(client.player.getXRot()),
                    onGround,
                    horizontalCollision
            );
        }
        if (packet instanceof ServerboundMovePlayerPacket.Pos) {
            return new ServerboundMovePlayerPacket.Pos(
                    packet.getX(client.player.getX()),
                    packet.getY(client.player.getY()),
                    packet.getZ(client.player.getZ()),
                    onGround,
                    horizontalCollision
            );
        }
        if (packet instanceof ServerboundMovePlayerPacket.Rot) {
            return new ServerboundMovePlayerPacket.Rot(
                    packet.getYRot(client.player.getYRot()),
                    packet.getXRot(client.player.getXRot()),
                    onGround,
                    horizontalCollision
            );
        }
        return new ServerboundMovePlayerPacket.StatusOnly(onGround, horizontalCollision);
    }
}
