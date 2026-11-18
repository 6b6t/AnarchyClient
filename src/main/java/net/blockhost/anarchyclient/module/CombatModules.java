package net.blockhost.anarchyclient.module;

import net.blockhost.anarchyclient.module.impl.AimAssistModule;
import net.blockhost.anarchyclient.module.impl.AnchorAuraModule;
import net.blockhost.anarchyclient.module.impl.AntiAnchorModule;
import net.blockhost.anarchyclient.module.impl.AntiAnvilModule;
import net.blockhost.anarchyclient.module.impl.AntiBedModule;
import net.blockhost.anarchyclient.module.impl.AntiBotModule;
import net.blockhost.anarchyclient.module.impl.AntiSpawnpointModule;
import net.blockhost.anarchyclient.module.impl.ArrowDamageModule;
import net.blockhost.anarchyclient.module.impl.ArrowDodgeModule;
import net.blockhost.anarchyclient.module.impl.AttributeSwapModule;
import net.blockhost.anarchyclient.module.impl.AutoAnvilModule;
import net.blockhost.anarchyclient.module.impl.AutoArmorModule;
import net.blockhost.anarchyclient.module.impl.AutoBedTrapModule;
import net.blockhost.anarchyclient.module.impl.AutoBowModule;
import net.blockhost.anarchyclient.module.impl.AutoBuffModule;
import net.blockhost.anarchyclient.module.impl.AutoCityModule;
import net.blockhost.anarchyclient.module.impl.AutoClickerModule;
import net.blockhost.anarchyclient.module.impl.AutoLogModule;
import net.blockhost.anarchyclient.module.impl.AutoPotModule;
import net.blockhost.anarchyclient.module.impl.AutoShootModule;
import net.blockhost.anarchyclient.module.impl.AutoTntModule;
import net.blockhost.anarchyclient.module.impl.AutoTotemModule;
import net.blockhost.anarchyclient.module.impl.AutoTrapModule;
import net.blockhost.anarchyclient.module.impl.AutoWeaponModule;
import net.blockhost.anarchyclient.module.impl.AutoWebModule;
import net.blockhost.anarchyclient.module.impl.AutoWitherModule;
import net.blockhost.anarchyclient.module.impl.BacktrackModule;
import net.blockhost.anarchyclient.module.impl.BedAuraModule;
import net.blockhost.anarchyclient.module.impl.BlockInModule;
import net.blockhost.anarchyclient.module.impl.BowAimbotModule;
import net.blockhost.anarchyclient.module.impl.BowSpamModule;
import net.blockhost.anarchyclient.module.impl.BurrowModule;
import net.blockhost.anarchyclient.module.impl.CriticalsModule;
import net.blockhost.anarchyclient.module.impl.CrystalAuraModule;
import net.blockhost.anarchyclient.module.impl.HitboxModule;
import net.blockhost.anarchyclient.module.impl.HoleFillerModule;
import net.blockhost.anarchyclient.module.impl.KeepSprintModule;
import net.blockhost.anarchyclient.module.impl.KillAuraModule;
import net.blockhost.anarchyclient.module.impl.KnockbackPlusModule;
import net.blockhost.anarchyclient.module.impl.MaceKillModule;
import net.blockhost.anarchyclient.module.impl.NoMissCooldownModule;
import net.blockhost.anarchyclient.module.impl.ProjectileAimbotModule;
import net.blockhost.anarchyclient.module.impl.ProjectilePuncherModule;
import net.blockhost.anarchyclient.module.impl.QuiverModule;
import net.blockhost.anarchyclient.module.impl.ShieldBypassModule;
import net.blockhost.anarchyclient.module.impl.SuperKnockbackModule;
import net.blockhost.anarchyclient.module.impl.SurroundModule;
import net.blockhost.anarchyclient.module.impl.SwordBlockModule;
import net.blockhost.anarchyclient.module.impl.TargetLockModule;
import net.blockhost.anarchyclient.module.impl.TeamsModule;
import net.blockhost.anarchyclient.module.impl.TickBaseModule;
import net.blockhost.anarchyclient.module.impl.TimerRangeModule;
import net.blockhost.anarchyclient.module.impl.VehicleOneHitModule;

/**
 * Registers the default combat modules.
 *
 * <p>Registration order is event dispatch order. See {@link ModuleRegistry}.
 */
final class CombatModules {

    private CombatModules() {
    }

    static void register(final ModuleManager modules) {
        modules.register(new AutoTotemModule());
        modules.register(new AttributeSwapModule());
        modules.register(new AutoLogModule());
        modules.register(new KillAuraModule());
        modules.register(new BacktrackModule());
        modules.register(new TickBaseModule());
        modules.register(new TimerRangeModule());
        modules.register(new AimAssistModule());
        modules.register(new TargetLockModule());
        modules.register(new TeamsModule());
        modules.register(new HitboxModule());
        modules.register(new KeepSprintModule());
        modules.register(new SuperKnockbackModule());
        modules.register(new NoMissCooldownModule());
        modules.register(new SwordBlockModule());
        modules.register(new AntiBotModule());
        modules.register(new AutoClickerModule());
        modules.register(new AutoWeaponModule());
        modules.register(new AutoArmorModule());
        modules.register(new AutoPotModule());
        modules.register(new AutoBuffModule());
        modules.register(new BowAimbotModule());
        modules.register(new AutoBowModule());
        modules.register(new ProjectileAimbotModule());
        modules.register(new ProjectilePuncherModule());
        modules.register(new AutoShootModule());
        modules.register(new CriticalsModule());
        modules.register(new MaceKillModule());
        modules.register(new AutoAnvilModule());
        modules.register(new AntiAnvilModule());
        modules.register(new AntiBedModule());
        modules.register(new AntiAnchorModule());
        modules.register(new AutoCityModule());
        modules.register(new BowSpamModule());
        modules.register(new QuiverModule());
        modules.register(new ArrowDodgeModule());
        modules.register(new ArrowDamageModule());
        modules.register(new BurrowModule());
        modules.register(new CrystalAuraModule());
        modules.register(new BedAuraModule());
        modules.register(new AnchorAuraModule());
        modules.register(new KnockbackPlusModule());
        modules.register(new ShieldBypassModule());
        modules.register(new AutoTntModule());
        modules.register(new AutoWitherModule());
        modules.register(new BlockInModule());
        modules.register(new SurroundModule());
        modules.register(new HoleFillerModule());
        modules.register(new AutoTrapModule());
        modules.register(new AutoBedTrapModule());
        modules.register(new AutoWebModule());
        modules.register(new VehicleOneHitModule());
        modules.register(new AntiSpawnpointModule());
    }
}
