package net.blockhost.anarchyclient.setting;

import java.util.List;
import org.immutables.value.Value;

@SettingStyle
@Value.Immutable
public interface StringSettingSpec {

    String id();

    String name();

    @Value.Default
    default String description() {
        return "";
    }

    String defaultValue();

    List<String> aliases();
}
