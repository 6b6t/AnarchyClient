package net.blockhost.anarchyclient.setting;

import java.util.List;
import net.minecraft.world.entity.EntityType;
import org.immutables.value.Value;

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
