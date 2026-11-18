package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class HitmarkerModule extends Module {

    private final NumberSetting duration = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("duration")
            .name("Duration")
            .defaultValue(300.0)
            .min(50.0)
            .max(1500.0)
            .step(50.0)
            .description("How long the marker stays on screen, in milliseconds.")
            .build()));
    private final NumberSetting size = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("size")
            .name("Size")
            .defaultValue(4.0)
            .min(2.0)
            .max(12.0)
            .step(1.0)
            .build()));
    private final NumberSetting gap = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("gap")
            .name("Gap")
            .defaultValue(3.0)
            .min(0.0)
            .max(10.0)
            .step(1.0)
            .build()));
    private final BooleanSetting sound = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("sound")
            .name("Sound")
            .defaultValue(false)
            .description("Plays a short click when you hit something.")
            .build()));
    private long hitMillis = -1L;
    private boolean killHit;

    public HitmarkerModule() {
        super("hitmarker", "Hitmarker", ModuleCategory.RENDER,
                "Draws an X around your crosshair for a moment each time you hit a living entity. It turns red on a hit that should kill.");
    }

    @Override
    public boolean attackEntity(final Minecraft client, final Player player, final Entity target) {
        if (target instanceof LivingEntity living && living.isAlive()) {
            this.hitMillis = System.currentTimeMillis();
            this.killHit = living.getHealth() + living.getAbsorptionAmount() <= 4.0F;
            if (this.sound.value()) {
                player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.4F, 1.6F);
            }
        }
        return false;
    }

    @Override
    public void renderHud(final Minecraft client, final GuiGraphicsExtractor graphics) {
        if (this.hitMillis < 0L || client.player == null) {
            return;
        }
        long elapsed = System.currentTimeMillis() - this.hitMillis;
        int alpha = alpha(elapsed, this.duration.value().longValue());
        if (alpha <= 0) {
            this.hitMillis = -1L;
            return;
        }
        int rgb = this.killHit ? 0xFF4040 : 0xFFFFFF;
        int color = alpha << 24 | rgb;
        int centerX = graphics.guiWidth() / 2;
        int centerY = graphics.guiHeight() / 2;
        int length = this.size.value().intValue();
        int offset = this.gap.value().intValue();
        for (int step = 0; step < length; step++) {
            int distance = offset + step;
            dot(graphics, centerX + distance, centerY + distance, color);
            dot(graphics, centerX - distance - 1, centerY + distance, color);
            dot(graphics, centerX + distance, centerY - distance - 1, color);
            dot(graphics, centerX - distance - 1, centerY - distance - 1, color);
        }
    }

    /** Fades the marker linearly to transparent over {@code durationMillis}. */
    static int alpha(final long elapsedMillis, final long durationMillis) {
        if (durationMillis <= 0L || elapsedMillis >= durationMillis) {
            return 0;
        }
        return (int) Math.round(255.0 * (1.0 - (double) Math.max(0L, elapsedMillis) / durationMillis));
    }

    private static void dot(final GuiGraphicsExtractor graphics, final int x, final int y, final int color) {
        graphics.fill(x, y, x + 1, y + 1, color);
    }

    @Override
    protected void onDisable() {
        this.hitMillis = -1L;
    }
}
