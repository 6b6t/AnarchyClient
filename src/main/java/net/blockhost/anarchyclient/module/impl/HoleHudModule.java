package net.blockhost.anarchyclient.module.impl;

import java.util.List;
import net.minecraft.client.Minecraft;

public final class HoleHudModule extends HudElementModule {

    public HoleHudModule() {
        super(
                "hole_hud",
                "Hole HUD",
                "Shows whether bedrock, obsidian, or other blast-proof blocks surround you on all four sides and below.",
                "Top Right");
    }

    @Override
    protected int color(final Minecraft client) {
        return this.inHole(client) ? HudText.accentColor() : 0xFFE56A6A;
    }

    @Override
    protected List<String> lines(final Minecraft client) {
        return List.of(this.inHole(client) ? "Hole safe" : "Hole unsafe");
    }

    private boolean inHole(final Minecraft client) {
        if (client.player == null || client.level == null) {
            return false;
        }
        return HoleEspModule.isHole(client.level, client.player.blockPosition());
    }
}
