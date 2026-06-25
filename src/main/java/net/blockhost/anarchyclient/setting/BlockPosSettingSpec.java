package net.blockhost.anarchyclient.setting;

import net.minecraft.core.BlockPos;
import org.immutables.value.Value;

import java.util.List;

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
