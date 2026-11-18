package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.rotation.Rotation;
import net.blockhost.anarchyclient.rotation.RotationManager;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

import java.util.List;

public final class AntiAimModule extends Module {

    private final SelectSetting yawMode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("yaw_mode")
            .name("Yaw")
            .defaultValue("Spin")
            .addAllOptions(List.of("Spin", "Jitter", "Backwards", "Static", "Off"))
            .build()));
    private final NumberSetting spinSpeed = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("spin_speed")
            .name("Spin Speed")
            .defaultValue(30.0)
            .min(1.0)
            .max(180.0)
            .step(1.0)
            .description("Degrees turned per tick in Spin mode.")
            .build()));
    private final NumberSetting staticYaw = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("static_yaw")
            .name("Static Yaw")
            .defaultValue(0.0)
            .min(-180.0)
            .max(180.0)
            .step(5.0)
            .build()));
    private final SelectSetting pitchMode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("pitch_mode")
            .name("Pitch")
            .defaultValue("Down")
            .addAllOptions(List.of("Down", "Up", "Jitter", "Static", "Off"))
            .build()));
    private final NumberSetting staticPitch = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("static_pitch")
            .name("Static Pitch")
            .defaultValue(0.0)
            .min(-90.0)
            .max(90.0)
            .step(5.0)
            .build()));
    private final BooleanSetting pauseOnUse = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("pause_on_use")
            .name("Pause On Use")
            .defaultValue(true)
            .description("Sends your real rotation while you attack, mine, or use items so they still aim correctly.")
            .build()));
    private volatile Rotation fake;
    private int ticks;

    public AntiAimModule() {
        super("anti_aim", "Anti Aim", ModuleCategory.MISC,
                "Sends spinning or jittering rotations to the server so other players see your head move oddly. "
                        + "Your own camera does not move, and other modules that rotate you take priority.");
        this.spinSpeed.visibleWhen(() -> "Spin".equals(this.yawMode.value()));
        this.staticYaw.visibleWhen(() -> "Static".equals(this.yawMode.value()));
        this.staticPitch.visibleWhen(() -> "Static".equals(this.pitchMode.value()));
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) {
            this.fake = null;
            return;
        }
        this.ticks++;
        boolean using = client.options.keyUse.isDown() || client.options.keyAttack.isDown() || player.isUsingItem();
        if (RotationManager.hasActiveRequest() || this.pauseOnUse.value() && using) {
            this.fake = null;
            return;
        }
        this.fake = new Rotation(
                yaw(this.yawMode.value(), player.getYRot(), this.ticks, this.spinSpeed.value().floatValue(),
                        this.staticYaw.value().floatValue()),
                pitch(this.pitchMode.value(), player.getXRot(), this.ticks, this.staticPitch.value().floatValue()));
    }

    static float yaw(final String mode, final float realYaw, final int ticks, final float spinSpeed,
                     final float staticYaw) {
        return switch (mode) {
            case "Spin" -> Rotation.wrapDegrees(ticks * spinSpeed);
            case "Jitter" -> Rotation.wrapDegrees(realYaw + (ticks % 2 == 0 ? 90.0F : -90.0F));
            case "Backwards" -> Rotation.wrapDegrees(realYaw + 180.0F);
            case "Static" -> staticYaw;
            default -> realYaw;
        };
    }

    static float pitch(final String mode, final float realPitch, final int ticks, final float staticPitch) {
        return switch (mode) {
            case "Down" -> 90.0F;
            case "Up" -> -90.0F;
            case "Jitter" -> ticks % 2 == 0 ? 90.0F : -90.0F;
            case "Static" -> staticPitch;
            default -> realPitch;
        };
    }

    @Override
    public Packet<?> replaceSendPacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        Rotation rotation = this.fake;
        LocalPlayer player = client.player;
        if (rotation == null || player == null || !(packet instanceof ServerboundMovePlayerPacket move)) {
            return packet;
        }
        if (move.hasPosition()) {
            return new ServerboundMovePlayerPacket.PosRot(move.getX(player.getX()), move.getY(player.getY()),
                    move.getZ(player.getZ()), rotation.yaw(), rotation.pitch(), move.isOnGround(),
                    move.horizontalCollision());
        }
        return new ServerboundMovePlayerPacket.Rot(rotation.yaw(), rotation.pitch(), move.isOnGround(),
                move.horizontalCollision());
    }

    @Override
    protected void onDisable() {
        this.fake = null;
        this.ticks = 0;
    }
}
