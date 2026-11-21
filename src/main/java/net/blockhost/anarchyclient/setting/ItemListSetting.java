package net.blockhost.anarchyclient.setting;

import java.util.ArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

public final class ItemListSetting extends RegistryListSetting<Item> {

    private ItemListSetting(final ItemListSettingSpec spec) {
        super(
                spec.id(),
                spec.name(),
                spec.description(),
                spec.defaultValue(),
                spec.aliases(),
                BuiltInRegistries.ITEM::getOptional,
                BuiltInRegistries.ITEM::getKey,
                new ArrayList<>(BuiltInRegistries.ITEM.keySet()));
    }

    public static ImmutableItemListSettingSpec.IdBuildStage builder() {
        return ImmutableItemListSettingSpec.builder();
    }

    public static ItemListSetting from(final ItemListSettingSpec spec) {
        return new ItemListSetting(spec);
    }
}
