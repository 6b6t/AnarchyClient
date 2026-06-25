package net.blockhost.anarchyclient.setting;

import org.immutables.value.Value;

import java.util.List;

@SettingStyle
@Value.Immutable
public interface SelectSettingSpec {

    String id();

    String name();

    @Value.Default
    default String description() {
        return "";
    }

    String defaultValue();

    List<String> options();

    List<String> aliases();
}
