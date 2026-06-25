package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class FullFlightModule extends Module {

    private final NumberSetting horizontal = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("horizontal")
            .name("Horizontal")
            .defaultValue(0.45)
            .min(0.05)
            .max(2.5)
            .step(0.05)
            .build()));
    private final NumberSetting vertical = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("vertical")
            .name("Vertical")
            .defaultValue(0.35)
            .min(0.05)
            .max(2.5)
            .step(0.05)
            .build()));
    private int ticks;

    public FullFlightModule() {
        super("full_flight", "Full Flight", ModuleCategory.MOVEMENT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) {
            return;
        }
        Vec3 horizontalVelocity = MovementVelocity.fromKeys(client, player.getYRot(), this.horizontal.value());
        double y = 0.0;
        if (client.options.keyJump.isDown()) {
            y += this.vertical.value();
        }
        if (client.options.keyShift.isDown()) {
            y -= this.vertical.value();
        }
        if (y == 0.0 && ++this.ticks % 20 == 0) {
            y = -0.04;
        }
        player.setDeltaMovement(horizontalVelocity.x, y, horizontalVelocity.z);
        player.resetFallDistance();
    }
}
