package net.blockhost.anarchyclient.setting;

import org.immutables.value.Value;

import java.util.List;

@SettingStyle
@Value.Immutable
public interface PacketListSettingSpec {

    String id();

    String name();

    @Value.Default
    default String description() {
        return "";
    }

    List<String> defaultValue();

    List<String> aliases();

    List<String> suggestions();
}
