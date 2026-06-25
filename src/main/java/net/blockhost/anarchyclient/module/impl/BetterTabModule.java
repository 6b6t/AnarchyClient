package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.server.ServerObserver;
import net.blockhost.anarchyclient.server.ServerProfileStore;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class BetterTabModule extends Module {

    private static BetterTabModule active;

    private final SelectSetting sort = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("sort")
            .name("Sort")
            .defaultValue("Default")
            .addAllOptions(List.of("Default", "Ping", "Name", "Name Length"))
            .build()));
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
    private final BooleanSetting highlightSelf = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("highlight_self")
            .name("Self")
            .defaultValue(true)
            .build()));
    private final BooleanSetting highlightFriends = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("highlight_friends")
            .name("Friends")
            .defaultValue(true)
            .build()));
    private final BooleanSetting highlightStaff = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("highlight_staff")
            .name("Staff")
            .defaultValue(true)
            .build()));
    private final StringSetting hideRegex = this.setting(StringSetting.from(StringSetting.builder()
            .id("hide_regex")
            .name("Hide")
            .defaultValue("")
            .description("Regex for names to hide from the tab overlay.")
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
        String playerName = playerInfo.getProfile().name();
        if (this.highlightSelf.value() && client != null && client.getUser() != null
                && playerName.equalsIgnoreCase(client.getUser().getName())) {
            result = Component.literal("* ").withStyle(ChatFormatting.GOLD).append(result);
        } else if (this.highlightFriends.value() && AnarchyClient.FRIENDS.isFriend(playerName)) {
            result = Component.literal("+ ").withStyle(ChatFormatting.AQUA).append(result);
        } else if (this.highlightStaff.value() && this.isStaff(playerName)) {
            result = Component.literal("! ").withStyle(ChatFormatting.RED).append(result);
        }
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

    public static List<PlayerInfo> rewritePlayerInfos(final List<PlayerInfo> infos) {
        BetterTabModule module = active;
        if (module == null || infos == null || infos.isEmpty()) {
            return infos;
        }
        Pattern hidePattern = module.hidePattern();
        Comparator<PlayerInfo> comparator = switch (module.sort.value()) {
            case "Ping" -> Comparator.comparingInt(PlayerInfo::getLatency);
            case "Name" -> Comparator.comparing(info -> info.getProfile().name(), String.CASE_INSENSITIVE_ORDER);
            case "Name Length" -> Comparator.comparingInt((PlayerInfo info) -> info.getProfile().name().length())
                    .thenComparing(info -> info.getProfile().name(), String.CASE_INSENSITIVE_ORDER);
            default -> null;
        };
        java.util.stream.Stream<PlayerInfo> stream = infos.stream()
                .filter(info -> hidePattern == null || !hidePattern.matcher(info.getProfile().name()).find());
        if (comparator != null) {
            stream = stream.sorted(comparator);
        }
        return stream.limit(80L).toList();
    }

    private boolean isStaff(final String playerName) {
        String normalized = normalize(playerName);
        return !normalized.isBlank()
                && ServerProfileStore.staffNames(ServerObserver.snapshot().rootDomain()).stream()
                .map(BetterTabModule::normalize)
                .anyMatch(normalized::equals);
    }

    private Pattern hidePattern() {
        String value = this.hideRegex.value();
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Pattern.compile(value, Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException ignored) {
            return null;
        }
    }

    private static String normalize(final String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
