package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.HitResult;

public final class AirPlaceModule extends Module {

    private final NumberSetting distance = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("distance")
            .name("Distance")
            .defaultValue(4.0)
            .min(1.0)
            .max(6.0)
            .step(0.5)
            .build()));

    public AirPlaceModule() {
        super("air_place", "Air Place", ModuleCategory.WORLD);
    }

    @Override
    public boolean itemUse(final Minecraft client, final InteractionHand hand) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || hand != InteractionHand.MAIN_HAND
                || !(player.getMainHandItem().getItem() instanceof BlockItem)
                || client.hitResult != null && client.hitResult.getType() != HitResult.Type.MISS) {
            return false;
        }
        BlockPos target = BlockPos.containing(player.getEyePosition().add(player.getLookAngle().scale(this.distance.value())));
        BlockPlacement.PlacementResult result = BlockPlacement.place(client, this, target, true, 60.0F);
        return result == BlockPlacement.PlacementResult.PLACED || result == BlockPlacement.PlacementResult.FILLED;
    }
}
