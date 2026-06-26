package net.blockhost.anarchyclient.module;

import net.blockhost.anarchyclient.setting.BlockListSetting;
import net.blockhost.anarchyclient.setting.BlockPosSetting;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.ColorListSetting;
import net.blockhost.anarchyclient.setting.ColorSetting;
import net.blockhost.anarchyclient.setting.EntityTypeListSetting;
import net.blockhost.anarchyclient.setting.ItemListSetting;
import net.blockhost.anarchyclient.setting.ModuleListSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.PacketListSetting;
import net.blockhost.anarchyclient.setting.ParticleTypeListSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.blockhost.anarchyclient.setting.Setting;
import net.blockhost.anarchyclient.setting.SoundEventListSetting;
import net.blockhost.anarchyclient.setting.StatusEffectListSetting;
import net.blockhost.anarchyclient.setting.StringListSetting;
import net.blockhost.anarchyclient.setting.TextValueSetting;
import net.blockhost.anarchyclient.setting.Vector3dSetting;

import java.util.List;
import java.util.Locale;
import java.util.Map;

final class ModuleMetadata {

    private static final Map<String, String> MODULE_DESCRIPTIONS = Map.ofEntries(
            Map.entry("server_observer", "Tracks server identity, brand, and connection details so other modules can adapt to the current server."),
            Map.entry("auto_config", "Applies safer module presets after server detection and can disable risky modules when checks look unstable."),
            Map.entry("anti_cheat_detect", "Looks for anti-cheat signals and exposes them to modules that need stricter timing or safer behavior."),
            Map.entry("plugin_scanner", "Inspects server plugin hints from available commands, messages, and protocol details."),
            Map.entry("payload_inspector", "Shows custom payload traffic so plugin channels and server messages are easier to debug."),
            Map.entry("yggdrasil_signature_fix", "Repairs profile signature edge cases that can break joins or authentication on some servers."),
            Map.entry("auto_totem", "Keeps a totem ready in the offhand by scoring danger, inventory state, and configured replacement rules."),
            Map.entry("attribute_swap", "Chooses stronger equipment when attribute values make one item better than another."),
            Map.entry("kill_aura", "Attacks selected targets automatically with configurable targeting, timing, rotation, and safety checks."),
            Map.entry("backtrack", "Tracks recent target positions and uses delayed hit information to improve combat reach decisions."),
            Map.entry("tick_base", "Shifts queued player actions across ticks for burst movement or combat timing."),
            Map.entry("timer_range", "Adjusts timer speed around nearby targets so timing changes stay tied to combat range."),
            Map.entry("aim_assist", "Smooths view movement toward valid targets without taking complete control away from the player."),
            Map.entry("teams", "Prevents combat modules from targeting players that match configured team checks."),
            Map.entry("auto_weapon", "Selects the best available weapon before attacks using damage, enchantment, and item checks."),
            Map.entry("auto_armor", "Equips stronger armor automatically while respecting delay and replacement settings."),
            Map.entry("auto_pot", "Uses configured potions when health or effect conditions make them useful."),
            Map.entry("projectile_aimbot", "Leads projectile shots toward selected targets with configurable range and aim rules."),
            Map.entry("criticals", "Times attack packets or movement so melee hits can register as critical hits."),
            Map.entry("crystal_aura", "Places and breaks end crystals around targets using damage, range, and self-safety checks."),
            Map.entry("bed_aura", "Uses beds as explosives in valid dimensions with placement, damage, and safety checks."),
            Map.entry("anchor_aura", "Places, charges, and detonates respawn anchors around targets when the configured checks pass."),
            Map.entry("storage_esp", "Highlights storage blocks in the world so chests, shulkers, and similar containers are easier to find."),
            Map.entry("ore_sim", "Estimates likely ore locations from terrain patterns and highlights candidates in the world."),
            Map.entry("xray", "Filters world rendering to make selected blocks easier to inspect through terrain."),
            Map.entry("waypoints", "Renders saved locations with labels and distance cues so routes are easier to follow."),
            Map.entry("no_render", "Suppresses selected visual effects, overlays, and entities that clutter the screen."),
            Map.entry("fullbright", "Raises scene brightness so caves and night areas stay readable without torches."),
            Map.entry("auto_sprint", "Keeps sprinting active when movement conditions allow it."),
            Map.entry("flight", "Provides configurable flight movement for creative-style travel or server-specific modes."),
            Map.entry("packet_fly", "Uses packet movement to handle flight-like movement on servers where normal movement is restricted."),
            Map.entry("freecam", "Detaches the camera from the player so areas can be inspected without moving the body."),
            Map.entry("smart_eat", "Chooses when to eat based on hunger, health, item quality, and combat pressure."),
            Map.entry("fast_exp", "Throws experience bottles for repairs with durability, delay, and safety controls."),
            Map.entry("auto_reconnect", "Reconnects after disconnects using configured delays and notification behavior."),
            Map.entry("sound_blocker", "Cancels selected sounds before they play so noisy effects can be filtered out."),
            Map.entry("packet_logger", "Records packet traffic for debugging protocol behavior and module interactions."),
            Map.entry("macro", "Runs configured chat or command actions from keybind-style triggers."),
            Map.entry("chat_spammer", "Sends configured chat messages on a timer with optional formatting and variation."),
            Map.entry("notebot", "Plays configured note patterns through note blocks or sound actions."),
            Map.entry("discord_presence", "Updates Discord Rich Presence with configurable client and server status."),
            Map.entry("nyan_cat_gif_spammer", "Sends animated Nyan Cat-style chat frames with configurable timing.")
    );

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
            new IconRule("vanish", "eye-off")
    );

    private ModuleMetadata() {
    }

    static String description(final Module module) {
        String explicit = MODULE_DESCRIPTIONS.get(module.id());
        if (explicit != null) {
            return explicit;
        }

        String id = module.id();
        String topic = topic(module.name());
        if (id.endsWith("_hud") || id.contains("_hud_")) {
            return "Shows " + topicWithout(module.name(), "HUD") + " information in a movable HUD element while you play.";
        }
        if (id.contains("esp") || id.contains("xray") || id.contains("wall_hack") || id.contains("chams")) {
            return "Highlights " + topic + " in the world so targets and points of interest are easier to track.";
        }
        if (id.endsWith("_aura") || id.contains("_aura_")) {
            return "Automates repeated " + topicWithout(module.name(), "Aura") + " actions around valid targets with range and safety checks.";
        }
        if (id.startsWith("auto_")) {
            return "Automatically handles " + topicWithout(module.name(), "Auto") + " when the configured conditions match.";
        }
        if (id.startsWith("anti_")) {
            return "Prevents or reduces " + topicWithout(module.name(), "Anti") + " behavior before it interrupts normal play.";
        }
        if (id.startsWith("no_")) {
            return "Suppresses " + topicWithout(module.name(), "No") + " behavior while the module is enabled.";
        }
        if (id.startsWith("better_")) {
            return "Improves " + topicWithout(module.name(), "Better") + " behavior with focused quality-of-life controls.";
        }
        if (id.startsWith("fast_")) {
            return "Reduces delay around " + topicWithout(module.name(), "Fast") + " actions while keeping timing configurable.";
        }
        if (id.contains("finder")) {
            return "Searches for " + topicWithout(module.name(), "Finder") + " and surfaces useful locations while exploring.";
        }
        if (id.contains("notifier") || id.contains("alert")) {
            return "Warns you about " + topic + " events so important server or player changes are easier to notice.";
        }
        if (id.contains("spoof")) {
            return "Adjusts outgoing " + topicWithout(module.name(), "Spoof") + " data for servers that expect different client behavior.";
        }
        if (id.contains("packet")) {
            return "Controls packet-level " + topicWithout(module.name(), "Packet") + " behavior for advanced debugging or bypass workflows.";
        }
        if (id.contains("timer") || id.equals("timer")) {
            return "Adjusts timing around " + topic + " behavior with configurable speed and activation rules.";
        }
        return switch (module.category()) {
            case COMBAT -> "Adjusts combat behavior for " + topic + " with target, timing, and safety settings.";
            case RENDER -> "Changes how " + topic + " is displayed so visual information is easier to read.";
            case MOVEMENT -> "Adjusts movement behavior for " + topic + " while preserving configurable player control.";
            case WORLD -> "Automates or assists world interactions related to " + topic + ".";
            case PLAYER -> "Automates player actions or inventory flow related to " + topic + ".";
            case HUD -> "Shows " + topic + " information in a configurable HUD element.";
            case MISC -> "Adds utility behavior for " + topic + " with settings for when it should run.";
            case FUN -> "Adds a configurable fun utility for " + topic + ".";
        };
    }

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

    static String settingDescription(final Module module, final Setting<?> setting) {
        String explicit = setting.description().strip();
        if (!explicit.isBlank()) {
            return explicit;
        }

        String id = setting.id().toLowerCase(Locale.ROOT);
        String settingName = natural(setting.name());
        String moduleName = module.name();
        if (setting instanceof BooleanSetting) {
            return booleanSettingDescription(moduleName, id, settingName);
        }
        if (setting instanceof NumberSetting) {
            return numberSettingDescription(moduleName, id, settingName);
        }
        if (setting instanceof SelectSetting) {
            return selectSettingDescription(moduleName, id, settingName);
        }
        if (setting instanceof ColorSetting || setting instanceof ColorListSetting) {
            return "Sets the color used by " + moduleName + " for this part of the display.";
        }
        if (setting instanceof BlockPosSetting || setting instanceof Vector3dSetting) {
            return "Sets the world position or vector that " + moduleName + " uses.";
        }
        if (setting instanceof BlockListSetting) {
            return "Lists the blocks that " + moduleName + " should include, ignore, or target.";
        }
        if (setting instanceof ItemListSetting) {
            return "Lists the items that " + moduleName + " should include, ignore, or prefer.";
        }
        if (setting instanceof EntityTypeListSetting) {
            return "Lists the entity types that " + moduleName + " should include or ignore.";
        }
        if (setting instanceof ModuleListSetting) {
            return "Lists other modules that " + moduleName + " should watch or control.";
        }
        if (setting instanceof PacketListSetting) {
            return "Lists packet names that " + moduleName + " should inspect, log, block, or allow.";
        }
        if (setting instanceof ParticleTypeListSetting) {
            return "Lists particle types that " + moduleName + " should include or suppress.";
        }
        if (setting instanceof SoundEventListSetting) {
            return "Lists sounds that " + moduleName + " should include, locate, or block.";
        }
        if (setting instanceof StatusEffectListSetting) {
            return "Lists status effects that " + moduleName + " should include or ignore.";
        }
        if (setting instanceof StringListSetting) {
            return "Lists text entries that " + moduleName + " should match or use.";
        }
        if (setting instanceof TextValueSetting) {
            return "Sets the text value that " + moduleName + " uses for " + settingName + ".";
        }
        return "Controls the " + settingName + " value used by " + moduleName + ".";
    }

    private static String booleanSettingDescription(final String moduleName, final String id, final String settingName) {
        if (id.startsWith("only_")) {
            return "Restricts " + moduleName + " so it only uses " + settingName + " behavior.";
        }
        if (id.startsWith("pause") || id.contains("pause")) {
            return "Pauses " + moduleName + " when " + settingName + " conditions are active.";
        }
        if (id.startsWith("ignore") || id.contains("ignore")) {
            return "Makes " + moduleName + " ignore " + settingName + " while choosing actions.";
        }
        if (id.contains("player") || id.contains("hostile") || id.contains("passive") || id.contains("mob")) {
            return "Allows " + moduleName + " to include " + settingName + " in its checks.";
        }
        if (id.contains("rotate") || id.contains("rotation")) {
            return "Rotates your view before " + moduleName + " performs the related action.";
        }
        if (id.contains("silent")) {
            return "Keeps " + moduleName + " changes quiet or client-side where the module supports it.";
        }
        if (id.contains("restore")) {
            return "Restores the previous player or inventory state after " + moduleName + " finishes.";
        }
        if (id.contains("notify") || id.contains("warn") || id.contains("chat")) {
            return "Shows a message or notification when " + moduleName + " detects a relevant event.";
        }
        if (id.contains("swing")) {
            return "Controls whether " + moduleName + " plays or sends swing actions.";
        }
        return "Toggles whether " + moduleName + " uses " + settingName + " behavior.";
    }

    private static String numberSettingDescription(final String moduleName, final String id, final String settingName) {
        if (id.contains("range") || id.contains("radius") || id.contains("distance")) {
            return "Sets how far " + moduleName + " can scan, render, or act from the player.";
        }
        if (id.contains("delay") || id.contains("cooldown") || id.contains("interval") || id.contains("ticks")) {
            return "Sets the wait time between " + moduleName + " actions.";
        }
        if (id.contains("speed") || id.contains("velocity") || id.contains("strength") || id.contains("boost")) {
            return "Sets the movement or action strength used by " + moduleName + ".";
        }
        if (id.contains("health") || id.contains("durability") || id.contains("threshold") || id.contains("percent")) {
            return "Sets the threshold that decides when " + moduleName + " starts, stops, or switches behavior.";
        }
        if (id.contains("min") || id.contains("max")) {
            return "Sets the " + settingName + " limit used by " + moduleName + ".";
        }
        if (id.contains("count") || id.contains("limit") || id.contains("packets")) {
            return "Limits how many actions " + moduleName + " can perform in one cycle.";
        }
        if (id.contains("pitch") || id.contains("yaw") || id.contains("angle")) {
            return "Sets the view angle used by " + moduleName + ".";
        }
        if (id.contains("opacity") || id.contains("alpha") || id.contains("scale")
                || id.contains("size") || id.contains("width") || id.contains("height")) {
            return "Adjusts the visual size or opacity used by " + moduleName + ".";
        }
        return "Sets the numeric " + settingName + " value used by " + moduleName + ".";
    }

    private static String selectSettingDescription(final String moduleName, final String id, final String settingName) {
        if (id.equals("mode") || id.endsWith("_mode")) {
            return "Chooses the operating mode for " + moduleName + ".";
        }
        if (id.contains("priority") || id.contains("sort")) {
            return "Chooses how " + moduleName + " prioritizes targets or actions.";
        }
        if (id.contains("corner") || id.contains("anchor")) {
            return "Chooses where " + moduleName + " is anchored on the screen.";
        }
        return "Chooses which " + settingName + " option " + moduleName + " uses.";
    }

    private static String topicWithout(final String value, final String word) {
        return topic(value.replace(word, ""));
    }

    private static String topic(final String value) {
        String result = natural(value)
                .replace("HUD", "")
                .replace("ESP", "")
                .strip();
        return result.isBlank() ? natural(value) : lowerFirst(result);
    }

    private static String natural(final String value) {
        return value.replace('_', ' ').strip();
    }

    private static String lowerFirst(final String value) {
        if (value.isBlank()) {
            return value;
        }
        if (value.length() > 1 && Character.isUpperCase(value.charAt(0)) && Character.isUpperCase(value.charAt(1))) {
            return value;
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    private record IconRule(String token, String icon) {
    }
}
