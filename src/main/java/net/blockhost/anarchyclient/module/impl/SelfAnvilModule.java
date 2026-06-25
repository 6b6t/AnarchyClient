package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;

public final class SelfAnvilModule extends Module {

    public SelfAnvilModule() {
        super("self_anvil", "Self Anvil", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player != null && client.level != null) {
            BlockPlacement.place(client, this, client.player.blockPosition().above(2), true, 70.0F,
                    stack -> stack.is(Items.ANVIL));
        }
    }
}
