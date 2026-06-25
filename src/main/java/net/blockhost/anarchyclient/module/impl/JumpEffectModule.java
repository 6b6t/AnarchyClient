package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.render.CuboidMarker;
import net.blockhost.anarchyclient.render.MarkerManager;
import net.blockhost.anarchyclient.render.MarkerStyle;
import net.minecraft.client.Minecraft;

public final class JumpEffectModule extends Module {

    private boolean wasOnGround;

    public JumpEffectModule() {
        super("jump_effect", "Jump Effect", ModuleCategory.RENDER);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null) {
            this.wasOnGround = false;
            return;
        }
        boolean onGround = client.player.onGround();
        if (this.wasOnGround && !onGround && client.player.getDeltaMovement().y > 0.0) {
            MarkerManager.put(new CuboidMarker("jump_effect:" + client.player.tickCount,
                    client.player.getBoundingBox().inflate(0.15).move(0.0, -0.2, 0.0), MarkerStyle.CYAN, 12));
        }
        this.wasOnGround = onGround;
    }
}
