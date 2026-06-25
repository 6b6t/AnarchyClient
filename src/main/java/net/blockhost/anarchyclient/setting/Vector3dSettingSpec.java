package net.blockhost.anarchyclient.setting;

import net.minecraft.world.phys.Vec3;
import org.immutables.value.Value;

import java.util.List;

@SettingStyle
@Value.Immutable
public interface Vector3dSettingSpec {

    String id();

    String name();

    @Value.Default
    default String description() {
        return "";
    }

    @Value.Default
    default Vec3 defaultValue() {
        return Vec3.ZERO;
    }

    List<String> aliases();
}
