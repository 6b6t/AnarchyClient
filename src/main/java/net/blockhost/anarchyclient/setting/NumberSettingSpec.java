package net.blockhost.anarchyclient.setting;

import org.immutables.value.Value;

import java.util.List;

@SettingStyle
@Value.Immutable
public interface NumberSettingSpec {

    String id();

    String name();

    @Value.Default
    default String description() {
        return "";
    }

    double defaultValue();

    double min();

    double max();

    double step();

    List<String> aliases();
}
