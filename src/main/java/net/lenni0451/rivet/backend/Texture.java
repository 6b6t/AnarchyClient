package net.lenni0451.rivet.backend;

public interface Texture {

    int width();

    int height();

    Texture subTexture(final int x, final int y, final int width, final int height);

}
