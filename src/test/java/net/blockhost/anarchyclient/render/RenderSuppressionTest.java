package net.blockhost.anarchyclient.render;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RenderSuppressionTest {

    @AfterEach
    void clear() {
        RenderSuppression.clear();
    }

    @Test
    void turningOffTheLastEffectRemovesTheRequest() {
        RenderSuppression.enable("no_render", RenderSuppression.Kind.VIEW_BOB);
        assertTrue(RenderSuppression.suppresses(RenderSuppression.Kind.VIEW_BOB));

        RenderSuppression.enable("no_render");

        assertFalse(RenderSuppression.suppresses(RenderSuppression.Kind.VIEW_BOB));
    }
}
