package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class NoWebModule extends Module {

    private static boolean active;
    private static String activeMode = "Ignore";

    private final SelectSetting mode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("mode")
            .name("Mode")
            .defaultValue("Ignore")
            .addAllOptions(List.of("Ignore", "Motion", "Vanilla"))
            .build()));
    private final NumberSetting motionSpeed = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("motion_speed")
            .name("Motion")
            .defaultValue(0.35)
            .min(0.05)
            .max(1.0)
            .step(0.05)
            .build()));

    public NoWebModule() {
        super("no_web", "No Web", ModuleCategory.MOVEMENT);
    }

    @Override
    protected void onEnable() {
        active = true;
        activeMode = this.mode.value();
    }

    @Override
    public void tick(final Minecraft client) {
        activeMode = this.mode.value();
        LocalPlayer player = client.player;
        if (player == null || client.level == null || !"Motion".equals(this.mode.value()) || !insideWeb(client, player)) {
            return;
        }
        Vec3 horizontal = MovementVelocity.fromKeys(client, player.getYRot(), this.motionSpeed.value());
        double y = client.options.keyJump.isDown() ? this.motionSpeed.value()
                : client.options.keyShift.isDown() ? -this.motionSpeed.value() : player.getDeltaMovement().y;
        player.setDeltaMovement(horizontal.x, y, horizontal.z);
    }

    @Override
    protected void onDisable() {
        active = false;
        activeMode = "Ignore";
    }

    public static boolean shouldIgnore(final BlockState state) {
        return active && "Ignore".equals(activeMode) && state != null && state.is(Blocks.COBWEB);
    }

    private static boolean insideWeb(final Minecraft client, final LocalPlayer player) {
        return client.level.getBlockState(player.blockPosition()).is(Blocks.COBWEB)
                || client.level.getBlockState(player.blockPosition().above()).is(Blocks.COBWEB);
    }
}
