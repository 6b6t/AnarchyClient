package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;

import java.util.List;
import java.util.Set;

public final class AutoCraftModule extends Module {

    private final StringSetting items = this.setting(StringSetting.from(StringSetting.builder()
            .id("items")
            .name("Items")
            .defaultValue("")
            .description("Comma-separated item ids.")
            .build()));
    private final BooleanSetting craftAll = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("craft_all")
            .name("Craft All")
            .defaultValue(false)
            .build()));
    private final BooleanSetting dropOutput = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("drop_output")
            .name("Drop")
            .defaultValue(false)
            .build()));
    private final NumberSetting delayTicks = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay_ticks")
            .name("Delay")
            .defaultValue(5.0)
            .min(0.0)
            .max(100.0)
            .step(1.0)
            .build()));
    private int cooldown;

    public AutoCraftModule() {
        super("auto_craft", "Auto Craft", ModuleCategory.PLAYER);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null || !(player.containerMenu instanceof CraftingMenu menu)) {
            this.cooldown = 0;
            return;
        }
        if (this.cooldown > 0) {
            this.cooldown--;
            return;
        }
        Set<Item> targets = ItemScan.parseItems(this.items.value());
        if (targets.isEmpty()) {
            return;
        }
        RecipeDisplayEntry recipe = findRecipe(client, player, targets);
        if (recipe == null) {
            return;
        }
        client.gameMode.handlePlaceRecipe(menu.containerId, recipe.id(), this.craftAll.value());
        if (this.dropOutput.value()) {
            ContainerActions.throwSlot(client, menu, CraftingMenu.RESULT_SLOT);
        } else {
            ContainerActions.quickMove(client, menu, CraftingMenu.RESULT_SLOT);
        }
        this.cooldown = this.delayTicks.value().intValue();
    }

    static RecipeDisplayEntry findRecipe(final Minecraft client, final LocalPlayer player, final Set<Item> targets) {
        if (client.level == null || targets.isEmpty()) {
            return null;
        }
        for (RecipeCollection collection : player.getRecipeBook().getCollections()) {
            List<RecipeDisplayEntry> recipes = collection.getSelectedRecipes(RecipeCollection.CraftableStatus.CRAFTABLE);
            for (RecipeDisplayEntry recipe : recipes) {
                List<ItemStack> results = recipe.display().result().resolveForStacks(SlotDisplayContext.fromLevel(client.level));
                for (ItemStack result : results) {
                    if (targets.contains(result.getItem())) {
                        return recipe;
                    }
                }
            }
        }
        return null;
    }
}
