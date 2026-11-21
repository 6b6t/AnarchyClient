package net.blockhost.anarchyclient.setting;

import java.util.List;
import net.minecraft.world.item.Item;
import org.immutables.value.Value;

@SettingStyle
@Value.Immutable
public interface ItemListSettingSpec {

    String id();

    String name();

    @Value.Default
    default String description() {
        return "";
    }

    List<Item> defaultValue();

    List<String> aliases();
}
