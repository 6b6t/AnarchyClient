package net.blockhost.anarchyclient.setting;

import org.immutables.value.Value;

import java.util.List;

@SettingStyle
@Value.Immutable
public interface ColorSettingSpec {

    String id();

    String name();

    @Value.Default
    default String description() {
        return "";
    }

    @Value.Default
    default SettingColor defaultValue() {
        return SettingColor.rgb(255, 255, 255);
    }

    @Value.Default
    default List<String> aliases() {
        return List.of();
    }
}
