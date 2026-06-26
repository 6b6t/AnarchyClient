package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BlockListSetting;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.EntityTypeListSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.blockhost.anarchyclient.setting.SettingGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

import java.util.List;

public final class NoInteractModule extends Module {

    private static final List<String> LIST_MODES = List.of("All", "Listed", "Unlisted");
    private static final List<String> HAND_MODES = List.of("Both", "Main Hand", "Off Hand");
    private static final List<String> INTERACT_MODES = List.of("None", "Interact", "Hit", "Both");
    private static final List<Block> DEFAULT_BLOCKS = List.of(
            Blocks.CHEST,
            Blocks.TRAPPED_CHEST,
            Blocks.ENDER_CHEST,
            Blocks.BARREL,
            Blocks.SHULKER_BOX,
            Blocks.CRAFTING_TABLE,
            Blocks.FURNACE,
            Blocks.ANVIL
    );
    private static final List<EntityType<?>> DEFAULT_ENTITIES = List.of(
            EntityTypes.ARMOR_STAND,
            EntityTypes.ITEM_FRAME,
            EntityTypes.GLOW_ITEM_FRAME,
            EntityTypes.VILLAGER
    );

    private final SettingGroup blocksGroup = this.settingGroup("blocks", "Blocks");
    private final SettingGroup entitiesGroup = this.settingGroup("entities", "Entities");

    private final BooleanSetting blocks = this.setting(this.blocksGroup, BooleanSetting.from(BooleanSetting.builder()
            .id("blocks")
            .name("Block Use")
            .defaultValue(true)
            .build()));
    private final SelectSetting blockMode = this.setting(this.blocksGroup, SelectSetting.from(SelectSetting.builder()
            .id("block_mode")
            .name("Use Mode")
            .defaultValue("All")
            .addAllOptions(LIST_MODES)
            .build()));
    private final BlockListSetting blockList = this.setting(this.blocksGroup, BlockListSetting.from(BlockListSetting.builder()
            .id("block_list")
            .name("Use Blocks")
            .addAllDefaultValue(DEFAULT_BLOCKS)
            .build()));
    private final SelectSetting blockHand = this.setting(this.blocksGroup, SelectSetting.from(SelectSetting.builder()
            .id("block_hand")
            .name("Use Hand")
            .defaultValue("Both")
            .addAllOptions(HAND_MODES)
            .build()));
    private final BooleanSetting mineBlocks = this.setting(this.blocksGroup, BooleanSetting.from(BooleanSetting.builder()
            .id("mine_blocks")
            .name("Block Mine")
            .defaultValue(false)
            .build()));
    private final SelectSetting mineMode = this.setting(this.blocksGroup, SelectSetting.from(SelectSetting.builder()
            .id("mine_mode")
            .name("Mine Mode")
            .defaultValue("Listed")
            .addAllOptions(LIST_MODES)
            .build()));
    private final BlockListSetting mineList = this.setting(this.blocksGroup, BlockListSetting.from(BlockListSetting.builder()
            .id("mine_list")
            .name("Mine Blocks")
            .build()));
    private final BooleanSetting items = this.setting(this.blocksGroup, BooleanSetting.from(BooleanSetting.builder()
            .id("items")
            .name("Item Use")
            .defaultValue(false)
            .build()));

    private final BooleanSetting entities = this.setting(this.entitiesGroup, BooleanSetting.from(BooleanSetting.builder()
            .id("entities")
            .name("Entity Use")
            .defaultValue(true)
            .build()));
    private final SelectSetting entityMode = this.setting(this.entitiesGroup, SelectSetting.from(SelectSetting.builder()
            .id("entity_mode")
            .name("Use Mode")
            .defaultValue("All")
            .addAllOptions(LIST_MODES)
            .build()));
    private final EntityTypeListSetting entityTypes = this.setting(this.entitiesGroup, EntityTypeListSetting.from(EntityTypeListSetting.builder()
            .id("entity_types")
            .name("Use Entities")
            .addAllDefaultValue(DEFAULT_ENTITIES)
            .build()));
    private final SelectSetting entityHand = this.setting(this.entitiesGroup, SelectSetting.from(SelectSetting.builder()
            .id("entity_hand")
            .name("Use Hand")
            .defaultValue("Both")
            .addAllOptions(HAND_MODES)
            .build()));
    private final BooleanSetting attackEntities = this.setting(this.entitiesGroup, BooleanSetting.from(BooleanSetting.builder()
            .id("attack_entities")
            .name("Entity Hit")
            .defaultValue(false)
            .build()));
    private final SelectSetting attackEntityMode = this.setting(this.entitiesGroup, SelectSetting.from(SelectSetting.builder()
            .id("attack_entity_mode")
            .name("Hit Mode")
            .defaultValue("Listed")
            .addAllOptions(LIST_MODES)
            .build()));
    private final EntityTypeListSetting attackEntityTypes = this.setting(this.entitiesGroup, EntityTypeListSetting.from(EntityTypeListSetting.builder()
            .id("attack_entity_types")
            .name("Hit Entities")
            .build()));
    private final SelectSetting friends = this.setting(this.entitiesGroup, SelectSetting.from(SelectSetting.builder()
            .id("friends")
            .name("Friends")
            .defaultValue("None")
            .addAllOptions(INTERACT_MODES)
            .build()));
    private final SelectSetting babies = this.setting(this.entitiesGroup, SelectSetting.from(SelectSetting.builder()
            .id("babies")
            .name("Babies")
            .defaultValue("None")
            .addAllOptions(INTERACT_MODES)
            .build()));
    private final SelectSetting nametagged = this.setting(this.entitiesGroup, SelectSetting.from(SelectSetting.builder()
            .id("nametagged")
            .name("Nametagged")
            .defaultValue("None")
            .addAllOptions(INTERACT_MODES)
            .build()));

    public NoInteractModule() {
        super("no_interact", "No Interact", ModuleCategory.PLAYER);
        this.blockList.visibleWhen(() -> !"All".equals(this.blockMode.value()));
        this.mineMode.visibleWhen(this.mineBlocks::value);
        this.mineList.visibleWhen(() -> this.mineBlocks.value() && !"All".equals(this.mineMode.value()));
        this.entityTypes.visibleWhen(() -> !"All".equals(this.entityMode.value()));
        this.attackEntityMode.visibleWhen(this.attackEntities::value);
        this.attackEntityTypes.visibleWhen(() -> this.attackEntities.value() && !"All".equals(this.attackEntityMode.value()));
    }

    @Override
    public boolean attackBlock(final Minecraft client, final BlockPos pos, final Direction direction) {
        return this.mineBlocks.value()
                && client.level != null
                && matchesList(this.mineMode.value(), this.mineList.value(), client.level.getBlockState(pos).getBlock());
    }

    @Override
    public boolean attackEntity(final Minecraft client, final Player player, final Entity target) {
        return this.attackEntities.value()
                && shouldCancelEntity(target, "Hit", this.attackEntityMode.value(), this.attackEntityTypes.value(),
                this.friends.value(), this.babies.value(), this.nametagged.value());
    }

    @Override
    public boolean blockInteract(final Minecraft client, final InteractionHand hand, final BlockHitResult hitResult) {
        return this.blocks.value()
                && client.level != null
                && matchesHand(this.blockHand.value(), hand)
                && matchesList(this.blockMode.value(), this.blockList.value(),
                client.level.getBlockState(hitResult.getBlockPos()).getBlock());
    }

    @Override
    public boolean entityInteract(final Minecraft client, final Player player, final Entity entity,
                                  final EntityHitResult hitResult, final InteractionHand hand) {
        return this.entities.value()
                && matchesHand(this.entityHand.value(), hand)
                && shouldCancelEntity(entity, "Interact", this.entityMode.value(), this.entityTypes.value(),
                this.friends.value(), this.babies.value(), this.nametagged.value());
    }

    @Override
    public boolean itemUse(final Minecraft client, final InteractionHand hand) {
        return this.items.value();
    }

    static boolean matchesHand(final String mode, final InteractionHand hand) {
        return switch (mode) {
            case "Main Hand" -> hand == InteractionHand.MAIN_HAND;
            case "Off Hand" -> hand == InteractionHand.OFF_HAND;
            default -> true;
        };
    }

    static <T> boolean matchesList(final String mode, final List<T> values, final T value) {
        return switch (mode) {
            case "Listed" -> values.contains(value);
            case "Unlisted" -> !values.contains(value);
            default -> true;
        };
    }

    static boolean matchesInteractMode(final String mode, final String action) {
        return "Both".equals(mode) || action.equals(mode);
    }

    private static boolean shouldCancelEntity(final Entity entity, final String action, final String listMode,
                                              final List<EntityType<?>> entityTypes, final String friendMode,
                                              final String babyMode, final String nametaggedMode) {
        return matchesList(listMode, entityTypes, entity.getType())
                || matchesInteractMode(friendMode, action)
                && entity instanceof Player
                && AnarchyClient.FRIENDS.isFriend(entity.getScoreboardName())
                || matchesInteractMode(babyMode, action)
                && entity instanceof Animal animal
                && animal.isBaby()
                || matchesInteractMode(nametaggedMode, action)
                && entity.hasCustomName();
    }
}
