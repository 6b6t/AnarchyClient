package net.blockhost.anarchyclient.setting;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;

import java.util.ArrayList;

public final class StatusEffectListSetting extends RegistryListSetting<MobEffect> {

    private StatusEffectListSetting(final StatusEffectListSettingSpec spec) {
        super(spec.id(), spec.name(), spec.description(), spec.defaultValue(), spec.aliases(),
                BuiltInRegistries.MOB_EFFECT::getOptional,
                BuiltInRegistries.MOB_EFFECT::getKey,
                new ArrayList<>(BuiltInRegistries.MOB_EFFECT.keySet()));
    }

    public static ImmutableStatusEffectListSettingSpec.IdBuildStage builder() {
        return ImmutableStatusEffectListSettingSpec.builder();
    }

    public static StatusEffectListSetting from(final StatusEffectListSettingSpec spec) {
        return new StatusEffectListSetting(spec);
    }
}
