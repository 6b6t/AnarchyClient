package net.blockhost.anarchyclient.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

public final class OpenScreenEvent extends CancellableAnarchyClientEvent {

    private final Minecraft client;
    private final @Nullable Screen screen;

    public OpenScreenEvent(final Minecraft client, final @Nullable Screen screen) {
        this.client = client;
        this.screen = screen;
    }

    public Minecraft client() {
        return this.client;
    }

    public @Nullable Screen screen() {
        return this.screen;
    }
}
