package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class TeamsModule extends Module {

    private static TeamsModule active;

    private final BooleanSetting scoreboard = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("scoreboard")
            .name("Scoreboard")
            .defaultValue(true)
            .build()));
    private final BooleanSetting namePrefix = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("name_prefix")
            .name("Prefix")
            .defaultValue(true)
            .build()));

    public TeamsModule() {
        super("teams", "Teams", ModuleCategory.COMBAT);
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

    public static boolean allows(final Entity entity, final Player player) {
        TeamsModule module = active;
        if (module == null || entity == null || player == null || entity == player) {
            return true;
        }
        if (module.scoreboard.value() && entity.getTeam() != null && player.getTeam() != null
                && entity.getTeam().isAlliedTo(player.getTeam())) {
            return false;
        }
        return !module.namePrefix.value() || !samePrefix(entity.getScoreboardName(), player.getScoreboardName());
    }

    static boolean samePrefix(final String first, final String second) {
        String a = prefix(first);
        String b = prefix(second);
        return !a.isBlank() && a.equalsIgnoreCase(b);
    }

    private static String prefix(final String name) {
        if (name == null || name.length() < 2) {
            return "";
        }
        char first = name.charAt(0);
        return switch (first) {
            case '[', '(', '<', '{' -> {
                int close = name.indexOf(switch (first) {
                    case '[' -> ']';
                    case '(' -> ')';
                    case '<' -> '>';
                    default -> '}';
                });
                yield close > 0 ? name.substring(0, close + 1) : "";
            }
            default -> "";
        };
    }
}
