package net.blockhost.anarchyclient.module.impl;

import com.mojang.authlib.GameProfile;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class FakePlayerModule extends Module {

    private static final AtomicInteger NEXT_ENTITY_ID = new AtomicInteger(-10_000);

    private final StringSetting name = this.setting(StringSetting.from(StringSetting.builder()
            .id("name")
            .name("Name")
            .defaultValue("AnarchyClient")
            .build()));
    private final NumberSetting health = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("health")
            .name("Health")
            .defaultValue(20.0)
            .min(1.0)
            .max(40.0)
            .step(1.0)
            .build()));
    private final BooleanSetting copyInventory = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("copy_inventory")
            .name("Copy Inventory")
            .defaultValue(true)
            .build()));
    private ClientLevel fakeLevel;
    private RemotePlayer fakePlayer;

    public FakePlayerModule() {
        super("fake_player", "Fake Player", ModuleCategory.MISC);
    }

    @Override
    public void tick(final Minecraft client) {
        if (this.fakePlayer == null && client.player != null && client.level != null) {
            this.spawn(client.player, client.level);
        }
    }

    @Override
    public void gameLeft(final Minecraft client, final ClientPacketListener listener) {
        this.fakePlayer = null;
    }

    @Override
    protected void onEnable() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.level != null) {
            this.spawn(client.player, client.level);
        }
    }

    @Override
    protected void onDisable() {
        this.despawn();
        this.clearDebugValues();
    }

    private void spawn(final LocalPlayer source, final ClientLevel level) {
        this.despawn();
        String profileName = sanitizeName(this.name.value());
        RemotePlayer clone = new RemotePlayer(level, new GameProfile(UUID.randomUUID(), profileName));
        clone.setId(NEXT_ENTITY_ID.getAndDecrement());
        clone.absSnapTo(source.getX(), source.getY(), source.getZ(), source.getYRot(), source.getXRot());
        clone.setOldPosAndRot();
        clone.setHealth(this.health.value().floatValue());
        clone.setPose(source.getPose());
        clone.setCustomName(Component.literal(profileName));
        clone.setCustomNameVisible(true);
        clone.yBodyRot = source.yBodyRot;
        clone.yBodyRotO = source.yBodyRotO;
        clone.yHeadRot = source.yHeadRot;
        clone.yHeadRotO = source.yHeadRotO;
        if (this.copyInventory.value()) {
            clone.getInventory().replaceWith(source.getInventory());
        } else {
            copyEquipment(source, clone);
        }
        level.addEntity(clone);
        this.fakeLevel = level;
        this.fakePlayer = clone;
        this.debugValue("status", "spawned");
        this.debugValue("entity", clone.getId());
    }

    private void despawn() {
        if (this.fakePlayer != null && this.fakeLevel != null && !this.fakePlayer.isRemoved()) {
            this.fakeLevel.removeEntity(this.fakePlayer.getId(), RemovalReason.DISCARDED);
        }
        this.fakePlayer = null;
        this.fakeLevel = null;
    }

    private static void copyEquipment(final LocalPlayer source, final RemotePlayer clone) {
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            ItemStack stack = source.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                clone.setItemSlot(slot, stack.copy());
            }
        }
    }

    static String sanitizeName(final String name) {
        String trimmed = name == null || name.isBlank() ? "AnarchyClient" : name.trim();
        return trimmed.length() > 16 ? trimmed.substring(0, 16) : trimmed;
    }
}
