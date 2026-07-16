package net.blockhost.anarchyclient.rivet;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;

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

    /** Extra vanilla blur iterations for the glass copy; user-tunable through the Theme tab. */
    private static int extraBlurPasses = 2;

    private static boolean active;
    private static boolean rerunningBlur;
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

    public static int extraBlurPasses() {
        return extraBlurPasses;
    }

    public static void extraBlurPasses(final int passes) {
        extraBlurPasses = Math.max(0, Math.min(5, passes));
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
        if (!active || rerunningBlur || mainTarget.getColorTexture() == null) {
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
        if (!active || rerunningBlur || sharpTexture == null || mainTarget.getColorTexture() == null) {
            return;
        }
        // Vanilla ran one blur; stack a few more passes onto the main target before capturing so the
        // glass reads as dense frosting. The flag keeps our own injections out of the reruns.
        rerunningBlur = true;
        try {
            for (int pass = 0; pass < extraBlurPasses; pass++) {
                Minecraft.getInstance().gameRenderer.processBlurEffect();
            }
        } finally {
            rerunningBlur = false;
        }
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
