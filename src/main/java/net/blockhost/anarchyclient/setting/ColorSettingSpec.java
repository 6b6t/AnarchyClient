package net.blockhost.anarchyclient.setting;

import java.util.List;
import org.immutables.value.Value;

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

    List<String> aliases();
}
