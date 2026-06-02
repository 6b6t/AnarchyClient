package net.blockhost.anarchyclient.rivet;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.joml.Matrix3x2fc;

import java.util.ArrayList;
import java.util.Collections;
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

        List<Float> breakpoints = roundedRectBreakpoints(x, width, radius);
        List<Vertex> vertices = new ArrayList<>(Math.max(0, breakpoints.size() - 1) * 4);
        for (int index = 0; index < breakpoints.size() - 1; index++) {
            float left = breakpoints.get(index);
            float right = breakpoints.get(index + 1);
            if (right - left <= EPSILON) {
                continue;
            }
            VerticalSpan leftSpan = roundedRectSpanAtX(left, x, y, width, height, radius);
            VerticalSpan rightSpan = roundedRectSpanAtX(right, x, y, width, height, radius);
            addVerticalStrip(vertices, left, leftSpan.top(), leftSpan.bottom(), right, rightSpan.top(), rightSpan.bottom(), color);
        }
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
        float innerX = x + stroke;
        float innerY = y + stroke;
        float innerWidth = width - stroke * 2F;
        float innerHeight = height - stroke * 2F;
        if (innerWidth <= EPSILON || innerHeight <= EPSILON) {
            return filledRoundedRect(x, y, width, height, radius, color);
        }

        float innerRadius = Math.max(0, radius - stroke);
        List<Float> breakpoints = roundedRectBreakpoints(x, width, radius);
        addRoundedRectBreakpoints(breakpoints, innerX, innerWidth, innerRadius);
        sortAndDedupe(breakpoints);

        List<Vertex> vertices = new ArrayList<>(Math.max(0, breakpoints.size() - 1) * 8);
        for (int index = 0; index < breakpoints.size() - 1; index++) {
            float left = breakpoints.get(index);
            float right = breakpoints.get(index + 1);
            if (right - left <= EPSILON) {
                continue;
            }

            VerticalSpan outerLeft = roundedRectSpanAtX(left, x, y, width, height, radius);
            VerticalSpan outerRight = roundedRectSpanAtX(right, x, y, width, height, radius);
            VerticalSpan innerLeft = roundedRectSpanAtX(left, innerX, innerY, innerWidth, innerHeight, innerRadius);
            VerticalSpan innerRight = roundedRectSpanAtX(right, innerX, innerY, innerWidth, innerHeight, innerRadius);
            if (innerLeft == null || innerRight == null) {
                addVerticalStrip(vertices, left, outerLeft.top(), outerLeft.bottom(), right, outerRight.top(), outerRight.bottom(), color);
                continue;
            }

            addVerticalStrip(vertices, left, outerLeft.top(), innerLeft.top(), right, outerRight.top(), innerRight.top(), color);
            addVerticalStrip(vertices, left, innerLeft.bottom(), outerLeft.bottom(), right, innerRight.bottom(), outerRight.bottom(), color);
        }
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
        float angle = (float) Math.atan2(dy, dx);
        vertices.addAll(filledArc(bodyX1, bodyY1, halfWidth, angle + QUARTER_CIRCLE, angle + HALF_CIRCLE + QUARTER_CIRCLE, color));
        vertices.addAll(filledArc(bodyX2, bodyY2, halfWidth, angle - QUARTER_CIRCLE, angle + QUARTER_CIRCLE, color));
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

    private static List<Float> roundedRectBreakpoints(final float x, final float width, final float radius) {
        List<Float> breakpoints = new ArrayList<>();
        addRoundedRectBreakpoints(breakpoints, x, width, radius);
        sortAndDedupe(breakpoints);
        return breakpoints;
    }

    private static void addRoundedRectBreakpoints(final List<Float> breakpoints, final float x, final float width, final float radius) {
        if (width <= 0) {
            return;
        }

        breakpoints.add(x);
        breakpoints.add(x + width);
        if (radius <= EPSILON) {
            return;
        }

        int segments = segmentsForArc(radius, QUARTER_CIRCLE);
        for (int index = 0; index <= segments; index++) {
            float progress = (float) index / segments;
            breakpoints.add(x + radius * progress);
            breakpoints.add(x + width - radius + radius * progress);
        }
    }

    private static VerticalSpan roundedRectSpanAtX(final float position, final float x, final float y,
                                                   final float width, final float height, final float radius) {
        if (position < x - EPSILON || position > x + width + EPSILON || width <= 0 || height <= 0) {
            return null;
        }
        if (radius <= EPSILON) {
            return new VerticalSpan(y, y + height);
        }

        float leftCenter = x + radius;
        float rightCenter = x + width - radius;
        if (position < leftCenter) {
            return roundedVerticalSpan(position - leftCenter, y, height, radius);
        }
        if (position > rightCenter) {
            return roundedVerticalSpan(position - rightCenter, y, height, radius);
        }
        return new VerticalSpan(y, y + height);
    }

    private static VerticalSpan roundedVerticalSpan(final float dx, final float y, final float height, final float radius) {
        float extent = (float) Math.sqrt(Math.max(0, radius * radius - dx * dx));
        return new VerticalSpan(y + radius - extent, y + height - radius + extent);
    }

    private static void addVerticalStrip(final List<Vertex> vertices, final float left, final float topLeft, final float bottomLeft,
                                         final float right, final float topRight, final float bottomRight, final int color) {
        if (right - left <= EPSILON || (bottomLeft - topLeft <= EPSILON && bottomRight - topRight <= EPSILON)) {
            return;
        }
        vertices.addAll(quad(
                new Vertex(left, topLeft, color),
                new Vertex(left, bottomLeft, color),
                new Vertex(right, bottomRight, color),
                new Vertex(right, topRight, color)
        ));
    }

    private static void sortAndDedupe(final List<Float> values) {
        Collections.sort(values);
        for (int index = values.size() - 1; index > 0; index--) {
            if (Math.abs(values.get(index) - values.get(index - 1)) <= EPSILON) {
                values.remove(index);
            }
        }
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

    private record VerticalSpan(float top, float bottom) {
    }
}
