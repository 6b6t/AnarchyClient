package net.blockhost.anarchyclient.rivet;

import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.backend.text.ShapedText;
import net.lenni0451.rivet.text.model.TextLine;
import net.minecraft.client.gui.Font;

final class MinecraftFont implements net.lenni0451.rivet.backend.text.Font {

    private final Font delegate;
    private final int size;

    MinecraftFont(final Font delegate) {
        this(delegate, delegate.lineHeight);
    }

    private MinecraftFont(final Font delegate, final int size) {
        this.delegate = delegate;
        this.size = size;
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public float height() {
        return this.size;
    }

    @Override
    public net.lenni0451.rivet.backend.text.Font derive(final int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Font size must be positive");
        }
        return size == this.size ? this : new MinecraftFont(this.delegate, size);
    }

    @Override
    public ShapedText shapeText(final String text, final Color color) {
        return new MinecraftShapedText(this.delegate, this.size, text, color);
    }

    @Override
    public ShapedText shapeText(final TextLine line) {
        return new MinecraftShapedText(this.delegate, this.size, line);
    }
}
