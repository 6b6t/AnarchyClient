#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>

in vec2 panelUv;
in vec4 vertexColor;

out vec4 fragColor;

float hash(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float noise(vec2 p) {
    vec2 cell = floor(p);
    vec2 local = fract(p);
    local = local * local * (3.0 - 2.0 * local);
    float a = hash(cell);
    float b = hash(cell + vec2(1.0, 0.0));
    float c = hash(cell + vec2(0.0, 1.0));
    float d = hash(cell + vec2(1.0, 1.0));
    return mix(mix(a, b, local.x), mix(c, d, local.x), local.y);
}

float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.55;
    for (int octave = 0; octave < 4; octave++) {
        value += amplitude * noise(p);
        p *= 2.0;
        amplitude *= 0.5;
    }
    return value;
}

float band(vec2 uv, float center, float drift, float thickness) {
    float wave = fbm(vec2(uv.x * 2.4 + drift, drift * 0.5)) - 0.5;
    float offset = uv.y - center - wave * 0.34;
    return exp(-offset * offset / max(thickness, 0.0008));
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
    float curtain = fbm(vec2(uv.x * 3.0 - seconds * 0.05, uv.y * 1.5 + seconds * 0.03));

    float topBand = band(uv, 0.30, seconds * 0.12, 0.020) * 1.05;
    float midBand = band(uv, 0.52, seconds * 0.09 + 7.0, 0.028) * 0.9;
    float lowBand = band(uv, 0.74, seconds * 0.15 + 21.0, 0.024) * 0.8;

    vec3 teal = vec3(0.04, 0.78, 0.62);
    vec3 emerald = vec3(0.10, 0.92, 0.42);
    vec3 violet = vec3(0.40, 0.30, 0.95);

    vec3 glow = teal * topBand + emerald * midBand + violet * lowBand;
    glow *= 0.55 + 0.45 * curtain;

    float verticalRays = pow(max(0.0, curtain - 0.45), 1.6) * smoothstep(0.95, 0.30, uv.y);

    vec3 shaded = color.rgb;
    shaded = mix(shaded, vec3(0.012, 0.03, 0.05), 0.35);
    shaded += glow * 0.85 * edgeFade;
    shaded += emerald * verticalRays * 0.14 * edgeFade;
    shaded *= 0.92 + edgeFade * 0.08;

    fragColor = vec4(shaded, color.a);
}
