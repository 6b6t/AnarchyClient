package net.blockhost.anarchyclient.module.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.StringListSetting;
import net.minecraft.network.chat.Component;

public final class ChatFilterModule extends Module {

    private static volatile ChatFilterModule active;

    private final StringListSetting filters = this.setting(StringListSetting.from(StringListSetting.builder()
            .id("filters")
            .name("Filters")
            .description("Hides chat lines that contain any of these entries. Case does not matter.")
            .addAllDefaultValue(List.of("discord.gg/"))
            .build()));
    private final BooleanSetting regex = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("regex")
            .name("Regex")
            .defaultValue(false)
            .description("Treats each filter as a regular expression instead of plain text.")
            .build()));
    private final BooleanSetting invert = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("invert")
            .name("Invert")
            .defaultValue(false)
            .description("Shows only the chat lines that match a filter.")
            .build()));
    private List<String> compiledSource = List.of();
    private boolean compiledRegex;
    private List<Pattern> compiled = List.of();

    public ChatFilterModule() {
        super(
                "chat_filter",
                "Chat Filter",
                ModuleCategory.MISC,
                "Hides chat messages that contain your filter words or match your regular expressions.");
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

    /** Returns whether the enabled Chat Filter hides {@code message}. Called from the chat mixin. */
    public static boolean hides(final Component message) {
        ChatFilterModule module = active;
        return module != null && message != null && module.shouldHide(message.getString());
    }

    synchronized boolean shouldHide(final String text) {
        List<String> source = this.filters.value();
        boolean useRegex = this.regex.value();
        if (!source.equals(this.compiledSource) || useRegex != this.compiledRegex) {
            this.compiled = compile(source, useRegex);
            this.compiledSource = List.copyOf(source);
            this.compiledRegex = useRegex;
        }
        if (this.compiled.isEmpty()) {
            return false;
        }
        return matches(this.compiled, text) != this.invert.value();
    }

    static List<Pattern> compile(final List<String> filters, final boolean regex) {
        List<Pattern> patterns = new ArrayList<>();
        for (String filter : filters) {
            if (filter == null || filter.isBlank()) {
                continue;
            }
            try {
                patterns.add(Pattern.compile(
                        regex ? filter : Pattern.quote(filter.toLowerCase(Locale.ROOT)),
                        regex ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : 0));
            } catch (PatternSyntaxException ignored) {
                // An invalid expression filters nothing; the other entries still apply.
            }
        }
        return List.copyOf(patterns);
    }

    static boolean matches(final List<Pattern> patterns, final String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        for (Pattern pattern : patterns) {
            String input = pattern.flags() == 0 ? lower : text;
            if (pattern.matcher(input).find()) {
                return true;
            }
        }
        return false;
    }
}
