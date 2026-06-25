package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class SpeedModule extends Module {

    private final NumberSetting speed = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("speed")
            .name("Speed")
            .defaultValue(0.32)
            .min(0.05)
            .max(1.5)
            .step(0.01)
            .build()));
    private final SelectSetting mode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("mode")
            .name("Mode")
            .defaultValue("Set")
            .addAllOptions(List.of("Set", "Ground", "Low Hop", "BHop", "Collision Safe"))
            .build()));
    private final BooleanSetting groundOnly = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("ground_only")
            .name("Ground Only")
            .defaultValue(true)
            .build()));

    public SpeedModule() {
        super("speed", "Speed", ModuleCategory.MOVEMENT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null
                || !MovementVelocity.moving(client)
                || player.isInWater()
                || player.isInLava()) {
            return;
        }
        if ((this.groundOnly.value() || "Ground".equals(this.mode.value())) && !player.onGround()) {
            return;
        }
        Vec3 horizontal = this.horizontalVelocity(client, player);
        if (horizontal != Vec3.ZERO) {
            player.setDeltaMovement(horizontal.x, player.getDeltaMovement().y, horizontal.z);
        }
    }

    private Vec3 horizontalVelocity(final Minecraft client, final LocalPlayer player) {
        double multiplier = switch (this.mode.value()) {
            case "Ground" -> player.onGround() ? 1.0 : 0.55;
            case "Low Hop" -> {
                if (player.onGround()) {
                    player.setDeltaMovement(player.getDeltaMovement().x, 0.18, player.getDeltaMovement().z);
                }
                yield player.onGround() ? 1.08 : 0.92;
            }
            case "BHop" -> {
                if (player.onGround()) {
                    player.jumpFromGround();
                }
                yield player.onGround() ? 1.15 : 1.02;
            }
            case "Collision Safe" -> player.horizontalCollision ? 0.35 : 0.9;
            default -> 1.0;
        };
        return MovementVelocity.fromKeys(client, player.getYRot(), this.speed.value() * multiplier);
    }

    static double modeMultiplier(final String mode, final boolean onGround, final boolean horizontalCollision) {
        return switch (mode) {
            case "Ground" -> onGround ? 1.0 : 0.55;
            case "Low Hop" -> onGround ? 1.08 : 0.92;
            case "BHop" -> onGround ? 1.15 : 1.02;
            case "Collision Safe" -> horizontalCollision ? 0.35 : 0.9;
            default -> 1.0;
        };
    }
}
