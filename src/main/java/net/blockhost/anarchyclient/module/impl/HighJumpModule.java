package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class HighJumpModule extends Module {

    private final NumberSetting height = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("height")
            .name("Height")
            .defaultValue(0.75)
            .min(0.42)
            .max(2.5)
            .step(0.05)
            .build()));

    private static final double VANILLA_JUMP = 0.42;
    private static final double DRAG = 0.98;

    private boolean wasOnGround;

    public HighJumpModule() {
        super(
                "high_jump",
                "High Jump",
                ModuleCategory.MOVEMENT,
                "Sets a higher upward velocity when you jump from the ground so you clear taller obstacles.");
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) {
            this.wasOnGround = false;
            return;
        }
        // Module ticks run after the player tick, so a jump has already left the ground by now.
        double velocityY = player.getDeltaMovement().y;
        if (this.wasOnGround
                && !player.onGround()
                && velocityY > 0.0
                && player.input != null
                && player.input.keyPresses.jump()) {
            player.setDeltaMovement(
                    player.getDeltaMovement().x,
                    boostedVelocity(velocityY, this.height.value()),
                    player.getDeltaMovement().z);
        }
        this.wasOnGround = player.onGround();
    }

    @Override
    protected void onDisable() {
        this.wasOnGround = false;
    }

    /** Adds the extra launch speed on top of the vanilla jump, after one tick of air drag. */
    static double boostedVelocity(final double velocityAfterJumpTick, final double height) {
        return velocityAfterJumpTick + (height - VANILLA_JUMP) * DRAG;
    }
}
