package net.blockhost.anarchyclient.setting;

import org.immutables.value.Value;

@SettingStyle
@Value.Immutable
public interface StringSettingSpec {

    String id();

    String name();

    String defaultValue();
}
