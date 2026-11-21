package net.blockhost.anarchyclient.setting;

import java.util.List;
import net.minecraft.sounds.SoundEvent;
import org.immutables.value.Value;

@SettingStyle
@Value.Immutable
public interface SoundEventListSettingSpec {

    String id();

    String name();

    @Value.Default
    default String description() {
        return "";
    }

    List<SoundEvent> defaultValue();

    List<String> aliases();
}
