package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.blockhost.anarchyclient.target.RenderedEntityCache;
import net.blockhost.anarchyclient.target.TargetClassifier;
import net.blockhost.anarchyclient.target.TargetPolicy;
import net.blockhost.anarchyclient.target.TargetQuery;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class TracersModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(128.0)
            .min(8.0)
            .max(256.0)
            .step(4.0)
            .build()));
    private final BooleanSetting players = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("players")
            .name("Players")
            .defaultValue(true)
            .build()));
    private final BooleanSetting hostileMobs = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("hostile_mobs")
            .name("Hostiles")
            .defaultValue(false)
            .build()));
    private final BooleanSetting passiveMobs = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("passive_mobs")
            .name("Passives")
            .defaultValue(false)
            .build()));
    private final BooleanSetting invisibles = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("invisibles")
            .name("Invisibles")
            .defaultValue(false)
            .build()));
    private final BooleanSetting ignoreFriends = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("ignore_friends")
            .name("Friends")
            .defaultValue(true)
            .build()));
    private final StringSetting friends = this.setting(StringSetting.from(StringSetting.builder()
            .id("friends")
            .name("Friend List")
            .defaultValue("")
            .build()));
    private final BooleanSetting ignoreTeams = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("ignore_teams")
            .name("Teams")
            .defaultValue(false)
            .build()));
    private final BooleanSetting antiBot = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("anti_bot")
            .name("Anti Bot")
            .defaultValue(true)
            .build()));
    private final NumberSetting opacity = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("opacity")
            .name("Opacity")
            .defaultValue(150.0)
            .min(40.0)
            .max(255.0)
            .step(5.0)
            .build()));

    public TracersModule() {
        super("tracers", "Tracers", ModuleCategory.RENDER);
    }

    @Override
    protected void onEnable() {
        RenderedEntityCache.subscribe(this.id());
    }

    @Override
    protected void onDisable() {
        RenderedEntityCache.unsubscribe(this.id());
    }

    @Override
    public void renderWorld(final LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.gameRenderer.mainCamera().entity() instanceof Player cameraPlayer ? cameraPlayer : null;
        PoseStack matrices = context.poseStack();
        SubmitNodeCollector submits = context.submitNodeCollector();
        if (client.level == null || player == null || matrices == null || submits == null) {
            return;
        }

        Vec3 camera = client.gameRenderer.mainCamera().position();
        Vec3 start = player.getEyePosition().subtract(camera);
        TargetPolicy policy = this.targetPolicy();
        for (LivingEntity entity : RenderedEntityCache.entities()) {
            if (!TargetQuery.allowed(entity, player, policy)) {
                continue;
            }
            double rangeValue = this.range.value();
            if (entity.distanceToSqr(player) > rangeValue * rangeValue) {
                continue;
            }
            Vec3 end = entity.getBoundingBox().getCenter().subtract(camera);
            WorldLineRenderer.line(matrices, submits, start, end, this.color(entity));
        }
    }

    private WorldLineRenderer.Color color(final Entity entity) {
        int alpha = this.opacity.value().intValue();
        if (TargetClassifier.isFriend(entity, this.friends.value())) {
            return new WorldLineRenderer.Color(98, 170, 255, alpha);
        }
        return switch (TargetClassifier.kind(entity)) {
            case HOSTILE -> new WorldLineRenderer.Color(255, 86, 86, alpha);
            case NEUTRAL -> new WorldLineRenderer.Color(255, 162, 72, alpha);
            case PASSIVE, WATER_CREATURE -> new WorldLineRenderer.Color(245, 205, 92, alpha);
            case PLAYER -> new WorldLineRenderer.Color(0, 212, 170, alpha);
            case UNKNOWN -> new WorldLineRenderer.Color(190, 190, 190, alpha);
        };
    }

    private TargetPolicy targetPolicy() {
        return TargetPolicy.of(
                this.players.value(),
                this.hostileMobs.value(),
                this.passiveMobs.value(),
                this.invisibles.value(),
                this.ignoreFriends.value(),
                this.friends.value(),
                this.ignoreTeams.value(),
                this.antiBot.value()
        );
    }
}
