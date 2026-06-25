package net.blockhost.anarchyclient.setting;

import org.immutables.value.Value;

import java.util.List;

@SettingStyle
@Value.Immutable
public interface ModuleListSettingSpec {

    String id();

    String name();

    @Value.Default
    default String description() {
        return "";
    }

    @Value.Default
    default List<String> defaultValue() {
        return List.of();
    }

    @Value.Default
    default List<String> aliases() {
        return List.of();
    }

    @Value.Default
    default List<String> suggestions() {
        return List.of();
    }
}
