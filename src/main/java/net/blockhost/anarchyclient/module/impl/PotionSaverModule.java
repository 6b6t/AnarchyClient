package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

import java.util.function.Function;

public final class PotionSaverModule extends Module {

    public PotionSaverModule() {
        super("potion_saver", "Potion Saver", ModuleCategory.PLAYER,
                "Blocks drinking a potion when you already have all of its effects at the same or a higher level and duration.");
    }

    @Override
    public boolean itemUse(final Minecraft client, final InteractionHand hand) {
        LocalPlayer player = client.player;
        if (player == null) {
            return false;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(Items.POTION)) {
            return false;
        }
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        return contents != null && wouldWaste(contents, player::getEffect);
    }

    /**
     * Returns whether drinking the potion would add nothing, because every effect it gives is already
     * active at the same or a higher amplifier with at least as much time left.
     */
    static boolean wouldWaste(final PotionContents contents,
                              final Function<Holder<MobEffect>, MobEffectInstance> activeEffects) {
        if (!contents.hasEffects()) {
            return false;
        }
        for (MobEffectInstance effect : contents.getAllEffects()) {
            if (effect.getEffect().value().isInstantaneous()) {
                return false;
            }
            MobEffectInstance active = activeEffects.apply(effect.getEffect());
            if (active == null || active.getAmplifier() < effect.getAmplifier() || !lastsAtLeast(active, effect)) {
                return false;
            }
        }
        return true;
    }

    private static boolean lastsAtLeast(final MobEffectInstance active, final MobEffectInstance effect) {
        if (effect.isInfiniteDuration()) {
            return active.isInfiniteDuration();
        }
        return !active.endsWithin(effect.getDuration() - 1);
    }
}
