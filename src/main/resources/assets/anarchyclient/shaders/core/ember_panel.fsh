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

void main() {
    vec4 color = vertexColor * ColorModulator;
    if (color.a <= 0.0) {
        discard;
    }

    vec2 uv = clamp(panelUv, vec2(0.0), vec2(1.0));
    float edge = min(min(uv.x, 1.0 - uv.x), min(uv.y, 1.0 - uv.y));
    float edgeFade = smoothstep(0.0, 0.06, edge);

    float seconds = GameTime * 1200.0;

    float embers = 0.0;
    float columnWidth = 0.085;
    for (int column = 0; column < 12; column++) {
        float columnSeed = hash(vec2(float(column), 4.2));
        float speed = mix(0.10, 0.26, columnSeed);
        float phase = fract(seconds * speed + columnSeed * 7.0);
        float height = 1.0 - phase;
        float sway = sin(seconds * mix(1.2, 2.4, columnSeed) + float(column) * 1.7) * 0.05;
        float centerX = (float(column) + 0.5) / 12.0 + sway + (columnSeed - 0.5) * 0.03;

        float dx = (uv.x - centerX) / columnWidth;
        float dy = (uv.y - height) * 11.0;
        float spark = exp(-(dx * dx) * 4.0 - (dy * dy));
        float fade = smoothstep(0.0, 0.18, phase) * (1.0 - smoothstep(0.78, 1.0, phase));
        embers += spark * fade;
    }

    float baseGlow = pow(uv.y, 2.4) * (0.5 + 0.22 * sin(seconds * 1.3 + uv.x * 6.0));
    float flicker = 0.72 + 0.28 * hash(vec2(floor(seconds * 11.0), floor(uv.x * 7.0)));

    vec3 deepRed = vec3(0.55, 0.06, 0.02);
    vec3 ember = vec3(1.0, 0.42, 0.08);
    vec3 spark = vec3(1.0, 0.86, 0.42);

    vec3 shaded = color.rgb;
    shaded = mix(shaded, vec3(0.05, 0.015, 0.01), 0.34);
    shaded += deepRed * baseGlow * 0.5 * edgeFade;
    shaded += ember * embers * flicker * 0.9 * edgeFade;
    shaded += spark * embers * embers * 0.6 * edgeFade;
    shaded *= 0.92 + edgeFade * 0.08;

    fragColor = vec4(shaded, color.a);
}
