package net.blockhost.anarchyclient.module.impl;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.blockhost.anarchyclient.i18n.Translations;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class StrongholdFinderModule extends Module {

    private final BooleanSetting notify = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("notify")
            .name("Notify")
            .defaultValue(true)
            .build()));
    private static final double MIN_TRAVEL = 2.0;

    private final List<EyeSample> samples = new ArrayList<>();
    private final IdentityHashMap<EyeOfEnder, Vec3> flyingEyes = new IdentityHashMap<>();
    private Level level;

    public StrongholdFinderModule() {
        super(
                "stronghold_finder",
                "Stronghold Finder",
                ModuleCategory.WORLD,
                "Estimates the stronghold position from the flight paths of two thrown eyes of ender and prints it in chat.");
    }

    @Override
    public void entityAdded(final Minecraft client, final Entity entity) {
        if (entity instanceof EyeOfEnder eye) {
            this.flyingEyes.put(eye, eye.position());
        }
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.level != this.level) {
            this.level = client.level;
            this.flyingEyes.clear();
            this.samples.clear();
        }
        if (client.player == null || this.flyingEyes.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<EyeOfEnder, Vec3>> iterator =
                this.flyingEyes.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<EyeOfEnder, Vec3> entry = iterator.next();
            EyeOfEnder eye = entry.getKey();
            if (eye.isRemoved()) {
                iterator.remove();
                continue;
            }
            Optional<Vec3> direction = flightDirection(entry.getValue(), eye.position());
            if (direction.isPresent()) {
                iterator.remove();
                this.addSample(client, new EyeSample(entry.getValue(), direction.get()));
            }
        }
    }

    private void addSample(final Minecraft client, final EyeSample sample) {
        this.samples.add(sample);
        while (this.samples.size() > 8) {
            this.samples.removeFirst();
        }
        Optional<Vec3> estimate = this.estimate();
        if (this.notify.value() && estimate.isPresent() && client.player != null) {
            Vec3 pos = estimate.orElseThrow();
            client.player.sendSystemMessage(Translations.component(
                    "anarchyclient.message.stronghold_finder.stronghold_estimate_x_z",
                    "Stronghold estimate: x %s, z %s.",
                    Math.round(pos.x),
                    Math.round(pos.z)));
        }
    }

    /** The eye flies straight toward the stronghold at first, so its horizontal travel gives the bearing. */
    static Optional<Vec3> flightDirection(final Vec3 start, final Vec3 current) {
        Vec3 travel = new Vec3(current.x - start.x, 0.0, current.z - start.z);
        if (travel.lengthSqr() < MIN_TRAVEL * MIN_TRAVEL) {
            return Optional.empty();
        }
        return Optional.of(travel.normalize());
    }

    @Override
    protected void onDisable() {
        this.samples.clear();
        this.flyingEyes.clear();
        this.level = null;
    }

    Optional<Vec3> estimate() {
        if (this.samples.size() < 2) {
            return Optional.empty();
        }
        EyeSample a = this.samples.get(this.samples.size() - 2);
        EyeSample b = this.samples.getLast();
        return estimate(a.position(), a.direction(), b.position(), b.direction());
    }

    static Optional<Vec3> estimate(
            final Vec3 firstPosition,
            final Vec3 firstDirection,
            final Vec3 secondPosition,
            final Vec3 secondDirection) {
        double cross = firstDirection.x * secondDirection.z - firstDirection.z * secondDirection.x;
        if (Math.abs(cross) < 1.0E-5) {
            return Optional.empty();
        }
        double dx = secondPosition.x - firstPosition.x;
        double dz = secondPosition.z - firstPosition.z;
        double t = (dx * secondDirection.z - dz * secondDirection.x) / cross;
        return Optional.of(new Vec3(
                firstPosition.x + firstDirection.x * t, firstPosition.y, firstPosition.z + firstDirection.z * t));
    }

    private record EyeSample(Vec3 position, Vec3 direction) {}
}
