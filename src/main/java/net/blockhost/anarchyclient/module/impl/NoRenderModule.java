package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.render.RenderSuppression;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.effect.MobEffects;

import java.util.ArrayList;
import java.util.List;

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
    private final BooleanSetting hurtCamera = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("hurt_camera")
            .name("Hurt Cam")
            .defaultValue(true)
            .build()));
    private final BooleanSetting viewBob = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("view_bob")
            .name("View Bob")
            .defaultValue(false)
            .build()));
    private final BooleanSetting cameraClip = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("camera_clip")
            .name("Camera Clip")
            .defaultValue(false)
            .build()));
    private final BooleanSetting blindness = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("blindness")
            .name("Blindness")
            .defaultValue(true)
            .build()));
    private final BooleanSetting darkness = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("darkness")
            .name("Darkness")
            .defaultValue(true)
            .build()));
    private final BooleanSetting weather = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("weather")
            .name("Weather")
            .defaultValue(false)
            .build()));
    private final BooleanSetting nausea = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("nausea")
            .name("Nausea")
            .defaultValue(true)
            .build()));

    public NoRenderModule() {
        super("no_render", "No Render", ModuleCategory.RENDER);
    }

    @Override
    protected void onEnable() {
        this.updateSuppression();
    }

    @Override
    public void tick(final Minecraft client) {
        this.updateSuppression();
        LocalPlayer player = client.player;
        if (player == null) {
            return;
        }
        if (this.blindness.value()) {
            player.removeEffect(MobEffects.BLINDNESS);
        }
        if (this.darkness.value()) {
            player.removeEffect(MobEffects.DARKNESS);
        }
        if (this.nausea.value()) {
            player.removeEffect(MobEffects.NAUSEA);
        }
        if (this.weather.value() && client.level != null) {
            client.level.setRainLevel(0.0F);
            client.level.setThunderLevel(0.0F);
        }
    }

    @Override
    public boolean particle(final Minecraft client, final ParticleOptions particle, final boolean alwaysShow) {
        return this.particles.value() && (!alwaysShow || this.forcedParticles.value());
    }

    @Override
    protected void onDisable() {
        RenderSuppression.disable(this.id());
    }

    private void updateSuppression() {
        List<RenderSuppression.Kind> kinds = new ArrayList<>();
        if (this.hurtCamera.value()) {
            kinds.add(RenderSuppression.Kind.HURT_CAMERA);
        }
        if (this.viewBob.value()) {
            kinds.add(RenderSuppression.Kind.VIEW_BOB);
        }
        if (this.cameraClip.value()) {
            kinds.add(RenderSuppression.Kind.CAMERA_CLIP);
        }
        if (this.blindness.value()) {
            kinds.add(RenderSuppression.Kind.BLINDNESS);
        }
        if (this.darkness.value()) {
            kinds.add(RenderSuppression.Kind.DARKNESS);
        }
        RenderSuppression.enable(this.id(), kinds.toArray(RenderSuppression.Kind[]::new));
    }
}
