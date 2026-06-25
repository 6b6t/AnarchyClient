#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>

/*
 * Panel caustics inspired by Shadertoy "WaterCaustic 4 DesktopBackground"
 * (tcGXzz, CC0). Rewritten for a single Minecraft GUI panel pass.
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

float wave(vec2 p, float t) {
    float value = 0.0;
    value += sin(p.x * 1.9 + t * 0.78 + sin(p.y * 1.3 - t * 0.31));
    value += sin(p.y * 2.4 - t * 0.63 + sin(p.x * 1.7 + t * 0.27));
    value += sin((p.x + p.y) * 1.6 + t * 0.52);
    return value / 3.0;
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

    vec2 p = panelPosition * 0.022;
    vec2 flow = vec2(
            wave(p + vec2(0.0, seconds * 0.11), seconds),
            wave(p.yx + vec2(seconds * 0.07, 3.2), seconds + 9.0)
    );
    vec2 q = p + flow * 0.55;

    float c1 = abs(sin(q.x * 3.7 + seconds * 0.9) + sin(q.y * 4.1 - seconds * 0.7));
    float c2 = abs(sin((q.x - q.y) * 3.2 + seconds * 0.38));
    float caustic = pow(1.0 - smoothstep(0.18, 0.92, c1 * 0.55 + c2 * 0.45), 2.2);
    caustic *= 0.72 + 0.28 * hash(floor(q * 4.0) + floor(seconds * 2.0));

    float depth = smoothstep(1.0, 0.05, uv.y);
    vec3 deep = vec3(0.0, 0.08, 0.11);
    vec3 teal = vec3(0.02, 0.55, 0.58);
    vec3 white = vec3(0.75, 0.96, 0.86);

    vec3 shaded = color.rgb;
    shaded = mix(shaded, deep, 0.34);
    shaded += teal * depth * 0.18 * edgeFade;
    shaded += white * caustic * 0.38 * edgeFade;
    shaded *= 0.92 + edgeFade * 0.08;

    fragColor = vec4(shaded, color.a);
}
