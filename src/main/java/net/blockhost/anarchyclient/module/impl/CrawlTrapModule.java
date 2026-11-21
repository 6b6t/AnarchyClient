package net.blockhost.anarchyclient.module.impl;

import java.util.ArrayList;
import java.util.List;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

public final class CrawlTrapModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(5.0)
            .min(2.0)
            .max(6.0)
            .step(0.5)
            .build()));
    private final NumberSetting blocksPerTick = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("blocks_per_tick")
            .name("Blocks")
            .defaultValue(2.0)
            .min(1.0)
            .max(6.0)
            .step(1.0)
            .build()));
    private final BooleanSetting onlyCrawling = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("only_crawling")
            .name("Only Crawling")
            .defaultValue(true)
            .description("Waits until the target is already crawling. Off also traps standing targets at head height.")
            .build()));
    private final BooleanSetting sides = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("sides")
            .name("Sides")
            .defaultValue(true)
            .description("Also fills the open blocks beside the target so they cannot crawl away.")
            .build()));
    private final BooleanSetting rotate = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("rotate")
            .name("Rotate")
            .defaultValue(true)
            .build()));

    public CrawlTrapModule() {
        super(
                "crawl_trap",
                "Crawl Trap",
                ModuleCategory.COMBAT,
                "Fills the block above a crawling enemy, and optionally the blocks beside them, so they stay stuck "
                        + "in the crawling pose. Pairs well with Piston Push.");
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }
        Player target = CombatTargets.nearestEnemy(client, this.range.value() + 1.0);
        if (target == null) {
            return;
        }
        boolean crawling = isCrawling(target);
        if (this.onlyCrawling.value() && !crawling) {
            this.debugValue("target", "standing");
            return;
        }
        List<BlockPos> positions = trapPositions(target.blockPosition(), crawling, this.sides.value());
        int placed = CombatPlacementPlanner.placeBatch(
                client,
                this,
                positions,
                CombatPlacementPlanner.HARD_BLOCKS,
                CombatPlacementPlanner.Options.of(
                        this.blocksPerTick.value().intValue(), this.rotate.value(), this.range.value()));
        this.debugValue("placed", placed);
    }

    static boolean isCrawling(final Player player) {
        return player.getPose() == Pose.SWIMMING && !player.isInWater();
    }

    /**
     * Returns the blocks that keep a target in the crawl pose. A crawling target only fills the
     * bottom cell, so the cell above is the roof; a standing target gets the roof one block higher.
     */
    static List<BlockPos> trapPositions(final BlockPos feet, final boolean crawling, final boolean sides) {
        List<BlockPos> positions = new ArrayList<>();
        positions.add(crawling ? feet.above() : feet.above(2));
        if (sides) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                positions.add(feet.relative(direction));
            }
        }
        return positions;
    }
}
