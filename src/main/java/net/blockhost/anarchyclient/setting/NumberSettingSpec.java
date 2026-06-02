package net.blockhost.anarchyclient.setting;

import org.immutables.value.Value;

@SettingStyle
@Value.Immutable
public interface NumberSettingSpec {

    String id();

    String name();

    double defaultValue();

    double min();

    double max();

    double step();
}
