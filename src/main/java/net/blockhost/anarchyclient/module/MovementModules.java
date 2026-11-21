package net.blockhost.anarchyclient.module;

import net.blockhost.anarchyclient.module.impl.AirJumpModule;
import net.blockhost.anarchyclient.module.impl.AnchorModule;
import net.blockhost.anarchyclient.module.impl.AntiVoidModule;
import net.blockhost.anarchyclient.module.impl.AutoFireworkModule;
import net.blockhost.anarchyclient.module.impl.AutoJumpModule;
import net.blockhost.anarchyclient.module.impl.AutoSprintModule;
import net.blockhost.anarchyclient.module.impl.AutoWalkModule;
import net.blockhost.anarchyclient.module.impl.AutoWaspModule;
import net.blockhost.anarchyclient.module.impl.AutoWindChargeModule;
import net.blockhost.anarchyclient.module.impl.AvoidHazardsModule;
import net.blockhost.anarchyclient.module.impl.BlinkModule;
import net.blockhost.anarchyclient.module.impl.BoatGlitchModule;
import net.blockhost.anarchyclient.module.impl.BoatPhaseModule;
import net.blockhost.anarchyclient.module.impl.BoostModule;
import net.blockhost.anarchyclient.module.impl.BounceModule;
import net.blockhost.anarchyclient.module.impl.ChorusExploitModule;
import net.blockhost.anarchyclient.module.impl.CollisionsModule;
import net.blockhost.anarchyclient.module.impl.EagleModule;
import net.blockhost.anarchyclient.module.impl.ElytraBoostModule;
import net.blockhost.anarchyclient.module.impl.ElytraFlyModule;
import net.blockhost.anarchyclient.module.impl.ElytraRecastModule;
import net.blockhost.anarchyclient.module.impl.ExtraElytraModule;
import net.blockhost.anarchyclient.module.impl.FastClimbModule;
import net.blockhost.anarchyclient.module.impl.FlightModule;
import net.blockhost.anarchyclient.module.impl.FreecamModule;
import net.blockhost.anarchyclient.module.impl.GlideModule;
import net.blockhost.anarchyclient.module.impl.HighJumpModule;
import net.blockhost.anarchyclient.module.impl.HoleSnapModule;
import net.blockhost.anarchyclient.module.impl.InventoryMoveModule;
import net.blockhost.anarchyclient.module.impl.JesusModule;
import net.blockhost.anarchyclient.module.impl.JetpackModule;
import net.blockhost.anarchyclient.module.impl.LongJumpModule;
import net.blockhost.anarchyclient.module.impl.NoFallModule;
import net.blockhost.anarchyclient.module.impl.NoPushModule;
import net.blockhost.anarchyclient.module.impl.NoSlowModule;
import net.blockhost.anarchyclient.module.impl.NoWebModule;
import net.blockhost.anarchyclient.module.impl.PacketFlyModule;
import net.blockhost.anarchyclient.module.impl.ParkourModule;
import net.blockhost.anarchyclient.module.impl.ReverseStepModule;
import net.blockhost.anarchyclient.module.impl.RoboWalkModule;
import net.blockhost.anarchyclient.module.impl.SafeWalkModule;
import net.blockhost.anarchyclient.module.impl.SlippyModule;
import net.blockhost.anarchyclient.module.impl.SneakModule;
import net.blockhost.anarchyclient.module.impl.SpeedModule;
import net.blockhost.anarchyclient.module.impl.SpiderModule;
import net.blockhost.anarchyclient.module.impl.SpiralModule;
import net.blockhost.anarchyclient.module.impl.StepModule;
import net.blockhost.anarchyclient.module.impl.StrafeModule;
import net.blockhost.anarchyclient.module.impl.TargetStrafeModule;
import net.blockhost.anarchyclient.module.impl.TimerModule;
import net.blockhost.anarchyclient.module.impl.TridentBoostModule;
import net.blockhost.anarchyclient.module.impl.VehicleBoostModule;
import net.blockhost.anarchyclient.module.impl.VehicleControlModule;
import net.blockhost.anarchyclient.module.impl.VelocityModule;

/**
 * Registers the default movement modules.
 *
 * <p>Registration order is event dispatch order. See {@link ModuleRegistry}.
 */
final class MovementModules {

    private MovementModules() {}

    static void register(final ModuleManager modules) {
        modules.register(new AutoSprintModule());
        modules.register(new AirJumpModule());
        modules.register(new AnchorModule());
        modules.register(new BounceModule());
        modules.register(new StrafeModule());
        modules.register(new TargetStrafeModule());
        modules.register(new AutoWaspModule());
        modules.register(new AutoWalkModule());
        modules.register(new AutoJumpModule());
        modules.register(new SneakModule());
        modules.register(new BoostModule());
        modules.register(new GlideModule());
        modules.register(new JetpackModule());
        modules.register(new ExtraElytraModule());
        modules.register(new NoFallModule());
        modules.register(new AntiVoidModule());
        modules.register(new VelocityModule());
        modules.register(new EagleModule());
        modules.register(new SafeWalkModule());
        modules.register(new StepModule());
        modules.register(new ReverseStepModule());
        modules.register(new FastClimbModule());
        modules.register(new SpiderModule());
        modules.register(new HighJumpModule());
        modules.register(new TridentBoostModule());
        modules.register(new InventoryMoveModule());
        modules.register(new FlightModule());
        modules.register(new PacketFlyModule());
        modules.register(new RoboWalkModule());
        modules.register(new BoatPhaseModule());
        modules.register(new BoatGlitchModule());
        modules.register(new ChorusExploitModule());
        modules.register(new JesusModule());
        modules.register(new SpeedModule());
        modules.register(new SlippyModule());
        modules.register(new TimerModule());
        modules.register(new CollisionsModule());
        modules.register(new LongJumpModule());
        modules.register(new VehicleControlModule());
        modules.register(new VehicleBoostModule());
        modules.register(new ElytraBoostModule());
        modules.register(new ElytraFlyModule());
        modules.register(new ElytraRecastModule());
        modules.register(new NoSlowModule());
        modules.register(new NoWebModule());
        modules.register(new NoPushModule());
        modules.register(new AvoidHazardsModule());
        modules.register(new AutoWindChargeModule());
        modules.register(new BlinkModule());
        modules.register(new FreecamModule());
        modules.register(new ParkourModule());
        modules.register(new SpiralModule());
        modules.register(new HoleSnapModule());
        modules.register(new AutoFireworkModule());
    }
}
