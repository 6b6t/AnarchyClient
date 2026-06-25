package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.StringListSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;

import java.util.List;

public final class NotebotModule extends Module {

    private final StringListSetting notes = this.setting(StringListSetting.from(StringListSetting.builder()
            .id("notes")
            .name("Notes")
            .defaultValue(List.of("0", "4", "7", "12"))
            .build()));
    private final NumberSetting delay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay")
            .name("Delay")
            .defaultValue(6.0)
            .min(1.0)
            .max(40.0)
            .step(1.0)
            .build()));
    private int noteIndex;
    private int cooldownTicks;

    public NotebotModule() {
        super("notebot", "Notebot", ModuleCategory.MISC);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || this.notes.value().isEmpty()) {
            return;
        }
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return;
        }
        int note = parseNote(this.notes.value().get(this.noteIndex));
        client.player.playSound(SoundEvents.NOTE_BLOCK_HARP.value(), 2.0F, pitch(note));
        this.noteIndex = (this.noteIndex + 1) % this.notes.value().size();
        this.cooldownTicks = this.delay.value().intValue();
    }

    static int parseNote(final String value) {
        try {
            return Math.max(0, Math.min(24, Integer.parseInt(value.trim())));
        } catch (RuntimeException exception) {
            return 0;
        }
    }

    static float pitch(final int note) {
        return (float) Math.pow(2.0, (Math.max(0, Math.min(24, note)) - 12) / 12.0);
    }
}
