package net.blockhost.anarchyclient.rivet;

import net.lenni0451.commons.math.MathUtils;
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
        return filledRoundedRect(x, y, width, height, cornerRadius, cornerRadius, cornerRadius, cornerRadius, color);
    }

    static List<Vertex> filledRoundedRect(final float x, final float y, final float width, final float height,
                                          final float topLeftRadius, final float bottomLeftRadius,
                                          final float bottomRightRadius, final float topRightRadius, final int color) {
        CornerRadii radii = clampedCornerRadii(width, height, topLeftRadius, bottomLeftRadius, bottomRightRadius, topRightRadius);
        if (radii.isSquare()) {
            return solidRect(x, y, width, height, color);
        }

        List<Float> breakpoints = roundedRectBreakpoints(x, width, radii);
        List<Vertex> vertices = new ArrayList<>(Math.max(0, breakpoints.size() - 1) * 4);
        for (int index = 0; index < breakpoints.size() - 1; index++) {
            float left = breakpoints.get(index);
            float right = breakpoints.get(index + 1);
            if (right - left <= EPSILON) {
                continue;
            }
            VerticalSpan leftSpan = roundedRectSpanAtX(left, x, y, width, height, radii);
            VerticalSpan rightSpan = roundedRectSpanAtX(right, x, y, width, height, radii);
            addVerticalStrip(vertices, left, leftSpan.top(), leftSpan.bottom(), right, rightSpan.top(), rightSpan.bottom(), color);
        }
        return vertices;
    }

    static List<Vertex> outlinedRoundedRect(final float x, final float y, final float width, final float height,
                                            final float cornerRadius, final float outlineWidth, final int color) {
        return outlinedRoundedRect(x, y, width, height, cornerRadius, cornerRadius, cornerRadius, cornerRadius, outlineWidth, color);
    }

    static List<Vertex> outlinedRoundedRect(final float x, final float y, final float width, final float height,
                                            final float topLeftRadius, final float bottomLeftRadius,
                                            final float bottomRightRadius, final float topRightRadius,
                                            final float outlineWidth, final int color) {
        if (outlineWidth <= 0) {
            return List.of();
        }

        CornerRadii radii = clampedCornerRadii(width, height, topLeftRadius, bottomLeftRadius, bottomRightRadius, topRightRadius);
        if (radii.isSquare()) {
            return outlinedRect(x, y, width, height, outlineWidth, color);
        }

        float stroke = Math.min(outlineWidth, Math.min(width, height) / 2F);
        float innerX = x + stroke;
        float innerY = y + stroke;
        float innerWidth = width - stroke * 2F;
        float innerHeight = height - stroke * 2F;
        if (innerWidth <= EPSILON || innerHeight <= EPSILON) {
            return filledRoundedRect(x, y, width, height, radii.topLeft(), radii.bottomLeft(), radii.bottomRight(), radii.topRight(), color);
        }

        CornerRadii innerRadii = clampedCornerRadii(
                innerWidth,
                innerHeight,
                radii.topLeft() - stroke,
                radii.bottomLeft() - stroke,
                radii.bottomRight() - stroke,
                radii.topRight() - stroke
        );
        List<Float> breakpoints = roundedRectBreakpoints(x, width, radii);
        addRoundedRectBreakpoints(breakpoints, innerX, innerWidth, innerRadii);
        sortAndDedupe(breakpoints);

        List<Vertex> vertices = new ArrayList<>(Math.max(0, breakpoints.size() - 1) * 8);
        for (int index = 0; index < breakpoints.size() - 1; index++) {
            float left = breakpoints.get(index);
            float right = breakpoints.get(index + 1);
            if (right - left <= EPSILON) {
                continue;
            }

            VerticalSpan outerLeft = roundedRectSpanAtX(left, x, y, width, height, radii);
            VerticalSpan outerRight = roundedRectSpanAtX(right, x, y, width, height, radii);
            VerticalSpan innerLeft = roundedRectSpanAtX(left, innerX, innerY, innerWidth, innerHeight, innerRadii);
            VerticalSpan innerRight = roundedRectSpanAtX(right, innerX, innerY, innerWidth, innerHeight, innerRadii);
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

        int left = MathUtils.floorInt(minX);
        int top = MathUtils.floorInt(minY);
        int right = MathUtils.ceilInt(maxX);
        int bottom = MathUtils.ceilInt(maxY);
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
                : Math.max(4, MathUtils.ceilInt(angleSpan / (float) (Math.PI / 8D)));
        int lengthMinimum = MathUtils.ceilInt(radius * angleSpan / 3F);
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

    private static CornerRadii clampedCornerRadii(final float width, final float height,
                                                  final float topLeftRadius, final float bottomLeftRadius,
                                                  final float bottomRightRadius, final float topRightRadius) {
        if (width <= 0 || height <= 0) {
            return CornerRadii.SQUARE;
        }
        if (topLeftRadius == bottomLeftRadius
                && topLeftRadius == bottomRightRadius
                && topLeftRadius == topRightRadius) {
            float radius = Math.max(0, Math.min(topLeftRadius, Math.min(width, height) / 2F));
            return new CornerRadii(radius, radius, radius, radius);
        }
        float topLeft = Math.max(0, Math.min(topLeftRadius, Math.min(width, height)));
        float topRight = Math.max(0, Math.min(topRightRadius, Math.min(width - topLeft, height)));
        float bottomLeft = Math.max(0, Math.min(bottomLeftRadius, Math.min(width, height - topLeft)));
        float bottomRight = Math.max(0, Math.min(bottomRightRadius, Math.min(width - bottomLeft, height - topRight)));
        return new CornerRadii(topLeft, bottomLeft, bottomRight, topRight);
    }

    private static List<Float> roundedRectBreakpoints(final float x, final float width, final CornerRadii radii) {
        List<Float> breakpoints = new ArrayList<>();
        addRoundedRectBreakpoints(breakpoints, x, width, radii);
        sortAndDedupe(breakpoints);
        return breakpoints;
    }

    private static void addRoundedRectBreakpoints(final List<Float> breakpoints, final float x, final float width, final CornerRadii radii) {
        if (width <= 0) {
            return;
        }

        breakpoints.add(x);
        breakpoints.add(x + width);
        addCornerBreakpoints(breakpoints, x, radii.topLeft(), false);
        addCornerBreakpoints(breakpoints, x, radii.bottomLeft(), false);
        addCornerBreakpoints(breakpoints, x + width, radii.topRight(), true);
        addCornerBreakpoints(breakpoints, x + width, radii.bottomRight(), true);
    }

    private static void addCornerBreakpoints(final List<Float> breakpoints, final float edge, final float radius,
                                             final boolean fromRight) {
        if (radius <= EPSILON) return;
        int segments = segmentsForArc(radius, QUARTER_CIRCLE);
        for (int index = 0; index <= segments; index++) {
            float offset = radius * index / segments;
            breakpoints.add(fromRight ? edge - radius + offset : edge + offset);
        }
    }

    private static VerticalSpan roundedRectSpanAtX(final float position, final float x, final float y,
                                                   final float width, final float height, final CornerRadii radii) {
        if (position < x - EPSILON || position > x + width + EPSILON || width <= 0 || height <= 0) {
            return null;
        }

        float top = y;
        if (radii.topLeft() > EPSILON && position < x + radii.topLeft()) {
            top = roundedTop(position - x - radii.topLeft(), y, radii.topLeft());
        } else if (radii.topRight() > EPSILON && position > x + width - radii.topRight()) {
            top = roundedTop(position - x - width + radii.topRight(), y, radii.topRight());
        }

        float bottom = y + height;
        if (radii.bottomLeft() > EPSILON && position < x + radii.bottomLeft()) {
            bottom = roundedBottom(position - x - radii.bottomLeft(), y + height, radii.bottomLeft());
        } else if (radii.bottomRight() > EPSILON && position > x + width - radii.bottomRight()) {
            bottom = roundedBottom(position - x - width + radii.bottomRight(), y + height, radii.bottomRight());
        }
        return new VerticalSpan(top, bottom);
    }

    private static float roundedTop(final float dx, final float y, final float radius) {
        float extent = (float) Math.sqrt(Math.max(0, radius * radius - dx * dx));
        return y + radius - extent;
    }

    private static float roundedBottom(final float dx, final float bottom, final float radius) {
        float extent = (float) Math.sqrt(Math.max(0, radius * radius - dx * dx));
        return bottom - radius + extent;
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

    record Vertex(float x, float y, int color) {
    }

    private record VerticalSpan(float top, float bottom) {
    }

    private record CornerRadii(float topLeft, float bottomLeft, float bottomRight, float topRight) {

        private static final CornerRadii SQUARE = new CornerRadii(0, 0, 0, 0);

        private boolean isSquare() {
            return this.topLeft <= EPSILON
                    && this.bottomLeft <= EPSILON
                    && this.bottomRight <= EPSILON
                    && this.topRight <= EPSILON;
        }
    }
}
