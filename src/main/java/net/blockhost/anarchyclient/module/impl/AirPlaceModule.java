package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ArmorStandItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class AirPlaceModule extends Module {

    private final SelectSetting hand = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("hand")
            .name("Hand")
            .defaultValue("Main Hand")
            .addAllOptions(List.of("Main Hand", "Off Hand", "Both"))
            .build()));
    private final NumberSetting distance = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("distance")
            .name("Distance")
            .defaultValue(4.0)
            .min(1.0)
            .max(6.0)
            .step(0.5)
            .build()));
    private final BooleanSetting render = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("render")
            .name("Render")
            .defaultValue(true)
            .build()));

    public AirPlaceModule() {
        super("air_place", "Air Place", ModuleCategory.WORLD);
    }

    @Override
    public boolean itemUse(final Minecraft client, final InteractionHand hand) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || !matchesHand(this.hand.value(), hand)
                || !placeable(player.getItemInHand(hand))
                || client.hitResult != null && client.hitResult.getType() != HitResult.Type.MISS) {
            return false;
        }
        BlockPos target = targetPos(player, this.distance.value());
        if (player.getItemInHand(hand).getItem() instanceof BlockItem) {
            BlockPlacement.PlacementResult result = BlockPlacement.place(client, this, target, true, 60.0F);
            return result == BlockPlacement.PlacementResult.PLACED || result == BlockPlacement.PlacementResult.FILLED;
        }
        return usePlaceableItem(client, player, hand, target);
    }

    @Override
    public void renderWorld(final LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        PoseStack matrices = context.poseStack();
        SubmitNodeCollector submits = context.submitNodeCollector();
        if (!this.render.value() || player == null || client.level == null || matrices == null || submits == null
                || client.hitResult != null && client.hitResult.getType() != HitResult.Type.MISS
                || !hasPlaceableHand(player, this.hand.value())) {
            return;
        }
        BlockPos target = targetPos(player, this.distance.value());
        if (!client.level.getBlockState(target).canBeReplaced()) {
            return;
        }
        Vec3 camera = client.gameRenderer.mainCamera().position();
        AABB box = new AABB(target).move(camera.scale(-1));
        WorldLineRenderer.boxNoDepth(matrices, submits, box, new WorldLineRenderer.Color(142, 234, 213, 180));
    }

    private static boolean usePlaceableItem(final Minecraft client, final LocalPlayer player, final InteractionHand hand,
                                            final BlockPos target) {
        if (client.gameMode == null || client.level == null || !client.level.getBlockState(target).canBeReplaced()) {
            return false;
        }
        Direction face = player.getMotionDirection().getOpposite();
        Vec3 hit = Vec3.atCenterOf(target);
        InteractionResult result = client.gameMode.useItemOn(player, hand, new BlockHitResult(hit, face, target, false));
        if (result.consumesAction()) {
            player.swing(hand);
            return true;
        }
        return false;
    }

    private static BlockPos targetPos(final LocalPlayer player, final double distance) {
        return BlockPos.containing(player.getEyePosition().add(player.getLookAngle().scale(distance)));
    }

    private static boolean hasPlaceableHand(final LocalPlayer player, final String mode) {
        return matchesHand(mode, InteractionHand.MAIN_HAND) && placeable(player.getMainHandItem())
                || matchesHand(mode, InteractionHand.OFF_HAND) && placeable(player.getOffhandItem());
    }

    static boolean matchesHand(final String mode, final InteractionHand hand) {
        return switch (mode) {
            case "Off Hand" -> hand == InteractionHand.OFF_HAND;
            case "Both" -> true;
            default -> hand == InteractionHand.MAIN_HAND;
        };
    }

    static boolean placeable(final ItemStack stack) {
        return stack != null && placeableItem(stack.getItem());
    }

    static boolean placeableItem(final Item item) {
        return item instanceof BlockItem
                || item instanceof ArmorStandItem
                || item instanceof SpawnEggItem
                || item instanceof FireworkRocketItem;
    }
}
