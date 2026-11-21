package net.blockhost.anarchyclient.module.impl;

import java.util.Locale;
import java.util.Map;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;

public final class AntiCyrillicModule extends Module {

    private static final Map<Character, String> TRANSLITERATION = Map.ofEntries(
            Map.entry('а', "a"),
            Map.entry('б', "b"),
            Map.entry('в', "v"),
            Map.entry('г', "g"),
            Map.entry('ґ', "g"),
            Map.entry('д', "d"),
            Map.entry('е', "e"),
            Map.entry('ё', "yo"),
            Map.entry('є', "ye"),
            Map.entry('ж', "zh"),
            Map.entry('з', "z"),
            Map.entry('и', "i"),
            Map.entry('і', "i"),
            Map.entry('ї', "yi"),
            Map.entry('й', "y"),
            Map.entry('к', "k"),
            Map.entry('л', "l"),
            Map.entry('м', "m"),
            Map.entry('н', "n"),
            Map.entry('о', "o"),
            Map.entry('п', "p"),
            Map.entry('р', "r"),
            Map.entry('с', "s"),
            Map.entry('т', "t"),
            Map.entry('у', "u"),
            Map.entry('ў', "w"),
            Map.entry('ф', "f"),
            Map.entry('х', "kh"),
            Map.entry('ц', "ts"),
            Map.entry('ч', "ch"),
            Map.entry('ш', "sh"),
            Map.entry('щ', "shch"),
            Map.entry('ъ', ""),
            Map.entry('ы', "y"),
            Map.entry('ь', ""),
            Map.entry('э', "e"),
            Map.entry('ю', "yu"),
            Map.entry('я', "ya"));

    private final BooleanSetting commands = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("commands")
            .name("Commands")
            .defaultValue(false)
            .description("Also converts the text of commands such as /msg.")
            .build()));

    public AntiCyrillicModule() {
        super(
                "anti_cyrillic",
                "Anti Cyrillic",
                ModuleCategory.MISC,
                "Converts Cyrillic letters in the chat messages you send into Latin letters, for servers that block them.");
    }

    @Override
    public String sendChatMessage(final Minecraft client, final String message, final boolean command) {
        if (command && !this.commands.value()) {
            return message;
        }
        return latinize(message);
    }

    /** Transliterates Cyrillic letters and keeps all other characters. */
    static String latinize(final String message) {
        StringBuilder result = null;
        for (int index = 0; index < message.length(); index++) {
            char character = message.charAt(index);
            String replacement = TRANSLITERATION.get(Character.toLowerCase(character));
            if (replacement == null) {
                if (result != null) {
                    result.append(character);
                }
                continue;
            }
            if (result == null) {
                result = new StringBuilder(message.length() + 8).append(message, 0, index);
            }
            if (Character.isUpperCase(character) && !replacement.isEmpty()) {
                boolean nextUpper = index + 1 < message.length() && Character.isUpperCase(message.charAt(index + 1));
                result.append(
                        nextUpper
                                ? replacement.toUpperCase(Locale.ROOT)
                                : Character.toUpperCase(replacement.charAt(0)) + replacement.substring(1));
            } else {
                result.append(replacement);
            }
        }
        return result == null ? message : result.toString();
    }
}
