package net.blockhost.anarchyclient.module.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Predicate;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.phys.EntityHitResult;

public final class AutoTameModule extends Module {

    /** Taming is random, so retry the same animal after a short pause. */
    private static final int RETRY_TICKS = 10;

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(4.5)
            .min(1.0)
            .max(6.0)
            .step(0.5)
            .build()));
    private final NumberSetting delay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay")
            .name("Delay")
            .defaultValue(4.0)
            .min(1.0)
            .max(40.0)
            .step(1.0)
            .description("Ticks between two feeding attempts.")
            .build()));
    private final Map<UUID, Integer> triedAt = new HashMap<>();
    private int ticks;
    private int cooldown;

    public AutoTameModule() {
        super(
                "auto_tame",
                "Auto Tame",
                ModuleCategory.WORLD,
                "Feeds untamed wolves, cats, and parrots near you with their taming item from your hotbar until they are tamed.");
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        this.ticks++;
        if (player == null || client.level == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }
        if (this.cooldown > 0) {
            this.cooldown--;
            return;
        }
        this.triedAt.values().removeIf(tick -> this.ticks - tick > RETRY_TICKS);
        double rangeSqr = this.range.value() * this.range.value();
        TamableAnimal best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof TamableAnimal animal)
                    || animal.isTame()
                    || !animal.isAlive()
                    || this.triedAt.containsKey(animal.getUUID())
                    || !CombatTargets.canReachEntity(player, animal)) {
                continue;
            }
            double distance = animal.distanceToSqr(player);
            if (distance > rangeSqr
                    || distance >= bestDistance
                    || InventoryActions.findHotbarSlot(player.getInventory(), tamingItem(animal))
                            .isEmpty()) {
                continue;
            }
            best = animal;
            bestDistance = distance;
        }
        if (best == null) {
            return;
        }
        OptionalInt slot = InventoryActions.findHotbarSlot(player.getInventory(), tamingItem(best));
        InventoryActions.selectHotbarSlot(player, slot.orElseThrow());
        InteractionResult result =
                client.gameMode.interact(player, best, new EntityHitResult(best), InteractionHand.MAIN_HAND);
        if (result.consumesAction()) {
            player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
        }
        this.triedAt.put(best.getUUID(), this.ticks);
        this.cooldown = this.delay.value().intValue();
    }

    /** Returns the item that tames this animal, or a predicate that matches nothing. */
    static Predicate<ItemStack> tamingItem(final TamableAnimal animal) {
        if (animal instanceof Wolf) {
            return stack -> stack.is(Items.BONE);
        }
        if (animal instanceof Cat cat) {
            return cat::isFood;
        }
        if (animal instanceof Parrot) {
            return stack -> stack.is(ItemTags.PARROT_FOOD);
        }
        return stack -> false;
    }

    @Override
    public void gameLeft(final Minecraft client, final ClientPacketListener listener) {
        this.triedAt.clear();
    }

    @Override
    protected void onDisable() {
        this.triedAt.clear();
        this.cooldown = 0;
    }
}
