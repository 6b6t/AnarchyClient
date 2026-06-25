package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

public final class AntiVoidModule extends Module {

    private final NumberSetting minY = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("min_y")
            .name("Min Y")
            .defaultValue(0.0)
            .min(-128.0)
            .max(128.0)
            .step(1.0)
            .build()));
    private final BooleanSetting freezeHorizontal = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("freeze_horizontal")
            .name("Freeze")
            .defaultValue(true)
            .build()));
    private final BooleanSetting jump = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("jump")
            .name("Jump")
            .defaultValue(false)
            .build()));
    private final BooleanSetting warn = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("warn")
            .name("Warn")
            .defaultValue(true)
            .build()));
    private boolean warned;

    public AntiVoidModule() {
        super("anti_void", "Anti Void", ModuleCategory.MOVEMENT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) {
            this.warned = false;
            return;
        }
        if (player.getY() > this.minY.value() + 4.0 || player.onGround()) {
            this.warned = false;
            return;
        }
        if (player.getY() > this.minY.value()) {
            return;
        }

        Vec3 velocity = player.getDeltaMovement();
        double upward = this.jump.value() ? Math.max(velocity.y, 0.42) : Math.max(velocity.y, 0.0);
        player.setDeltaMovement(
                this.freezeHorizontal.value() ? 0.0 : velocity.x,
                upward,
                this.freezeHorizontal.value() ? 0.0 : velocity.z
        );
        player.fallDistance = 0.0;
        if (this.warn.value() && !this.warned) {
            player.sendSystemMessage(Component.literal("Anti Void stopped a fall below Y " + this.minY.value().intValue()));
            this.warned = true;
        }
    }
}
