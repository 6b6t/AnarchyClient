package net.blockhost.anarchyclient.module;

import net.blockhost.anarchyclient.event.AnarchyEventBus;
import net.blockhost.anarchyclient.event.AnarchyClientEvent;
import net.blockhost.anarchyclient.event.ClientInputEvent;
import net.blockhost.anarchyclient.event.ClientTickEvent;
import net.blockhost.anarchyclient.event.HudRenderEvent;
import net.blockhost.anarchyclient.event.PreventEdgeFallEvent;
import net.blockhost.anarchyclient.event.WorldRenderEvent;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Player;

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

    public void updateInput(final Minecraft client, final ClientInput input) {
        this.call(new ClientInputEvent(client, input));
    }

    public boolean preventEdgeFall(final Minecraft client, final Player player) {
        return this.call(new PreventEdgeFallEvent(client, player)).isCancelled();
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
}
