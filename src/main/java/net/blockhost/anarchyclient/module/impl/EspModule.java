package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.blockhost.anarchyclient.target.RenderedEntityCache;
import net.blockhost.anarchyclient.target.TargetClassifier;
import net.blockhost.anarchyclient.target.TargetKind;
import net.blockhost.anarchyclient.target.TargetPolicy;
import net.blockhost.anarchyclient.target.TargetQuery;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class EspModule extends Module {

    private final SelectSetting mode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("mode")
            .name("Mode")
            .defaultValue("Box")
            .addAllOptions(List.of("Box", "Shader"))
            .build()));
    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(96.0)
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
    private final BooleanSetting lineOfSightFade = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("line_of_sight_fade")
            .name("LOS Fade")
            .defaultValue(true)
            .build()));
    private final NumberSetting opacity = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("opacity")
            .name("Opacity")
            .defaultValue(170.0)
            .min(40.0)
            .max(255.0)
            .step(5.0)
            .build()));
    private final SelectSetting colorMode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("color_mode")
            .name("Color")
            .defaultValue("Type")
            .addAllOptions(List.of("Type", "Distance", "Aqua"))
            .build()));

    public EspModule() {
        super("esp", "ESP", ModuleCategory.RENDER);
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
        float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        boolean shaderMode = "Shader".equals(this.mode.value());
        TargetPolicy policy = this.targetPolicy();
        for (LivingEntity entity : RenderedEntityCache.entities()) {
            if (!TargetQuery.allowed(entity, player, policy)) {
                continue;
            }
            double distanceSqr = entity.distanceToSqr(player);
            double rangeValue = this.range.value();
            if (distanceSqr > rangeValue * rangeValue) {
                continue;
            }

            int alpha = this.opacity.value().intValue();
            if (this.lineOfSightFade.value() && !player.hasLineOfSight(entity)) {
                alpha = Math.max(35, alpha / 2);
            }

            WorldLineRenderer.Color color = this.color(entity, Math.sqrt(distanceSqr), alpha);
            if (shaderMode) {
                EspOutlineRegistry.set(entity.getId(), ARGB.color(255, color.red(), color.green(), color.blue()));
            } else {
                WorldLineRenderer.boxNoDepth(matrices, submits,
                        WorldLineRenderer.interpolatedBox(entity, partialTick, 0.04, camera), color);
            }
        }
    }

    boolean shouldRender(final Entity entity, final Player player) {
        return TargetQuery.allowed(entity, player, this.targetPolicy());
    }

    private WorldLineRenderer.Color color(final Entity entity, final double distance, final int alpha) {
        if (TargetClassifier.isPlayer(entity) && AnarchyClient.FRIENDS.isFriend(entity.getScoreboardName())) {
            return new WorldLineRenderer.Color(98, 170, 255, alpha);
        }
        return switch (this.colorMode.value()) {
            case "Distance" -> {
                int red = (int) Math.min(255, distance * 3);
                int green = Math.max(80, 255 - red);
                yield new WorldLineRenderer.Color(red, green, 80, alpha);
            }
            case "Aqua" -> new WorldLineRenderer.Color(0, 212, 170, alpha);
            default -> {
                yield switch (TargetClassifier.kind(entity)) {
                    case PLAYER -> new WorldLineRenderer.Color(0, 212, 170, alpha);
                    case HOSTILE -> new WorldLineRenderer.Color(255, 86, 86, alpha);
                    case NEUTRAL -> new WorldLineRenderer.Color(255, 162, 72, alpha);
                    case PASSIVE, WATER_CREATURE -> new WorldLineRenderer.Color(245, 205, 92, alpha);
                    case UNKNOWN -> new WorldLineRenderer.Color(190, 190, 190, alpha);
                };
            }
        };
    }

    private TargetPolicy targetPolicy() {
        return TargetPolicy.of(
                this.players.value(),
                this.hostileMobs.value(),
                this.passiveMobs.value(),
                this.invisibles.value(),
                this.ignoreFriends.value(),
                this.ignoreTeams.value(),
                this.antiBot.value()
        );
    }
}
