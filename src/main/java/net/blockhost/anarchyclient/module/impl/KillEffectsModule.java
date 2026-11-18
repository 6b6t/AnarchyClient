package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public final class KillEffectsModule extends Module {

    private static final AtomicInteger NEXT_ENTITY_ID = new AtomicInteger(-20_000);

    private final BooleanSetting lightning = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("lightning")
            .name("Lightning")
            .defaultValue(true)
            .description("Shows a client-side lightning bolt where the player died.")
            .build()));
    private final BooleanSetting sound = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("sound")
            .name("Sound")
            .defaultValue(true)
            .description("Plays a sound for each death.")
            .build()));
    private final BooleanSetting friends = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("friends")
            .name("Friends")
            .defaultValue(false)
            .description("Also plays the effects when a friend dies.")
            .build()));
    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(64.0)
            .min(4.0)
            .max(256.0)
            .step(4.0)
            .build()));
    private final Set<Integer> dead = new HashSet<>();

    public KillEffectsModule() {
        super("kill_effects", "Kill Effects", ModuleCategory.RENDER,
                "Shows client-side lightning and plays a sound where nearby players die. Only you see and hear it.");
    }

    @Override
    public void tick(final Minecraft client) {
        ClientLevel level = client.level;
        if (client.player == null || level == null) {
            return;
        }
        double rangeSqr = this.range.value() * this.range.value();
        Set<Integer> seen = new HashSet<>();
        for (Player player : level.players()) {
            if (player == client.player || !player.isDeadOrDying()) {
                continue;
            }
            seen.add(player.getId());
            if (!this.dead.add(player.getId()) || player.distanceToSqr(client.player) > rangeSqr) {
                continue;
            }
            if (!this.friends.value() && AnarchyClient.FRIENDS.isFriend(player.getScoreboardName())) {
                continue;
            }
            this.play(level, player);
        }
        this.dead.retainAll(seen);
    }

    private void play(final ClientLevel level, final Player victim) {
        if (this.lightning.value()) {
            LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
            if (bolt != null) {
                bolt.setId(NEXT_ENTITY_ID.getAndDecrement());
                bolt.setVisualOnly(true);
                // Keep the thunder quiet; the separate death sound below is the audible cue.
                bolt.setSilent(true);
                bolt.snapTo(victim.getX(), victim.getY(), victim.getZ());
                level.addEntity(bolt);
            }
        }
        if (this.sound.value()) {
            level.playLocalSound(victim.getX(), victim.getY(), victim.getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT,
                    SoundSource.PLAYERS, 1.0F, 1.0F, false);
        }
    }

    @Override
    public void gameLeft(final Minecraft client, final ClientPacketListener listener) {
        this.dead.clear();
    }

    @Override
    protected void onDisable() {
        this.dead.clear();
    }
}
