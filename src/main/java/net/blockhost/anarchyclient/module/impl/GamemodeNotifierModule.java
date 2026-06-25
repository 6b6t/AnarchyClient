package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.world.level.GameType;

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

    public GamemodeNotifierModule() {
        super("gamemode_notifier", "Gamemode Notifier", ModuleCategory.MISC);
    }

    @Override
    public boolean receivePacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (!(packet instanceof ClientboundPlayerInfoUpdatePacket update)
                || !update.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE)) {
            return false;
        }
        ClientPacketListener listener = client.getConnection();
        if (listener == null || client.player == null) {
            return false;
        }
        for (ClientboundPlayerInfoUpdatePacket.Entry entry : update.entries()) {
            PlayerInfo current = listener.getPlayerInfo(entry.profileId());
            if (current == null || current.getGameMode() == entry.gameMode() || !shouldNotify(entry.gameMode())) {
                continue;
            }
            String name = current.getProfile().name();
            client.player.sendSystemMessage(Component.literal(name + " changed gamemode to " + entry.gameMode().getName() + "."));
        }
        return false;
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
