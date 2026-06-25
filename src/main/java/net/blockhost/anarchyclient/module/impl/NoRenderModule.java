package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;

public final class NoRenderModule extends Module {

    private final BooleanSetting particles = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("particles")
            .name("Particles")
            .defaultValue(true)
            .build()));
    private final BooleanSetting forcedParticles = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("forced_particles")
            .name("Forced")
            .defaultValue(false)
            .build()));

    public NoRenderModule() {
        super("no_render", "No Render", ModuleCategory.RENDER);
    }

    @Override
    public boolean particle(final Minecraft client, final ParticleOptions particle, final boolean alwaysShow) {
        return this.particles.value() && (!alwaysShow || this.forcedParticles.value());
    }
}
