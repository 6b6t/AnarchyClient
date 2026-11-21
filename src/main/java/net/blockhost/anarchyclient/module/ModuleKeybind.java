package net.blockhost.anarchyclient.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.Locale;
import org.lwjgl.sdl.SDLScancode;

public final class ModuleKeybind {

    private int key;
    private ModuleBindAction action;

    public ModuleKeybind() {
        this(InputConstants.UNKNOWN.getValue(), ModuleBindAction.TOGGLE);
    }

    public ModuleKeybind(final int key, final ModuleBindAction action) {
        this.key = key;
        this.action = action == null ? ModuleBindAction.TOGGLE : action;
    }

    public static ModuleKeybind unbound() {
        return new ModuleKeybind();
    }

    public boolean bound() {
        return this.key > InputConstants.UNKNOWN.getValue() && this.key < SDLScancode.SDL_SCANCODE_COUNT;
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
        json.addProperty("key_format", "sdl_scancode");
        json.addProperty("action", this.action.name().toLowerCase(Locale.ROOT));
        return json;
    }

    public void fromJson(final JsonObject json) {
        if (json == null) {
            return;
        }
        JsonElement keyJson = json.get("key");
        if (keyJson != null && keyJson.isJsonPrimitive()) {
            int savedKey = keyJson.getAsInt();
            this.key = json.has("key_format")
                            && "sdl_scancode".equals(json.get("key_format").getAsString())
                    ? savedKey
                    : LegacyKeyCodes.toScancode(savedKey);
        }
        JsonElement actionJson = json.get("action");
        if (actionJson != null && actionJson.isJsonPrimitive()) {
            this.action = ModuleBindAction.parse(actionJson.getAsString());
        }
    }
}
