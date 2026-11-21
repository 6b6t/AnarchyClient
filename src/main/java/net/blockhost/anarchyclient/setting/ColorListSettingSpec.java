package net.blockhost.anarchyclient.setting;

import java.util.List;
import org.immutables.value.Value;

@SettingStyle
@Value.Immutable
public interface ColorListSettingSpec {

    String id();

    String name();

    @Value.Default
    default String description() {
        return "";
    }

    List<SettingColor> defaultValue();

    List<String> aliases();
}
