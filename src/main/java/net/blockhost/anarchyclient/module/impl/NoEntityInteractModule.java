package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.EntityTypeListSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;

import java.util.List;

public final class NoEntityInteractModule extends Module {

    private final EntityTypeListSetting entityTypes = this.setting(EntityTypeListSetting.from(EntityTypeListSetting.builder()
            .id("entity_types")
            .name("Entities")
            .addAllDefaultValue(List.of(EntityTypes.ARMOR_STAND, EntityTypes.ITEM_FRAME, EntityTypes.GLOW_ITEM_FRAME))
            .build()));

    public NoEntityInteractModule() {
        super("no_entity_interact", "No Entity Interact", ModuleCategory.PLAYER);
    }

    @Override
    public boolean entityInteract(final Minecraft client, final Player player, final Entity entity,
                                  final EntityHitResult hitResult, final InteractionHand hand) {
        return this.entityTypes.value().contains(entity.getType());
    }
}
