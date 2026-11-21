package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;

public final class CollisionsModule extends Module {

    public CollisionsModule() {
        super(
                "collisions",
                "Collisions",
                ModuleCategory.MOVEMENT,
                "Turns off client-side block collision so the player can move through blocks the server allows.");
    }

    /**
     * Vanilla resets {@code noPhysics} at the start of every player tick, so setting it in the end-of-tick hook
     * never reached a move. Input is read in {@code aiStep}, after that reset and before the player moves.
     */
    @Override
    public void updateInput(final Minecraft client, final ClientInput input) {
        LocalPlayer player = client.player;
        if (player != null && player.input == input) {
            player.noPhysics = true;
        }
    }

    @Override
    protected void onDisable() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.noPhysics = false;
        }
    }
}
