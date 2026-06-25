package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class SoundLocatorModule extends Module {

    private final StringSetting sounds = this.setting(StringSetting.from(StringSetting.builder()
            .id("sounds")
            .name("Sounds")
            .defaultValue("")
            .build()));
    private final BooleanSetting logChat = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("log_chat")
            .name("Chat")
            .defaultValue(false)
            .build()));
    private final BooleanSetting renderMarkers = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("render_markers")
            .name("Render")
            .defaultValue(true)
            .build()));
    private final NumberSetting lifetime = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("lifetime")
            .name("Lifetime")
            .defaultValue(60.0)
            .min(5.0)
            .max(400.0)
            .step(5.0)
            .build()));
    private final NumberSetting markerSize = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("marker_size")
            .name("Size")
            .defaultValue(0.6)
            .min(0.2)
            .max(3.0)
            .step(0.1)
            .build()));
    private final NumberSetting maxMarkers = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("max_markers")
            .name("Markers")
            .defaultValue(64.0)
            .min(1.0)
            .max(256.0)
            .step(1.0)
            .build()));
    private final List<SoundMarker> markers = new ArrayList<>();
    private String lastSounds = "";
    private Set<Identifier> parsedSounds = Set.of();

    public SoundLocatorModule() {
        super("sound_locator", "Sound Locator", ModuleCategory.RENDER);
    }

    @Override
    public void tick(final Minecraft client) {
        synchronized (this.markers) {
            this.markers.replaceAll(SoundMarker::tick);
            this.markers.removeIf(SoundMarker::expired);
        }
    }

    @Override
    public void soundPacket(final Minecraft client, final ClientboundSoundPacket packet) {
        if (!this.lastSounds.equals(this.sounds.value())) {
            this.parsedSounds = SoundScan.parseSoundIds(this.sounds.value());
            this.lastSounds = this.sounds.value();
        }
        Identifier id = soundId(packet);
        if (!this.parsedSounds.isEmpty() && !this.parsedSounds.contains(id)) {
            return;
        }
        Vec3 position = new Vec3(packet.getX(), packet.getY(), packet.getZ());
        synchronized (this.markers) {
            this.markers.add(new SoundMarker(id, position, this.lifetime.value().intValue()));
            while (this.markers.size() > this.maxMarkers.value().intValue()) {
                this.markers.removeFirst();
            }
        }
        if (this.logChat.value() && client.player != null) {
            client.player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "%s at %.1f %.1f %.1f".formatted(id, position.x, position.y, position.z)
            ));
        }
    }

    @Override
    public void renderWorld(final LevelRenderContext context) {
        if (!this.renderMarkers.value()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        PoseStack matrices = context.poseStack();
        SubmitNodeCollector submits = context.submitNodeCollector();
        if (client.level == null || matrices == null || submits == null) {
            return;
        }
        Vec3 camera = client.gameRenderer.mainCamera().position();
        double halfSize = this.markerSize.value() / 2.0;
        List<SoundMarker> snapshot;
        synchronized (this.markers) {
            snapshot = List.copyOf(this.markers);
        }
        for (SoundMarker marker : snapshot) {
            AABB box = new AABB(
                    marker.position().x - halfSize,
                    marker.position().y - halfSize,
                    marker.position().z - halfSize,
                    marker.position().x + halfSize,
                    marker.position().y + halfSize,
                    marker.position().z + halfSize
            ).move(camera.scale(-1));
            WorldLineRenderer.fillNoDepth(matrices, submits, box, new WorldLineRenderer.Color(250, 80, 80, 45));
            WorldLineRenderer.boxNoDepth(matrices, submits, box, new WorldLineRenderer.Color(250, 80, 80, 210));
        }
    }

    @Override
    protected void onDisable() {
        synchronized (this.markers) {
            this.markers.clear();
        }
    }

    private static Identifier soundId(final ClientboundSoundPacket packet) {
        return packet.getSound().unwrapKey()
                .map(key -> key.identifier())
                .orElse(packet.getSound().value().location());
    }

    private record SoundMarker(Identifier sound, Vec3 position, int ticksRemaining) {

        SoundMarker tick() {
            return new SoundMarker(this.sound, this.position, this.ticksRemaining - 1);
        }

        boolean expired() {
            return this.ticksRemaining <= 0;
        }
    }
}
