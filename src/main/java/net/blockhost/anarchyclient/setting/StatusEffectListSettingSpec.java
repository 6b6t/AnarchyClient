package net.blockhost.anarchyclient.setting;

import net.minecraft.world.effect.MobEffect;
import org.immutables.value.Value;

import java.util.List;

@SettingStyle
@Value.Immutable
public interface StatusEffectListSettingSpec {

    String id();

    String name();

    @Value.Default
    default String description() {
        return "";
    }

    List<MobEffect> defaultValue();

    List<String> aliases();
}
