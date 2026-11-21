package net.blockhost.anarchyclient.module.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.module.ModuleManager;
import net.minecraft.client.Minecraft;

public final class ModuleInfoHudModule extends HudElementModule {

    private final ModuleManager modules;

    public ModuleInfoHudModule(final ModuleManager modules) {
        super(
                "module_info_hud",
                "Module Info",
                "Shows how many modules are enabled in total and in each category.",
                "Bottom Left");
        this.modules = modules;
    }

    @Override
    protected int color(final Minecraft client) {
        return HudText.accentColor();
    }

    @Override
    protected List<String> lines(final Minecraft client) {
        List<Module> enabled =
                this.modules.all().stream().filter(Module::enabled).toList();
        Map<ModuleCategory, Long> byCategory =
                enabled.stream().collect(Collectors.groupingBy(Module::category, Collectors.counting()));
        List<String> lines = new ArrayList<>();
        lines.add("Modules " + enabled.size() + "/" + this.modules.all().size());
        byCategory.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().displayName()))
                .forEach(entry -> lines.add(entry.getKey().displayName() + " " + entry.getValue()));
        return lines;
    }
}
