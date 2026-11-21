package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class KnockbackPlusModule extends Module {

    private final BooleanSetting onlyKillAura = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("only_kill_aura")
            .name("Only Kill Aura")
            .defaultValue(false)
            .build()));
    private boolean pendingStop;

    public KnockbackPlusModule() {
        super(
                "knockback_plus",
                "Knockback Plus",
                ModuleCategory.COMBAT,
                "Sends a sprint packet before each attack so your hits deal sprint knockback, then restores your real sprint state on the next tick.");
    }

    @Override
    public void tick(final Minecraft client) {
        this.restoreSprintState(client);
    }

    private void restoreSprintState(final Minecraft client) {
        if (!this.pendingStop) {
            return;
        }
        this.pendingStop = false;
        ClientPacketListener connection = client.getConnection();
        if (client.player != null && connection != null && !client.player.isSprinting()) {
            connection.send(new ServerboundPlayerCommandPacket(
                    client.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
        }
    }

    @Override
    public boolean attackEntity(final Minecraft client, final Player player, final Entity target) {
        ClientPacketListener connection = client.getConnection();
        if (!(target instanceof LivingEntity) || connection == null) {
            return false;
        }
        if (this.onlyKillAura.value() && !killAuraEnabled()) {
            return false;
        }
        connection.send(
                new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
        // LocalPlayer only sends sprint changes it made itself, so undo the fake sprint if the client is not sprinting.
        if (!player.isSprinting()) {
            this.pendingStop = true;
        }
        return false;
    }

    @Override
    protected void onDisable() {
        this.restoreSprintState(Minecraft.getInstance());
    }

    private static boolean killAuraEnabled() {
        return AnarchyClient.MODULES.find("kill_aura").map(Module::enabled).orElse(false);
    }
}
