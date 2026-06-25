package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;
import java.util.Random;

public final class AutoClickerModule extends Module {

    private final SelectSetting mode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("mode")
            .name("Mode")
            .defaultValue("Attack")
            .addAllOptions(List.of("Attack", "Use", "Both"))
            .build()));
    private final NumberSetting minCps = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("min_cps")
            .name("Min CPS")
            .defaultValue(8.0)
            .min(1.0)
            .max(30.0)
            .step(1.0)
            .build()));
    private final NumberSetting maxCps = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("max_cps")
            .name("Max CPS")
            .defaultValue(12.0)
            .min(1.0)
            .max(30.0)
            .step(1.0)
            .build()));
    private final BooleanSetting holdRequired = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("hold_required")
            .name("Hold Required")
            .defaultValue(true)
            .build()));
    private final BooleanSetting weaponsOnly = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("weapons_only")
            .name("Weapons Only")
            .defaultValue(true)
            .build()));
    private final BooleanSetting requireCooldown = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("require_cooldown")
            .name("Cooldown")
            .defaultValue(false)
            .build()));
    private final AttackCadence cadence = new AttackCadence(new Random());

    public AutoClickerModule() {
        super("auto_clicker", "Auto Clicker", ModuleCategory.COMBAT, List.of("trigger_bot"));
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null || client.gui.screen() != null) {
            this.cadence.clear();
            return;
        }
        if (!this.shouldClick(client, player) || !this.cadence.ready()) {
            return;
        }

        if (this.shouldAttack(client) && this.attack(client, player)) {
            this.cadence.reset(this.minCps.value(), this.maxCps.value());
            return;
        }
        if (this.shouldUse(client) && this.use(client, player)) {
            this.cadence.reset(this.minCps.value(), this.maxCps.value());
        }
    }

    private boolean shouldClick(final Minecraft client, final LocalPlayer player) {
        return (!this.weaponsOnly.value() || isWeapon(player.getMainHandItem()))
                && (!this.requireCooldown.value() || player.getAttackStrengthScale(0.0F) >= 1.0F)
                && (!this.holdRequired.value() || this.shouldAttack(client) && client.options.keyAttack.isDown()
                || this.shouldUse(client) && client.options.keyUse.isDown());
    }

    private boolean shouldAttack(final Minecraft client) {
        String value = this.mode.value();
        return ("Attack".equals(value) || "Both".equals(value))
                && (!this.holdRequired.value() || client.options.keyAttack.isDown());
    }

    private boolean shouldUse(final Minecraft client) {
        String value = this.mode.value();
        return ("Use".equals(value) || "Both".equals(value))
                && (!this.holdRequired.value() || client.options.keyUse.isDown());
    }

    private boolean attack(final Minecraft client, final LocalPlayer player) {
        if (client.hitResult instanceof EntityHitResult entityHit) {
            client.gameMode.attack(player, entityHit.getEntity());
            player.swing(InteractionHand.MAIN_HAND);
            return true;
        }
        if (client.hitResult instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
            boolean started = client.gameMode.startDestroyBlock(blockHit.getBlockPos(), blockHit.getDirection());
            if (started) {
                player.swing(InteractionHand.MAIN_HAND);
            }
            return started;
        }
        return false;
    }

    private boolean use(final Minecraft client, final LocalPlayer player) {
        if (client.hitResult instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
            if (client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, blockHit).consumesAction()) {
                player.swing(InteractionHand.MAIN_HAND);
                return true;
            }
        }
        if (client.gameMode.useItem(player, InteractionHand.MAIN_HAND).consumesAction()) {
            player.swing(InteractionHand.MAIN_HAND);
            return true;
        }
        return false;
    }

    static boolean isWeapon(final ItemStack stack) {
        return !stack.isEmpty()
                && (stack.has(net.minecraft.core.component.DataComponents.WEAPON)
                || stack.is(ItemTags.SWORDS)
                || stack.is(ItemTags.AXES));
    }

    static int delayTicks(final double minCps, final double maxCps) {
        double upper = Math.max(1.0, Math.max(minCps, maxCps));
        return Math.max(1, (int) Math.round(20.0 / upper));
    }
}
