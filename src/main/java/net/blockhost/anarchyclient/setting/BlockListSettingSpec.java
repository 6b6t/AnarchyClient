package net.blockhost.anarchyclient.setting;

import java.util.List;
import net.minecraft.world.level.block.Block;
import org.immutables.value.Value;

@SettingStyle
@Value.Immutable
public interface BlockListSettingSpec {

    String id();

    String name();

    @Value.Default
    default String description() {
        return "";
    }

    List<Block> defaultValue();

    List<String> aliases();
}
