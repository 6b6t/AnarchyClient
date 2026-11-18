package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

public final class SpiralModule extends Module {

    private static final int[][] DIRECTIONS = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};

    private final NumberSetting spacing = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("spacing")
            .name("Spacing")
            .defaultValue(64.0)
            .min(8.0)
            .max(512.0)
            .step(8.0)
            .description("Blocks between two rings of the spiral. Match it to your render distance to scan every chunk once.")
            .build()));
    private final BooleanSetting sprint = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("sprint")
            .name("Sprint")
            .defaultValue(true)
            .build()));
    private double originX;
    private double originZ;
    private double cornerX;
    private double cornerZ;
    private int leg;
    private boolean started;

    public SpiralModule() {
        super("spiral", "Spiral", ModuleCategory.MOVEMENT,
                "Walks or flies outward in a square spiral from where you turned it on, to search an area evenly. "
                        + "It only turns you and holds forward, so pair it with Elytra Fly or Auto Walk settings as needed.");
    }

    @Override
    protected void onEnable() {
        this.started = false;
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) {
            return;
        }
        if (!this.started) {
            this.originX = player.getX();
            this.originZ = player.getZ();
            this.leg = 0;
            double[] corner = corner(this.originX, this.originZ, 0, this.spacing.value());
            this.cornerX = corner[0];
            this.cornerZ = corner[1];
            this.started = true;
        }
        double dx = this.cornerX - player.getX();
        double dz = this.cornerZ - player.getZ();
        if (dx * dx + dz * dz < 1.0) {
            this.leg++;
            double[] corner = corner(this.originX, this.originZ, this.leg, this.spacing.value());
            this.cornerX = corner[0];
            this.cornerZ = corner[1];
            dx = this.cornerX - player.getX();
            dz = this.cornerZ - player.getZ();
        }
        player.setYRot((float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F);
        this.debugValue("leg", this.leg);
        this.debugValue("corner", Math.round(this.cornerX) + ", " + Math.round(this.cornerZ));
    }

    @Override
    public void updateInput(final Minecraft client, final ClientInput input) {
        if (!this.started || client.player == null || input == null) {
            return;
        }
        input.keyPresses = InputStates.withForward(input.keyPresses, true);
        if (this.sprint.value()) {
            input.keyPresses = InputStates.withSprint(input.keyPresses, true);
        }
        InputStates.refreshMoveVector(input);
    }

    /**
     * Returns the end of leg {@code leg} of a square spiral that starts at the origin. Leg
     * lengths grow 1, 1, 2, 2, 3, 3 times the spacing, turning a quarter each leg.
     */
    static double[] corner(final double originX, final double originZ, final int leg, final double spacing) {
        double x = originX;
        double z = originZ;
        for (int index = 0; index <= leg; index++) {
            int[] direction = DIRECTIONS[index % DIRECTIONS.length];
            double length = spacing * (index / 2 + 1);
            x += direction[0] * length;
            z += direction[1] * length;
        }
        return new double[] {x, z};
    }

    @Override
    protected void onDisable() {
        this.started = false;
    }
}
