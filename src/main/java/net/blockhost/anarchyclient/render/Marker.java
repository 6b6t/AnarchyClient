package net.blockhost.anarchyclient.render;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.world.phys.Vec3;

public interface Marker {

    String id();

    int ageTicks();

    int lifetimeTicks();

    Marker ticked();

    void render(LevelRenderContext context, Vec3 camera);

    default boolean expired() {
        return this.lifetimeTicks() > 0 && this.ageTicks() >= this.lifetimeTicks();
    }
}
