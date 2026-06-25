package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class StrongholdFinderModule extends Module {

    private final BooleanSetting notify = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("notify")
            .name("Notify")
            .defaultValue(true)
            .build()));
    private final List<EyeSample> samples = new ArrayList<>();

    public StrongholdFinderModule() {
        super("stronghold_finder", "Stronghold Finder", ModuleCategory.WORLD);
    }

    @Override
    public void sentPacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (!(packet instanceof ServerboundUseItemPacket use) || client.player == null) {
            return;
        }
        InteractionHand hand = use.getHand();
        if (!client.player.getItemInHand(hand).is(Items.ENDER_EYE)) {
            return;
        }
        EyeSample sample = new EyeSample(client.player.position(), client.player.getViewVector(0.0F));
        this.samples.add(sample);
        while (this.samples.size() > 8) {
            this.samples.removeFirst();
        }
        Optional<Vec3> estimate = this.estimate();
        if (this.notify.value() && estimate.isPresent() && client.player != null) {
            Vec3 pos = estimate.orElseThrow();
            client.player.sendSystemMessage(Component.literal(
                    "Stronghold estimate: x " + Math.round(pos.x) + ", z " + Math.round(pos.z) + "."
            ));
        }
    }

    @Override
    protected void onDisable() {
        this.samples.clear();
    }

    Optional<Vec3> estimate() {
        if (this.samples.size() < 2) {
            return Optional.empty();
        }
        EyeSample a = this.samples.get(this.samples.size() - 2);
        EyeSample b = this.samples.getLast();
        return estimate(a.position(), a.direction(), b.position(), b.direction());
    }

    static Optional<Vec3> estimate(final Vec3 firstPosition, final Vec3 firstDirection,
                                   final Vec3 secondPosition, final Vec3 secondDirection) {
        double cross = firstDirection.x * secondDirection.z - firstDirection.z * secondDirection.x;
        if (Math.abs(cross) < 1.0E-5) {
            return Optional.empty();
        }
        double dx = secondPosition.x - firstPosition.x;
        double dz = secondPosition.z - firstPosition.z;
        double t = (dx * secondDirection.z - dz * secondDirection.x) / cross;
        return Optional.of(new Vec3(firstPosition.x + firstDirection.x * t, firstPosition.y, firstPosition.z + firstDirection.z * t));
    }

    private record EyeSample(Vec3 position, Vec3 direction) {
    }
}
