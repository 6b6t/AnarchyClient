package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;

public final class FlamethrowerModule extends Module {

    /** How long a block that refused the igniter is left alone before it is tried again. */
    private static final int REFUSED_COOLDOWN_TICKS = 40;

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(4.0)
            .min(1.0)
            .max(7.0)
            .step(1.0)
            .build()));
    private final NumberSetting actions = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("actions")
            .name("Actions")
            .defaultValue(1.0)
            .min(1.0)
            .max(4.0)
            .step(1.0)
            .build()));
    private final Map<BlockPos, Integer> refused = new HashMap<>();

    public FlamethrowerModule() {
        super("flamethrower", "Flamethrower", ModuleCategory.WORLD,
                "Sets fire to the tops of nearby blocks with flint and steel or fire charges, never right next to you.");
    }

    @Override
    protected void onDisable() {
        this.refused.clear();
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }
        this.refused.entrySet().removeIf(entry -> {
            entry.setValue(entry.getValue() - 1);
            return entry.getValue() <= 0;
        });
        LocalPlayer player = client.player;
        // Keep the fire at least a block away from the player so it never lands at their feet.
        AABB safeZone = player.getBoundingBox().inflate(1.0, 0.0, 1.0);
        int used = 0;
        for (BlockTargetScanner.BlockTarget target : BlockTargetScanner.scan(
                client,
                this.range.value().intValue(),
                this.range.value().intValue(),
                BlockTargetScanner.SortMode.CLOSEST,
                32,
                candidate -> !candidate.state().isAir() && client.level.isEmptyBlock(candidate.pos().above())
                        && !this.refused.containsKey(candidate.pos())
                        && !safeZone.intersects(new AABB(candidate.pos().above()))
                        && CombatTargets.canReachBlock(player, candidate.pos())
        )) {
            WorldInteraction.ActionResult result = WorldInteraction.useOnBlock(client, this, target.pos(), Direction.UP,
                    stack -> stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE), true);
            if (result == WorldInteraction.ActionResult.REFUSED) {
                // Fire cannot go here (water, snow, a torch); leave the block alone for a while.
                this.refused.put(target.pos(), REFUSED_COOLDOWN_TICKS);
                continue;
            }
            // Waiting means the rotation has not reached the server yet; retry the same block next tick.
            if (result != WorldInteraction.ActionResult.DONE || ++used >= this.actions.value().intValue()) {
                return;
            }
        }
    }
}
