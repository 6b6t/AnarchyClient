package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class KeepSprintModule extends Module {

    private int restoreTicks;

    public KeepSprintModule() {
        super("keep_sprint", "Keep Sprint", ModuleCategory.COMBAT);
    }

    @Override
    public boolean attackEntity(final Minecraft client, final Player player, final Entity target) {
        if (player != null && player.isSprinting()) {
            this.restoreTicks = 3;
        }
        return false;
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player != null && this.restoreTicks-- > 0) {
            client.player.setSprinting(true);
        }
    }
}
