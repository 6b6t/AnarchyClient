package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class ItemTagsModule extends Module {

    private final BooleanSetting registryId = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("registry_id")
            .name("Registry")
            .defaultValue(true)
            .build()));

    public ItemTagsModule() {
        super("item_tags", "Item Tags", ModuleCategory.RENDER);
    }

    @Override
    public void itemTooltip(final Minecraft client, final ItemStack stack, final List<Component> lines) {
        if (stack == null || stack.isEmpty() || !this.registryId.value()) {
            return;
        }
        lines.add(Component.literal("Id: " + BuiltInRegistries.ITEM.getKey(stack.getItem())).withStyle(ChatFormatting.DARK_GRAY));
    }
}
