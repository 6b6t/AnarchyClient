#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>

in vec2 panelUv;
in vec2 panelPosition;
in vec4 vertexColor;

out vec4 fragColor;

float hash(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float gridLine(vec2 position, float spacing, float thickness) {
    vec2 cell = abs(fract(position / spacing) - 0.5) * spacing;
    float distanceToLine = min(cell.x, cell.y);
    return 1.0 - smoothstep(thickness, thickness + 0.9, distanceToLine);
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
    vec2 drift = vec2(seconds * 1.6, -seconds * 2.4);

    float fine = gridLine(panelPosition + drift, 12.0, 0.6);
    float coarse = gridLine(panelPosition + drift * 0.5, 48.0, 1.0);

    vec2 nodeGrid = floor((panelPosition + drift * 0.5) / 48.0);
    float nodePulse = 0.5 + 0.5 * sin(seconds * 2.4 + hash(nodeGrid) * 6.2831);
    vec2 nodeLocal = fract((panelPosition + drift * 0.5) / 48.0) - 0.5;
    float node = (1.0 - smoothstep(0.04, 0.16, length(nodeLocal))) * nodePulse;

    float scan = fract(uv.y * 1.4 - seconds * 0.18);
    float sweep = smoothstep(0.0, 0.05, scan) * (1.0 - smoothstep(0.05, 0.34, scan));

    vec3 cyan = vec3(0.10, 0.80, 0.95);
    vec3 deepCyan = vec3(0.05, 0.42, 0.66);

    vec3 shaded = color.rgb;
    shaded = mix(shaded, vec3(0.015, 0.035, 0.05), 0.32);
    shaded += deepCyan * fine * 0.16 * edgeFade;
    shaded += cyan * coarse * 0.30 * edgeFade;
    shaded += cyan * node * 0.55 * edgeFade;
    shaded += cyan * sweep * 0.10 * edgeFade;
    shaded *= 0.92 + edgeFade * 0.08;

    fragColor = vec4(shaded, color.a);
}
