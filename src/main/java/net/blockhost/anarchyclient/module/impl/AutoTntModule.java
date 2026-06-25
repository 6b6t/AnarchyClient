package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.TntBlock;

public final class AutoTntModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(4.0)
            .min(1.0)
            .max(8.0)
            .step(1.0)
            .build()));
    private final NumberSetting verticalRange = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("vertical_range")
            .name("Vertical")
            .defaultValue(4.0)
            .min(1.0)
            .max(8.0)
            .step(1.0)
            .build()));
    private final NumberSetting delay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay")
            .name("Delay")
            .defaultValue(2.0)
            .min(0.0)
            .max(40.0)
            .step(1.0)
            .build()));
    private final BooleanSetting fireCharges = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("fire_charges")
            .name("Fire Charges")
            .defaultValue(true)
            .build()));
    private final BooleanSetting protectDurability = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("protect_durability")
            .name("Protect Durability")
            .defaultValue(true)
            .build()));
    private final NumberSetting minimumDurability = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("minimum_durability")
            .name("Min Durability")
            .defaultValue(10.0)
            .min(1.0)
            .max(63.0)
            .step(1.0)
            .build()));
    private final BooleanSetting rotate = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("rotate")
            .name("Rotate")
            .defaultValue(true)
            .build()));
    private int cooldownTicks;

    public AutoTntModule() {
        super("auto_tnt", "Auto TNT", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return;
        }

        int horizontal = this.range.value().intValue();
        int vertical = this.verticalRange.value().intValue();
        for (BlockTargetScanner.BlockTarget target : BlockTargetScanner.scan(
                client,
                horizontal,
                vertical,
                BlockTargetScanner.SortMode.CLOSEST,
                scanLimit(horizontal, vertical),
                candidate -> candidate.state().getBlock() instanceof TntBlock
        )) {
            WorldInteraction.ActionResult result = WorldInteraction.useOnBlock(
                    client,
                    this,
                    target.pos(),
                    Direction.UP,
                    this::isIgniter,
                    this.rotate.value()
            );
            if (result == WorldInteraction.ActionResult.DONE) {
                this.cooldownTicks = this.delay.value().intValue();
                return;
            }
            if (result == WorldInteraction.ActionResult.MISSING_ITEM) {
                return;
            }
        }
    }

    @Override
    protected void onDisable() {
        this.cooldownTicks = 0;
    }

    private boolean isIgniter(final ItemStack stack) {
        return isIgniter(stack, this.fireCharges.value(), this.protectDurability.value(),
                this.minimumDurability.value().intValue());
    }

    static boolean isIgniter(final ItemStack stack, final boolean fireCharges, final boolean protectDurability,
                             final int minimumDurability) {
        return acceptsIgniter(
                stack.is(Items.FLINT_AND_STEEL),
                stack.is(Items.FIRE_CHARGE),
                fireCharges,
                protectDurability,
                stack.getMaxDamage(),
                stack.getDamageValue(),
                minimumDurability
        );
    }

    static boolean acceptsIgniter(final boolean flintAndSteel, final boolean fireCharge,
                                  final boolean fireCharges, final boolean protectDurability,
                                  final int maxDamage, final int damageValue, final int minimumDurability) {
        if (fireCharge) {
            return fireCharges;
        }
        if (!flintAndSteel) {
            return false;
        }
        return !protectDurability || hasEnoughDurability(maxDamage, damageValue, minimumDurability);
    }

    static boolean hasEnoughDurability(final int maxDamage, final int damageValue, final int minimumDurability) {
        return maxDamage <= 0 || remainingDurability(maxDamage, damageValue) > Math.max(0, minimumDurability);
    }

    static int remainingDurability(final int maxDamage, final int damageValue) {
        return Math.max(0, maxDamage - Math.max(0, damageValue));
    }

    static int scanLimit(final int horizontalRadius, final int verticalRadius) {
        long width = (long) Math.max(0, horizontalRadius) * 2L + 1L;
        long height = (long) Math.max(0, verticalRadius) * 2L + 1L;
        return (int) Math.min(2_048L, width * width * height);
    }
}
