package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;

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
            AutoLogModule.consumeSafetyDisconnect();
            this.lastServer = copyServer(client.getCurrentServer());
            this.reconnectTicks = this.delaySeconds.value().intValue() * 20;
            return;
        }
        if (!(client.gui.screen() instanceof DisconnectedScreen) || this.lastServer == null) {
            return;
        }
        if (AutoLogModule.consumeSafetyDisconnect()) {
            // Auto Log left on purpose; rejoining would put the player back into the same danger.
            this.lastServer = null;
            return;
        }
        if (this.reconnectTicks > 0) {
            this.reconnectTicks--;
            return;
        }
        if (this.notify.value()) {
            SystemToast.addOrUpdate(client.gui.toastManager(), RECONNECT_TOAST, Component.literal("Auto Reconnect"),
                    Component.literal("Reconnecting to " + this.lastServer.ip + "..."));
        }
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

    static ServerData copyServer(final ServerData server) {
        ServerData copy = new ServerData(server.name, server.ip, server.type());
        copy.copyFrom(server);
        return copy;
    }
}
