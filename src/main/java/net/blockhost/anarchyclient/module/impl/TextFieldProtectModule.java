package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class TextFieldProtectModule extends Module {

    private static TextFieldProtectModule active;

    private final StringSetting patterns = this.setting(StringSetting.from(StringSetting.builder()
            .id("patterns")
            .name("Patterns")
            .defaultValue("login, register, password, passwort, email, mail, changepassword")
            .description("Comma-separated field labels or regex patterns to mask while rendering.")
            .build()));

    public TextFieldProtectModule() {
        super("text_field_protect", "Text Field Protect", ModuleCategory.MISC);
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

    public static boolean shouldMask(final Component label, final String value) {
        TextFieldProtectModule module = active;
        if (module == null || value == null || value.isEmpty()) {
            return false;
        }
        String haystack = ((label == null ? "" : label.getString()) + " " + value).toLowerCase(Locale.ROOT);
        for (Pattern pattern : module.compiledPatterns()) {
            if (pattern.matcher(haystack).find()) {
                return true;
            }
        }
        return false;
    }

    public static String mask(final String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return "*".repeat(value.length());
    }

    private List<Pattern> compiledPatterns() {
        List<Pattern> result = new ArrayList<>();
        String value = this.patterns.value();
        if (value == null || value.isBlank()) {
            return List.of();
        }
        for (String token : value.split("[,;|]")) {
            String pattern = token.trim();
            if (pattern.isEmpty()) {
                continue;
            }
            try {
                result.add(Pattern.compile(pattern.toLowerCase(Locale.ROOT), Pattern.CASE_INSENSITIVE));
            } catch (PatternSyntaxException ignored) {
                result.add(Pattern.compile(Pattern.quote(pattern.toLowerCase(Locale.ROOT)), Pattern.CASE_INSENSITIVE));
            }
        }
        return List.copyOf(result);
    }
}
