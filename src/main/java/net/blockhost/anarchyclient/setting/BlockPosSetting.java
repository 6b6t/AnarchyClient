package net.blockhost.anarchyclient.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.core.BlockPos;

public final class BlockPosSetting extends Setting<BlockPos> implements TextValueSetting {

    private BlockPosSetting(final BlockPosSettingSpec spec) {
        super(spec.id(), spec.name(), spec.description(), spec.defaultValue(), spec.aliases());
    }

    public static ImmutableBlockPosSettingSpec.IdBuildStage builder() {
        return ImmutableBlockPosSettingSpec.builder();
    }

    public static BlockPosSetting from(final BlockPosSettingSpec spec) {
        return new BlockPosSetting(spec);
    }

    @Override
    public String valueString() {
        BlockPos value = this.value();
        return value.getX() + "," + value.getY() + "," + value.getZ();
    }

    @Override
    public void valueFromString(final String value) {
        this.value(parse(value));
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(this.valueString());
    }

    @Override
    public void fromJson(final JsonElement element) {
        if (element == null) {
            return;
        }
        if (element.isJsonPrimitive()) {
            this.valueFromString(element.getAsString());
            return;
        }
        if (!element.isJsonObject()) {
            return;
        }
        JsonObject object = element.getAsJsonObject();
        if (object.has("x") && object.has("y") && object.has("z")) {
            this.value(new BlockPos(object.get("x").getAsInt(), object.get("y").getAsInt(), object.get("z").getAsInt()));
        }
    }

    static BlockPos parse(final String value) {
        String[] parts = value == null ? new String[0] : value.trim().split("[,;\\s]+");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Expected x,y,z.");
        }
        try {
            return new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Expected integer block coordinates.", exception);
        }
    }
}
