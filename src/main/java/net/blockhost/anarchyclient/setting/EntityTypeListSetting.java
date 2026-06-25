package net.blockhost.anarchyclient.setting;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;

public final class EntityTypeListSetting extends RegistryListSetting<EntityType<?>> {

    private EntityTypeListSetting(final EntityTypeListSettingSpec spec) {
        super(spec.id(), spec.name(), spec.description(), spec.defaultValue(), spec.aliases(),
                BuiltInRegistries.ENTITY_TYPE::getOptional,
                BuiltInRegistries.ENTITY_TYPE::getKey,
                new ArrayList<>(BuiltInRegistries.ENTITY_TYPE.keySet()));
    }

    public static ImmutableEntityTypeListSettingSpec.IdBuildStage builder() {
        return ImmutableEntityTypeListSettingSpec.builder();
    }

    public static EntityTypeListSetting from(final EntityTypeListSettingSpec spec) {
        return new EntityTypeListSetting(spec);
    }
}
