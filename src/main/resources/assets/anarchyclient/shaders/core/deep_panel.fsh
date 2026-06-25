#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>

/*
 * CC0. Adapted from Shadertoy "Neonwave sunrise" by mrange
 * (7dyyRy) for the Minecraft GUI panel pipeline.
 */

in vec2 panelUv;
in vec2 panelPosition;
in vec4 vertexColor;

out vec4 fragColor;

const float PI = 3.141592654;

vec3 hsv2rgb(vec3 c) {
    vec4 k = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + k.xyz) * 6.0 - k.www);
    return c.z * mix(k.xxx, clamp(p - k.xxx, 0.0, 1.0), c.y);
}

float hash(vec2 p) {
    float a = dot(p, vec2(127.1, 311.7));
    return fract(sin(a) * 43758.5453123);
}

float vnoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm(vec2 p) {
    float sum = 0.0;
    float amp = 0.56;
    for (int i = 0; i < 5; i++) {
        sum += vnoise(p) * amp;
        p *= 2.02;
        amp *= 0.48;
    }
    return sum;
}

float lineGrid(vec2 p, float spacing, float thickness) {
    vec2 cell = abs(fract(p / spacing) - 0.5) * spacing;
    float d = min(cell.x, cell.y);
    return 1.0 - smoothstep(thickness, thickness + 0.7, d);
}

float mountainLayer(vec2 uv, float time, float offset, float scale, float height) {
    float n = fbm(vec2(uv.x * scale + time * 0.05 + offset, offset));
    float ridge = 0.50 + height * (n - 0.30);
    return smoothstep(ridge - 0.012, ridge + 0.012, uv.y);
}

float stars(vec2 uv, float time) {
    vec2 grid = uv * vec2(150.0, 82.0);
    vec2 cell = floor(grid);
    vec2 cellUv = fract(grid) - 0.5;
    float seed = hash(cell);
    float star = step(0.986, seed);
    float glow = exp(-dot(cellUv, cellUv) * mix(70.0, 180.0, seed));
    float twinkle = 0.48 + 0.52 * sin(time * mix(2.0, 5.2, seed) + seed * 18.0);
    float topMask = 1.0 - smoothstep(0.10, 0.70, uv.y);
    return star * glow * twinkle * topMask;
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

    vec2 centered = uv - 0.5;
    centered.x *= 1.75;
    float horizon = 0.56 + sin(seconds * 0.18) * 0.012;

    vec3 skyTop = hsv2rgb(vec3(0.64, 0.74, 0.18));
    vec3 skyLow = hsv2rgb(vec3(0.88, 0.68, 0.42));
    vec3 sky = mix(skyTop, skyLow, smoothstep(0.02, 0.72, uv.y));
    sky += hsv2rgb(vec3(0.78, 0.70, 0.95)) * stars(uv + vec2(seconds * 0.002, 0.0), seconds) * 0.75;

    vec2 moonPos = vec2(0.52 + sin(seconds * 0.06) * 0.025, 0.30);
    float moonDistance = length((uv - moonPos) * vec2(1.0, 1.35));
    float moon = 1.0 - smoothstep(0.065, 0.082, moonDistance);
    float moonGlow = exp(-moonDistance * 11.0);
    sky += hsv2rgb(vec3(0.72, 0.46, 0.92)) * moonGlow * 0.20;
    sky = mix(sky, hsv2rgb(vec3(0.75, 0.48, 0.95)), moon * 0.72);

    float nearMountains = mountainLayer(uv, seconds, 2.0, 3.0, 0.33);
    float farMountains = mountainLayer(uv, seconds, 7.0, 2.0, 0.22);
    vec3 farColor = hsv2rgb(vec3(0.68, 0.55, 0.24));
    vec3 nearColor = hsv2rgb(vec3(0.86, 0.78, 0.30));
    sky = mix(sky, farColor, farMountains * smoothstep(0.28, 0.78, uv.y) * 0.68);
    sky = mix(sky, nearColor, nearMountains * smoothstep(0.34, 0.86, uv.y) * 0.78);

    float floorMask = smoothstep(horizon - 0.015, horizon + 0.025, uv.y);
    float perspective = 1.0 / max(0.06, uv.y - horizon + 0.08);
    vec2 floorPos = vec2(centered.x * perspective * 62.0, perspective * 24.0 + seconds * 8.0);
    float grid = lineGrid(floorPos, 8.0, 0.38) * floorMask;
    float wave = fbm(vec2(centered.x * 2.2 + seconds * 0.07, uv.y * 9.0 - seconds * 0.30));
    float water = floorMask * (0.18 + 0.82 * smoothstep(0.38, 0.92, wave));

    vec3 neonCyan = hsv2rgb(vec3(0.56, 0.82, 0.78));
    vec3 neonPink = hsv2rgb(vec3(0.88, 0.78, 0.86));
    sky += neonCyan * grid * 0.26 * edgeFade;
    sky += neonPink * water * 0.075 * edgeFade;

    vec3 shaded = color.rgb;
    shaded = mix(shaded, sky, 0.40 * edgeFade);
    shaded += sky * 0.15 * edgeFade;
    shaded *= 0.91 + edgeFade * 0.09;

    fragColor = vec4(shaded, color.a);
}
