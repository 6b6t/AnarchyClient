package net.blockhost.anarchyclient.setting;

import java.util.List;
import org.immutables.value.Value;

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
