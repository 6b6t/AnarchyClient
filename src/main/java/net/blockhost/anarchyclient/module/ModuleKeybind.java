package net.blockhost.anarchyclient.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.lwjgl.glfw.GLFW;

public final class ModuleKeybind {

    private int key;
    private ModuleBindAction action;

    public ModuleKeybind() {
        this(GLFW.GLFW_KEY_UNKNOWN, ModuleBindAction.TOGGLE);
    }

    public ModuleKeybind(final int key, final ModuleBindAction action) {
        this.key = key;
        this.action = action == null ? ModuleBindAction.TOGGLE : action;
    }

    public static ModuleKeybind unbound() {
        return new ModuleKeybind();
    }

    public boolean bound() {
        return this.key != GLFW.GLFW_KEY_UNKNOWN;
    }

    public int key() {
        return this.key;
    }

    public void key(final int key) {
        this.key = key;
    }

    public ModuleBindAction action() {
        return this.action;
    }

    public void action(final ModuleBindAction action) {
        this.action = action == null ? ModuleBindAction.TOGGLE : action;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("key", this.key);
        json.addProperty("action", this.action.name().toLowerCase(java.util.Locale.ROOT));
        return json;
    }

    public void fromJson(final JsonObject json) {
        if (json == null) {
            return;
        }
        JsonElement keyJson = json.get("key");
        if (keyJson != null && keyJson.isJsonPrimitive()) {
            this.key = keyJson.getAsInt();
        }
        JsonElement actionJson = json.get("action");
        if (actionJson != null && actionJson.isJsonPrimitive()) {
            this.action = ModuleBindAction.parse(actionJson.getAsString());
        }
    }
}
