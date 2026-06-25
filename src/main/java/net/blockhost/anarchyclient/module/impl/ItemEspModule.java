package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class ItemEspModule extends Module {

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
    private final NumberSetting opacity = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("opacity")
            .name("Opacity")
            .defaultValue(180.0)
            .min(40.0)
            .max(255.0)
            .step(5.0)
            .build()));
    private final BooleanSetting valuableOnly = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("valuable_only")
            .name("Valuable")
            .defaultValue(false)
            .build()));

    public ItemEspModule() {
        super("item_esp", "Item ESP", ModuleCategory.RENDER);
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
        double maxDistanceSqr = this.range.value() * this.range.value();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof ItemEntity itemEntity) || itemEntity.distanceToSqr(player) > maxDistanceSqr) {
                continue;
            }
            if (this.valuableOnly.value() && !isValuable(itemEntity)) {
                continue;
            }
            if (shaderMode) {
                EspOutlineRegistry.set(entity.getId(), ARGB.color(255, 92, 214, 255));
            } else {
                WorldLineRenderer.boxNoDepth(matrices, submits,
                        WorldLineRenderer.interpolatedBox(itemEntity, partialTick, 0.08, camera),
                        new WorldLineRenderer.Color(92, 214, 255, this.opacity.value().intValue()));
            }
        }
    }

    private static boolean isValuable(final ItemEntity entity) {
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(entity.getItem().getItem()).getPath();
        return id.contains("diamond")
                || id.contains("netherite")
                || id.contains("totem")
                || id.contains("elytra")
                || id.contains("shulker")
                || id.contains("enchanted")
                || id.contains("golden_apple");
    }
}
