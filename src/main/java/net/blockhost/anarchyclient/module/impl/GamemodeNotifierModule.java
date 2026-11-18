package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.i18n.Translations;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.util.MainThread;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.world.level.GameType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GamemodeNotifierModule extends Module {

    private final BooleanSetting survival = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("survival")
            .name("Survival")
            .defaultValue(false)
            .build()));
    private final BooleanSetting creative = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("creative")
            .name("Creative")
            .defaultValue(true)
            .build()));
    private final BooleanSetting adventure = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("adventure")
            .name("Adventure")
            .defaultValue(false)
            .build()));
    private final BooleanSetting spectator = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("spectator")
            .name("Spectator")
            .defaultValue(true)
            .build()));

    private final Map<UUID, GameType> knownModes = new HashMap<>();

    public GamemodeNotifierModule() {
        super("gamemode_notifier", "Gamemode Notifier", ModuleCategory.MISC,
                "Sends a chat message when a player switches to one of the selected gamemodes.");
    }

    @Override
    public void tick(final Minecraft client) {
        ClientPacketListener listener = client.getConnection();
        if (listener == null) {
            this.knownModes.clear();
            return;
        }
        this.knownModes.clear();
        for (PlayerInfo info : listener.getOnlinePlayers()) {
            this.knownModes.put(info.getProfile().id(), info.getGameMode());
        }
    }

    @Override
    protected void onDisable() {
        this.knownModes.clear();
    }

    @Override
    public boolean receivePacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (packet instanceof ClientboundPlayerInfoUpdatePacket update
                && update.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE)) {
            MainThread.run(() -> this.notifyChanges(client, update));
        }
        return false;
    }

    /**
     * Runs after vanilla applied the update, so the previous mode comes from the last tick's snapshot.
     */
    private void notifyChanges(final Minecraft client, final ClientboundPlayerInfoUpdatePacket update) {
        ClientPacketListener listener = client.getConnection();
        if (!this.enabled() || listener == null || client.player == null) {
            return;
        }
        for (ClientboundPlayerInfoUpdatePacket.Entry entry : update.entries()) {
            GameType previous = this.knownModes.put(entry.profileId(), entry.gameMode());
            PlayerInfo current = listener.getPlayerInfo(entry.profileId());
            if (previous == null || current == null || previous == entry.gameMode() || !shouldNotify(entry.gameMode())) {
                continue;
            }
            String name = current.getProfile().name();
            client.player.sendSystemMessage(Translations.component("anarchyclient.message.gamemode_notifier.changed_gamemode_to", "%s changed gamemode to %s.", name, entry.gameMode().getName()));
        }
    }

    private boolean shouldNotify(final GameType gameType) {
        return switch (gameType) {
            case SURVIVAL -> this.survival.value();
            case CREATIVE -> this.creative.value();
            case ADVENTURE -> this.adventure.value();
            case SPECTATOR -> this.spectator.value();
        };
    }
}
