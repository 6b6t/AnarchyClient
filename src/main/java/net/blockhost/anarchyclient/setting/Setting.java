package net.blockhost.anarchyclient.setting;

import com.google.gson.JsonElement;
import java.util.List;
import java.util.function.Supplier;
import net.blockhost.anarchyclient.i18n.Translations;
import org.jspecify.annotations.Nullable;

public abstract class Setting<T> {

    private final String id;
    private final String name;
    private final String description;
    private final List<String> aliases;
    private final T defaultValue;
    private T value;
    private @Nullable Supplier<Boolean> visibilityCondition;
    private @Nullable String translationKey;

    protected Setting(final String id, final String name, final T defaultValue) {
        this(id, name, "", defaultValue, List.of());
    }

    protected Setting(final String id, final String name, final T defaultValue, final List<String> aliases) {
        this(id, name, "", defaultValue, aliases);
    }

    protected Setting(
            final String id,
            final String name,
            final String description,
            final T defaultValue,
            final List<String> aliases) {
        this.id = id;
        this.name = name;
        this.description = description == null ? "" : description;
        this.aliases = List.copyOf(aliases);
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public final String id() {
        return this.id;
    }

    /** The setting's name in the selected language; English when it has no translation. */
    public final String name() {
        return this.translationKey == null ? this.name : Translations.text(this.translationKey, this.name);
    }

    /** The English name, as written in code. */
    public final String englishName() {
        return this.name;
    }

    public final String description() {
        if (this.translationKey == null || this.description.isEmpty()) {
            return this.description;
        }
        return Translations.text(this.translationKey + ".description", this.description);
    }

    /** The English description, as written in code. */
    public final String englishDescription() {
        return this.description;
    }

    /**
     * Sets the key this setting's name is translated under ({@code setting.anarchyclient.<module>.<setting>}). The
     * owning module calls it when the setting is registered.
     */
    public final void translationKey(final String key) {
        this.translationKey = key;
    }

    /** The translation key, or {@code null} for a setting that belongs to no module. */
    public final @Nullable String translationKey() {
        return this.translationKey;
    }

    public final List<String> aliases() {
        return this.aliases;
    }

    public final T defaultValue() {
        return this.defaultValue;
    }

    public final T value() {
        return this.value;
    }

    public final void value(final T value) {
        this.value = this.sanitize(value);
    }

    public final boolean visible() {
        return this.visibilityCondition == null || this.visibilityCondition.get();
    }

    public final void visibleWhen(final Supplier<Boolean> condition) {
        this.visibilityCondition = condition;
    }

    protected T sanitize(final T value) {
        return value;
    }

    public abstract JsonElement toJson();

    public abstract void fromJson(final @Nullable JsonElement element);
}
