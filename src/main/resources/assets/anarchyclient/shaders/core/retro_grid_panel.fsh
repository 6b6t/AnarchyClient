#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>

/*
 * Panel retro grid inspired by Shadertoy "Retrogrid for Amy" (tXKcWD, MIT).
 * Rewritten as a restrained background for settings and module panels.
 */

in vec2 panelUv;
in vec2 panelPosition;
in vec4 vertexColor;

out vec4 fragColor;

float lineGrid(vec2 p, float spacing, float thickness) {
    vec2 cell = abs(fract(p / spacing) - 0.5) * spacing;
    float d = min(cell.x, cell.y);
    return 1.0 - smoothstep(thickness, thickness + 1.15, d);
}

float horizonLine(vec2 uv, float y, float thickness) {
    return 1.0 - smoothstep(thickness, thickness + 0.008, abs(uv.y - y));
}

void main() {
    vec4 color = vertexColor * ColorModulator;
    if (color.a <= 0.0) {
        discard;
    }

    vec2 uv = clamp(panelUv, vec2(0.0), vec2(1.0));
    float edge = min(min(uv.x, 1.0 - uv.x), min(uv.y, 1.0 - uv.y));
    float edgeFade = smoothstep(0.0, 0.055, edge);
    float seconds = GameTime * 1200.0;

    vec2 centered = uv - vec2(0.5, 0.56);
    float perspective = 1.0 / max(0.10, uv.y + 0.13);
    vec2 floorPos = vec2(centered.x * perspective * 190.0, perspective * 42.0 + seconds * 18.0);
    float grid = lineGrid(floorPos, 12.0, 0.72) * smoothstep(0.18, 0.92, uv.y);

    float scan = horizonLine(uv, 0.56 + sin(seconds * 0.7) * 0.012, 0.004);
    float sunBand = 0.0;
    for (int i = 0; i < 5; i++) {
        sunBand += horizonLine(uv, 0.18 + float(i) * 0.045, 0.004);
    }
    float vignette = smoothstep(0.78, 0.12, length(centered * vec2(1.05, 0.8)));

    vec3 magenta = vec3(0.78, 0.16, 0.52);
    vec3 amber = vec3(0.95, 0.50, 0.16);
    vec3 cyan = vec3(0.12, 0.72, 0.88);

    vec3 shaded = color.rgb;
    shaded = mix(shaded, vec3(0.035, 0.018, 0.050), 0.30);
    shaded += cyan * grid * 0.34 * edgeFade;
    shaded += magenta * scan * 0.22 * edgeFade;
    shaded += amber * sunBand * 0.045 * edgeFade * smoothstep(0.62, 0.0, uv.y);
    shaded += magenta * vignette * 0.035 * edgeFade;
    shaded *= 0.92 + edgeFade * 0.08;

    fragColor = vec4(shaded, color.a);
}
