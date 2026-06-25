package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.ItemListSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class ItemHighlightModule extends Module {

    private final ItemListSetting items = this.setting(ItemListSetting.from(ItemListSetting.builder()
            .id("items")
            .name("Items")
            .addAllDefaultValue(List.of(Items.TOTEM_OF_UNDYING, Items.END_CRYSTAL, Items.ENCHANTED_GOLDEN_APPLE))
            .build()));
    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(96.0)
            .min(8.0)
            .max(256.0)
            .step(8.0)
            .build()));

    public ItemHighlightModule() {
        super("item_highlight", "Item Highlight", ModuleCategory.RENDER);
    }

    @Override
    public void renderWorld(final LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        PoseStack matrices = context.poseStack();
        SubmitNodeCollector submits = context.submitNodeCollector();
        if (client.player == null || client.level == null || matrices == null || submits == null) {
            return;
        }
        Vec3 camera = client.gameRenderer.mainCamera().position();
        double rangeSqr = this.range.value() * this.range.value();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof ItemEntity item
                    && item.distanceToSqr(client.player) <= rangeSqr
                    && this.items.value().contains(item.getItem().getItem())) {
                AABB box = item.getBoundingBox().inflate(0.08).move(camera.scale(-1));
                WorldLineRenderer.boxNoDepth(matrices, submits, box, new WorldLineRenderer.Color(255, 215, 95, 220));
            }
        }
    }
}
