package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.render.CuboidMarker;
import net.blockhost.anarchyclient.render.MarkerManager;
import net.blockhost.anarchyclient.render.MarkerStyle;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.ItemListSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class ItemHighlightModule extends Module {

    private final SelectSetting itemMode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("item_mode")
            .name("Items")
            .defaultValue("Listed")
            .addAllOptions(List.of("Listed", "Valuable", "All"))
            .build()));
    private final ItemListSetting items = this.setting(ItemListSetting.from(ItemListSetting.builder()
            .id("items")
            .name("Item List")
            .addAllDefaultValue(List.of(Items.TOTEM_OF_UNDYING, Items.END_CRYSTAL, Items.ENCHANTED_GOLDEN_APPLE))
            .build()));
    private final SelectSetting renderMode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("render_mode")
            .name("Render")
            .defaultValue("Box")
            .addAllOptions(List.of("Box", "Shader", "Marker"))
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
    private final BooleanSetting crystals = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("crystals")
            .name("Crystals")
            .defaultValue(false)
            .build()));
    private final NumberSetting crystalRange = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("crystal_range")
            .name("Crystal Range")
            .defaultValue(24.0)
            .min(4.0)
            .max(96.0)
            .step(4.0)
            .build()));

    public ItemHighlightModule() {
        super("item_highlight", "Item Highlight", ModuleCategory.RENDER);
        this.items.visibleWhen(() -> "Listed".equals(this.itemMode.value()));
        this.opacity.visibleWhen(() -> "Box".equals(this.renderMode.value()));
        this.crystalRange.visibleWhen(this.crystals::value);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }
        if ("Marker".equals(this.renderMode.value())) {
            double rangeSqr = this.range.value() * this.range.value();
            for (Entity entity : client.level.entitiesForRendering()) {
                if (entity instanceof ItemEntity itemEntity
                        && itemEntity.distanceToSqr(client.player) <= rangeSqr
                        && this.matchesItem(itemEntity)) {
                    MarkerManager.put(new CuboidMarker("item_highlight:item:" + entity.getId(),
                            entity.getBoundingBox().inflate(0.05), MarkerStyle.CYAN, 4));
                }
            }
        }
        if (this.crystals.value()) {
            double crystalRangeSqr = this.crystalRange.value() * this.crystalRange.value();
            for (Entity entity : client.level.entitiesForRendering()) {
                if (entity instanceof EndCrystal && entity.distanceToSqr(client.player) <= crystalRangeSqr) {
                    MarkerManager.put(new CuboidMarker("item_highlight:crystal:" + entity.getId(),
                            entity.getBoundingBox(), MarkerStyle.CYAN, 4));
                }
            }
        }
    }

    @Override
    public void renderWorld(final LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.gameRenderer.mainCamera().entity() instanceof Player cameraPlayer ? cameraPlayer : null;
        PoseStack matrices = context.poseStack();
        SubmitNodeCollector submits = context.submitNodeCollector();
        if (client.level == null || player == null || matrices == null || submits == null
                || "Marker".equals(this.renderMode.value())) {
            return;
        }

        Vec3 camera = client.gameRenderer.mainCamera().position();
        float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        boolean shaderMode = "Shader".equals(this.renderMode.value());
        double maxDistanceSqr = this.range.value() * this.range.value();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof ItemEntity itemEntity)
                    || itemEntity.distanceToSqr(player) > maxDistanceSqr
                    || !this.matchesItem(itemEntity)) {
                continue;
            }
            if (shaderMode) {
                EspOutlineRegistry.set(entity.getId(), ARGB.color(255, 92, 214, 255));
            } else {
                AABB box = WorldLineRenderer.interpolatedBox(itemEntity, partialTick, 0.08, camera);
                WorldLineRenderer.boxNoDepth(matrices, submits, box,
                        new WorldLineRenderer.Color(255, 215, 95, this.opacity.value().intValue()));
            }
        }
    }

    private boolean matchesItem(final ItemEntity entity) {
        return switch (this.itemMode.value()) {
            case "All" -> true;
            case "Valuable" -> isValuable(entity);
            default -> this.items.value().contains(entity.getItem().getItem());
        };
    }

    private static boolean isValuable(final ItemEntity entity) {
        String id = BuiltInRegistries.ITEM.getKey(entity.getItem().getItem()).getPath();
        return id.contains("diamond")
                || id.contains("netherite")
                || id.contains("totem")
                || id.contains("elytra")
                || id.contains("shulker")
                || id.contains("enchanted")
                || id.contains("golden_apple");
    }
}
