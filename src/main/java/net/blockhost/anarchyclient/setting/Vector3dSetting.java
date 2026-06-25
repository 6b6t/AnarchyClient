package net.blockhost.anarchyclient.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.world.phys.Vec3;

public final class Vector3dSetting extends Setting<Vec3> implements TextValueSetting {

    private Vector3dSetting(final Vector3dSettingSpec spec) {
        super(spec.id(), spec.name(), spec.description(), spec.defaultValue(), spec.aliases());
    }

    public static ImmutableVector3dSettingSpec.IdBuildStage builder() {
        return ImmutableVector3dSettingSpec.builder();
    }

    public static Vector3dSetting from(final Vector3dSettingSpec spec) {
        return new Vector3dSetting(spec);
    }

    @Override
    public String valueString() {
        Vec3 value = this.value();
        return value.x + "," + value.y + "," + value.z;
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
            this.value(new Vec3(object.get("x").getAsDouble(), object.get("y").getAsDouble(), object.get("z").getAsDouble()));
        }
    }

    static Vec3 parse(final String value) {
        String[] parts = value == null ? new String[0] : value.trim().split("[,;\\s]+");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Expected x,y,z.");
        }
        try {
            return new Vec3(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Expected numeric coordinates.", exception);
        }
    }
}
