package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.world.entity.player.PlayerModelPart;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class SkinFlickerModule extends Module {

    private final SelectSetting mode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("mode")
            .name("Mode")
            .defaultValue("Blink")
            .addAllOptions(List.of("Blink", "Wave", "Random"))
            .build()));
    private final NumberSetting delay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay")
            .name("Delay")
            .defaultValue(4.0)
            .min(1.0)
            .max(40.0)
            .step(1.0)
            .description("Ticks between changes. The server sees each change, so very low values send many packets.")
            .build()));
    private final BooleanSetting cape = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("cape")
            .name("Cape")
            .defaultValue(false)
            .description("Also flickers your cape.")
            .build()));
    private final Map<PlayerModelPart, Boolean> original = new EnumMap<>(PlayerModelPart.class);
    private int ticks;
    private int step;

    public SkinFlickerModule() {
        super("skin_flicker", "Skin Flicker", ModuleCategory.MISC,
                "Toggles the outer layers of your skin on a timer so other players see them flicker. Restores your settings when turned off.");
    }

    @Override
    protected void onEnable() {
        Minecraft client = Minecraft.getInstance();
        this.original.clear();
        if (client.options != null) {
            for (PlayerModelPart part : PlayerModelPart.values()) {
                this.original.put(part, client.options.isModelPartEnabled(part));
            }
        }
        this.ticks = 0;
        this.step = 0;
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.options == null || ++this.ticks < this.delay.value().intValue()) {
            return;
        }
        this.ticks = 0;
        List<PlayerModelPart> parts = parts(this.cape.value());
        Options options = client.options;
        switch (this.mode.value()) {
            case "Wave" -> {
                for (int index = 0; index < parts.size(); index++) {
                    options.setModelPart(parts.get(index), index == this.step % parts.size());
                }
            }
            case "Random" -> {
                for (PlayerModelPart part : parts) {
                    options.setModelPart(part, ThreadLocalRandom.current().nextBoolean());
                }
            }
            default -> {
                boolean visible = this.step % 2 == 0;
                for (PlayerModelPart part : parts) {
                    options.setModelPart(part, visible);
                }
            }
        }
        this.step++;
        options.broadcastOptions();
    }

    static List<PlayerModelPart> parts(final boolean includeCape) {
        List<PlayerModelPart> parts = new ArrayList<>();
        for (PlayerModelPart part : PlayerModelPart.values()) {
            if (includeCape || part != PlayerModelPart.CAPE) {
                parts.add(part);
            }
        }
        return parts;
    }

    @Override
    protected void onDisable() {
        Minecraft client = Minecraft.getInstance();
        if (client.options == null || this.original.isEmpty()) {
            return;
        }
        this.original.forEach(client.options::setModelPart);
        client.options.broadcastOptions();
        this.original.clear();
    }
}
