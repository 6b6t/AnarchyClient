package net.blockhost.anarchyclient.ui;

import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.config.ClientConfig;
import net.blockhost.anarchyclient.module.ModuleManager;
import net.blockhost.anarchyclient.rivet.Blaze3DBackend;
import net.blockhost.anarchyclient.rivet.Blaze3DRenderer;
import net.blockhost.anarchyclient.rivet.GlassBackdrop;
import net.blockhost.anarchyclient.rivet.RivetInputMapper;
import net.fabricmc.loader.api.FabricLoader;
import net.lenni0451.rivet.Rivet;
import net.lenni0451.rivet.backend.render.SnappedRenderer;
import net.lenni0451.rivet.input.keyboard.CharEvent;
import net.lenni0451.rivet.input.keyboard.Key;
import net.lenni0451.rivet.input.keyboard.KeyEvent;
import net.lenni0451.rivet.input.keyboard.ModifierKey;
import net.lenni0451.rivet.input.mouse.MouseButton;
import net.lenni0451.rivet.input.mouse.MouseMoveEvent;
import net.lenni0451.rivet.input.mouse.MouseScrollEvent;
import net.lenni0451.rivet.layout.fullsize.FullSizeLayout;
import net.lenni0451.rivet.math.Size;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

public final class AnarchyClientScreen extends Screen {

    // The menu is designed for GUI Scale 2. Countering Minecraft's GUI scale with 2/factor keeps
    // the menu's real on-screen size constant no matter what GUI Scale the user picks, and gives
    // it the same virtual room (real pixels / 2) so it always fits the screen. Clamped like the
    // BleachHack auto-fit so extreme factors cannot shrink the UI into unreadability.
    private static final float REFERENCE_GUI_SCALE = 2F;

    private final ModuleManager modules;
    private final ClientConfig config;
    private final Set<MouseButton> heldMouseButtons = EnumSet.noneOf(MouseButton.class);
    private Rivet rivet;
    private ModulePanel panel;

    public AnarchyClientScreen(final ModuleManager modules, final ClientConfig config) {
        super(Component.literal("AnarchyClient"));
        this.modules = modules;
        this.config = config;
    }

    @Override
    protected void init() {
        Minecraft client = Minecraft.getInstance();
        GlassBackdrop.activate();
        this.rivet = new Rivet(new Blaze3DBackend(client), FullSizeLayout.INSTANCE, this.virtualSize());
        this.rivet.theme(new AnarchyClientTheme(this.config.uiPreferences().guiTheme()));
        this.panel = new ModulePanel(this.modules, this.config);
        this.rivet.root().addChild(this.panel);
    }

    private float uiScale() {
        int factor = Math.max(1, Minecraft.getInstance().getWindow().getGuiScale());
        return Math.max(0.5F, Math.min(2F, REFERENCE_GUI_SCALE / factor));
    }

    private Size virtualSize() {
        float scale = this.uiScale();
        return new Size(this.width / scale, this.height / scale);
    }

    @Override
    public void resize(final int width, final int height) {
        super.resize(width, height);
        if (this.rivet != null) {
            this.rivet.size(this.virtualSize());
        }
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTick) {
        if (this.rivet == null) {
            return;
        }
        float scale = this.uiScale();
        this.rivet.size(this.virtualSize());
        this.rivet.onMouseMove(new MouseMoveEvent(mouseX / scale, mouseY / scale, this.heldMouseButtons));
        if (this.panel != null) {
            this.panel.mousePosition(mouseX / scale, mouseY / scale);
        }
        graphics.pose().pushMatrix();
        graphics.pose().scale(scale, scale);
        try {
            this.rivet.render(new SnappedRenderer<>(new Blaze3DRenderer(Minecraft.getInstance(), graphics)));
        } catch (Throwable throwable) {
            // Never let a render failure crash the game: log it and close the menu cleanly.
            AnarchyClient.LOGGER.error("AnarchyClient menu render failed; closing menu", throwable);
            graphics.pose().popMatrix();
            this.onClose();
            return;
        }
        graphics.pose().popMatrix();
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTick) {
        // Triggers the vanilla blur pass. GlassBackdrop captures the blurred frame for the glass
        // panels and restores the sharp frame, so the game stays fully visible behind the menu.
        graphics.blurBeforeThisStratum();
    }

    @Override
    public void mouseMoved(final double mouseX, final double mouseY) {
        if (this.rivet != null) {
            float scale = this.uiScale();
            this.rivet.onMouseMove(new MouseMoveEvent((float) (mouseX / scale), (float) (mouseY / scale), this.heldMouseButtons));
        }
    }

    @Override
    public boolean mouseClicked(final net.minecraft.client.input.MouseButtonEvent event, final boolean doubleClick) {
        if (this.rivet == null) return false;
        net.lenni0451.rivet.input.mouse.MouseButtonEvent mapped = this.toVirtualSpace(RivetInputMapper.mouse(event));
        this.heldMouseButtons.add(mapped.button());
        return this.rivet.onMouseDown(mapped.withHeldButtons(this.heldMouseButtons));
    }

    @Override
    public boolean mouseReleased(final net.minecraft.client.input.MouseButtonEvent event) {
        if (this.rivet == null) return false;
        net.lenni0451.rivet.input.mouse.MouseButtonEvent mapped = this.toVirtualSpace(RivetInputMapper.mouse(event));
        try {
            return this.rivet.onMouseUp(mapped.withHeldButtons(this.heldMouseButtons));
        } finally {
            this.heldMouseButtons.remove(mapped.button());
        }
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX, final double scrollY) {
        if (this.rivet == null) {
            return false;
        }
        float scale = this.uiScale();
        return this.rivet.onMouseScroll(new MouseScrollEvent(
                (float) (mouseX / scale), (float) (mouseY / scale), (float) scrollX, (float) scrollY));
    }

    /** Rivet lays out and hit-tests in virtual (reference scale) coordinates; convert real clicks. */
    private net.lenni0451.rivet.input.mouse.MouseButtonEvent toVirtualSpace(final net.lenni0451.rivet.input.mouse.MouseButtonEvent event) {
        float scale = this.uiScale();
        return scale == 1F ? event : event.withXBy(x -> x / scale).withYBy(y -> y / scale);
    }

    @Override
    public boolean keyPressed(final net.minecraft.client.input.KeyEvent event) {
        if (event.isEscape()) {
            this.onClose();
            return true;
        }
        Optional<KeyEvent> key = RivetInputMapper.key(event);
        if (this.rivet != null && key.filter(AnarchyClientScreen::isLayoutDumpKey).isPresent()) {
            this.dumpLayoutTree();
            return true;
        }
        return this.rivet != null && key.map(this.rivet::onKeyDown).orElse(false);
    }

    @Override
    public boolean keyReleased(final net.minecraft.client.input.KeyEvent event) {
        return this.rivet != null && RivetInputMapper.key(event).map(this.rivet::onKeyUp).orElse(false);
    }

    @Override
    public boolean charTyped(final net.minecraft.client.input.CharacterEvent event) {
        return this.rivet != null && this.rivet.onCharTyped(new CharEvent(event.codepoint()));
    }

    @Override
    public void onClose() {
        GlassBackdrop.deactivate();
        this.config.save();
        if (this.rivet != null) {
            this.rivet.dispose();
            this.rivet = null;
        }
        this.heldMouseButtons.clear();
        super.onClose();
    }

    @Override
    public void removed() {
        // Screens replaced via setScreen() skip onClose(); the glass capture must still stop so
        // vanilla menus keep their normal full-screen blur.
        GlassBackdrop.deactivate();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static boolean isLayoutDumpKey(final KeyEvent event) {
        return event.key().isEquivalent(Key.L)
                && event.modifiers().contains(ModifierKey.CONTROL)
                && event.modifiers().contains(ModifierKey.SHIFT);
    }

    private void dumpLayoutTree() {
        if (this.rivet == null) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        Path directory = FabricLoader.getInstance().getConfigDir().resolve(AnarchyClient.MOD_ID + "-debug");
        try {
            Path file = LayoutTreeDumper.writeSnapshot(this.rivet, directory);
            AnarchyClient.LOGGER.info("Layout tree snapshot saved to {}", file);
            if (client.player != null) {
                client.player.sendSystemMessage(Component.literal("Layout tree saved to " + file + "."));
            }
        } catch (IOException exception) {
            AnarchyClient.LOGGER.warn("Failed to save layout tree snapshot", exception);
            if (client.player != null) {
                client.player.sendSystemMessage(Component.literal("Failed to save layout tree snapshot."));
            }
        }
    }
}
