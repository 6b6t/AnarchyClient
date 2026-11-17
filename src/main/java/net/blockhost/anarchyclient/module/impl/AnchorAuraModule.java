package net.blockhost.anarchyclient.module.impl;

import com.google.gson.JsonObject;
import net.minecraft.world.InteractionHand;
import net.blockhost.anarchyclient.config.ModuleConfigMigration;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.render.CuboidMarker;
import net.blockhost.anarchyclient.render.MarkerManager;
import net.blockhost.anarchyclient.render.MarkerStyle;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

public final class AnchorAuraModule extends Module implements ModuleConfigMigration {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(5.0)
            .min(1.0)
            .max(8.0)
            .step(0.5)
            .build()));
    private final NumberSetting minDamage = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("min_damage")
            .name("Min Damage")
            .defaultValue(6.0)
            .min(0.0)
            .max(36.0)
            .step(0.5)
            .build()));
    private final NumberSetting maxSelfDamage = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("max_self_damage")
            .name("Self Damage")
            .defaultValue(12.0)
            .min(0.0)
            .max(36.0)
            .step(1.0)
            .build()));
    private final NumberSetting facePlaceHealth = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("face_place_health")
            .name("Face Place")
            .defaultValue(8.0)
            .min(0.0)
            .max(36.0)
            .step(1.0)
            .build()));
    private final NumberSetting delay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay")
            .name("Delay")
            .defaultValue(3.0)
            .min(0.0)
            .max(20.0)
            .step(1.0)
            .build()));
    private final BooleanSetting place = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("place")
            .name("Place")
            .defaultValue(true)
            .build()));
    private final BooleanSetting charge = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("charge")
            .name("Charge")
            .defaultValue(true)
            .build()));
    private final BooleanSetting render = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("render")
            .name("Render")
            .defaultValue(true)
            .build()));
    private int cooldown;

    public AnchorAuraModule() {
        super("anchor_aura", "Anchor Aura", ModuleCategory.COMBAT,
                "Places, charges with one glowstone, and detonates respawn anchors within reach near targets. "
                        + "Inactive in the Nether. Does not charge or detonate while you hold glowstone in your offhand.");
    }

    /** Moves configs saved with the old defaults to the current ones. */
    @Override
    public void migrateConfig(final JsonObject moduleJson, final int savedVersion) {
        if (savedVersion >= VANILLA_DAMAGE_DEFAULTS) {
            return;
        }
        ModuleConfigMigration.upgradeDefault(moduleJson, "min_damage", 5.0, 6.0);
        ModuleConfigMigration.upgradeDefault(moduleJson, "max_self_damage", 8.0, 12.0);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null) {
            return;
        }
        // Anchors set the spawn point instead of exploding in the Nether.
        if (client.level.dimension() == Level.NETHER) {
            return;
        }
        if (this.cooldown > 0) {
            this.cooldown--;
            return;
        }
        Player target = CombatTargets.nearestEnemy(client, this.range.value());
        if (target == null) {
            return;
        }
        ScoredAnchor anchor = bestAnchor(client, target, this.range.value().intValue(), this.maxSelfDamage.value(),
                minimumDamage(target, this.minDamage.value(), this.facePlaceHealth.value()));
        if (anchor != null) {
            BlockState state = client.level.getBlockState(anchor.pos());
            int charges = state.getValue(RespawnAnchorBlock.CHARGE);
            if (charges > 0) {
                this.detonate(client, anchor);
                return;
            }
            // With glowstone in the offhand the anchor cannot be detonated, so charging would only leave it armed.
            if (this.charge.value() && !client.player.getOffhandItem().is(Items.GLOWSTONE)) {
                WorldInteraction.ActionResult result = WorldInteraction.useOnBlock(client, this, anchor.pos(), Direction.UP,
                        stack -> stack.is(Items.GLOWSTONE), true);
                if (result == WorldInteraction.ActionResult.DONE) {
                    this.cooldown = this.delay.value().intValue();
                }
                return;
            }
            return;
        }
        if (!this.place.value()) {
            return;
        }
        ScoredAnchor placement = bestAnchorPlacement(client, target, this.maxSelfDamage.value(), minimumDamage(target, this.minDamage.value(), this.facePlaceHealth.value()));
        if (placement != null) {
            BlockPlacement.PlacementResult result = BlockPlacement.place(client, this, placement.pos(), true, 70.0F,
                    stack -> stack.is(Items.RESPAWN_ANCHOR));
            if (result == BlockPlacement.PlacementResult.PLACED) {
                this.cooldown = this.delay.value().intValue();
                this.mark(placement.pos());
            }
        }
    }

    /**
     * Clicks a charged anchor with a non-glowstone item. Clicking with glowstone would add another
     * charge instead of detonating, burning up to four glowstone per anchor.
     */
    private void detonate(final Minecraft client, final ScoredAnchor anchor) {
        LocalPlayer player = client.player;
        OptionalInt slot = detonationSlot(player.getInventory());
        if (slot.isEmpty() || player.getOffhandItem().is(Items.GLOWSTONE)) {
            // An offhand glowstone charges the anchor instead of letting the main hand detonate it.
            return;
        }
        InventoryActions.selectHotbarSlot(player, slot.getAsInt());
        InteractionResult result = client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(anchor.pos()), Direction.UP, anchor.pos(), false));
        if (!result.consumesAction()) {
            return;
        }
        player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
        this.mark(anchor.pos());
        this.cooldown = this.delay.value().intValue();
        this.debugValue("damage", String.format("%.1f/%.1f", anchor.targetDamage(), anchor.selfDamage()));
    }

    /** Returns the selected hotbar slot if it holds no glowstone, else the first hotbar slot without glowstone. */
    static OptionalInt detonationSlot(final Inventory inventory) {
        int selected = inventory.getSelectedSlot();
        if (!inventory.getItem(selected).is(Items.GLOWSTONE)) {
            return OptionalInt.of(selected);
        }
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            if (!inventory.getItem(slot).is(Items.GLOWSTONE)) {
                return OptionalInt.of(slot);
            }
        }
        return OptionalInt.empty();
    }

    static BlockPos findAnchor(final Minecraft client, final BlockPos center, final int radius) {
        if (client.level == null) {
            return null;
        }
        for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
            for (int y = center.getY() - 1; y <= center.getY() + 2; y++) {
                for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (client.level.getBlockState(pos).getBlock() instanceof RespawnAnchorBlock) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }

    private static ScoredAnchor bestAnchor(final Minecraft client, final Player target, final int radius,
                                           final double maxSelfDamage, final double minDamage) {
        if (client.level == null || client.player == null) {
            return null;
        }
        List<ScoredAnchor> scores = new ArrayList<>();
        for (int x = target.blockPosition().getX() - radius; x <= target.blockPosition().getX() + radius; x++) {
            for (int y = target.blockPosition().getY() - 1; y <= target.blockPosition().getY() + 2; y++) {
                for (int z = target.blockPosition().getZ() - radius; z <= target.blockPosition().getZ() + radius; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (client.level.getBlockState(pos).getBlock() instanceof RespawnAnchorBlock
                            && CombatTargets.canReachBlock(client.player, pos)) {
                        score(client, target, pos, maxSelfDamage, minDamage).ifPresent(scores::add);
                    }
                }
            }
        }
        return best(scores);
    }

    private static ScoredAnchor bestAnchorPlacement(final Minecraft client, final Player target,
                                                    final double maxSelfDamage, final double minDamage) {
        if (client.level == null || client.player == null) {
            return null;
        }
        List<ScoredAnchor> scores = new ArrayList<>();
        BlockPos center = target.blockPosition();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            for (int y = 0; y <= 1; y++) {
                BlockPos pos = center.relative(direction).above(y);
                if (CombatTargets.canReachBlock(client.player, pos) && CombatPlacementPlanner.isReplaceable(client.level, pos)) {
                    score(client, target, pos, maxSelfDamage, minDamage).ifPresent(scores::add);
                }
            }
        }
        return best(scores);
    }

    private static Optional<ScoredAnchor> score(final Minecraft client, final Player target, final BlockPos pos,
                                                         final double maxSelfDamage, final double minDamage) {
        Vec3 explosion = DamageEstimator.blockCenter(pos);
        // Vanilla removes the anchor before it explodes, so the anchor does not shield anyone.
        Set<BlockPos> removed = Set.of(pos.immutable());
        double targetDamage = DamageEstimator.explosionDamage(target, explosion, DamageEstimator.ANCHOR_POWER, removed);
        double selfDamage = DamageEstimator.explosionDamage(client.player, explosion, DamageEstimator.ANCHOR_POWER, removed);
        double selfHealth = client.player.getHealth() + client.player.getAbsorptionAmount();
        if (targetDamage < minDamage || selfDamage > maxSelfDamage || selfDamage >= selfHealth) {
            return Optional.empty();
        }
        return Optional.of(new ScoredAnchor(pos.immutable(), targetDamage, selfDamage));
    }

    private static ScoredAnchor best(final List<ScoredAnchor> scores) {
        return scores.stream()
                .max(Comparator.comparingDouble(ScoredAnchor::value))
                .orElse(null);
    }

    private static double minimumDamage(final Player target, final double minDamage, final double facePlaceHealth) {
        return target.getHealth() + target.getAbsorptionAmount() <= facePlaceHealth ? 0.0 : minDamage;
    }

    private void mark(final BlockPos pos) {
        if (this.render.value()) {
            MarkerManager.put(new CuboidMarker(this.id() + ":anchor", new AABB(pos), MarkerStyle.CYAN, 8));
        }
    }

    private record ScoredAnchor(BlockPos pos, double targetDamage, double selfDamage) {

        double value() {
            return this.targetDamage - this.selfDamage * 0.7;
        }
    }
}
