package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

public final class AutoFishModule extends Module {

    private final NumberSetting minHookTicks = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("min_hook_ticks")
            .name("Min Ticks")
            .defaultValue(25.0)
            .min(5.0)
            .max(100.0)
            .step(5.0)
            .build()));
    private final BooleanSetting autoCast = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("auto_cast")
            .name("Auto Cast")
            .defaultValue(true)
            .build()));
    private final BooleanSetting soundTrigger = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("sound_trigger")
            .name("Sound Trigger")
            .defaultValue(true)
            .build()));
    private final NumberSetting soundDistance = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("sound_distance")
            .name("Sound Range")
            .defaultValue(1.0)
            .min(0.0)
            .max(10.0)
            .step(0.25)
            .build()));
    private final NumberSetting reelDelayTicksSetting = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("reel_delay_ticks")
            .name("Reel Delay")
            .defaultValue(5.0)
            .min(0.0)
            .max(20.0)
            .step(1.0)
            .build()));
    private final NumberSetting recastDelayTicksSetting = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("recast_delay_ticks")
            .name("Recast Delay")
            .defaultValue(10.0)
            .min(0.0)
            .max(40.0)
            .step(1.0)
            .build()));
    private int recastCooldownTicks;
    private int reelDelayTicks;
    private boolean caughtFish;

    public AutoFishModule() {
        super("auto_fish", "Auto Fish", ModuleCategory.PLAYER);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }
        if (!player.getMainHandItem().is(Items.FISHING_ROD) && !player.getOffhandItem().is(Items.FISHING_ROD)) {
            return;
        }
        InteractionHand hand = player.getMainHandItem().is(Items.FISHING_ROD) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        if (player.fishing == null) {
            this.caughtFish = false;
            this.reelDelayTicks = 0;
            if (this.autoCast.value() && this.isRecastReady()) {
                client.gameMode.useItem(player, hand);
                this.resetRecastCooldown();
            }
            return;
        }

        if (player.fishing.tickCount >= this.minHookTicks.value()
                && player.fishing.getDeltaMovement().y < -0.08
                && player.fishing.isInWater()) {
            this.triggerReel();
        }
        if (this.caughtFish) {
            if (this.reelDelayTicks > 0) {
                this.reelDelayTicks--;
                return;
            }
            client.gameMode.useItem(player, hand);
            this.caughtFish = false;
            this.resetRecastCooldown();
        }
    }

    @Override
    public void soundPacket(final Minecraft client, final ClientboundSoundPacket packet) {
        LocalPlayer player = client.player;
        if (!this.soundTrigger.value()
                || player == null
                || player.fishing == null
                || player.fishing.isRemoved()
                || !packet.getSound().value().equals(SoundEvents.FISHING_BOBBER_SPLASH)) {
            return;
        }
        double maxDistance = this.soundDistance.value();
        if (player.fishing.distanceToSqr(packet.getX(), packet.getY(), packet.getZ()) <= maxDistance * maxDistance) {
            this.triggerReel();
        }
    }

    boolean isRecastReady() {
        if (this.recastCooldownTicks > 0) {
            this.recastCooldownTicks--;
            return false;
        }
        return true;
    }

    void resetRecastCooldown() {
        this.recastCooldownTicks = this.recastDelayTicksSetting.value().intValue();
    }

    private void triggerReel() {
        if (!this.caughtFish) {
            this.reelDelayTicks = this.reelDelayTicksSetting.value().intValue();
        }
        this.caughtFish = true;
    }
}
