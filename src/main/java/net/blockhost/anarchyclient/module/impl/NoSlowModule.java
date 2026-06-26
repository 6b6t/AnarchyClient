package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.mixin.ClientInputAccessor;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class NoSlowModule extends Module {

    private static NoSlowModule active;

    private final BooleanSetting consume = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("consume")
            .name("Consume")
            .defaultValue(true)
            .build()));
    private final BooleanSetting bows = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("bows")
            .name("Bows")
            .defaultValue(true)
            .build()));
    private final BooleanSetting shields = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("shields")
            .name("Shields")
            .defaultValue(true)
            .build()));
    private final BooleanSetting tridents = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("tridents")
            .name("Tridents")
            .defaultValue(true)
            .build()));
    private final BooleanSetting utilityItems = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("utility_items")
            .name("Utilities")
            .defaultValue(false)
            .build()));
    private final BooleanSetting cobweb = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("cobweb")
            .name("Cobweb")
            .defaultValue(false)
            .build()));
    private final BooleanSetting honey = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("honey")
            .name("Honey")
            .defaultValue(true)
            .build()));
    private final BooleanSetting soulSand = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("soul_sand")
            .name("Soul Sand")
            .defaultValue(true)
            .build()));
    private final BooleanSetting slime = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("slime")
            .name("Slime")
            .defaultValue(true)
            .build()));
    private final BooleanSetting berryBush = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("berry_bush")
            .name("Berry Bush")
            .defaultValue(true)
            .build()));
    private final BooleanSetting powderSnow = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("powder_snow")
            .name("Powder Snow")
            .defaultValue(true)
            .build()));

    public NoSlowModule() {
        super("no_slow", "No Slow", ModuleCategory.MOVEMENT);
    }

    @Override
    protected void onEnable() {
        active = this;
    }

    @Override
    protected void onDisable() {
        if (active == this) {
            active = null;
        }
    }

    @Override
    public void updateInput(final Minecraft client, final ClientInput input) {
        LocalPlayer player = client.player;
        if (player == null || input == null || !player.isUsingItem() || !this.shouldBypass(player.getUseItem())) {
            return;
        }
        ((ClientInputAccessor) input).anarchyclient$setMoveVector(InputStates.moveVector(input.keyPresses));
    }

    public static boolean shouldIgnoreStuckBlock(final BlockState state) {
        NoSlowModule module = active;
        return module != null && module.matchesStuckBlock(state);
    }

    private boolean shouldBypass(final ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return switch (stack.getUseAnimation()) {
            case EAT, DRINK -> this.consume.value();
            case BOW, CROSSBOW -> this.bows.value();
            case BLOCK -> this.shields.value();
            case TRIDENT, SPEAR -> this.tridents.value();
            case SPYGLASS, TOOT_HORN, BRUSH, BUNDLE -> this.utilityItems.value();
            default -> false;
        };
    }

    private boolean matchesStuckBlock(final BlockState state) {
        return this.cobweb.value() && state.is(Blocks.COBWEB)
                || this.honey.value() && state.is(Blocks.HONEY_BLOCK)
                || this.soulSand.value() && state.is(Blocks.SOUL_SAND)
                || this.slime.value() && state.is(Blocks.SLIME_BLOCK)
                || this.berryBush.value() && state.is(Blocks.SWEET_BERRY_BUSH)
                || this.powderSnow.value() && state.is(Blocks.POWDER_SNOW);
    }
}
