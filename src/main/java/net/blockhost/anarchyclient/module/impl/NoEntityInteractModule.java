package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.EntityTypeListSetting;
import net.blockhost.anarchyclient.setting.ItemListSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;

import java.util.List;

public final class NoEntityInteractModule extends Module {

    private final SelectSetting entityMode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("entity_mode")
            .name("Entities")
            .defaultValue("Listed")
            .addAllOptions(List.of("Listed", "Unlisted"))
            .build()));
    private final SelectSetting hand = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("hand")
            .name("Hand")
            .defaultValue("Both")
            .addAllOptions(List.of("Both", "Main Hand", "Off Hand"))
            .build()));
    private final SelectSetting heldItemMode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("held_item_mode")
            .name("Held Items")
            .defaultValue("Any")
            .addAllOptions(List.of("Any", "Listed", "Unlisted"))
            .build()));
    private final ItemListSetting heldItems = this.setting(ItemListSetting.from(ItemListSetting.builder()
            .id("held_items")
            .name("Held List")
            .addAllDefaultValue(List.of(Items.SHEARS, Items.TNT, Items.WATER_BUCKET, Items.LAVA_BUCKET, Items.COBWEB))
            .build()));
    private final EntityTypeListSetting entityTypes = this.setting(EntityTypeListSetting.from(EntityTypeListSetting.builder()
            .id("entity_types")
            .name("Entities")
            .addAllDefaultValue(List.of(EntityTypes.ARMOR_STAND, EntityTypes.ITEM_FRAME, EntityTypes.GLOW_ITEM_FRAME))
            .build()));

    public NoEntityInteractModule() {
        super("no_entity_interact", "No Entity Interact", ModuleCategory.PLAYER);
        this.heldItems.visibleWhen(() -> !"Any".equals(this.heldItemMode.value()));
    }

    @Override
    public boolean entityInteract(final Minecraft client, final Player player, final Entity entity,
                                  final EntityHitResult hitResult, final InteractionHand hand) {
        return NoInteractModule.matchesHand(this.hand.value(), hand)
                && NoInteractModule.matchesList(this.entityMode.value(), this.entityTypes.value(), entity.getType())
                && ("Any".equals(this.heldItemMode.value())
                || NoInteractModule.matchesList(this.heldItemMode.value(), this.heldItems.value(), player.getItemInHand(hand).getItem()));
    }
}
