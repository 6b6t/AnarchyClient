package net.blockhost.anarchyclient.setting;

import java.util.List;
import net.minecraft.core.BlockPos;
import org.immutables.value.Value;

@SettingStyle
@Value.Immutable
public interface BlockPosSettingSpec {

    String id();

    String name();

    @Value.Default
    default String description() {
        return "";
    }

    @Value.Default
    default BlockPos defaultValue() {
        return BlockPos.ZERO;
    }

    List<String> aliases();
}
