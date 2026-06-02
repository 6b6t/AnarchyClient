package net.blockhost.anarchyclient.rivet;

import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.backend.Backend;
import net.lenni0451.rivet.backend.text.ShapedText;
import net.lenni0451.rivet.backend.text.ShapedTextBlock;
import net.lenni0451.rivet.input.keyboard.Key;
import net.lenni0451.rivet.text.model.TextBlock;
import net.lenni0451.rivet.text.model.TextLine;
import net.lenni0451.rivet.text.model.TextSection;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;

public final class Blaze3DBackend implements Backend {

    private final Minecraft client;

    public Blaze3DBackend(final Minecraft client) {
        this.client = client;
    }

    @Override
    public float getTextHeight() {
        return this.client.font.lineHeight;
    }

    @Override
    public ShapedText shapeText(final String text, final Color color) {
        return new MinecraftShapedText(this.client.font, text);
    }

    @Override
    public ShapedText shapeText(final TextLine line) {
        StringBuilder builder = new StringBuilder();
        for (TextSection section : line.sections()) {
            builder.append(section.text());
        }
        return new MinecraftShapedText(this.client.font, builder.toString());
    }

    @Override
    public ShapedTextBlock shapeText(final TextBlock block) {
        StringBuilder builder = new StringBuilder();
        for (TextLine line : block.lines()) {
            if (!builder.isEmpty()) {
                builder.append('\n');
            }
            for (TextSection section : line.sections()) {
                builder.append(section.text());
            }
        }
        return new MinecraftShapedText(this.client.font, builder.toString());
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
        return GLFW.glfwGetKey(this.client.getWindow().handle(), RivetInputMapper.toGlfw(key)) == GLFW.GLFW_PRESS;
    }
}
