package net.blockhost.anarchyclient.setting;

import java.util.List;
import org.immutables.value.Value;

@SettingStyle
@Value.Immutable
public interface BooleanSettingSpec {

    String id();

    String name();

    @Value.Default
    default String description() {
        return "";
    }

    boolean defaultValue();

    List<String> aliases();
}
