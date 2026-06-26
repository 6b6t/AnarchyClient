package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class PacketFlyModule extends Module {

    private final SelectSetting mode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("mode")
            .name("Mode")
            .defaultValue("Normal")
            .addAllOptions(List.of("Normal", "Clip", "Setback"))
            .build()));
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
    private final NumberSetting antiKickTicks = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("anti_kick_ticks")
            .name("Anti Kick")
            .defaultValue(12.0)
            .min(0.0)
            .max(60.0)
            .step(1.0)
            .build()));
    private final NumberSetting bounds = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("bounds")
            .name("Bounds")
            .defaultValue(1337.0)
            .min(10.0)
            .max(4096.0)
            .step(1.0)
            .build()));
    private final BooleanSetting sprint = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("sprint")
            .name("Sprint")
            .defaultValue(false)
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
        if (this.sprint.value()) {
            player.setSprinting(true);
        }
        Vec3 horizontal = MovementVelocity.fromKeys(client, player.getYRot(), this.speed.value());
        double y = client.options.keyJump.isDown() ? this.vertical.value()
                : client.options.keyShift.isDown() ? -this.vertical.value() : 0.0;
        int antiKick = this.antiKickTicks.value().intValue();
        if (y == 0.0 && antiKick > 0 && ++this.ticks % antiKick == 0) {
            y = -0.04;
        }
        Vec3 next = player.position().add(horizontal.x, y, horizontal.z);
        player.setDeltaMovement(horizontal.x, y, horizontal.z);
        if ("Clip".equals(this.mode.value())) {
            player.setPos(next.x, next.y, next.z);
        }
        client.getConnection().send(new ServerboundMovePlayerPacket.Pos(next, false, false));
        if ("Setback".equals(this.mode.value())) {
            Vec3 current = player.position();
            client.getConnection().send(new ServerboundMovePlayerPacket.Pos(current, true, player.horizontalCollision));
        } else {
            client.getConnection().send(new ServerboundMovePlayerPacket.Pos(next.x, next.y - this.bounds.value(), next.z,
                    true, player.horizontalCollision));
        }
        this.debugValue("mode", this.mode.value());
    }

    @Override
    protected void onDisable() {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.noPhysics = false;
        }
        this.ticks = 0;
    }
}
