package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class JesusModule extends Module {

    private final SelectSetting mode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("mode")
            .name("Mode")
            .defaultValue("Float")
            .addAllOptions(List.of("Float", "Dolphin", "Trampoline"))
            .build()));
    private final NumberSetting floatSpeed = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("float_speed")
            .name("Float Speed")
            .defaultValue(0.1)
            .min(0.02)
            .max(0.5)
            .step(0.02)
            .build()));

    public JesusModule() {
        super("jesus", "Jesus", ModuleCategory.MOVEMENT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || (!player.isInWater() && !player.isInLava()) || client.options.keyShift.isDown()) {
            return;
        }
        Vec3 velocity = player.getDeltaMovement();
        switch (this.mode.value()) {
            case "Dolphin" -> {
                Vec3 horizontal = MovementVelocity.fromKeys(client, player.getYRot(), this.floatSpeed.value() * 2.5);
                player.setDeltaMovement(horizontal.x, Math.max(velocity.y, this.floatSpeed.value() * 0.5), horizontal.z);
            }
            case "Trampoline" -> player.setDeltaMovement(velocity.x, Math.max(velocity.y, this.floatSpeed.value() * 2.2), velocity.z);
            default -> player.setDeltaMovement(velocity.x, Math.max(velocity.y, this.floatSpeed.value()), velocity.z);
        }
    }
}
