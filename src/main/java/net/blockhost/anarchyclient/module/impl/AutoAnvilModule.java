package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

import java.util.List;

public final class AutoAnvilModule extends Module {

    private final SelectSetting targetMode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("target")
            .name("Target")
            .defaultValue("Enemy")
            .addAllOptions(List.of("Enemy", "Self"))
            .build()));
    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(5.0)
            .min(1.0)
            .max(8.0)
            .step(0.5)
            .build()));

    public AutoAnvilModule() {
        super("auto_anvil", "Auto Anvil", ModuleCategory.COMBAT);
        this.range.visibleWhen(() -> "Enemy".equals(this.targetMode.value()));
    }

    @Override
    public void tick(final Minecraft client) {
        Player target = "Self".equals(this.targetMode.value()) ? client.player : CombatTargets.nearestEnemy(client, this.range.value());
        if (target != null) {
            BlockPlacement.place(client, this, target.blockPosition().above(2), true, 70.0F,
                    stack -> stack.is(Items.ANVIL));
        }
    }
}
