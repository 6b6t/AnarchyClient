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

    List<EntityType<?>> defaultValue();

    List<String> aliases();
}
