package net.blockhost.anarchyclient.module.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MinecraftBootstrapExtension.class)
class PotionSaverModuleTest {

    @Test
    void blocksPotionWhoseEffectIsAlreadyActiveForLonger() {
        Map<Holder<MobEffect>, MobEffectInstance> active =
                Map.of(MobEffects.SPEED, new MobEffectInstance(MobEffects.SPEED, 9600));

        assertTrue(PotionSaverModule.wouldWaste(new PotionContents(Potions.SWIFTNESS), active::get));
    }

    @Test
    void allowsPotionThatExtendsOrStrengthensTheEffect() {
        Map<Holder<MobEffect>, MobEffectInstance> shortSpeed =
                Map.of(MobEffects.SPEED, new MobEffectInstance(MobEffects.SPEED, 100));

        assertFalse(PotionSaverModule.wouldWaste(new PotionContents(Potions.SWIFTNESS), shortSpeed::get));
        assertFalse(PotionSaverModule.wouldWaste(new PotionContents(Potions.STRONG_SWIFTNESS), shortSpeed::get));
    }

    @Test
    void allowsPotionWithMissingEffect() {
        assertFalse(PotionSaverModule.wouldWaste(new PotionContents(Potions.SWIFTNESS), effect -> null));
    }

    @Test
    void allowsWaterAndInstantPotions() {
        assertFalse(PotionSaverModule.wouldWaste(new PotionContents(Potions.WATER), effect -> null));
        assertFalse(PotionSaverModule.wouldWaste(new PotionContents(Potions.HEALING), effect -> null));
    }
}
