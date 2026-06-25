package net.blockhost.anarchyclient.setting;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

import java.util.ArrayList;

public final class SoundEventListSetting extends RegistryListSetting<SoundEvent> {

    private SoundEventListSetting(final SoundEventListSettingSpec spec) {
        super(spec.id(), spec.name(), spec.description(), spec.defaultValue(), spec.aliases(),
                BuiltInRegistries.SOUND_EVENT::getOptional,
                BuiltInRegistries.SOUND_EVENT::getKey,
                new ArrayList<>(BuiltInRegistries.SOUND_EVENT.keySet()));
    }

    public static ImmutableSoundEventListSettingSpec.IdBuildStage builder() {
        return ImmutableSoundEventListSettingSpec.builder();
    }

    public static SoundEventListSetting from(final SoundEventListSettingSpec spec) {
        return new SoundEventListSetting(spec);
    }
}
