package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

import java.util.List;

public final class AutoWebModule extends Module {

    private final SelectSetting targetMode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("target")
            .name("Target")
            .defaultValue("Enemy")
            .addAllOptions(List.of("Enemy", "Self"))
            .build()));
    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(4.5)
            .min(1.0)
            .max(6.0)
            .step(0.5)
            .build()));
    private final BooleanSetting rotate = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("rotate")
            .name("Rotate")
            .defaultValue(true)
            .build()));

    public AutoWebModule() {
        super("auto_web", "Auto Web", ModuleCategory.COMBAT);
        this.range.visibleWhen(() -> "Enemy".equals(this.targetMode.value()));
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }
        Player target = "Self".equals(this.targetMode.value()) ? player : nearestPlayer(client, player, this.range.value());
        if (target == null) {
            return;
        }
        for (BlockPos pos : List.of(target.blockPosition(), target.blockPosition().above())) {
            if (BlockPlacement.place(client, this, pos, this.rotate.value(), 70.0F,
                    stack -> stack.is(Items.COBWEB)) == BlockPlacement.PlacementResult.PLACED) {
                return;
            }
        }
    }

    private static Player nearestPlayer(final Minecraft client, final LocalPlayer player, final double range) {
        double rangeSqr = range * range;
        Player best = null;
        double bestDistance = Double.MAX_VALUE;
        for (net.minecraft.world.entity.Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof Player target) || target == player || !target.isAlive()) {
                continue;
            }
            double distance = target.distanceToSqr(player);
            if (distance <= rangeSqr && distance < bestDistance) {
                best = target;
                bestDistance = distance;
            }
        }
        return best;
    }
}
