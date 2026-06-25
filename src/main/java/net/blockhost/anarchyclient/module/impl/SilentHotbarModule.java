package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.inventory.SilentHotbar;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public final class SilentHotbarModule extends HudElementModule {

    public SilentHotbarModule() {
        super("silent_hotbar", "Silent Hotbar", "Bottom Left");
    }

    @Override
    protected List<String> lines(final Minecraft client) {
        List<String> lines = new ArrayList<>();
        lines.add("Silent Hotbar");
        SilentHotbar.activeOwner().ifPresentOrElse(
                owner -> lines.add(owner + " slot " + SilentHotbar.activeSlot().orElse(-1)),
                () -> lines.add("idle")
        );
        return lines;
    }
}
