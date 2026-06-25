package net.blockhost.anarchyclient.setting;

import net.minecraft.sounds.SoundEvent;
import org.immutables.value.Value;

import java.util.List;

@SettingStyle
@Value.Immutable
public interface SoundEventListSettingSpec {

    String id();

    String name();

    @Value.Default
    default String description() {
        return "";
    }

    @Value.Default
    default List<SoundEvent> defaultValue() {
        return List.of();
    }

    @Value.Default
    default List<String> aliases() {
        return List.of();
    }
}
