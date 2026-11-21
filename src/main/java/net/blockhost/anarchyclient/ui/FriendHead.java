package net.blockhost.anarchyclient.ui;

import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.rivet.Blaze3DRenderCommand;
import net.lenni0451.rivet.backend.render.Renderer;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.math.Rectangle;
import net.lenni0451.rivet.math.Size;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.world.item.component.ResolvableProfile;

/**
 * A player's face (head + hat overlay) drawn from their skin. Resolves the skin asynchronously
 * through vanilla's skin cache, falling back to the default Steve/Alex skin until it loads.
 */
final class FriendHead extends Component {

    private static boolean renderFailureLogged;

    private final ResolvableProfile profile;
    private final int size;

    FriendHead(final String name, final int size) {
        this.profile = ResolvableProfile.createUnresolved(name);
        this.size = size;
        this.capabilities().all(false);
    }

    @Override
    public void render(final Renderer renderer, final Size bounds) {
        renderer.custom(new Blaze3DRenderCommand(
                graphics -> {
                    try {
                        PlayerFaceExtractor.extractRenderState(graphics, this.profile, 0, 0, this.size);
                    } catch (RuntimeException exception) {
                        // A skin that fails to resolve must never take down the render pass.
                        if (!renderFailureLogged) {
                            renderFailureLogged = true;
                            AnarchyClient.LOGGER.warn(
                                    "Friend head render failed; further failures are not logged", exception);
                        }
                    }
                },
                new Rectangle(0, 0, this.size, this.size)));
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        return new Size(this.size, this.size);
    }
}
