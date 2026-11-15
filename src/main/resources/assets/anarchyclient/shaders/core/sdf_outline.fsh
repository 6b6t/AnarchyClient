#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>

/*
 * Anti-aliased hairline outline for the same rounded-rect / circle mesh as sdf_fill: the UVs carry
 * the distance to the nearest edges in corner-radius units, so scaling that distance by the
 * pixels-per-unit derivative gives the distance from the contour in framebuffer pixels. Keeping only
 * the first pixel inside the contour leaves a smooth one-pixel ring instead of a tessellated one.
 */

layout(location = 0) in vec2 panelUv;
layout(location = 1) in vec2 panelPosition;
layout(location = 2) in vec4 vertexColor;

layout(location = 0) out vec4 fragColor;

const float WIDTH_PX = 1.0;

void main() {
    vec4 color = vertexColor * ColorModulator;

    vec2 d = max(panelUv, vec2(0.0));
    float sdf = (d.x < 1.0 && d.y < 1.0) ? 1.0 - length(vec2(1.0) - d) : min(d.x, d.y);
    float unitPx = 1.0 / max(max(fwidth(d.x), fwidth(d.y)), 1e-6);
    float distancePx = sdf * unitPx;

    // Inside the contour by at least half a pixel, and no deeper than the outline width.
    float alpha = clamp(distancePx + 0.5, 0.0, 1.0) * clamp(WIDTH_PX - distancePx + 0.5, 0.0, 1.0);
    if (alpha <= 0.0) {
        discard;
    }

    fragColor = vec4(color.rgb, color.a * alpha);
}
