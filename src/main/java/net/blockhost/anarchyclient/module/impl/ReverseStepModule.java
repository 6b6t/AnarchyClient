package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class ReverseStepModule extends Module {

    private final NumberSetting speed = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("speed")
            .name("Speed")
            .defaultValue(0.7)
            .min(0.1)
            .max(3.0)
            .step(0.1)
            .build()));

    private boolean rising;

    public ReverseStepModule() {
        super("reverse_step", "Reverse Step", ModuleCategory.MOVEMENT,
                "Pulls the player down faster when walking off ledges. Ignores jumps, flight, and elytra gliding.");
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) {
            return;
        }
        Vec3 velocity = player.getDeltaMovement();
        this.rising = airborneAfterRising(this.rising, player.onGround(), velocity.y);
        if (player.onGround() || this.rising || player.isInWater() || player.isInLava() || player.isFallFlying()
                || player.getAbilities().flying || player.isPassenger() || player.onClimbable()
                || client.options.keyJump.isDown()) {
            return;
        }
        if (velocity.y < 0.0) {
            player.setDeltaMovement(velocity.x, velocity.y - this.speed.value(), velocity.z);
        }
    }

    @Override
    protected void onDisable() {
        this.rising = false;
    }

    /** Tracks whether this airborne phase went upward (a jump or knockback), which is not a ledge drop. */
    static boolean airborneAfterRising(final boolean rising, final boolean onGround, final double verticalVelocity) {
        if (onGround) {
            return false;
        }
        return rising || verticalVelocity > 0.0;
    }
}
