package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class CollisionsModule extends Module {

    public CollisionsModule() {
        super("collisions", "Collisions", ModuleCategory.MOVEMENT);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player != null) {
            client.player.noPhysics = true;
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
