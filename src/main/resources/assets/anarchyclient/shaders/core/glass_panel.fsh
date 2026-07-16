#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>

/*
 * Liquid glass panel. The mesh (GlassPanelRenderState) provides UVs holding the distance to the
 * nearest vertical/horizontal panel edge in corner-radius units, from which an exact rounded-rect
 * SDF is reconstructed: anti-aliased corners, edge refraction with chromatic dispersion, and a
 * specular rim that follows the rounded contour. One draw call per panel.
 */

uniform sampler2D Sampler0;

in vec2 panelUv;
in vec2 panelPosition;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 tint = vertexColor * ColorModulator;

    // Rounded-rect SDF in corner-radius units, then converted to framebuffer pixels.
    vec2 d = max(panelUv, vec2(0.0));
    float sdf = (d.x < 1.0 && d.y < 1.0) ? 1.0 - length(vec2(1.0) - d) : min(d.x, d.y);
    float radiusPx = 1.0 / max(max(fwidth(d.x), fwidth(d.y)), 1e-6);
    float distPx = sdf * radiusPx;
    float alpha = clamp(distPx + 0.5, 0.0, 1.0);

    // Inward direction (SDF gradient in framebuffer space); drives refraction and the rim light.
    vec2 inward = vec2(dFdx(sdf), dFdy(sdf));
    inward = inward / max(length(inward), 1e-5);

    if (alpha <= 0.0) {
        discard;
    }

    vec2 sceneSize = vec2(textureSize(Sampler0, 0));
    vec2 screenUv = gl_FragCoord.xy / sceneSize;

    // Refraction band along the contour: bend the blurred scene toward the panel center.
    float band = clamp(radiusPx * 1.1, 8.0, 30.0);
    float bend = pow(1.0 - smoothstep(0.0, band, distPx), 1.8);
    vec2 offset = inward * bend * band * 0.35 / sceneSize;

    vec3 background;
    background.r = texture(Sampler0, clamp(screenUv + offset * 1.05, vec2(0.001), vec2(0.999))).r;
    background.g = texture(Sampler0, clamp(screenUv + offset, vec2(0.001), vec2(0.999))).g;
    background.b = texture(Sampler0, clamp(screenUv + offset * 0.95, vec2(0.001), vec2(0.999))).b;

    // Frosted look: gentle saturation lift so the glass feels dense, not muddy.
    float luma = dot(background, vec3(0.299, 0.587, 0.114));
    background = mix(vec3(luma), background, 1.12);

    // Glass tint layered over the refracted background.
    vec3 glass = mix(background, tint.rgb, tint.a);

    // Specular rim hugging the contour, brightest where the edge faces the light from above.
    float rim = 1.0 - smoothstep(0.4, 2.2, distPx);
    float topness = clamp(-inward.y, 0.0, 1.0);
    glass += vec3(1.0) * rim * (0.10 + 0.28 * topness);

    fragColor = vec4(glass, alpha);
}
