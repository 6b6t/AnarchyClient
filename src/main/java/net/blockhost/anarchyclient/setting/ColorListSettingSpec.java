package net.blockhost.anarchyclient.setting;

import org.immutables.value.Value;

import java.util.List;

@SettingStyle
@Value.Immutable
public interface ColorListSettingSpec {

    String id();

    String name();

    @Value.Default
    default String description() {
        return "";
    }

    @Value.Default
    default List<SettingColor> defaultValue() {
        return List.of();
    }

    @Value.Default
    default List<String> aliases() {
        return List.of();
    }
}
