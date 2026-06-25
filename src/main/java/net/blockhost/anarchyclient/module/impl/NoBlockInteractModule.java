package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BlockListSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

public final class NoBlockInteractModule extends Module {

    private final BlockListSetting blocks = this.setting(BlockListSetting.from(BlockListSetting.builder()
            .id("blocks")
            .name("Blocks")
            .defaultValue(List.of(
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
        return client.level != null && this.blocks.value().contains(client.level.getBlockState(hitResult.getBlockPos()).getBlock());
    }
}
