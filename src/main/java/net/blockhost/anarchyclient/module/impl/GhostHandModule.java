package net.blockhost.anarchyclient.module.impl;

import java.util.List;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BlockListSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;

public final class GhostHandModule extends Module {

    private final BlockListSetting passThrough = this.setting(BlockListSetting.from(BlockListSetting.builder()
            .id("pass_through")
            .name("Pass Through")
            .addAllDefaultValue(List.of(Blocks.SHORT_GRASS, Blocks.TALL_GRASS, Blocks.VINE, Blocks.SNOW))
            .build()));

    public GhostHandModule() {
        super(
                "ghost_hand",
                "Ghost Hand",
                ModuleCategory.PLAYER,
                "Cancels right-clicks on grass, vines, snow, and other listed blocks.");
    }

    @Override
    public boolean blockInteract(final Minecraft client, final InteractionHand hand, final BlockHitResult hitResult) {
        return client.level != null
                && this.passThrough
                        .value()
                        .contains(client.level
                                .getBlockState(hitResult.getBlockPos())
                                .getBlock());
    }
}
