package net.blockhost.anarchyclient.rivet;

import net.lenni0451.rivet.backend.Backend;
import net.lenni0451.rivet.backend.text.Font;
import net.lenni0451.rivet.input.keyboard.Key;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;

public final class Blaze3DBackend implements Backend {

    private final Minecraft client;
    private final Font font;

    public Blaze3DBackend(final Minecraft client) {
        this.client = client;
        this.font = new MinecraftFont(client.font);
    }

    @Override
    public Font font() {
        return this.font;
    }

    @Override
    @Nullable
    public String getClipboard() {
        String clipboard = GLFW.glfwGetClipboardString(this.client.getWindow().handle());
        return clipboard == null || clipboard.isEmpty() ? null : clipboard;
    }

    @Override
    public void setClipboard(final String clipboard) {
        GLFW.glfwSetClipboardString(this.client.getWindow().handle(), clipboard);
    }

    @Override
    public boolean isKeyDown(final Key key) {
        return RivetInputMapper.toGlfw(key)
                .stream()
                .anyMatch(glfwKey -> GLFW.glfwGetKey(this.client.getWindow().handle(), glfwKey) == GLFW.GLFW_PRESS);
    }
}
