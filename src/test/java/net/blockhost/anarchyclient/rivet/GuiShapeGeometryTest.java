package net.blockhost.anarchyclient.rivet;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.joml.Matrix3x2f;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiShapeGeometryTest {

    private static final int COLOR = 0xFF336699;

    @Test
    void lineUsesSinglePerpendicularQuad() {
        List<GuiShapeGeometry.Vertex> vertices = GuiShapeGeometry.line(0, 0, 10, 0, 4, COLOR);

        assertEquals(4, vertices.size());
        assertVertex(vertices.getFirst(), 0, 2);
        assertVertex(vertices.get(1), 10, 2);
        assertVertex(vertices.get(2), 10, -2);
        assertVertex(vertices.get(3), 0, -2);
        assertGuiWinding(vertices.getFirst(), vertices.get(1), vertices.get(2), vertices.get(3));
    }

    @Test
    void diagonalLineUsesRotatedQuad() {
        List<GuiShapeGeometry.Vertex> vertices = GuiShapeGeometry.line(0, 0, 10, 10, 2, COLOR);
        float offset = (float) Math.sqrt(0.5D);

        assertEquals(4, vertices.size());
        assertVertex(vertices.getFirst(), -offset, offset);
        assertVertex(vertices.get(1), 10 - offset, 10 + offset);
        assertVertex(vertices.get(2), 10 + offset, 10 - offset);
        assertVertex(vertices.get(3), offset, -offset);
        assertGuiWinding(vertices.getFirst(), vertices.get(1), vertices.get(2), vertices.get(3));
    }

    @Test
    void filledCircleUsesVisibleGuiWedges() {
        List<GuiShapeGeometry.Vertex> vertices = GuiShapeGeometry.filledCircle(5, 6, 8, COLOR);

        assertEquals(GuiShapeGeometry.segmentsForArc(8, GuiShapeGeometry.FULL_CIRCLE) * 4, vertices.size());
        assertVertex(vertices.get(1), 5, 6);
        assertVertex(vertices.get(2), 5, 6);
        assertGuiWinding(vertices.getFirst(), vertices.get(1), vertices.get(2), vertices.get(3));
    }

    @Test
    void ringArcBuildsContinuousQuadSegments() {
        int segments = GuiShapeGeometry.segmentsForArc(12, GuiShapeGeometry.QUARTER_CIRCLE);
        List<GuiShapeGeometry.Vertex> vertices = GuiShapeGeometry.ringArc(
                0,
                0,
                12,
                10,
                0,
                GuiShapeGeometry.QUARTER_CIRCLE,
                COLOR
        );

        assertEquals(segments * 4, vertices.size());
        assertVertex(vertices.get(3), vertices.get(4).x(), vertices.get(4).y());
        assertGuiWinding(vertices.getFirst(), vertices.get(1), vertices.get(2), vertices.get(3));
    }

    @Test
    void boundsIntersectsScissor() {
        List<GuiShapeGeometry.Vertex> vertices = GuiShapeGeometry.gradientRect(0, 0, 10, 10, COLOR, COLOR, COLOR, COLOR);
        ScreenRectangle bounds = GuiShapeGeometry.bounds(vertices, new Matrix3x2f(), new ScreenRectangle(4, 3, 3, 5));

        assertEquals(new ScreenRectangle(4, 3, 3, 5), bounds);
    }

    private static void assertVertex(final GuiShapeGeometry.Vertex vertex, final float x, final float y) {
        assertEquals(x, vertex.x(), 0.0001F);
        assertEquals(y, vertex.y(), 0.0001F);
        assertEquals(COLOR, vertex.color());
    }

    private static void assertGuiWinding(final GuiShapeGeometry.Vertex first, final GuiShapeGeometry.Vertex second,
                                         final GuiShapeGeometry.Vertex third, final GuiShapeGeometry.Vertex fourth) {
        float area = (first.x() * second.y() - second.x() * first.y())
                + (second.x() * third.y() - third.x() * second.y())
                + (third.x() * fourth.y() - fourth.x() * third.y())
                + (fourth.x() * first.y() - first.x() * fourth.y());
        assertTrue(area <= 0.0001F);
    }
}
