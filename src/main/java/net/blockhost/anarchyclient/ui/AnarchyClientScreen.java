package net.blockhost.anarchyclient.ui;

import net.blockhost.anarchyclient.config.ClientConfig;
import net.blockhost.anarchyclient.module.ModuleManager;
import net.blockhost.anarchyclient.module.impl.BackgroundModule;
import net.blockhost.anarchyclient.rivet.BackgroundDesign;
import net.blockhost.anarchyclient.rivet.Blaze3DBackend;
import net.blockhost.anarchyclient.rivet.Blaze3DRenderer;
import net.blockhost.anarchyclient.rivet.RivetInputMapper;
import net.lenni0451.rivet.Rivet;
import net.lenni0451.rivet.input.keyboard.CharEvent;
import net.lenni0451.rivet.input.mouse.MouseMoveEvent;
import net.lenni0451.rivet.input.mouse.MouseScrollEvent;
import net.lenni0451.rivet.layout.fullsize.FullSizeLayout;
import net.lenni0451.rivet.math.Size;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class AnarchyClientScreen extends Screen {

    private static final int MATRIX_BACKDROP_TOP = 0x78020A05;
    private static final int MATRIX_BACKDROP_BOTTOM = 0xB8001008;

    private final ModuleManager modules;
    private final ClientConfig config;
    private Rivet rivet;

    public AnarchyClientScreen(final ModuleManager modules, final ClientConfig config) {
        super(Component.literal("AnarchyClient"));
        this.modules = modules;
        this.config = config;
    }

    @Override
    protected void init() {
        Minecraft client = Minecraft.getInstance();
        this.rivet = new Rivet(new Blaze3DBackend(client), FullSizeLayout.INSTANCE, new Size(this.width, this.height));
        this.rivet.theme(new AnarchyClientTheme()).snapToInteger(true);
        this.rivet.root().addChild(new ModulePanel(this.modules, this.config));
    }

    @Override
    public void resize(final int width, final int height) {
        super.resize(width, height);
        if (this.rivet != null) {
            this.rivet.size(new Size(width, height));
        }
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTick) {
        if (this.rivet != null) {
            this.rivet.size(new Size(this.width, this.height));
            this.rivet.onMouseMove(new MouseMoveEvent(mouseX, mouseY));
            new Blaze3DRenderer(Minecraft.getInstance(), graphics, this.backgroundDesign()).render(this.rivet.render());
        }
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTick) {
        graphics.blurBeforeThisStratum();
        graphics.fillGradient(0, 0, this.width, this.height, MATRIX_BACKDROP_TOP, MATRIX_BACKDROP_BOTTOM);
    }

    private BackgroundDesign backgroundDesign() {
        return this.modules.find("background")
                .filter(BackgroundModule.class::isInstance)
                .map(BackgroundModule.class::cast)
                .map(BackgroundModule::selectedDesign)
                .orElse(BackgroundDesign.NONE);
    }

    @Override
    public void mouseMoved(final double mouseX, final double mouseY) {
        if (this.rivet != null) {
            this.rivet.onMouseMove(new MouseMoveEvent((float) mouseX, (float) mouseY));
        }
    }

    @Override
    public boolean mouseClicked(final net.minecraft.client.input.MouseButtonEvent event, final boolean doubleClick) {
        return this.rivet != null && this.rivet.onMouseDown(RivetInputMapper.mouse(event));
    }

    @Override
    public boolean mouseReleased(final net.minecraft.client.input.MouseButtonEvent event) {
        return this.rivet != null && this.rivet.onMouseUp(RivetInputMapper.mouse(event));
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX, final double scrollY) {
        return this.rivet != null && this.rivet.onMouseScroll(new MouseScrollEvent((float) mouseX, (float) mouseY, (float) scrollX, (float) scrollY));
    }

    @Override
    public boolean keyPressed(final net.minecraft.client.input.KeyEvent event) {
        if (event.isEscape()) {
            this.onClose();
            return true;
        }
        return this.rivet != null && RivetInputMapper.key(event).map(this.rivet::onKeyDown).orElse(false);
    }

    @Override
    public boolean keyReleased(final net.minecraft.client.input.KeyEvent event) {
        return this.rivet != null && RivetInputMapper.key(event).map(this.rivet::onKeyUp).orElse(false);
    }

    @Override
    public boolean charTyped(final net.minecraft.client.input.CharacterEvent event) {
        String text = event.codepointAsString();
        if (text.isEmpty()) {
            return false;
        }
        if (this.rivet == null) {
            return false;
        }
        boolean handled = false;
        for (int index = 0; index < text.length(); index++) {
            handled |= this.rivet.onCharTyped(new CharEvent(text.charAt(index)));
        }
        return handled;
    }

    @Override
    public void onClose() {
        this.config.save();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
