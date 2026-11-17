package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.HitResult;

public final class NoMissCooldownModule extends Module {

    private int lastMissTick;

    public NoMissCooldownModule() {
        super("no_miss_cooldown", "No Miss Cooldown", ModuleCategory.COMBAT);
    }

    @Override
    public boolean mouseClick(final Minecraft client, final MouseButtonInfo buttonInfo,
                              final int action) {
        if (client.player != null && action == 1 && client.hitResult != null
                && client.hitResult.getType() == HitResult.Type.MISS) {
            this.lastMissTick = client.player.tickCount;
        }
        return false;
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player != null && this.lastMissTick > 0 && client.player.tickCount - this.lastMissTick == 1) {
            this.debugValue("last_miss", this.lastMissTick);
        }
    }

    @Override
    protected void onDisable() {
        this.clearDebugValues();
    }

    static Component missCooldownNote() {
        return Component.literal("Miss tracked");
    }
}
