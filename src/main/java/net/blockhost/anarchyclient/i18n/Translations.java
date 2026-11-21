package net.blockhost.anarchyclient.i18n;

import java.util.IllegalFormatException;
import java.util.Locale;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Looks up client text in the language selected in Minecraft's language menu.
 *
 * <p>Every lookup carries its English text as the fallback, so a missing translation (or no translation file at
 * all, as in tests) shows English. The English files under {@code assets/anarchyclient/lang} list every key;
 * {@code TranslationKeysTest} keeps them in sync with the code.
 */
public final class Translations {

    private Translations() {}

    /** The translation of {@code key}, or {@code fallback} when the selected language and English both lack it. */
    public static String text(final String key, final String fallback) {
        Language language = Language.getInstance();
        return language.has(key) ? language.getOrDefault(key, fallback) : fallback;
    }

    /**
     * Like {@link #text}, with {@code %s} placeholders filled in. A translation whose placeholders do not match the
     * arguments falls back to English rather than throwing.
     */
    public static String format(final String key, final String fallback, final Object... args) {
        try {
            return String.format(Locale.ROOT, text(key, fallback), args);
        } catch (IllegalFormatException exception) {
            return String.format(Locale.ROOT, fallback, args);
        }
    }

    /** A chat component that is translated when it is shown, so it follows a language change. */
    public static MutableComponent component(final String key, final String fallback, final Object... args) {
        return Component.translatableWithFallback(key, fallback, args);
    }

    /** Whether the selected language is written right to left (Arabic, Persian, Hebrew). */
    public static boolean rightToLeft() {
        return Language.getInstance().isDefaultRightToLeft();
    }

    /** Turns a free-form value such as a select option into a key segment: lower case, underscores. */
    public static String keySegment(final String value) {
        String slug = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
        slug = slug.replaceAll("^_+|_+$", "");
        return slug.isEmpty() ? "_" : slug;
    }
}
