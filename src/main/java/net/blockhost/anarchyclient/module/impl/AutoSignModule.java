package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

public final class AutoSignModule extends Module {

    static final int PLACEMENT_WINDOW_TICKS = 20;

    private final StringSetting line1 = this.setting(StringSetting.from(StringSetting.builder().id("line_1").name("Line 1").defaultValue("AnarchyClient").build()));
    private final StringSetting line2 = this.setting(StringSetting.from(StringSetting.builder().id("line_2").name("Line 2").defaultValue("").build()));
    private final StringSetting line3 = this.setting(StringSetting.from(StringSetting.builder().id("line_3").name("Line 3").defaultValue("").build()));
    private final StringSetting line4 = this.setting(StringSetting.from(StringSetting.builder().id("line_4").name("Line 4").defaultValue("").build()));
    private final BooleanSetting front = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("front")
            .name("Front")
            .defaultValue(true)
            .build()));
    private volatile ClientboundOpenSignEditorPacket pendingEditor;
    // Read on the network thread.
    private volatile int placementTicks;

    public AutoSignModule() {
        super("auto_sign", "Auto Sign", ModuleCategory.WORLD,
                "Writes the configured four lines to each sign you place instead of opening the sign editor.");
    }

    @Override
    public boolean blockInteract(final Minecraft client, final InteractionHand hand, final BlockHitResult hitResult) {
        if (client.player != null && isSignItem(client.player.getItemInHand(hand))) {
            this.placementTicks = PLACEMENT_WINDOW_TICKS;
        }
        return false;
    }

    @Override
    public boolean receivePacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        // Runs on the network thread, so hand the editor request to the client tick. Only requests right
        // after placing a sign are taken over; everything else goes to vanilla untouched.
        if (packet instanceof ClientboundOpenSignEditorPacket editor && this.placementTicks > 0) {
            this.pendingEditor = editor;
            return true;
        }
        return false;
    }

    @Override
    public void tick(final Minecraft client) {
        ClientboundOpenSignEditorPacket editor = this.pendingEditor;
        this.pendingEditor = null;
        if (editor != null && !this.handleEditor(client, editor)) {
            // The sign's block entity has not arrived yet; try again next tick.
            this.pendingEditor = editor;
        }
        if (this.placementTicks > 0) {
            this.placementTicks--;
        }
    }

    @Override
    public void gameLeft(final Minecraft client, final ClientPacketListener listener) {
        this.pendingEditor = null;
        this.placementTicks = 0;
    }

    @Override
    protected void onDisable() {
        // A request taken over but not handled yet still has to open the editor.
        ClientboundOpenSignEditorPacket editor = this.pendingEditor;
        Minecraft client = Minecraft.getInstance();
        if (editor != null && client.getConnection() != null) {
            client.getConnection().handleOpenSignEditor(editor);
        }
        this.pendingEditor = null;
        this.placementTicks = 0;
    }

    /** Handles the request, or returns false to retry while the sign's block entity is still missing. */
    private boolean handleEditor(final Minecraft client, final ClientboundOpenSignEditorPacket editor) {
        LocalPlayer player = client.player;
        ClientPacketListener connection = client.getConnection();
        if (player == null || client.level == null || connection == null) {
            return true;
        }
        if (!(client.level.getBlockEntity(editor.pos()) instanceof SignBlockEntity sign)) {
            if (this.placementTicks > 0) {
                return false;
            }
            connection.handleOpenSignEditor(editor);
            return true;
        }
        if (!isFreshlyPlaced(this.placementTicks > 0, sign.getText(editor.slot()), client.isTextFilteringEnabled())) {
            connection.handleOpenSignEditor(editor);
            return true;
        }
        connection.send(new ServerboundSignUpdatePacket(
                editor.pos(),
                List.of(trim(this.line1.value()), trim(this.line2.value()), trim(this.line3.value()), trim(this.line4.value())),
                this.front.value() ? SignTextSlot.FRONT : SignTextSlot.BACK
        ));
        this.placementTicks = 0;
        return true;
    }

    static boolean isSignItem(final ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof SignBlock;
    }

    /**
     * The server also opens the editor when you click an existing sign, so only treat it as a new sign
     * right after placing one and while the edited side is still blank.
     */
    static boolean isFreshlyPlaced(final boolean recentlyPlacedSign, final SignText text, final boolean filtered) {
        return recentlyPlacedSign && !text.hasMessage(filtered);
    }

    static String trim(final String value) {
        if (value == null) {
            return "";
        }
        return value.length() > 384 ? value.substring(0, 384) : value;
    }
}
