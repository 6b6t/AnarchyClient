package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class StepModule extends Module {

    private static final Identifier MODIFIER_ID = Identifier.fromNamespaceAndPath(AnarchyClient.MOD_ID, "step_height");

    private final NumberSetting height = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("height")
            .name("Height")
            .defaultValue(1.0)
            .min(0.6)
            .max(2.5)
            .step(0.1)
            .build()));

    public StepModule() {
        super("step", "Step", ModuleCategory.MOVEMENT);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null) {
            return;
        }
        AttributeInstance attribute = client.player.getAttribute(Attributes.STEP_HEIGHT);
        if (attribute == null) {
            return;
        }
        double amount = Math.max(0.0, this.height.value() - attribute.getBaseValue());
        attribute.addOrUpdateTransientModifier(new AttributeModifier(
                MODIFIER_ID,
                amount,
                AttributeModifier.Operation.ADD_VALUE
        ));
    }

    @Override
    protected void onDisable() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            AttributeInstance attribute = client.player.getAttribute(Attributes.STEP_HEIGHT);
            if (attribute != null) {
                attribute.removeModifier(MODIFIER_ID);
            }
        }
    }
}
