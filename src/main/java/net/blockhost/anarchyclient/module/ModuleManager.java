package net.blockhost.anarchyclient.module;

import net.blockhost.anarchyclient.event.AnarchyEventBus;
import net.blockhost.anarchyclient.event.AnarchyClientEvent;
import net.blockhost.anarchyclient.event.CameraTransformEvent;
import net.blockhost.anarchyclient.event.ChatMessageEvent;
import net.blockhost.anarchyclient.event.ClientInputEvent;
import net.blockhost.anarchyclient.event.ClientTickEvent;
import net.blockhost.anarchyclient.event.FovEvent;
import net.blockhost.anarchyclient.event.HudRenderEvent;
import net.blockhost.anarchyclient.event.SendChatEvent;
import net.blockhost.anarchyclient.event.TabPlayerNameEvent;
import net.blockhost.anarchyclient.event.PreventEdgeFallEvent;
import net.blockhost.anarchyclient.event.WorldRenderEvent;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.ClientInput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ModuleManager {

    private final AnarchyEventBus events = new AnarchyEventBus();
    private final Map<String, Module> modules = new LinkedHashMap<>();
    private final Map<Module, ModuleEventListener> eventListeners = new IdentityHashMap<>();
    private final ModuleKeybindController keybindController = new ModuleKeybindController();

    public void register(final Module module) {
        Module previous = this.modules.putIfAbsent(module.id(), module);
        if (previous != null) {
            throw new IllegalArgumentException("Duplicate module id: " + module.id());
        }
        module.activationListener(this::onActivationChanged);
        if (module.enabled()) {
            this.registerEvents(module);
        }
    }

    public Optional<Module> find(final String id) {
        return Optional.ofNullable(this.modules.get(id));
    }

    public Collection<Module> all() {
        return List.copyOf(this.modules.values());
    }

    public List<Module> byCategory(final ModuleCategory category) {
        List<Module> result = new ArrayList<>();
        for (Module module : this.modules.values()) {
            if (module.category() == category) {
                result.add(module);
            }
        }
        return result;
    }

    public <T extends AnarchyClientEvent> T call(final T event) {
        return this.events.call(event);
    }

    public void tick(final Minecraft client) {
        this.call(new ClientTickEvent(client));
    }

    public void handleKeybinds(final Minecraft client) {
        boolean allowNewPresses = client != null && client.gui.screen() == null;
        for (Module module : this.modules.values()) {
            ModuleKeybind keybind = module.keybind();
            if (!keybind.bound()) {
                this.keybindController.clear(module);
                continue;
            }
            this.keybindController.update(module, keyPressed(client, keybind.key()), allowNewPresses);
        }
    }

    public void updateInput(final Minecraft client, final ClientInput input) {
        this.call(new ClientInputEvent(client, input));
    }

    public boolean preventEdgeFall(final Minecraft client, final Player player) {
        return this.call(new PreventEdgeFallEvent(client, player)).isCancelled();
    }

    public Component chatMessage(final Minecraft client, final Component message) {
        return this.call(new ChatMessageEvent(client, message)).message();
    }

    public String sendChatMessage(final Minecraft client, final String message, final boolean command) {
        return this.call(new SendChatEvent(client, message, command)).message();
    }

    public Component tabPlayerName(final Minecraft client, final PlayerInfo playerInfo, final Component name) {
        return this.call(new TabPlayerNameEvent(client, playerInfo, name)).name();
    }

    public float fov(final Minecraft client, final float fov) {
        return this.call(new FovEvent(client, fov)).fov();
    }

    public CameraTransformEvent cameraTransform(final Minecraft client, final Vec3 position, final float yaw,
                                                final float pitch) {
        return this.call(new CameraTransformEvent(client, position, yaw, pitch));
    }

    public void renderWorld(final LevelRenderContext context) {
        this.call(new WorldRenderEvent(context));
    }

    public void renderHud(final Minecraft client, final GuiGraphicsExtractor graphics) {
        this.call(new HudRenderEvent(client, graphics));
    }

    private void onActivationChanged(final Module module, final boolean enabled) {
        if (enabled) {
            this.registerEvents(module);
        } else {
            this.unregisterEvents(module);
        }
    }

    private void registerEvents(final Module module) {
        if (!this.eventListeners.containsKey(module)) {
            ModuleEventListener listener = new ModuleEventListener(module);
            this.eventListeners.put(module, listener);
            this.events.register(listener);
        }
    }

    private void unregisterEvents(final Module module) {
        ModuleEventListener listener = this.eventListeners.remove(module);
        if (listener != null) {
            this.events.unregister(listener);
        }
    }

    private static boolean keyPressed(final Minecraft client, final int key) {
        return client != null
                && client.getWindow() != null
                && GLFW.glfwGetKey(client.getWindow().handle(), key) == GLFW.GLFW_PRESS;
    }
}
