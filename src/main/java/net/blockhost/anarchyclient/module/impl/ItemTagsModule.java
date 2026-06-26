package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.ItemListSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.blockhost.anarchyclient.ui.AnarchyClientScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ItemTagsModule extends Module {

    private final BooleanSetting labels = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("labels")
            .name("Labels")
            .defaultValue(true)
            .build()));
    private final SelectSetting itemMode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("item_mode")
            .name("Items")
            .defaultValue("All")
            .addAllOptions(List.of("All", "Listed", "Valuable"))
            .build()));
    private final ItemListSetting items = this.setting(ItemListSetting.from(ItemListSetting.builder()
            .id("items")
            .name("Item List")
            .addAllDefaultValue(List.of(Items.TOTEM_OF_UNDYING, Items.END_CRYSTAL, Items.ENCHANTED_GOLDEN_APPLE))
            .build()));
    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(48.0)
            .min(4.0)
            .max(160.0)
            .step(4.0)
            .build()));
    private final NumberSetting maxLabels = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("max_labels")
            .name("Max Labels")
            .defaultValue(32.0)
            .min(4.0)
            .max(128.0)
            .step(4.0)
            .build()));
    private final BooleanSetting counts = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("counts")
            .name("Counts")
            .defaultValue(true)
            .build()));
    private final BooleanSetting background = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("background")
            .name("Background")
            .defaultValue(true)
            .build()));
    private final BooleanSetting registryId = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("registry_id")
            .name("Registry")
            .defaultValue(true)
            .build()));

    public ItemTagsModule() {
        super("item_tags", "Item Tags", ModuleCategory.RENDER);
        this.items.visibleWhen(() -> "Listed".equals(this.itemMode.value()));
    }

    @Override
    public void itemTooltip(final Minecraft client, final ItemStack stack, final List<Component> lines) {
        if (stack == null || stack.isEmpty() || !this.registryId.value()) {
            return;
        }
        lines.add(Component.literal("Id: " + BuiltInRegistries.ITEM.getKey(stack.getItem())).withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public void renderHud(final Minecraft client, final GuiGraphicsExtractor graphics) {
        if (!this.labels.value()
                || client.player == null
                || client.level == null
                || client.gui.screen() instanceof AnarchyClientScreen) {
            return;
        }
        double rangeSqr = this.range.value() * this.range.value();
        float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        List<ItemEntity> itemEntities = new ArrayList<>();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof ItemEntity itemEntity
                    && itemEntity.distanceToSqr(client.player) <= rangeSqr
                    && this.matchesItem(itemEntity)) {
                itemEntities.add(itemEntity);
            }
        }
        itemEntities.stream()
                .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(client.player)))
                .limit(this.maxLabels.value().longValue())
                .forEach(entity -> this.renderLabel(client, graphics, client.player, entity, partialTick));
    }

    private void renderLabel(final Minecraft client, final GuiGraphicsExtractor graphics, final Player player,
                             final ItemEntity entity, final float partialTick) {
        double x = Mth.lerp(partialTick, entity.xo, entity.getX());
        double y = Mth.lerp(partialTick, entity.yo, entity.getY()) + entity.getBoundingBox().getYsize() + 0.35;
        double z = Mth.lerp(partialTick, entity.zo, entity.getZ());
        Vec3 projected = client.gameRenderer.projectPointToScreen(new Vec3(x, y, z));
        if (projected.z > 1.0) {
            return;
        }
        String text = this.label(entity.getItem(), Math.sqrt(entity.distanceToSqr(player)));
        int screenX = (int) ((projected.x + 1.0) * 0.5 * graphics.guiWidth());
        int screenY = (int) ((1.0 - projected.y) * 0.5 * graphics.guiHeight());
        int halfWidth = client.font.width(text) / 2;
        if (this.background.value()) {
            graphics.fill(screenX - halfWidth - 2, screenY - 2, screenX + halfWidth + 2, screenY + 9, 0x70000000);
        }
        graphics.text(client.font, text, screenX - halfWidth, screenY, 0xFFFFE0A3, true);
    }

    private String label(final ItemStack stack, final double distance) {
        String text = stack.getHoverName().getString();
        if (this.counts.value() && stack.getCount() > 1) {
            text += " x" + stack.getCount();
        }
        return text + " " + Math.round(distance) + "m";
    }

    private boolean matchesItem(final ItemEntity entity) {
        return switch (this.itemMode.value()) {
            case "Listed" -> this.items.value().contains(entity.getItem().getItem());
            case "Valuable" -> isValuable(entity);
            default -> true;
        };
    }

    private static boolean isValuable(final Entity entity) {
        if (!(entity instanceof ItemEntity itemEntity)) {
            return false;
        }
        String id = BuiltInRegistries.ITEM.getKey(itemEntity.getItem().getItem()).getPath();
        return id.contains("diamond")
                || id.contains("netherite")
                || id.contains("totem")
                || id.contains("elytra")
                || id.contains("shulker")
                || id.contains("enchanted")
                || id.contains("golden_apple");
    }
}
