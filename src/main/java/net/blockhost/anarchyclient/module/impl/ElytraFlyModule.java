package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class ElytraFlyModule extends Module {

    private final SelectSetting mode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("mode")
            .name("Mode")
            .defaultValue("Control")
            .addAllOptions(List.of("Control", "Boost", "Pitch 40"))
            .build()));
    private final NumberSetting horizontalSpeed = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("horizontal_speed")
            .name("Horizontal")
            .defaultValue(1.2)
            .min(0.1)
            .max(4.0)
            .step(0.1)
            .build()));
    private final NumberSetting verticalSpeed = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("vertical_speed")
            .name("Vertical")
            .defaultValue(0.6)
            .min(0.05)
            .max(2.0)
            .step(0.05)
            .build()));
    private final BooleanSetting autoStart = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("auto_start")
            .name("Auto Start")
            .defaultValue(true)
            .build()));
    private final BooleanSetting preserveY = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("preserve_y")
            .name("Preserve Y")
            .defaultValue(false)
            .build()));

    public ElytraFlyModule() {
        super("elytra_fly", "Elytra Fly", ModuleCategory.MOVEMENT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) {
            return;
        }
        if (!player.isFallFlying()) {
            if (this.autoStart.value() && !player.onGround() && player.getDeltaMovement().y < 0.0 && client.getConnection() != null) {
                client.getConnection().send(new ServerboundPlayerCommandPacket(player,
                        ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
            }
            return;
        }
        Vec3 horizontal = MovementVelocity.fromKeys(client, player.getYRot(), this.horizontalSpeed.value());
        double y = this.preserveY.value() ? player.getDeltaMovement().y : 0.0;
        if (client.options.keyJump.isDown()) {
            y += this.verticalSpeed.value();
        }
        if (client.options.keyShift.isDown()) {
            y -= this.verticalSpeed.value();
        }
        switch (this.mode.value()) {
            case "Boost" -> player.setDeltaMovement(
                    player.getLookAngle().x * this.horizontalSpeed.value(),
                    player.getLookAngle().y * this.verticalSpeed.value(),
                    player.getLookAngle().z * this.horizontalSpeed.value()
            );
            case "Pitch 40" -> player.setDeltaMovement(horizontal.x, Math.max(y, -0.02), horizontal.z);
            default -> player.setDeltaMovement(horizontal.x, y, horizontal.z);
        }
    }
}
