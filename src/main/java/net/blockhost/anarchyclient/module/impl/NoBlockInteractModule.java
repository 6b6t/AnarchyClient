package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BlockListSetting;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

public final class NoBlockInteractModule extends Module {

    private final SelectSetting mode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("mode")
            .name("Mode")
            .defaultValue("Listed")
            .addAllOptions(List.of("Listed", "Unlisted"))
            .build()));
    private final SelectSetting hand = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("hand")
            .name("Hand")
            .defaultValue("Both")
            .addAllOptions(List.of("Both", "Main Hand", "Off Hand"))
            .build()));
    private final BooleanSetting requirePlaceable = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("require_placeable")
            .name("Placeable Only")
            .defaultValue(false)
            .build()));
    private final BlockListSetting blocks = this.setting(BlockListSetting.from(BlockListSetting.builder()
            .id("blocks")
            .name("Blocks")
            .addAllDefaultValue(List.of(
                    Blocks.CHEST,
                    Blocks.TRAPPED_CHEST,
                    Blocks.ENDER_CHEST,
                    Blocks.BARREL,
                    Blocks.SHULKER_BOX,
                    Blocks.CRAFTING_TABLE,
                    Blocks.FURNACE,
                    Blocks.ANVIL
            ))
            .build()));

    public NoBlockInteractModule() {
        super("no_block_interact", "No Block Interact", ModuleCategory.PLAYER);
    }

    @Override
    public boolean blockInteract(final Minecraft client, final InteractionHand hand, final BlockHitResult hitResult) {
        return client.level != null
                && NoInteractModule.matchesHand(this.hand.value(), hand)
                && (!this.requirePlaceable.value() || client.player != null && AirPlaceModule.placeable(client.player.getItemInHand(hand)))
                && NoInteractModule.matchesList(
                this.mode.value(),
                this.blocks.value(),
                client.level.getBlockState(hitResult.getBlockPos()).getBlock()
        );
    }
}
