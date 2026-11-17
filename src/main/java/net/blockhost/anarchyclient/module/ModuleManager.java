package net.blockhost.anarchyclient.module;

import com.mojang.blaze3d.platform.InputConstants;
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
import net.blockhost.anarchyclient.notification.ToggleNotifications;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.ClientInput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ModuleManager {

    private final AnarchyEventBus events = new AnarchyEventBus();
    private final Map<String, Module> modules = new LinkedHashMap<>();
    private final ModuleKeybindController keybindController = new ModuleKeybindController();
    private Runnable activationObserver = () -> {
    };

    public void register(final Module module) {
        Module previous = this.modules.putIfAbsent(module.id(), module);
        if (previous != null) {
            throw new IllegalArgumentException("Duplicate module id: " + module.id());
        }
        module.activationListener(this::onActivationChanged);
        // Register every listener once, in registry order. Listeners skip disabled modules, so dispatch
        // order stays the registry order however modules are toggled.
        this.events.register(new ModuleEventListener(module));
    }

    /**
     * Sets a callback that runs after any module is enabled or disabled, for example to save the config.
     * It can run on any thread that toggles a module.
     */
    public void activationObserver(final Runnable observer) {
        this.activationObserver = observer == null ? () -> {
        } : observer;
    }

    public Optional<Module> find(final String id) {
        Module direct = this.modules.get(id);
        if (direct != null) {
            return Optional.of(direct);
        }
        for (Module module : this.modules.values()) {
            if (module.aliases().contains(id)) {
                return Optional.of(module);
            }
        }
        return Optional.empty();
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

    public void renderWorld(final Minecraft client, final LevelRenderContext context) {
        this.call(new WorldRenderEvent(client, context));
    }

    public void renderHud(final Minecraft client, final GuiGraphicsExtractor graphics) {
        this.call(new HudRenderEvent(client, graphics));
    }

    private void onActivationChanged(final Module module, final boolean enabled) {
        this.activationObserver.run();
        // Only toast in-world: config load restores module states before the player exists, and there is
        // no client at all under tests.
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.player != null) {
            ToggleNotifications.push(module.name(), enabled);
        }
    }

    private static boolean keyPressed(final Minecraft client, final int key) {
        return client != null
                && client.getWindow() != null
                && InputConstants.isKeyDown(key);
    }
}
