package net.blockhost.anarchyclient.rivet;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.joml.Matrix3x2fc;

import java.util.ArrayList;
import java.util.List;

final class GuiShapeGeometry {

    static final float QUARTER_CIRCLE = (float) (Math.PI / 2D);
    static final float HALF_CIRCLE = (float) Math.PI;
    static final float FULL_CIRCLE = (float) (Math.PI * 2D);

    private static final float EPSILON = 0.0001F;

    private GuiShapeGeometry() {
    }

    static List<Vertex> gradientRect(final float x, final float y, final float width, final float height,
                                     final int topLeftColor, final int bottomLeftColor, final int bottomRightColor, final int topRightColor) {
        if (width <= 0 || height <= 0) {
            return List.of();
        }
        return List.of(
                new Vertex(x, y, topLeftColor),
                new Vertex(x, y + height, bottomLeftColor),
                new Vertex(x + width, y + height, bottomRightColor),
                new Vertex(x + width, y, topRightColor)
        );
    }

    static List<Vertex> filledRoundedRect(final float x, final float y, final float width, final float height,
                                          final float cornerRadius, final int color) {
        float radius = clampedCornerRadius(width, height, cornerRadius);
        if (radius <= EPSILON) {
            return solidRect(x, y, width, height, color);
        }
        if (isCapsuleRadius(width, height, radius)) {
            return capsule(x, y, width, height, color);
        }

        List<Vertex> vertices = new ArrayList<>();
        vertices.addAll(solidRect(x + radius, y, width - radius * 2F, height, color));
        vertices.addAll(solidRect(x, y + radius, width, height - radius * 2F, color));
        vertices.addAll(filledArc(x + radius, y + radius, radius, HALF_CIRCLE, HALF_CIRCLE + QUARTER_CIRCLE, color));
        vertices.addAll(filledArc(x + width - radius, y + radius, radius, HALF_CIRCLE + QUARTER_CIRCLE, FULL_CIRCLE, color));
        vertices.addAll(filledArc(x + width - radius, y + height - radius, radius, 0, QUARTER_CIRCLE, color));
        vertices.addAll(filledArc(x + radius, y + height - radius, radius, QUARTER_CIRCLE, HALF_CIRCLE, color));
        return vertices;
    }

    static List<Vertex> outlinedRoundedRect(final float x, final float y, final float width, final float height,
                                            final float cornerRadius, final float outlineWidth, final int color) {
        if (outlineWidth <= 0) {
            return List.of();
        }

        float radius = clampedCornerRadius(width, height, cornerRadius);
        if (radius <= EPSILON) {
            return outlinedRect(x, y, width, height, outlineWidth, color);
        }

        float stroke = Math.min(outlineWidth, Math.min(width, height) / 2F);
        List<Vertex> vertices = new ArrayList<>();
        vertices.addAll(solidRect(x + radius, y, width - radius * 2F, stroke, color));
        vertices.addAll(solidRect(x + radius, y + height - stroke, width - radius * 2F, stroke, color));
        vertices.addAll(solidRect(x, y + radius, stroke, height - radius * 2F, color));
        vertices.addAll(solidRect(x + width - stroke, y + radius, stroke, height - radius * 2F, color));
        vertices.addAll(ringArc(x + radius, y + radius, radius, Math.max(0, radius - stroke), HALF_CIRCLE, HALF_CIRCLE + QUARTER_CIRCLE, color));
        vertices.addAll(ringArc(x + width - radius, y + radius, radius, Math.max(0, radius - stroke), HALF_CIRCLE + QUARTER_CIRCLE, FULL_CIRCLE, color));
        vertices.addAll(ringArc(x + width - radius, y + height - radius, radius, Math.max(0, radius - stroke), 0, QUARTER_CIRCLE, color));
        vertices.addAll(ringArc(x + radius, y + height - radius, radius, Math.max(0, radius - stroke), QUARTER_CIRCLE, HALF_CIRCLE, color));
        return vertices;
    }

    static List<Vertex> solidRect(final float x, final float y, final float width, final float height, final int color) {
        return gradientRect(x, y, width, height, color, color, color, color);
    }

    static List<Vertex> line(final float x1, final float y1, final float x2, final float y2, final float width, final int color) {
        if (width <= 0) {
            return List.of();
        }

        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.hypot(dx, dy);
        if (length <= EPSILON) {
            return filledCircle(x1, y1, width / 2F, color);
        }

        float halfWidth = width / 2F;
        if (length <= width) {
            return filledCircle(x1 + dx / 2F, y1 + dy / 2F, halfWidth, color);
        }

        float unitX = dx / length;
        float unitY = dy / length;
        float offsetX = -dy / length * halfWidth;
        float offsetY = dx / length * halfWidth;
        float bodyX1 = x1 + unitX * halfWidth;
        float bodyY1 = y1 + unitY * halfWidth;
        float bodyX2 = x2 - unitX * halfWidth;
        float bodyY2 = y2 - unitY * halfWidth;

        List<Vertex> vertices = new ArrayList<>(4 + segmentsForArc(halfWidth, FULL_CIRCLE) * 8);
        vertices.addAll(quad(
                new Vertex(bodyX1 + offsetX, bodyY1 + offsetY, color),
                new Vertex(bodyX1 - offsetX, bodyY1 - offsetY, color),
                new Vertex(bodyX2 - offsetX, bodyY2 - offsetY, color),
                new Vertex(bodyX2 + offsetX, bodyY2 + offsetY, color)
        ));
        vertices.addAll(filledCircle(bodyX1, bodyY1, halfWidth, color));
        vertices.addAll(filledCircle(bodyX2, bodyY2, halfWidth, color));
        return vertices;
    }

    static List<Vertex> filledTriangle(final float x1, final float y1, final float x2, final float y2,
                                       final float x3, final float y3, final int color) {
        Vertex first = new Vertex(x1, y1, color);
        Vertex second = new Vertex(x2, y2, color);
        Vertex third = new Vertex(x3, y3, color);
        if (signedArea(first, second, third) > 0) {
            return List.of(first, third, second, second);
        }
        return List.of(first, second, third, third);
    }

    static List<Vertex> filledCircle(final float x, final float y, final float radius, final int color) {
        return filledArc(x, y, radius, 0, FULL_CIRCLE, color);
    }

    static List<Vertex> filledArc(final float x, final float y, final float radius, final float startAngle, final float endAngle, final int color) {
        if (radius <= 0) {
            return List.of();
        }

        float angleSpan = Math.abs(endAngle - startAngle);
        int segments = segmentsForArc(radius, angleSpan);
        if (segments <= 0) {
            return List.of();
        }

        List<Vertex> vertices = new ArrayList<>(segments * 4);
        float step = (endAngle - startAngle) / segments;
        for (int index = 0; index < segments; index++) {
            float angleA = startAngle + step * index;
            float angleB = startAngle + step * (index + 1);
            vertices.addAll(quad(
                    arcVertex(x, y, radius, angleA, color),
                    new Vertex(x, y, color),
                    new Vertex(x, y, color),
                    arcVertex(x, y, radius, angleB, color)
            ));
        }
        return vertices;
    }

    static List<Vertex> ringArc(final float x, final float y, final float outerRadius, final float innerRadius,
                                final float startAngle, final float endAngle, final int color) {
        if (outerRadius <= 0 || outerRadius <= innerRadius) {
            return List.of();
        }

        float angleSpan = Math.abs(endAngle - startAngle);
        int segments = segmentsForArc(outerRadius, angleSpan);
        if (segments <= 0) {
            return List.of();
        }

        float clampedInnerRadius = Math.max(0, innerRadius);
        List<Vertex> vertices = new ArrayList<>(segments * 4);
        float step = (endAngle - startAngle) / segments;
        for (int index = 0; index < segments; index++) {
            float angleA = startAngle + step * index;
            float angleB = startAngle + step * (index + 1);
            vertices.addAll(quad(
                    arcVertex(x, y, outerRadius, angleA, color),
                    arcVertex(x, y, clampedInnerRadius, angleA, color),
                    arcVertex(x, y, clampedInnerRadius, angleB, color),
                    arcVertex(x, y, outerRadius, angleB, color)
            ));
        }
        return vertices;
    }

    static ScreenRectangle bounds(final List<Vertex> vertices, final Matrix3x2fc pose, final ScreenRectangle scissorArea) {
        if (vertices.isEmpty()) {
            return null;
        }

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (Vertex vertex : vertices) {
            minX = Math.min(minX, vertex.x());
            minY = Math.min(minY, vertex.y());
            maxX = Math.max(maxX, vertex.x());
            maxY = Math.max(maxY, vertex.y());
        }

        int left = floor(minX);
        int top = floor(minY);
        int right = ceil(maxX);
        int bottom = ceil(maxY);
        ScreenRectangle transformedBounds = new ScreenRectangle(
                left,
                top,
                Math.max(1, right - left),
                Math.max(1, bottom - top)
        ).transformMaxBounds(pose);
        if (scissorArea == null) {
            return transformedBounds;
        }
        return scissorArea.intersection(transformedBounds);
    }

    static int segmentsForArc(final float radius, final float angleSpan) {
        if (radius <= 0 || angleSpan <= EPSILON) {
            return 0;
        }

        int angleMinimum = angleSpan >= FULL_CIRCLE - EPSILON
                ? 32
                : Math.max(4, ceil(angleSpan / (float) (Math.PI / 8D)));
        int lengthMinimum = ceil(radius * angleSpan / 3F);
        return Math.min(384, Math.max(angleMinimum, lengthMinimum));
    }

    private static Vertex arcVertex(final float x, final float y, final float radius, final float angle, final int color) {
        return new Vertex(
                (float) (x + Math.cos(angle) * radius),
                (float) (y + Math.sin(angle) * radius),
                color
        );
    }

    private static List<Vertex> capsule(final float x, final float y, final float width, final float height, final int color) {
        if (width <= 0 || height <= 0) {
            return List.of();
        }
        if (width >= height) {
            return line(x, y + height / 2F, x + width, y + height / 2F, height, color);
        }
        return line(x + width / 2F, y, x + width / 2F, y + height, width, color);
    }

    private static List<Vertex> outlinedRect(final float x, final float y, final float width, final float height,
                                             final float outlineWidth, final int color) {
        if (width <= 0 || height <= 0 || outlineWidth <= 0) {
            return List.of();
        }

        float stroke = Math.min(outlineWidth, Math.min(width, height) / 2F);
        List<Vertex> vertices = new ArrayList<>();
        vertices.addAll(solidRect(x, y, width, stroke, color));
        vertices.addAll(solidRect(x, y + height - stroke, width, stroke, color));
        vertices.addAll(solidRect(x, y, stroke, height, color));
        vertices.addAll(solidRect(x + width - stroke, y, stroke, height, color));
        return vertices;
    }

    private static float clampedCornerRadius(final float width, final float height, final float cornerRadius) {
        if (width <= 0 || height <= 0 || cornerRadius <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(cornerRadius, Math.min(width, height) / 2F));
    }

    private static boolean isCapsuleRadius(final float width, final float height, final float radius) {
        return radius >= Math.min(width, height) / 2F - EPSILON;
    }

    private static List<Vertex> quad(final Vertex first, final Vertex second, final Vertex third, final Vertex fourth) {
        if (signedArea(first, second, third, fourth) > 0) {
            return List.of(first, fourth, third, second);
        }
        return List.of(first, second, third, fourth);
    }

    private static float signedArea(final Vertex first, final Vertex second, final Vertex third) {
        return (first.x() * second.y() - second.x() * first.y())
                + (second.x() * third.y() - third.x() * second.y())
                + (third.x() * first.y() - first.x() * third.y());
    }

    private static float signedArea(final Vertex first, final Vertex second, final Vertex third, final Vertex fourth) {
        return (first.x() * second.y() - second.x() * first.y())
                + (second.x() * third.y() - third.x() * second.y())
                + (third.x() * fourth.y() - fourth.x() * third.y())
                + (fourth.x() * first.y() - first.x() * fourth.y());
    }

    private static int floor(final float value) {
        return (int) Math.floor(value);
    }

    private static int ceil(final float value) {
        return (int) Math.ceil(value);
    }

    record Vertex(float x, float y, int color) {
    }
}
