package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.i18n.Translations;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

public final class AutoReconnectModule extends Module {

    private final NumberSetting delaySeconds = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay")
            .name("Delay")
            .defaultValue(5.0)
            .min(1.0)
            .max(120.0)
            .step(1.0)
            .build()));
    private final BooleanSetting notify = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("notify")
            .name("Notify")
            .defaultValue(true)
            .build()));

    private static final SystemToast.SystemToastId RECONNECT_TOAST = new SystemToast.SystemToastId();

    /** Set when Silent Disconnect skipped the disconnected screen that would otherwise start a reconnect. */
    private static boolean silentDisconnect;

    private ServerData lastServer;
    private int reconnectTicks;

    public AutoReconnectModule() {
        super("auto_reconnect", "Auto Reconnect", ModuleCategory.MISC, "Reconnects to the last server after a disconnect, except when Auto Log disconnected you.");
    }

    @Override
    public void tick(final Minecraft client) {
        if (client == null) {
            return;
        }
        if (client.level != null && client.getCurrentServer() != null) {
            // Auto Log disconnects during its tick, so the level can still exist here. Keep its flag
            // until the disconnected screen decides whether to reconnect.
            this.lastServer = copyServer(client.getCurrentServer());
            this.reconnectTicks = this.delaySeconds.value().intValue() * 20;
            return;
        }
        if (client.gui.screen() instanceof ConnectScreen) {
            // The player is already joining a server, so a reconnect would only interrupt it.
            silentDisconnect = false;
            return;
        }
        boolean disconnected = client.gui.screen() instanceof DisconnectedScreen || silentDisconnect;
        if (!disconnected || this.lastServer == null) {
            return;
        }
        if (AutoLogModule.consumeSafetyDisconnect()) {
            // Auto Log left on purpose; rejoining would put the player back into the same danger.
            this.lastServer = null;
            silentDisconnect = false;
            return;
        }
        if (this.reconnectTicks > 0) {
            this.reconnectTicks--;
            return;
        }
        if (this.notify.value()) {
            SystemToast.addOrUpdate(client.gui.toastManager(), RECONNECT_TOAST, Translations.component("anarchyclient.message.auto_reconnect.auto_reconnect", "Auto Reconnect"),
                    Translations.component("anarchyclient.message.auto_reconnect.reconnecting_to", "Reconnecting to %s...", this.lastServer.ip));
        }
        silentDisconnect = false;
        ConnectScreen.startConnecting(
                new TitleScreen(),
                client,
                ServerAddress.parseString(this.lastServer.ip),
                this.lastServer,
                false,
                null
        );
        this.reconnectTicks = this.delaySeconds.value().intValue() * 20;
    }

    @Override
    public void gameJoined(final Minecraft client, final ClientPacketListener listener) {
        // A new session started, so an Auto Log disconnect from an earlier session no longer applies.
        AutoLogModule.clearSafetyDisconnect();
        silentDisconnect = false;
    }

    @Override
    protected void onEnable() {
        silentDisconnect = false;
    }

    @Override
    protected void onDisable() {
        silentDisconnect = false;
    }

    /** Called on the client thread when the server dropped the connection but Silent Disconnect hid the screen. */
    public static void noteSilentDisconnect() {
        silentDisconnect = true;
    }

    static ServerData copyServer(final ServerData server) {
        ServerData copy = new ServerData(server.name, server.ip, server.type());
        copy.copyFrom(server);
        return copy;
    }
}
