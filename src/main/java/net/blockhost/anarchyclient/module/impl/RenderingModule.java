package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.render.RenderSuppression;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.blockhost.anarchyclient.setting.StatusEffectListSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class RenderingModule extends Module {

    private final SelectSetting fovMode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("fov_mode")
            .name("FOV")
            .defaultValue("Scale")
            .addAllOptions(List.of("Scale", "Fixed"))
            .build()));
    private final NumberSetting fovScale = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("fov_scale")
            .name("FOV Scale")
            .defaultValue(1.0)
            .min(0.25)
            .max(2.5)
            .step(0.05)
            .build()));
    private final NumberSetting fixedFov = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("fixed_fov")
            .name("Fixed FOV")
            .defaultValue(90.0)
            .min(30.0)
            .max(120.0)
            .step(1.0)
            .build()));
    private final NumberSetting minimumFov = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("min_fov")
            .name("Min FOV")
            .defaultValue(30.0)
            .min(1.0)
            .max(160.0)
            .step(1.0)
            .build()));
    private final NumberSetting maximumFov = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("max_fov")
            .name("Max FOV")
            .defaultValue(140.0)
            .min(1.0)
            .max(180.0)
            .step(1.0)
            .build()));
    private final NumberSetting yOffset = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("y_offset")
            .name("Y Offset")
            .defaultValue(0.0)
            .min(-3.0)
            .max(3.0)
            .step(0.05)
            .build()));
    private final NumberSetting yawOffset = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("yaw_offset")
            .name("Yaw Offset")
            .defaultValue(0.0)
            .min(-180.0)
            .max(180.0)
            .step(5.0)
            .build()));
    private final NumberSetting pitchOffset = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("pitch_offset")
            .name("Pitch Offset")
            .defaultValue(0.0)
            .min(-89.0)
            .max(89.0)
            .step(1.0)
            .build()));
    private final BooleanSetting hurtCamera = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("hurt_camera")
            .name("Hurt Cam")
            .defaultValue(false)
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
    private final BooleanSetting dynamicFov = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("dynamic_fov")
            .name("Dynamic FOV")
            .defaultValue(false)
            .build()));
    private final BooleanSetting blindness = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("blindness")
            .name("Blindness")
            .defaultValue(false)
            .build()));
    private final BooleanSetting darkness = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("darkness")
            .name("Darkness")
            .defaultValue(false)
            .build()));
    private final BooleanSetting removeStatusEffects = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("remove_status_effects")
            .name("Status")
            .defaultValue(false)
            .build()));
    private final StatusEffectListSetting statusEffects = this.setting(StatusEffectListSetting.from(StatusEffectListSetting.builder()
            .id("status_effects")
            .name("Effects")
            .addAllDefaultValue(List.of(
                    MobEffects.BLINDNESS.value(),
                    MobEffects.DARKNESS.value(),
                    MobEffects.LEVITATION.value(),
                    MobEffects.MINING_FATIGUE.value(),
                    MobEffects.NAUSEA.value()
            ))
            .build()));

    public RenderingModule() {
        super("rendering", "Rendering", ModuleCategory.RENDER);
        this.fovScale.visibleWhen(() -> "Scale".equals(this.fovMode.value()));
        this.minimumFov.visibleWhen(() -> "Scale".equals(this.fovMode.value()));
        this.maximumFov.visibleWhen(() -> "Scale".equals(this.fovMode.value()));
        this.fixedFov.visibleWhen(() -> "Fixed".equals(this.fovMode.value()));
        this.statusEffects.visibleWhen(this.removeStatusEffects::value);
    }

    @Override
    protected void onEnable() {
        this.updateSuppression();
    }

    @Override
    protected void onDisable() {
        RenderSuppression.disable(this.id());
    }

    @Override
    public void tick(final Minecraft client) {
        this.updateSuppression();
        if (!this.removeStatusEffects.value()) {
            return;
        }
        LocalPlayer player = client.player;
        if (player == null) {
            return;
        }
        for (MobEffect effect : this.statusEffects.value()) {
            player.removeEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect));
        }
    }

    @Override
    public float fov(final Minecraft client, final float fov) {
        if ("Fixed".equals(this.fovMode.value())) {
            return this.fixedFov.value().floatValue();
        }
        return clamp((float) (fov * this.fovScale.value()), this.minimumFov.value().floatValue(),
                this.maximumFov.value().floatValue());
    }

    @Override
    public CameraTransform cameraTransform(final Minecraft client, final Vec3 position, final float yaw, final float pitch) {
        return new CameraTransform(
                position.add(0.0, this.yOffset.value(), 0.0),
                yaw + this.yawOffset.value().floatValue(),
                clampPitch(pitch + this.pitchOffset.value().floatValue())
        );
    }

    static float clamp(final float value, final float min, final float max) {
        return Math.max(min, Math.min(max, value));
    }

    static float clampPitch(final float pitch) {
        return Math.max(-90.0F, Math.min(90.0F, pitch));
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
        if (this.dynamicFov.value() || "Fixed".equals(this.fovMode.value())) {
            kinds.add(RenderSuppression.Kind.DYNAMIC_FOV);
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
