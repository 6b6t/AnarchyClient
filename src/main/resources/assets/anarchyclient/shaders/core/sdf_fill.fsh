#version 330

#moj_import <minecraft:dynamictransforms.glsl>

/*
 * Anti-aliased rounded-rectangle / circle fill. The mesh UVs carry the distance to the nearest
 * edges in corner-radius units (see GlassPanelRenderState); a square with radius = half its size
 * renders as a perfect circle. Alpha smooths over one framebuffer pixel at the contour.
 */

in vec2 panelUv;
in vec2 panelPosition;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 color = vertexColor * ColorModulator;

    vec2 d = max(panelUv, vec2(0.0));
    float sdf = (d.x < 1.0 && d.y < 1.0) ? 1.0 - length(vec2(1.0) - d) : min(d.x, d.y);
    float radiusPx = 1.0 / max(max(fwidth(d.x), fwidth(d.y)), 1e-6);
    float alpha = clamp(sdf * radiusPx + 0.5, 0.0, 1.0);
    if (alpha <= 0.0) {
        discard;
    }

    fragColor = vec4(color.rgb, color.a * alpha);
}
