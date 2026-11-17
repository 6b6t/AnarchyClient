package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class KeepSprintModule extends Module {

    private Vec3 motionBeforeHit;

    public KeepSprintModule() {
        super("keep_sprint", "Keep Sprint", ModuleCategory.COMBAT,
                "Keeps your sprint and speed after a sprint hit instead of slowing down.");
    }

    @Override
    public boolean attackEntity(final Minecraft client, final Player player, final Entity target) {
        if (player instanceof LocalPlayer && player.isSprinting()) {
            this.motionBeforeHit = player.getDeltaMovement();
        }
        return false;
    }

    /**
     * Runs during the player's movement update, after the attack slowed the player down but
     * before the player moves and sends its sprint state.
     */
    @Override
    public void updateInput(final Minecraft client, final ClientInput input) {
        LocalPlayer player = client.player;
        Vec3 before = this.motionBeforeHit;
        this.motionBeforeHit = null;
        if (player == null || before == null) {
            return;
        }
        player.setDeltaMovement(restoredMotion(before, player.getDeltaMovement()));
        player.setSprinting(true);
    }

    @Override
    protected void onDisable() {
        this.motionBeforeHit = null;
    }

    /**
     * Undoes the vanilla sprint-hit slowdown (horizontal motion times 0.6) without adding speed
     * the player did not have before the hit.
     */
    static Vec3 restoredMotion(final Vec3 before, final Vec3 now) {
        if (now.horizontalDistanceSqr() >= before.horizontalDistanceSqr()) {
            return now;
        }
        return new Vec3(before.x, now.y, before.z);
    }
}
