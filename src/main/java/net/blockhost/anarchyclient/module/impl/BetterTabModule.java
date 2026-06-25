package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;

public final class BetterTabModule extends Module {

    private final BooleanSetting ping = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("ping")
            .name("Ping")
            .defaultValue(true)
            .build()));
    private final BooleanSetting gamemode = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("gamemode")
            .name("Gamemode")
            .defaultValue(false)
            .build()));

    public BetterTabModule() {
        super("better_tab", "Better Tab", ModuleCategory.RENDER);
    }

    @Override
    public Component tabPlayerName(final Minecraft client, final PlayerInfo playerInfo, final Component name) {
        if (playerInfo == null || name == null) {
            return name;
        }
        Component result = name.copy();
        if (this.gamemode.value()) {
            GameType mode = playerInfo.getGameMode();
            result = Component.literal("[" + mode.getName() + "] ")
                    .withStyle(ChatFormatting.DARK_GRAY)
                    .append(result);
        }
        if (this.ping.value()) {
            result = Component.literal(playerInfo.getLatency() + "ms ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(result);
        }
        return result;
    }
}
