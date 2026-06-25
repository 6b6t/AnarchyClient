package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.Set;

public final class AutoRenameModule extends Module {

    private final StringSetting items = this.setting(StringSetting.from(StringSetting.builder()
            .id("items")
            .name("Items")
            .defaultValue("")
            .description("Comma-separated item ids.")
            .build()));
    private final StringSetting name = this.setting(StringSetting.from(StringSetting.builder()
            .id("name")
            .name("Name")
            .defaultValue("")
            .build()));
    private final BooleanSetting labelShulkers = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("label_shulkers")
            .name("Shulkers")
            .defaultValue(false)
            .build()));
    private final BooleanSetting labelBundles = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("label_bundles")
            .name("Bundles")
            .defaultValue(false)
            .build()));
    private final NumberSetting delayTicks = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay_ticks")
            .name("Delay")
            .defaultValue(2.0)
            .min(0.0)
            .max(80.0)
            .step(1.0)
            .build()));
    private int cooldown;

    public AutoRenameModule() {
        super("auto_rename", "Auto Rename", ModuleCategory.PLAYER);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null || !(player.containerMenu instanceof AnvilMenu menu)) {
            this.cooldown = 0;
            return;
        }
        if (this.cooldown > 0) {
            this.cooldown--;
            return;
        }
        if (menu.getSlot(AnvilMenu.RESULT_SLOT).hasItem() && player.experienceLevel >= 1) {
            ContainerActions.quickMove(client, menu, AnvilMenu.RESULT_SLOT);
            this.cooldown = this.delayTicks.value().intValue();
            return;
        }
        if (menu.getSlot(AnvilMenu.INPUT_SLOT).hasItem()) {
            String targetName = targetName(menu.getSlot(AnvilMenu.INPUT_SLOT).getItem(), this.name.value(),
                    this.labelShulkers.value(), this.labelBundles.value());
            if (!targetName.isBlank()
                    && !targetName.equals(menu.getSlot(AnvilMenu.INPUT_SLOT).getItem().getHoverName().getString())) {
                if (menu.setItemName(targetName) && player.connection != null) {
                    player.connection.send(new ServerboundRenameItemPacket(targetName));
                    this.cooldown = this.delayTicks.value().intValue();
                }
            }
            return;
        }
        Set<Item> targets = ItemScan.parseItems(this.items.value());
        for (int slot = 3; slot < menu.slots.size(); slot++) {
            ItemStack stack = menu.getSlot(slot).getItem();
            if (!stack.isEmpty() && shouldRename(stack, targets, this.name.value(), this.labelShulkers.value(), this.labelBundles.value())) {
                ContainerActions.quickMove(client, menu, slot);
                this.cooldown = this.delayTicks.value().intValue();
                return;
            }
        }
    }

    static boolean shouldRename(final ItemStack stack, final Set<Item> targets, final String name,
                                final boolean labelShulkers, final boolean labelBundles) {
        if (stack.isEmpty()) {
            return false;
        }
        if ((labelShulkers || labelBundles) && !targetName(stack, name, labelShulkers, labelBundles).isBlank()
                && !stack.has(DataComponents.CUSTOM_NAME)) {
            return true;
        }
        return !name.isBlank() && targets.contains(stack.getItem()) && !name.equals(stack.getHoverName().getString());
    }

    static String targetName(final ItemStack stack, final String configuredName,
                             final boolean labelShulkers, final boolean labelBundles) {
        if (labelShulkers) {
            ItemContainerContents container = stack.get(DataComponents.CONTAINER);
            if (container != null) {
                for (ItemStackTemplate item : container.nonEmptyItems()) {
                    return item.create().getHoverName().getString();
                }
            }
        }
        if (labelBundles) {
            BundleContents bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
            if (bundle != null) {
                for (ItemStackTemplate item : bundle.items()) {
                    return item.create().getHoverName().getString();
                }
            }
        }
        return configuredName == null ? "" : configuredName;
    }
}
