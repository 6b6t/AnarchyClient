package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.phys.BlockHitResult;

public final class AntiBedModule extends Module {

    private final BooleanSetting breakBeds = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("break_beds")
            .name("Break Beds")
            .defaultValue(true)
            .build()));
    private final BooleanSetting placeStringTop = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("place_string_top")
            .name("String Top")
            .defaultValue(false)
            .build()));
    private final BooleanSetting placeStringMiddle = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("place_string_middle")
            .name("String Middle")
            .defaultValue(true)
            .build()));
    private final BooleanSetting placeStringBottom = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("place_string_bottom")
            .name("String Bottom")
            .defaultValue(false)
            .build()));
    private final BooleanSetting onlyInHole = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("only_in_hole")
            .name("Only In Hole")
            .defaultValue(true)
            .build()));
    private final BooleanSetting rotate = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("rotate")
            .name("Rotate")
            .defaultValue(true)
            .build()));

    public AntiBedModule() {
        super("anti_bed", "Anti Bed", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }
        BlockPos feet = client.player.blockPosition();
        BlockPos head = feet.above();
        if (this.breakBeds.value() && client.level.getBlockState(head).getBlock() instanceof BedBlock) {
            WorldInteraction.breakBlock(client, head, Direction.UP, stack -> true);
        }
        if (this.onlyInHole.value() && !isInHole(client, feet)) {
            return;
        }
        if (this.placeStringTop.value()) {
            this.placeString(client, feet.above(2));
        }
        if (this.placeStringMiddle.value()) {
            this.placeString(client, head);
        }
        if (this.placeStringBottom.value()) {
            this.placeString(client, feet);
        }
    }

    @Override
    public boolean blockInteract(final Minecraft client, final InteractionHand hand, final BlockHitResult hitResult) {
        return client.level != null && client.level.getBlockState(hitResult.getBlockPos()).getBlock() instanceof BedBlock;
    }

    @Override
    public boolean itemUse(final Minecraft client, final InteractionHand hand) {
        return client.player != null && client.player.getItemInHand(hand).getItem() instanceof BedItem;
    }

    private void placeString(final Minecraft client, final BlockPos pos) {
        if (client.level != null && client.level.getBlockState(pos).canBeReplaced()) {
            BlockPlacement.place(client, this, pos, this.rotate.value(), 60.0F, stack -> stack.is(Items.STRING));
        }
    }

    static boolean isInHole(final Minecraft client, final BlockPos feet) {
        if (client.level == null) {
            return false;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (!client.level.getBlockState(feet.relative(direction)).isSolid()) {
                return false;
            }
        }
        return client.level.getBlockState(feet.below()).isSolid();
    }
}
