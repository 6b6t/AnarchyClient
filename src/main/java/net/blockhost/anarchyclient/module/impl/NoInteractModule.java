package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

public final class NoInteractModule extends Module {

    private final BooleanSetting blocks = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("blocks")
            .name("Blocks")
            .defaultValue(true)
            .build()));
    private final BooleanSetting entities = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("entities")
            .name("Entities")
            .defaultValue(true)
            .build()));
    private final BooleanSetting items = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("items")
            .name("Items")
            .defaultValue(false)
            .build()));

    public NoInteractModule() {
        super("no_interact", "No Interact", ModuleCategory.PLAYER);
    }

    @Override
    public boolean blockInteract(final Minecraft client, final InteractionHand hand, final BlockHitResult hitResult) {
        return this.blocks.value();
    }

    @Override
    public boolean entityInteract(final Minecraft client, final Player player, final Entity entity,
                                  final EntityHitResult hitResult, final InteractionHand hand) {
        return this.entities.value();
    }

    @Override
    public boolean itemUse(final Minecraft client, final InteractionHand hand) {
        return this.items.value();
    }
}
