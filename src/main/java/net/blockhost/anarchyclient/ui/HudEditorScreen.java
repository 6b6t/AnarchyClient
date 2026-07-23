package net.blockhost.anarchyclient.ui;

import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.config.ClientConfig;
import net.blockhost.anarchyclient.event.HudRenderEvent;
import net.blockhost.anarchyclient.module.ModuleManager;
import net.blockhost.anarchyclient.rivet.Blaze3DRenderer;
import net.blockhost.anarchyclient.rivet.GlassBackdrop;
import net.blockhost.anarchyclient.rivet.GlassPanelCommand;
import net.blockhost.anarchyclient.rivet.SoftShadowCommand;
import net.lenni0451.commons.color.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * BleachHack/Meteor-style HUD editor: renders the live HUD elements at their stored positions and lets
 * the player drag them around. No module list — element placement only; per-module settings stay in the
 * menu inspector. Opened from the button on the HUD tab. Positions are held by {@link HudLayout}.
 *
 * <p>The chrome is drawn through the same {@link Blaze3DRenderer} glass/SDF pipeline as the main menu —
 * anti-aliased rounded frames, an accent active stripe, a soft drop shadow and a frosted-glass hint chip
 * — so the editor matches the liquid-glass design instead of using raw debug rectangles.</p>
 */
public final class HudEditorScreen extends Screen {

    private static final int SNAP = 6;
    private static final int MARGIN = 6;
    private static final float FRAME_PAD = 3F;
    private static final float FRAME_RADIUS = 6F;
    private static final Color FRAME_FILL = Color.fromRGBA(255, 255, 255, 12);
    private static final Color FRAME_OUTLINE = Color.fromRGBA(255, 255, 255, 70);
    private static final Color SHADOW = Color.fromRGBA(0, 0, 0, 110);

    // Set only while this screen renders its own HUD preview pass, so the normal (vanilla-registered)
    // HUD path stays suppressed and nothing double-draws. Read by every HUD module's render guard.
    private static boolean rendering;

    private final ModuleManager modules;
    private final ClientConfig config;

    private String dragId;
    private int dragOffsetX;
    private int dragOffsetY;
    private boolean dirty;

    public HudEditorScreen(final ModuleManager modules, final ClientConfig config) {
        super(Component.literal("HUD Editor"));
        this.modules = modules;
        this.config = config;
    }

    /** True while a HUD module must not render itself (menu open, or the editor's non-preview passes). */
    public static boolean suppressed(final Minecraft client) {
        Screen screen = client.gui.screen();
        if (screen instanceof AnarchyClientScreen) {
            return true;
        }
        if (screen instanceof HudEditorScreen) {
            return !rendering;
        }
        return false;
    }

    @Override
    protected void init() {
        // Capture a blurred copy of the frame so the frosted-glass hint chip can refract it; the game
        // itself stays sharp (GlassBackdrop restores the crisp frame after the capture).
        GlassBackdrop.activate();
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
                                  final float partialTick) {
        // Triggers the vanilla blur pass GlassBackdrop samples. The game stays fully visible and crisp.
        graphics.blurBeforeThisStratum();
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
                                   final float partialTick) {
        // Draw the HUD elements ourselves so they appear over the game while this screen is open.
        HudLayout.clearBounds();
        rendering = true;
        try {
            AnarchyClient.MODULES.call(new HudRenderEvent(this.minecraft, graphics));
        } catch (Throwable throwable) {
            AnarchyClient.LOGGER.error("HUD editor preview render failed", throwable);
        } finally {
            rendering = false;
        }

        Blaze3DRenderer renderer = new Blaze3DRenderer(this.minecraft, graphics);
        Color accent = GlassTheme.accent();
        for (HudLayout.Element element : HudLayout.elements()) {
            this.renderHandle(renderer, graphics, element, element.id().equals(this.dragId) || element.contains(mouseX, mouseY), accent);
        }
        this.renderHint(renderer, graphics);
    }

    /** A rounded, anti-aliased frame around an element with an accent active stripe and floating label. */
    private void renderHandle(final Blaze3DRenderer renderer, final GuiGraphicsExtractor graphics,
                              final HudLayout.Element element, final boolean active, final Color accent) {
        float x = element.x() - FRAME_PAD;
        float y = element.y() - FRAME_PAD;
        float width = element.width() + FRAME_PAD * 2F;
        float height = element.height() + FRAME_PAD * 2F;

        renderer.fillRoundedRect(x, y, width, height, FRAME_RADIUS, active ? accent.multiplyAlpha(0.16F) : FRAME_FILL);
        renderer.outlineRoundedRect(x, y, width, height, FRAME_RADIUS, active ? 1.5F : 1F, active ? accent : FRAME_OUTLINE);
        if (active) {
            renderer.fillRoundedRect(x + 1.5F, y + 4F, 2F, Math.max(2F, height - 8F), 1F, accent);
        }
        int labelColor = (active ? accent : GlassTheme.MUTED).toARGB();
        graphics.text(this.font, element.name(), Math.round(x) + 4, Math.round(y) - 11, labelColor, true);
    }

    /** Frosted-glass hint chip, bottom-center, matching the menu's panels. */
    private void renderHint(final Blaze3DRenderer renderer, final GuiGraphicsExtractor graphics) {
        String hint = "Drag to move   •   Right-click to reset   •   Esc to save & exit";
        float textWidth = this.font.width(hint);
        float width = textWidth + 24F;
        float height = 20F;
        float x = (graphics.guiWidth() - width) / 2F;
        float y = graphics.guiHeight() - height - 10F;
        float radius = Math.min(GlassTheme.cornerRadius(), height / 2F);

        renderer.custom(new SoftShadowCommand(x, y, width, height, radius, 10F, 3F, SHADOW));
        renderer.custom(new GlassPanelCommand(x, y, width, height, radius, GlassTheme.glass()));
        graphics.text(this.font, hint, Math.round(x + 12F), Math.round(y + (height - 8F) / 2F), GlassTheme.TEXT.toARGB(), false);
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        HudLayout.Element hit = topElementAt(event.x(), event.y());
        if (hit == null) {
            return super.mouseClicked(event, doubleClick);
        }
        if (event.button() == 1) {
            HudLayout.reset(hit.id());
            this.dirty = true;
            return true;
        }
        if (event.button() == 0) {
            this.dragId = hit.id();
            this.dragOffsetX = (int) Math.round(event.x()) - hit.x();
            this.dragOffsetY = (int) Math.round(event.y()) - hit.y();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(final MouseButtonEvent event, final double dragX, final double dragY) {
        if (this.dragId == null) {
            return super.mouseDragged(event, dragX, dragY);
        }
        HudLayout.Element element = elementById(this.dragId);
        if (element == null) {
            // The element stopped rendering mid-drag; keep its stored position instead of writing a
            // zero-sized snap over it.
            return true;
        }
        int x = snap((int) Math.round(event.x()) - this.dragOffsetX, element.width(), this.width);
        int y = snap((int) Math.round(event.y()) - this.dragOffsetY, element.height(), this.height);
        HudLayout.move(this.dragId, x, y);
        this.dirty = true;
        return true;
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        if (this.dragId != null) {
            this.dragId = null;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public void onClose() {
        if (this.dirty) {
            HudLayout.save();
        }
        GlassBackdrop.deactivate();
        // Return to the client menu rather than the game, so the editor feels like part of the menu.
        this.minecraft.gui.setScreen(new AnarchyClientScreen(this.modules, this.config));
    }

    @Override
    public void removed() {
        // Screens swapped via setScreen() skip onClose(); the glass capture must still stop.
        GlassBackdrop.deactivate();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private HudLayout.Element topElementAt(final double px, final double py) {
        HudLayout.Element found = null;
        for (HudLayout.Element element : HudLayout.elements()) {
            if (element.contains(px, py)) {
                found = element;
            }
        }
        return found;
    }

    private static HudLayout.Element elementById(final String id) {
        for (HudLayout.Element element : HudLayout.elements()) {
            if (element.id().equals(id)) {
                return element;
            }
        }
        return null;
    }

    /** Snap an axis to the near screen edge (inset {@value #MARGIN}) or center within {@value #SNAP} px. */
    static int snap(final int value, final int size, final int screen) {
        if (size >= screen) {
            // Does not fit on this axis: 0 is the only position that is not off-screen.
            return 0;
        }
        int clamped = Math.max(0, Math.min(value, screen - size));
        if (Math.abs(clamped - MARGIN) <= SNAP) {
            return MARGIN;
        }
        if (Math.abs(clamped + size - (screen - MARGIN)) <= SNAP) {
            return Math.max(0, screen - MARGIN - size);
        }
        int centered = (screen - size) / 2;
        if (Math.abs(clamped - centered) <= SNAP) {
            return Math.max(0, centered);
        }
        return clamped;
    }
}
