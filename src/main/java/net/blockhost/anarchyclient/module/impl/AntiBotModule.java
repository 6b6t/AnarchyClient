package net.blockhost.anarchyclient.module.impl;

import com.mojang.authlib.GameProfile;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class AntiBotModule extends Module {

    private final BooleanSetting missingTabEntry = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("missing_tab_entry")
            .name("No Tab")
            .defaultValue(true)
            .build()));
    private final BooleanSetting invalidProfile = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("invalid_profile")
            .name("Profile")
            .defaultValue(true)
            .build()));
    private final BooleanSetting defaultGameMode = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("default_game_mode")
            .name("Gamemode")
            .defaultValue(false)
            .build()));
    private final BooleanSetting negativePing = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("negative_ping")
            .name("Ping")
            .defaultValue(false)
            .build()));
    private final BooleanSetting removeInvisible = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("remove_invisible")
            .name("Invisible")
            .defaultValue(false)
            .build()));

    public AntiBotModule() {
        super("anti_bot", "Anti Bot", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        ClientPacketListener connection = client.getConnection();
        if (player == null || client.level == null || connection == null) {
            return;
        }
        List<Player> bots = new ArrayList<>();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof Player other && other != player && this.shouldRemove(other, connection)) {
                bots.add(other);
            }
        }
        bots.forEach(Entity::discard);
    }

    private boolean shouldRemove(final Player player, final ClientPacketListener connection) {
        PlayerInfo info = connection.getPlayerInfo(player.getUUID());
        return shouldRemoveCandidate(
                player.isInvisible(),
                this.removeInvisible.value(),
                this.missingTabEntry.value(),
                this.invalidProfile.value(),
                this.defaultGameMode.value(),
                this.negativePing.value(),
                info != null,
                info == null ? null : info.getGameMode(),
                info == null ? null : info.getProfile(),
                info == null ? 0 : info.getLatency()
        );
    }

    static boolean shouldRemoveCandidate(final boolean invisible, final boolean removeInvisible,
                                         final boolean missingTabEntry, final boolean invalidProfile,
                                         final boolean defaultGameMode, final boolean negativePing,
                                         final boolean hasTabEntry, final GameType gameMode,
                                         final GameProfile profile, final int latency) {
        if (removeInvisible && invisible) {
            return true;
        }
        if (missingTabEntry && !hasTabEntry) {
            return true;
        }
        if (invalidProfile && invalidProfile(profile)) {
            return true;
        }
        if (defaultGameMode && gameMode == GameType.DEFAULT_MODE) {
            return true;
        }
        return negativePing && latency < 0;
    }

    private static boolean invalidProfile(final GameProfile profile) {
        if (profile == null || profile.id() == null || profile.id().equals(new UUID(0L, 0L))) {
            return true;
        }
        String name = profile.name();
        return name == null || name.isBlank();
    }
}
