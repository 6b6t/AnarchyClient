package net.blockhost.anarchyclient.module.impl;

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
        super("echest_farmer", "EChest Farmer", ModuleCategory.WORLD,
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
            this.target = player.blockPosition().relative(player.getDirection()).immutable();
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
            // Something else is in the spot; choose a new one next tick.
            this.target = null;
            return;
        }
        if (this.cooldown <= 0) {
            BlockPlacement.place(client, this, pos, true, 70.0F, stack -> stack.is(Items.ENDER_CHEST),
                    BlockPlacer.PlacementOptions.NON_FULL);
        }
    }

    @Override
    protected void onDisable() {
        this.miner.stop(Minecraft.getInstance());
        this.target = null;
        this.cooldown = 0;
    }

    /** A pickaxe that drops obsidian from an ender chest; Silk Touch would drop the chest itself. */
    static boolean isFarmingPickaxe(final ItemStack stack) {
        return stack.is(ItemTags.PICKAXES)
                && stack.getEnchantments().keySet().stream().noneMatch(enchantment -> enchantment.is(Enchantments.SILK_TOUCH));
    }
}
