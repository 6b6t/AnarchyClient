package net.blockhost.anarchyclient.setting;

import org.immutables.value.Value;

import java.util.List;

@SettingStyle
@Value.Immutable
public interface SelectSettingSpec {

    String id();

    String name();

    String defaultValue();

    List<String> options();

    @Value.Default
    default List<String> aliases() {
        return List.of();
    }
}
