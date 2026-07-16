package net.blockhost.anarchyclient.rivet;

import net.lenni0451.rivet.backend.render.deferred.RenderCommand;
import net.lenni0451.rivet.math.Rectangle;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.function.Consumer;

public record Blaze3DRenderCommand(Consumer<GuiGraphicsExtractor> action, Rectangle bounds) implements RenderCommand.Custom {
}
