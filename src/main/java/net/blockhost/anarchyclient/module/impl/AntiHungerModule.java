package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
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
            .description("Sends grounded movement as airborne. Lets a grounded packet through before the server's fall distance can hurt you. This can flag anti-cheats.")
            .build()));
    /** Stay well under the 3 block safe fall distance the server applies when it next sees you land. */
    static final double MAX_SPOOFED_DROP = 2.0;
    private double serverFallDistance;
    private double lastSentY = Double.NaN;

    public AntiHungerModule() {
        super("anti_hunger", "Anti Hunger", ModuleCategory.PLAYER,
                "Reduces hunger loss by hiding sprinting and other actions from the server. This can flag anti-cheats.");
    }

    @Override
    protected void onDisable() {
        this.resetFallTracking();
    }

    @Override
    public void gameJoined(final Minecraft client, final ClientPacketListener listener) {
        this.resetFallTracking();
    }

    @Override
    public Packet<?> replaceSendPacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (client.player == null || !(packet instanceof ServerboundMovePlayerPacket move)) {
            return packet;
        }
        double drop = 0.0;
        if (move.hasPosition()) {
            double y = move.getY(client.player.getY());
            drop = Double.isNaN(this.lastSentY) ? 0.0 : Math.max(0.0, this.lastSentY - y);
            this.lastSentY = y;
        }
        boolean spoof = this.keepFalling.value()
                && move.isOnGround()
                && !client.player.isPassenger()
                && !client.player.isInWater()
                && !client.player.isSwimming()
                && !client.player.isUnderWater()
                && client.player.fallDistance <= 0.0F
                && canSpoof(this.serverFallDistance, drop);
        boolean sentOnGround = move.isOnGround() && !spoof;
        this.serverFallDistance = nextServerFallDistance(this.serverFallDistance, drop, sentOnGround);
        return spoof ? withGround(move, client, false) : packet;
    }

    /** The server adds every downward step while it thinks you are airborne and resets on a grounded packet. */
    static double nextServerFallDistance(final double serverFallDistance, final double drop, final boolean onGround) {
        return onGround ? 0.0 : serverFallDistance + drop;
    }

    static boolean canSpoof(final double serverFallDistance, final double drop) {
        return serverFallDistance + drop < MAX_SPOOFED_DROP;
    }

    private void resetFallTracking() {
        this.serverFallDistance = 0.0;
        this.lastSentY = Double.NaN;
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
