package net.blockhost.anarchyclient.module;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ModuleManager {

    private final Map<String, Module> modules = new LinkedHashMap<>();

    public void register(final Module module) {
        Module previous = this.modules.putIfAbsent(module.id(), module);
        if (previous != null) {
            throw new IllegalArgumentException("Duplicate module id: " + module.id());
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

    public void tick(final Minecraft client) {
        for (Module module : this.modules.values()) {
            if (module.enabled()) {
                module.tick(client);
            }
        }
    }

    public void renderWorld(final LevelRenderContext context) {
        for (Module module : this.modules.values()) {
            if (module.enabled()) {
                module.renderWorld(context);
            }
        }
    }

    public void renderHud(final Minecraft client, final GuiGraphicsExtractor graphics) {
        for (Module module : this.modules.values()) {
            if (module.enabled()) {
                module.renderHud(client, graphics);
            }
        }
    }
}
