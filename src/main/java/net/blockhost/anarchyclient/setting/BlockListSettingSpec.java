package net.blockhost.anarchyclient.setting;

import net.minecraft.world.level.block.Block;
import org.immutables.value.Value;

import java.util.List;

@SettingStyle
@Value.Immutable
public interface BlockListSettingSpec {

    String id();

    String name();

    @Value.Default
    default String description() {
        return "";
    }

    @Value.Default
    default List<Block> defaultValue() {
        return List.of();
    }

    @Value.Default
    default List<String> aliases() {
        return List.of();
    }
}
