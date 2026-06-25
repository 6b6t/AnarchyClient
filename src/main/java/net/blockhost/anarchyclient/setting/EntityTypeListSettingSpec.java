package net.blockhost.anarchyclient.setting;

import net.minecraft.world.entity.EntityType;
import org.immutables.value.Value;

import java.util.List;

@SettingStyle
@Value.Immutable
public interface EntityTypeListSettingSpec {

    String id();

    String name();

    @Value.Default
    default String description() {
        return "";
    }

    @Value.Default
    default List<EntityType<?>> defaultValue() {
        return List.of();
    }

    @Value.Default
    default List<String> aliases() {
        return List.of();
    }
}
