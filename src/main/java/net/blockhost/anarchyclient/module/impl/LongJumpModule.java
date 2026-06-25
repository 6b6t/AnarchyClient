package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class LongJumpModule extends Module {

    private final NumberSetting horizontal = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("horizontal")
            .name("Horizontal")
            .defaultValue(0.8)
            .min(0.1)
            .max(3.0)
            .step(0.05)
            .build()));
    private final NumberSetting vertical = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("vertical")
            .name("Vertical")
            .defaultValue(0.55)
            .min(0.1)
            .max(2.0)
            .step(0.05)
            .build()));
    private boolean launched;
    private int airTicks;

    public LongJumpModule() {
        super("long_jump", "Long Jump", ModuleCategory.MOVEMENT);
    }

    @Override
    protected void onEnable() {
        this.launched = false;
        this.airTicks = 0;
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) {
            return;
        }
        if (!this.launched && player.onGround() && MovementVelocity.moving(client)) {
            Vec3 horizontalVelocity = MovementVelocity.fromKeys(client, player.getYRot(), this.horizontal.value());
            player.setDeltaMovement(horizontalVelocity.x, this.vertical.value(), horizontalVelocity.z);
            this.launched = true;
            this.airTicks = 0;
            return;
        }
        if (this.launched) {
            this.airTicks++;
            if (this.airTicks > 5 && player.onGround()) {
                this.enabled(false);
            }
        }
    }
}
