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

    @Value.Default
    default List<MobEffect> defaultValue() {
        return List.of();
    }

    @Value.Default
    default List<String> aliases() {
        return List.of();
    }
}
