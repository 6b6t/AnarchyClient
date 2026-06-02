package net.blockhost.anarchyclient.rivet;

import net.lenni0451.rivet.backend.Texture;
import net.minecraft.resources.Identifier;

public record MinecraftTexture(Identifier identifier, int textureWidth, int textureHeight, int x, int y, int width, int height) implements Texture {

    public MinecraftTexture(final Identifier identifier, final int width, final int height) {
        this(identifier, width, height, 0, 0, width, height);
    }

    @Override
    public Texture subTexture(final int x, final int y, final int width, final int height) {
        return new MinecraftTexture(this.identifier, this.textureWidth, this.textureHeight, this.x + x, this.y + y, width, height);
    }
}
