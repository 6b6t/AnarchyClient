package net.blockhost.anarchyclient.setting;

import org.immutables.value.Value;

import java.util.List;

@SettingStyle
@Value.Immutable
public interface BooleanSettingSpec {

    String id();

    String name();

    @Value.Default
    default String description() {
        return "";
    }

    boolean defaultValue();

    @Value.Default
    default List<String> aliases() {
        return List.of();
    }
}
