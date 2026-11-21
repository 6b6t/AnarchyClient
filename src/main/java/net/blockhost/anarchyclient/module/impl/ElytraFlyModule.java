package net.blockhost.anarchyclient.module.impl;

import java.util.List;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.phys.Vec3;

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
        super(
                "elytra_fly",
                "Elytra Fly",
                ModuleCategory.MOVEMENT,
                "Controls elytra flight speed and height directly from movement keys, with optional auto start when falling while wearing a usable elytra.");
    }

    @Override
    public void tick(final Minecraft client) {
        ClientPacketListener connection = client.getConnection();
        LocalPlayer player = client.player;
        if (player == null) {
            return;
        }
        if (!player.isFallFlying()) {
            // tryToStartFallFlying checks for a usable elytra and sets the client flag, so this sends once per start.
            if (this.autoStart.value()
                    && player.getDeltaMovement().y < 0.0
                    && !player.getAbilities().flying
                    && connection != null
                    && player.tryToStartFallFlying()) {
                connection.send(new ServerboundPlayerCommandPacket(
                        player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
            }
            return;
        }
        Vec3 horizontal = MovementVelocity.fromKeys(client, player.getYRot(), this.horizontalSpeed.value());
        double y = this.preserveY.value() ? player.getDeltaMovement().y : 0.0;
        if (MovementKeys.jump(client)) {
            y += this.verticalSpeed.value();
        }
        if (MovementKeys.sneak(client)) {
            y -= this.verticalSpeed.value();
        }
        switch (this.mode.value()) {
            case "Boost" ->
                player.setDeltaMovement(
                        player.getLookAngle().x * this.horizontalSpeed.value(),
                        player.getLookAngle().y * this.verticalSpeed.value(),
                        player.getLookAngle().z * this.horizontalSpeed.value());
            case "Pitch 40" -> player.setDeltaMovement(horizontal.x, Math.max(y, -0.02), horizontal.z);
            default -> player.setDeltaMovement(horizontal.x, y, horizontal.z);
        }
    }
}
