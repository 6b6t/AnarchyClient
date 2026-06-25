package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
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

    public KnockbackPlusModule() {
        super("knockback_plus", "Knockback Plus", ModuleCategory.COMBAT);
    }

    @Override
    public boolean attackEntity(final Minecraft client, final Player player, final Entity target) {
        if (!(target instanceof LivingEntity) || client.getConnection() == null) {
            return false;
        }
        if (this.onlyKillAura.value() && !killAuraEnabled()) {
            return false;
        }
        client.getConnection().send(new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
        return false;
    }

    private static boolean killAuraEnabled() {
        return AnarchyClient.MODULES.find("kill_aura").map(Module::enabled).orElse(false);
    }
}
