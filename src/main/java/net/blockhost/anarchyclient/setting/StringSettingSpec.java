package net.blockhost.anarchyclient.setting;

import org.immutables.value.Value;

import java.util.List;

@SettingStyle
@Value.Immutable
public interface StringSettingSpec {

    String id();

    String name();

    @Value.Default
    default String description() {
        return "";
    }

    String defaultValue();

    @Value.Default
    default List<String> aliases() {
        return List.of();
    }
}
