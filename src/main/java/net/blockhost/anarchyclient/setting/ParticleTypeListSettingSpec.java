package net.blockhost.anarchyclient.setting;

import net.minecraft.core.particles.ParticleType;
import org.immutables.value.Value;

import java.util.List;

@SettingStyle
@Value.Immutable
public interface ParticleTypeListSettingSpec {

    String id();

    String name();

    @Value.Default
    default String description() {
        return "";
    }

    List<ParticleType<?>> defaultValue();

    List<String> aliases();
}
