package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.i18n.Translations;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.multiplayer.ClientLevel;

public final class AntiQuitModule extends Module {

    private static volatile AntiQuitModule active;
    private static boolean confirmed;

    public AntiQuitModule() {
        super("anti_quit", "Anti Quit", ModuleCategory.MISC,
                "Asks for confirmation when you press Disconnect or Save and Quit in the pause menu, so a misclick does not log you out.");
    }

    @Override
    protected void onEnable() {
        active = this;
    }

    @Override
    protected void onDisable() {
        if (active == this) {
            active = null;
        }
    }

    /**
     * Called before the client leaves the world from the pause menu. Returns {@code true} when the
     * module opened a confirmation screen instead, so the caller must not disconnect yet.
     */
    public static boolean interceptDisconnect(final Minecraft client) {
        if (active == null || confirmed || client.level == null) {
            return false;
        }
        client.gui.setScreen(new ConfirmScreen(
                accepted -> {
                    if (accepted) {
                        confirmed = true;
                        try {
                            client.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE);
                        } finally {
                            confirmed = false;
                        }
                    } else {
                        client.gui.setScreen(new PauseScreen(true));
                    }
                },
                Translations.component("anarchyclient.message.anti_quit.title", "Leave this world?"),
                Translations.component("anarchyclient.message.anti_quit.message",
                        "Anti Quit is on. Choose Yes to disconnect.")));
        return true;
    }
}
