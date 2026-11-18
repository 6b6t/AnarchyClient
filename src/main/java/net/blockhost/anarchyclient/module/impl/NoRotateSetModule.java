package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerRotationPacket;
import net.minecraft.world.entity.Relative;

public final class NoRotateSetModule extends Module {

    private static NoRotateSetModule active;
    private static float[] savedRotation;

    private final BooleanSetting positionCorrections = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("position_corrections")
            .name("Position Packets")
            .defaultValue(false)
            .description("Keeps your view after server position corrections. The correction itself still applies.")
            .build()));

    public NoRotateSetModule() {
        super("no_rotate_set", "No Rotate Set", ModuleCategory.PLAYER,
                "Ignores server packets that force the camera rotation, and optionally the rotation in position corrections.");
    }

    @Override
    public boolean receivePacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        // A setback must reach vanilla so it confirms the teleport; cancelling it leaves the player stuck.
        // Position packets are handled by the hooks below, around vanilla's handler on the client thread.
        return client.player != null && shouldCancel(packet);
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
        savedRotation = null;
    }

    /** Called on the client thread right before vanilla applies a position packet. */
    public static void beforeSetback(final LocalPlayer player, final ClientboundPlayerPositionPacket packet) {
        NoRotateSetModule module = active;
        savedRotation = module != null && module.positionCorrections.value() && player != null && hasRotation(packet)
                ? new float[]{player.getYRot(), player.getXRot()}
                : null;
    }

    /** Called on the client thread right after vanilla applied a position packet; puts the view back. */
    public static void afterSetback(final LocalPlayer player) {
        float[] rotation = savedRotation;
        savedRotation = null;
        if (rotation == null || player == null) {
            return;
        }
        player.setYRot(rotation[0]);
        player.setXRot(rotation[1]);
        player.yRotO = rotation[0];
        player.xRotO = rotation[1];
    }

    static boolean shouldCancel(final Packet<?> packet) {
        return packet instanceof ClientboundPlayerRotationPacket || packet instanceof ClientboundPlayerLookAtPacket;
    }

    static boolean hasRotation(final ClientboundPlayerPositionPacket packet) {
        return !packet.relatives().contains(Relative.X_ROT)
                || !packet.relatives().contains(Relative.Y_ROT)
                || packet.change().xRot() != 0.0F
                || packet.change().yRot() != 0.0F;
    }
}
