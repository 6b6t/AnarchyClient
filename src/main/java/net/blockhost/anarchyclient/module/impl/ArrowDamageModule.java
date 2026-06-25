package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ArrowDamageModule extends Module {

    private final NumberSetting packets = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("packets")
            .name("Packets")
            .defaultValue(8.0)
            .min(1.0)
            .max(40.0)
            .step(1.0)
            .build()));
    private final NumberSetting offset = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("offset")
            .name("Offset")
            .defaultValue(0.0625)
            .min(0.001)
            .max(0.5)
            .step(0.001)
            .build()));
    private final BooleanSetting tridents = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("tridents")
            .name("Tridents")
            .defaultValue(true)
            .build()));

    public ArrowDamageModule() {
        super("arrow_damage", "Arrow Damage", ModuleCategory.COMBAT);
    }

    @Override
    public void itemStopUse(final Minecraft client, final InteractionHand hand, final ItemStack stack,
                            final int remainingTicks) {
        LocalPlayer player = client.player;
        if (player == null || client.getConnection() == null || !isSupported(stack, this.tridents.value())) {
            return;
        }
        int usedTicks = Math.max(0, stack.getUseDuration(player) - remainingTicks);
        if (usedTicks < 3) {
            return;
        }
        client.getConnection().send(new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
        for (int index = 0; index < this.packets.value().intValue(); index++) {
            double y = player.getY() + this.offset.value();
            client.getConnection().send(new ServerboundMovePlayerPacket.Pos(player.getX(), y, player.getZ(), false, player.horizontalCollision));
            client.getConnection().send(new ServerboundMovePlayerPacket.Pos(player.getX(), player.getY(), player.getZ(), true, player.horizontalCollision));
        }
    }

    static boolean isSupported(final ItemStack stack, final boolean tridents) {
        return stack.getItem() == Items.BOW || stack.getItem() == Items.CROSSBOW || tridents && stack.getItem() == Items.TRIDENT;
    }
}
