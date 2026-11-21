package net.blockhost.anarchyclient.module;

import java.util.List;

final class ModuleIcons {

    private static final List<IconRule> ICON_RULES = List.of(
            new IconRule("totem", "shield"),
            new IconRule("armor", "shield-check"),
            new IconRule("shield", "shield"),
            new IconRule("aura", "target"),
            new IconRule("target", "target"),
            new IconRule("aim", "crosshair"),
            new IconRule("bow", "crosshair"),
            new IconRule("shoot", "crosshair"),
            new IconRule("weapon", "axe"),
            new IconRule("combat", "skull"),
            new IconRule("crystal", "gem"),
            new IconRule("bed", "home"),
            new IconRule("anchor", "anchor"),
            new IconRule("tnt", "flame"),
            new IconRule("wither", "skull"),
            new IconRule("esp", "eye"),
            new IconRule("xray", "eye"),
            new IconRule("sight", "eye"),
            new IconRule("chams", "eye"),
            new IconRule("render", "eye"),
            new IconRule("hud", "layout-list"),
            new IconRule("radar", "radio"),
            new IconRule("waypoint", "map-pin"),
            new IconRule("map", "map"),
            new IconRule("sound", "volume"),
            new IconRule("note", "music"),
            new IconRule("chat", "message-square"),
            new IconRule("message", "send"),
            new IconRule("packet", "server"),
            new IconRule("payload", "database"),
            new IconRule("server", "server"),
            new IconRule("plugin", "plug-zap"),
            new IconRule("debug", "bug"),
            new IconRule("logger", "file-text"),
            new IconRule("recorder", "file-text"),
            new IconRule("move", "move"),
            new IconRule("speed", "gauge"),
            new IconRule("timer", "timer"),
            new IconRule("step", "move-vertical"),
            new IconRule("jump", "arrow-up"),
            new IconRule("fly", "plane"),
            new IconRule("elytra", "plane"),
            new IconRule("boat", "car"),
            new IconRule("vehicle", "car"),
            new IconRule("fish", "droplet"),
            new IconRule("farm", "carrot"),
            new IconRule("tree", "palmtree"),
            new IconRule("moss", "palmtree"),
            new IconRule("lawn", "palmtree"),
            new IconRule("mine", "hammer"),
            new IconRule("ore", "gem"),
            new IconRule("block", "box"),
            new IconRule("chest", "package-search"),
            new IconRule("inventory", "package"),
            new IconRule("item", "package"),
            new IconRule("shop", "shopping-cart"),
            new IconRule("sign", "edit"),
            new IconRule("book", "book-open"),
            new IconRule("name", "user"),
            new IconRule("friend", "users"),
            new IconRule("bot", "bot"),
            new IconRule("ghost", "ghost"),
            new IconRule("discord", "radio"),
            new IconRule("macro", "keyboard"),
            new IconRule("click", "mouse-pointer-click"),
            new IconRule("weather", "cloud"),
            new IconRule("time", "clock"),
            new IconRule("light", "sun"),
            new IconRule("flame", "flame"),
            new IconRule("fire", "flame"),
            new IconRule("exploit", "bug"),
            new IconRule("crash", "server-crash"),
            new IconRule("alert", "bell-ring"),
            new IconRule("notifier", "bell"),
            new IconRule("flag", "flag"),
            new IconRule("config", "settings-2"),
            new IconRule("protect", "lock"),
            new IconRule("spoof", "wifi"),
            new IconRule("vanish", "eye-off"));

    private ModuleIcons() {}

    static String icon(final Module module) {
        String id = module.id();
        for (IconRule rule : ICON_RULES) {
            if (id.contains(rule.token())) {
                return rule.icon();
            }
        }
        return switch (module.category()) {
            case COMBAT -> "target";
            case RENDER -> "eye";
            case MOVEMENT -> "move";
            case WORLD -> "hammer";
            case PLAYER -> "user";
            case HUD -> "layout-list";
            case MISC -> "settings";
            case FUN -> "gamepad";
        };
    }

    private record IconRule(String token, String icon) {}
}
