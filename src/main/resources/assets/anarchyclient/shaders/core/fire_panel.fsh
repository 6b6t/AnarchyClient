#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>

/*
 * Panel fire inspired by Shadertoy "Simple Cinematic Fire (294ch)"
 * (lcGfWW, MIT). Rewritten for a soft low-contrast menu background.
 */

in vec2 panelUv;
in vec2 panelPosition;
in vec4 vertexColor;

out vec4 fragColor;

float hash(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(vec2 p) {
    float value = 0.0;
    float amp = 0.55;
    for (int i = 0; i < 4; i++) {
        value += noise(p) * amp;
        p *= 2.05;
        amp *= 0.48;
    }
    return value;
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

    vec2 p = panelPosition * 0.018;
    float plume = fbm(vec2(p.x * 0.72, p.y * 0.42 - seconds * 0.72));
    plume += 0.35 * fbm(vec2(p.x * 1.4 + seconds * 0.11, p.y * 0.72 - seconds * 1.10));

    float height = smoothstep(1.0, 0.12, uv.y);
    float flame = smoothstep(0.50, 1.20, plume + height * 0.75);
    flame *= smoothstep(1.0, 0.24, uv.y);
    float sparks = step(0.985, hash(floor(p * 5.5) + floor(seconds * 8.0)))
            * smoothstep(0.95, 0.20, uv.y);

    vec3 coal = vec3(0.08, 0.018, 0.012);
    vec3 red = vec3(0.70, 0.10, 0.035);
    vec3 orange = vec3(1.0, 0.43, 0.10);
    vec3 gold = vec3(1.0, 0.82, 0.35);

    vec3 shaded = color.rgb;
    shaded = mix(shaded, coal, 0.30);
    shaded += red * flame * 0.22 * edgeFade;
    shaded += orange * pow(flame, 2.0) * 0.30 * edgeFade;
    shaded += gold * sparks * 0.22 * edgeFade;
    shaded *= 0.92 + edgeFade * 0.08;

    fragColor = vec4(shaded, color.a);
}
