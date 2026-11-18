package net.blockhost.anarchyclient.module.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;

/**
 * Movement keys as movement modules should see them.
 *
 * <p>Direction keys come from the player's input for this tick, not the physical keys, so Auto Walk's forward
 * counts and Freecam (which gives the body no input) stops them. Jump and sneak stay physical: Sneak forces the
 * sneak input, which must not read as "descend", so they are only gated on Freecam.
 */
final class MovementKeys {

    private MovementKeys() {
    }

    static Input input(final Minecraft client) {
        LocalPlayer player = client.player;
        return player == null ? Input.EMPTY : player.input.keyPresses;
    }

    static boolean forward(final Minecraft client) {
        return input(client).forward();
    }

    static boolean backward(final Minecraft client) {
        return input(client).backward();
    }

    static boolean left(final Minecraft client) {
        return input(client).left();
    }

    static boolean right(final Minecraft client) {
        return input(client).right();
    }

    static boolean jump(final Minecraft client) {
        return !FreecamModule.detachesCamera() && client.options.keyJump.isDown();
    }

    static boolean sneak(final Minecraft client) {
        return !FreecamModule.detachesCamera() && client.options.keyShift.isDown();
    }
}
