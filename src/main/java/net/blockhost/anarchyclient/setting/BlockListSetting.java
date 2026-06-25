package net.blockhost.anarchyclient.setting;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;

public final class BlockListSetting extends RegistryListSetting<Block> {

    private BlockListSetting(final BlockListSettingSpec spec) {
        super(spec.id(), spec.name(), spec.description(), spec.defaultValue(), spec.aliases(),
                BuiltInRegistries.BLOCK::getOptional,
                BuiltInRegistries.BLOCK::getKey,
                new ArrayList<>(BuiltInRegistries.BLOCK.keySet()));
    }

    public static ImmutableBlockListSettingSpec.IdBuildStage builder() {
        return ImmutableBlockListSettingSpec.builder();
    }

    public static BlockListSetting from(final BlockListSettingSpec spec) {
        return new BlockListSetting(spec);
    }
}
