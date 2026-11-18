package net.blockhost.anarchyclient.module;

import net.blockhost.anarchyclient.module.impl.AmbienceModule;
import net.blockhost.anarchyclient.module.impl.AnimationsModule;
import net.blockhost.anarchyclient.module.impl.AspectModule;
import net.blockhost.anarchyclient.module.impl.BedPlatesModule;
import net.blockhost.anarchyclient.module.impl.BetterBeaconsModule;
import net.blockhost.anarchyclient.module.impl.BetterInventoryModule;
import net.blockhost.anarchyclient.module.impl.BetterTabModule;
import net.blockhost.anarchyclient.module.impl.BetterTooltipsModule;
import net.blockhost.anarchyclient.module.impl.BlockOutlineModule;
import net.blockhost.anarchyclient.module.impl.BlockSelectionModule;
import net.blockhost.anarchyclient.module.impl.BlurModule;
import net.blockhost.anarchyclient.module.impl.BordersModule;
import net.blockhost.anarchyclient.module.impl.BossStackModule;
import net.blockhost.anarchyclient.module.impl.BreadcrumbsModule;
import net.blockhost.anarchyclient.module.impl.BreakIndicatorsModule;
import net.blockhost.anarchyclient.module.impl.ChamsModule;
import net.blockhost.anarchyclient.module.impl.CityEspModule;
import net.blockhost.anarchyclient.module.impl.CrosshairModule;
import net.blockhost.anarchyclient.module.impl.DamageParticlesModule;
import net.blockhost.anarchyclient.module.impl.DebugRenderModule;
import net.blockhost.anarchyclient.module.impl.EntityOwnerModule;
import net.blockhost.anarchyclient.module.impl.EspModule;
import net.blockhost.anarchyclient.module.impl.FreeLookModule;
import net.blockhost.anarchyclient.module.impl.FullbrightModule;
import net.blockhost.anarchyclient.module.impl.HandViewModule;
import net.blockhost.anarchyclient.module.impl.HeavenModule;
import net.blockhost.anarchyclient.module.impl.HitFXModule;
import net.blockhost.anarchyclient.module.impl.HitmarkerModule;
import net.blockhost.anarchyclient.module.impl.HoleEspModule;
import net.blockhost.anarchyclient.module.impl.ItemHighlightModule;
import net.blockhost.anarchyclient.module.impl.ItemPhysicsModule;
import net.blockhost.anarchyclient.module.impl.ItemTagsModule;
import net.blockhost.anarchyclient.module.impl.JumpEffectModule;
import net.blockhost.anarchyclient.module.impl.KillEffectsModule;
import net.blockhost.anarchyclient.module.impl.LightOverlayModule;
import net.blockhost.anarchyclient.module.impl.LogoutSpotsModule;
import net.blockhost.anarchyclient.module.impl.NametagsModule;
import net.blockhost.anarchyclient.module.impl.NewChunksModule;
import net.blockhost.anarchyclient.module.impl.NoRenderModule;
import net.blockhost.anarchyclient.module.impl.NoSwingModule;
import net.blockhost.anarchyclient.module.impl.OreSimModule;
import net.blockhost.anarchyclient.module.impl.PopChamsModule;
import net.blockhost.anarchyclient.module.impl.ProtectionZonesModule;
import net.blockhost.anarchyclient.module.impl.RenderingModule;
import net.blockhost.anarchyclient.module.impl.SkeletonEspModule;
import net.blockhost.anarchyclient.module.impl.SkyboxModule;
import net.blockhost.anarchyclient.module.impl.SmoothCameraModule;
import net.blockhost.anarchyclient.module.impl.SoundLocatorModule;
import net.blockhost.anarchyclient.module.impl.StorageEspModule;
import net.blockhost.anarchyclient.module.impl.TimeChangerModule;
import net.blockhost.anarchyclient.module.impl.TntTimerModule;
import net.blockhost.anarchyclient.module.impl.TracersModule;
import net.blockhost.anarchyclient.module.impl.TrailModule;
import net.blockhost.anarchyclient.module.impl.TrajectoriesModule;
import net.blockhost.anarchyclient.module.impl.TrueSightModule;
import net.blockhost.anarchyclient.module.impl.TunnelEspModule;
import net.blockhost.anarchyclient.module.impl.VoidEspModule;
import net.blockhost.anarchyclient.module.impl.WallHackModule;
import net.blockhost.anarchyclient.module.impl.WaypointsModule;
import net.blockhost.anarchyclient.module.impl.WeatherChangerModule;
import net.blockhost.anarchyclient.module.impl.XrayModule;
import net.blockhost.anarchyclient.module.impl.ZoomModule;

/**
 * Registers the default rendering modules.
 *
 * <p>Registration order is event dispatch order. See {@link ModuleRegistry}.
 */
final class RenderModules {

    private RenderModules() {
    }

    static void register(final ModuleManager modules) {
        modules.register(new EspModule());
        modules.register(new CrosshairModule());
        modules.register(new AspectModule());
        modules.register(new SkeletonEspModule());
        modules.register(new TracersModule());
        modules.register(new StorageEspModule());
        modules.register(new HoleEspModule());
        modules.register(new LightOverlayModule());
        modules.register(new DebugRenderModule());
        modules.register(new BedPlatesModule());
        modules.register(new JumpEffectModule());
        modules.register(new OreSimModule());
        modules.register(new XrayModule());
        modules.register(new WallHackModule());
        modules.register(new CityEspModule());
        modules.register(new TunnelEspModule());
        modules.register(new NewChunksModule());
        modules.register(new LogoutSpotsModule());
        modules.register(new SoundLocatorModule());
        modules.register(new VoidEspModule());
        modules.register(new BreadcrumbsModule());
        modules.register(new TrailModule());
        modules.register(new BreakIndicatorsModule());
        modules.register(new BlockSelectionModule());
        modules.register(new BlockOutlineModule());
        modules.register(new FreeLookModule());
        modules.register(new HandViewModule());
        modules.register(new AnimationsModule());
        modules.register(new SmoothCameraModule());
        modules.register(new TimeChangerModule());
        modules.register(new WeatherChangerModule());
        modules.register(new AmbienceModule());
        modules.register(new RenderingModule());
        modules.register(new BossStackModule());
        modules.register(new ItemHighlightModule());
        modules.register(new ItemTagsModule());
        modules.register(new HitFXModule());
        modules.register(new DamageParticlesModule());
        modules.register(new ProtectionZonesModule());
        modules.register(new PopChamsModule());
        modules.register(new ChamsModule());
        modules.register(new EntityOwnerModule());
        modules.register(new ItemPhysicsModule());
        modules.register(new WaypointsModule());
        modules.register(new NametagsModule());
        modules.register(new BetterTabModule());
        modules.register(new BetterTooltipsModule());
        modules.register(new BetterInventoryModule());
        modules.register(new BetterBeaconsModule());
        modules.register(new TrueSightModule());
        modules.register(new NoRenderModule());
        modules.register(new TrajectoriesModule());
        modules.register(new NoSwingModule());
        modules.register(new TntTimerModule());
        modules.register(new ZoomModule());
        modules.register(new BlurModule());
        modules.register(new FullbrightModule());
        modules.register(new HeavenModule());
        modules.register(new KillEffectsModule());
        modules.register(new HitmarkerModule());
        modules.register(new SkyboxModule());
        modules.register(new BordersModule());
    }
}
