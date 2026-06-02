package net.blockhost.anarchyclient.setting;

import org.immutables.value.Value;

@SettingStyle
@Value.Immutable
public interface BooleanSettingSpec {

    String id();

    String name();

    boolean defaultValue();
}
