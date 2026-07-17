package net.blockhost.anarchyclient.rivet;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

import java.util.ArrayList;
import java.util.List;

/**
 * Captures a blurred copy of the game framebuffer for glass panels while the client menu is open.
 *
 * <p>When vanilla runs its menu blur ({@code GameRenderer.processBlurEffect}), this class snapshots the
 * sharp frame beforehand, keeps the blurred result as a sampleable texture, and restores the sharp frame
 * afterwards. The game therefore stays crisp behind the menu while glass panels sample
 * {@link #blurredView()} to blur only the pixels they cover.</p>
 */
public final class GlassBackdrop {

    private static boolean active;
    private static GpuTexture sharpTexture;
    private static GpuTexture blurredTexture;
    private static GpuTextureView blurredView;
    private static boolean capturedThisFrame;
    // Resources retired on resize. Closed one frame later so draws recorded against the old view finish first.
    private static final List<AutoCloseable> retired = new ArrayList<>();

    private GlassBackdrop() {
    }

    public static void activate() {
        active = true;
    }

    public static void deactivate() {
        active = false;
        closeRetired();
        if (blurredView != null) {
            blurredView.close();
            blurredView = null;
        }
        if (blurredTexture != null) {
            blurredTexture.close();
            blurredTexture = null;
        }
        if (sharpTexture != null) {
            sharpTexture.close();
            sharpTexture = null;
        }
        capturedThisFrame = false;
    }

    /**
     * The blurred game scene, sized to the main render target, or {@code null} before the first capture.
     */
    public static GpuTextureView blurredView() {
        return capturedThisFrame ? blurredView : null;
    }

    /**
     * Called right before vanilla blurs the main render target. Snapshots the sharp frame.
     */
    public static void onBlurStart(final RenderTarget mainTarget) {
        if (!active || mainTarget.getColorTexture() == null) {
            return;
        }
        closeRetired();
        ensureTextures(mainTarget);
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.copyTextureToTexture(mainTarget.getColorTexture(), sharpTexture, 0, 0, 0, 0, 0, mainTarget.width, mainTarget.height);
    }

    /**
     * Called right after vanilla blurs the main render target. Keeps the blurred copy for glass panels
     * and restores the sharp frame so the game stays visible behind the menu.
     */
    public static void onBlurEnd(final RenderTarget mainTarget) {
        if (!active || sharpTexture == null || mainTarget.getColorTexture() == null) {
            return;
        }
        // Vanilla just blurred the main target (strength = the menu-blur video setting, which the
        // Theme tab drives). Capture that blurred copy for the glass, then restore the sharp frame.
        // Note: re-running processBlurEffect here to stack passes is unsafe — the blur post chain's
        // ring buffer cannot be re-submitted within the same frame and trips a GPU fence assert.
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.copyTextureToTexture(mainTarget.getColorTexture(), blurredTexture, 0, 0, 0, 0, 0, mainTarget.width, mainTarget.height);
        encoder.copyTextureToTexture(sharpTexture, mainTarget.getColorTexture(), 0, 0, 0, 0, 0, mainTarget.width, mainTarget.height);
        capturedThisFrame = true;
    }

    private static void ensureTextures(final RenderTarget mainTarget) {
        GpuTexture color = mainTarget.getColorTexture();
        if (sharpTexture != null
                && sharpTexture.getWidth(0) == color.getWidth(0)
                && sharpTexture.getHeight(0) == color.getHeight(0)
                && sharpTexture.getFormat() == color.getFormat()) {
            return;
        }
        if (blurredView != null) {
            retired.add(blurredView);
            retired.add(blurredTexture);
            retired.add(sharpTexture);
        }
        int width = color.getWidth(0);
        int height = color.getHeight(0);
        sharpTexture = RenderSystem.getDevice().createTexture(
                () -> "AnarchyClient glass sharp scene",
                GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_COPY_DST,
                color.getFormat(), width, height, 1, 1
        );
        blurredTexture = RenderSystem.getDevice().createTexture(
                () -> "AnarchyClient glass blurred scene",
                GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                color.getFormat(), width, height, 1, 1
        );
        blurredView = RenderSystem.getDevice().createTextureView(blurredTexture);
        capturedThisFrame = false;
    }

    private static void closeRetired() {
        for (AutoCloseable closeable : retired) {
            try {
                closeable.close();
            } catch (Exception ignored) {
                // GPU resources do not throw on close.
            }
        }
        retired.clear();
    }
}
