package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;

public final class PacketFlyModule extends Module {

    private final NumberSetting speed = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("speed")
            .name("Speed")
            .defaultValue(0.25)
            .min(0.02)
            .max(1.5)
            .step(0.01)
            .build()));
    private final NumberSetting vertical = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("vertical")
            .name("Vertical")
            .defaultValue(0.18)
            .min(0.02)
            .max(1.5)
            .step(0.01)
            .build()));
    private final BooleanSetting noClip = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("noclip")
            .name("No Clip")
            .defaultValue(true)
            .build()));
    private int ticks;

    public PacketFlyModule() {
        super("packet_fly", "Packet Fly", ModuleCategory.MOVEMENT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.getConnection() == null) {
            return;
        }
        if (this.noClip.value()) {
            player.noPhysics = true;
        }
        Vec3 horizontal = MovementVelocity.fromKeys(client, player.getYRot(), this.speed.value());
        double y = client.options.keyJump.isDown() ? this.vertical.value()
                : client.options.keyShift.isDown() ? -this.vertical.value() : 0.0;
        if (y == 0.0 && ++this.ticks % 12 == 0) {
            y = -0.04;
        }
        Vec3 next = player.position().add(horizontal.x, y, horizontal.z);
        player.setDeltaMovement(horizontal.x, y, horizontal.z);
        client.getConnection().send(new ServerboundMovePlayerPacket.Pos(next, false, false));
        client.getConnection().send(new ServerboundMovePlayerPacket.Pos(next.x, next.y - 1337.0, next.z, true, false));
    }

    @Override
    protected void onDisable() {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.noPhysics = false;
        }
    }
}
