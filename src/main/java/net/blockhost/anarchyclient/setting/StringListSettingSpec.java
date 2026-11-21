package net.blockhost.anarchyclient.setting;

import java.util.List;
import org.immutables.value.Value;

@SettingStyle
@Value.Immutable
public interface StringListSettingSpec {

    String id();

    String name();

    @Value.Default
    default String description() {
        return "";
    }

    List<String> defaultValue();

    List<String> aliases();

    List<String> suggestions();

    /**
     * Whether whitespace also separates typed entries. Use it for identifier lists, whose entries
     * never contain spaces; free text lists keep it off so entries may hold spaces.
     */
    @Value.Default
    default boolean splitOnWhitespace() {
        return false;
    }
}
