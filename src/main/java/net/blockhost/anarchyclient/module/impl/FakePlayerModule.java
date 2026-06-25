package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.render.CuboidMarker;
import net.blockhost.anarchyclient.render.MarkerManager;
import net.blockhost.anarchyclient.render.MarkerStyle;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.minecraft.client.Minecraft;

public final class FakePlayerModule extends Module {

    private static final String MARKER_ID = "fake_player:self";

    public FakePlayerModule() {
        super("fake_player", "Fake Player", ModuleCategory.MISC);
    }

    @Override
    protected void onEnable() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            MarkerManager.put(new CuboidMarker(MARKER_ID, client.player.getBoundingBox(), MarkerStyle.CYAN, 0));
        }
    }

    @Override
    protected void onDisable() {
        MarkerManager.remove(MARKER_ID);
    }
}
