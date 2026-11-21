package net.blockhost.anarchyclient.module.impl;

import java.util.List;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.placement.BlockPlacer;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class EChestFarmerModule extends Module {

    private final NumberSetting delayTicks = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay_ticks")
            .name("Delay")
            .defaultValue(5.0)
            .min(0.0)
            .max(40.0)
            .step(1.0)
            .build()));
    private final SingleBlockMiner miner = new SingleBlockMiner(this);
    private @Nullable BlockPos target;
    private int cooldown;

    public EChestFarmerModule() {
        super(
                "echest_farmer",
                "EChest Farmer",
                ModuleCategory.WORLD,
                "Places an ender chest in front of you and mines it with a non-Silk Touch pickaxe to farm obsidian.");
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            this.miner.stop(client);
            return;
        }
        if (this.cooldown > 0) {
            this.cooldown--;
        }
        if (this.target == null || !CombatTargets.canReachBlock(player, this.target)) {
            this.miner.stop(client);
            this.target = chooseTarget(client.level, player.blockPosition(), player.getDirection());
            if (this.target == null) {
                this.debugValue("status", "no free spot next to you");
                return;
            }
        }
        BlockPos pos = this.target;
        BlockState state = client.level.getBlockState(pos);
        if (state.is(Blocks.ENDER_CHEST)) {
            if (!WorldInteraction.hasHotbarItem(player.getInventory(), EChestFarmerModule::isFarmingPickaxe)) {
                this.miner.stop(client);
                return;
            }
            if (!this.miner.continueCurrent(client, pos::equals, EChestFarmerModule::isFarmingPickaxe)) {
                this.miner.start(client, pos, Direction.UP, EChestFarmerModule::isFarmingPickaxe);
            }
            if (!client.level.getBlockState(pos).is(Blocks.ENDER_CHEST)) {
                this.cooldown = this.delayTicks.value().intValue();
            }
            return;
        }
        if (!state.canBeReplaced()) {
            // Something else took the spot; choose another one next tick.
            this.target = null;
            return;
        }
        if (this.cooldown <= 0) {
            BlockPlacement.place(
                    client,
                    this,
                    pos,
                    true,
                    70.0F,
                    stack -> stack.is(Items.ENDER_CHEST),
                    BlockPlacer.PlacementOptions.NON_FULL);
        }
    }

    @Override
    protected void onDisable() {
        this.miner.stop(Minecraft.getInstance());
        this.target = null;
        this.cooldown = 0;
    }

    /**
     * Picks the spot to farm at: an ender chest already beside the player, else a free spot, trying the facing
     * direction first and then the other sides.
     */
    static BlockPos chooseTarget(final BlockGetter level, final BlockPos feet, final Direction facing) {
        List<Direction> directions =
                List.of(facing, facing.getClockWise(), facing.getCounterClockWise(), facing.getOpposite());
        for (Direction direction : directions) {
            BlockPos pos = feet.relative(direction);
            if (level.getBlockState(pos).is(Blocks.ENDER_CHEST)) {
                return pos.immutable();
            }
        }
        for (Direction direction : directions) {
            BlockPos pos = feet.relative(direction);
            if (level.getBlockState(pos).canBeReplaced()
                    && !level.getBlockState(pos.below()).canBeReplaced()) {
                return pos.immutable();
            }
        }
        return null;
    }

    /** A pickaxe that drops obsidian from an ender chest; Silk Touch would drop the chest itself. */
    static boolean isFarmingPickaxe(final ItemStack stack) {
        return stack.is(ItemTags.PICKAXES)
                && stack.getEnchantments().keySet().stream()
                        .noneMatch(enchantment -> enchantment.is(Enchantments.SILK_TOUCH));
    }
}
