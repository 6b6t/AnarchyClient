package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class MiddleClickActionModule extends Module {

    private final SelectSetting mode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("mode")
            .name("Mode")
            .defaultValue("Pearl")
            .addAllOptions(List.of("Pearl", "XP Bottle", "Command", "Add Friend"))
            .build()));
    private final StringSetting command = this.setting(StringSetting.from(StringSetting.builder()
            .id("command")
            .name("Command")
            .defaultValue("/msg {target} hi")
            .build()));
    private boolean wasPressed;

    public MiddleClickActionModule() {
        super("middle_click_action", "Middle Click", ModuleCategory.MISC);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null || client.gui.screen() != null) {
            this.wasPressed = false;
            return;
        }
        boolean pressed = InputStates.mousePressed(client, GLFW.GLFW_MOUSE_BUTTON_MIDDLE);
        if (pressed && !this.wasPressed) {
            this.execute(client, player);
        }
        this.wasPressed = pressed;
    }

    private void execute(final Minecraft client, final LocalPlayer player) {
        switch (this.mode.value()) {
            case "XP Bottle" -> useHotbarItem(client, player, Items.EXPERIENCE_BOTTLE);
            case "Command" -> {
                String target = client.hitResult instanceof EntityHitResult hit ? hit.getEntity().getScoreboardName() : "";
                ChatActions.send(client, this.command.value().replace("{target}", target));
            }
            case "Add Friend" -> {
                if (client.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof Player target) {
                    AnarchyClient.FRIENDS.add(target.getScoreboardName());
                }
            }
            default -> useHotbarItem(client, player, Items.ENDER_PEARL);
        }
    }

    private static void useHotbarItem(final Minecraft client, final LocalPlayer player, final Item item) {
        Inventory inventory = player.getInventory();
        int previousSlot = inventory.getSelectedSlot();
        InventoryActions.findHotbarSlot(inventory, stack -> stack.is(item)).ifPresent(slot -> {
            InventoryActions.selectHotbarSlot(player, slot);
            client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
            InventoryActions.selectHotbarSlot(player, previousSlot);
        });
    }
}
