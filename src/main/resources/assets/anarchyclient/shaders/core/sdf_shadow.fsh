#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>

/*
 * Soft drop shadow. Same distance-field mesh as sdf_fill, but the alpha fades smoothly over the
 * whole corner-radius unit instead of a single pixel, producing a continuous falloff instead of
 * stacked rectangle rings.
 */

layout(location = 0) in vec2 panelUv;
layout(location = 1) in vec2 panelPosition;
layout(location = 2) in vec4 vertexColor;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 color = vertexColor * ColorModulator;

    vec2 d = max(panelUv, vec2(0.0));
    float sdf = (d.x < 1.0 && d.y < 1.0) ? 1.0 - length(vec2(1.0) - d) : min(d.x, d.y);
    float fade = smoothstep(0.0, 1.0, sdf);
    // Slightly concave falloff reads as penumbra rather than a linear gradient band.
    fade *= fade;
    if (fade <= 0.0) {
        discard;
    }

    fragColor = vec4(color.rgb, color.a * fade);
}
