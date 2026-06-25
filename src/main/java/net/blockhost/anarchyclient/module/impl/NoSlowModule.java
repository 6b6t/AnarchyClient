package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.mixin.ClientInputAccessor;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;

public final class NoSlowModule extends Module {

    public NoSlowModule() {
        super("no_slow", "No Slow", ModuleCategory.MOVEMENT);
    }

    @Override
    public void updateInput(final Minecraft client, final ClientInput input) {
        LocalPlayer player = client.player;
        if (player == null || input == null || !player.isUsingItem()) {
            return;
        }
        ((ClientInputAccessor) input).anarchyclient$setMoveVector(InputStates.moveVector(input.keyPresses));
    }
}
