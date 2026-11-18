package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.i18n.Translations;
import net.blockhost.anarchyclient.inventory.SilentHotbar;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

import java.util.Optional;

public final class EasyPearlModule extends Module {

    private final NumberSetting pitch = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("pitch")
            .name("Pitch")
            .defaultValue(80.0)
            .min(-90.0)
            .max(90.0)
            .step(1.0)
            .build()));
    private boolean pending;

    /** How long a press waits for a usable pearl (for example while a screen closes) before giving up. */
    private static final int ARMED_TICKS = 20;

    private int ticksLeft;

    public EasyPearlModule() {
        super("easy_pearl", "Easy Pearl", ModuleCategory.PLAYER,
                "Throws an ender pearl from either hand at a set pitch with one keypress, then turns itself off.");
    }

    @Override
    protected void onEnable() {
        this.pending = true;
        this.ticksLeft = ARMED_TICKS;
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        // A one-shot throw must not stay armed: with no world (enabled from config at startup), or once the short
        // window passes without a usable pearl, turn off instead of throwing a pearl picked up much later.
        if (!this.pending || player == null || client.gameMode == null || --this.ticksLeft < 0) {
            if (player != null && this.pending) {
                player.sendSystemMessage(Translations.component("anarchyclient.message.easy_pearl.easy_pearl_no_ender_pearl_to", "Easy Pearl: no ender pearl to throw."));
            }
            this.pending = false;
            this.enabled(false);
            return;
        }
        if (client.gui.screen() != null) {
            return;
        }
        Optional<InteractionHand> hand = SilentHotbar.usableHand(player, this.id(),
                stack -> stack.is(Items.ENDER_PEARL), SilentHotbar.PRIORITY_COMBAT, 4, true);
        if (hand.isEmpty()) {
            return;
        }
        // The use packet carries the rotation, so the pitch can be restored right after the throw.
        float previousPitch = player.getXRot();
        player.setXRot(this.pitch.value().floatValue());
        client.gameMode.useItem(player, hand.orElseThrow());
        player.setXRot(previousPitch);
        this.pending = false;
        this.enabled(false);
    }
}
