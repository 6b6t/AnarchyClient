package net.blockhost.anarchyclient.setting;

import java.util.List;
import org.immutables.value.Value;

@SettingStyle
@Value.Immutable
public interface ModuleListSettingSpec {

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
