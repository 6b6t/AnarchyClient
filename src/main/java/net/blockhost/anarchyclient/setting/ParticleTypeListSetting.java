package net.blockhost.anarchyclient.setting;

import java.util.ArrayList;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

public final class ParticleTypeListSetting extends RegistryListSetting<ParticleType<?>> {

    private ParticleTypeListSetting(final ParticleTypeListSettingSpec spec) {
        super(
                spec.id(),
                spec.name(),
                spec.description(),
                spec.defaultValue(),
                spec.aliases(),
                BuiltInRegistries.PARTICLE_TYPE::getOptional,
                BuiltInRegistries.PARTICLE_TYPE::getKey,
                new ArrayList<>(BuiltInRegistries.PARTICLE_TYPE.keySet()));
    }

    public static ImmutableParticleTypeListSettingSpec.IdBuildStage builder() {
        return ImmutableParticleTypeListSettingSpec.builder();
    }

    public static ParticleTypeListSetting from(final ParticleTypeListSettingSpec spec) {
        return new ParticleTypeListSetting(spec);
    }
}
