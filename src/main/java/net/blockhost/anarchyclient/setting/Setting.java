package net.blockhost.anarchyclient.setting;

import com.google.gson.JsonElement;

import java.util.List;

public abstract class Setting<T> {

    private final String id;
    private final String name;
    private final List<String> aliases;
    private final T defaultValue;
    private T value;

    protected Setting(final String id, final String name, final T defaultValue) {
        this(id, name, defaultValue, List.of());
    }

    protected Setting(final String id, final String name, final T defaultValue, final List<String> aliases) {
        this.id = id;
        this.name = name;
        this.aliases = List.copyOf(aliases);
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public final String id() {
        return this.id;
    }

    public final String name() {
        return this.name;
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

    protected T sanitize(final T value) {
        return value;
    }

    public abstract JsonElement toJson();

    public abstract void fromJson(final JsonElement element);
}
