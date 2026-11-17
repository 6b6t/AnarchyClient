package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.phys.HitResult;

import java.util.OptionalInt;

public final class LavacastModule extends Module {

    private final NumberSetting interval = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("interval")
            .name("Interval")
            .defaultValue(12.0)
            .min(1.0)
            .max(100.0)
            .step(1.0)
            .build()));
    private int ticks;
    private boolean lava = true;

    public LavacastModule() {
        super("lavacast", "Lavacast", ModuleCategory.WORLD,
                "Alternates lava and water buckets on the block you look at to build a lavacast.");
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null || client.hitResult == null
                || client.hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }
        if (++this.ticks < this.interval.value().intValue()) {
            return;
        }
        this.ticks = 0;
        Item bucket = this.lava ? Items.LAVA_BUCKET : Items.WATER_BUCKET;
        OptionalInt slot = InventoryActions.findHotbarSlot(player.getInventory(), stack -> stack.is(bucket));
        if (slot.isEmpty()) {
            return;
        }
        InventoryActions.selectHotbarSlot(player, slot.getAsInt());
        // Buckets ignore the clicked block and pour where the player looks, so they need a plain item use.
        InteractionResult result = client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        if (result.consumesAction()) {
            player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
            this.lava = !this.lava;
        }
    }

    @Override
    protected void onDisable() {
        this.ticks = 0;
        this.lava = true;
    }
}
