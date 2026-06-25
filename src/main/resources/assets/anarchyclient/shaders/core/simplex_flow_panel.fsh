#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>

/*
 * CC0 OpenSimplex2S derivative-noise flow adapted from a Shadertoy-style
 * fragment shader pasted into the project issue thread.
 */

in vec2 panelUv;
in vec2 panelPosition;
in vec4 vertexColor;

out vec4 fragColor;

vec4 permute(vec4 t) {
    return t * (t * 34.0 + 133.0);
}

vec3 grad(float hash) {
    vec3 cube = mod(floor(hash / vec3(1.0, 2.0, 4.0)), 2.0) * 2.0 - 1.0;
    vec3 cuboct = cube;
    cuboct[int(hash / 16.0)] = 0.0;
    float type = mod(floor(hash / 8.0), 2.0);
    vec3 rhomb = (1.0 - type) * cube + type * (cuboct + cross(cube, cuboct));
    vec3 gradient = cuboct * 1.22474487139 + rhomb;
    gradient *= (1.0 - 0.042942436724648037 * type) * 3.5946317686139184;
    return gradient;
}

vec4 os2NoiseWithDerivativesPart(vec3 x) {
    vec3 b = floor(x);
    vec4 i4 = vec4(x - b, 2.5);

    vec3 v1 = b + floor(dot(i4, vec4(0.25)));
    vec3 v2 = b + vec3(1.0, 0.0, 0.0) + vec3(-1.0, 1.0, 1.0) * floor(dot(i4, vec4(-0.25, 0.25, 0.25, 0.35)));
    vec3 v3 = b + vec3(0.0, 1.0, 0.0) + vec3(1.0, -1.0, 1.0) * floor(dot(i4, vec4(0.25, -0.25, 0.25, 0.35)));
    vec3 v4 = b + vec3(0.0, 0.0, 1.0) + vec3(1.0, 1.0, -1.0) * floor(dot(i4, vec4(0.25, 0.25, -0.25, 0.35)));

    vec4 hashes = permute(mod(vec4(v1.x, v2.x, v3.x, v4.x), 289.0));
    hashes = permute(mod(hashes + vec4(v1.y, v2.y, v3.y, v4.y), 289.0));
    hashes = mod(permute(mod(hashes + vec4(v1.z, v2.z, v3.z, v4.z), 289.0)), 48.0);

    vec3 d1 = x - v1;
    vec3 d2 = x - v2;
    vec3 d3 = x - v3;
    vec3 d4 = x - v4;
    vec4 a = max(0.75 - vec4(dot(d1, d1), dot(d2, d2), dot(d3, d3), dot(d4, d4)), 0.0);
    vec4 aa = a * a;
    vec4 aaaa = aa * aa;
    vec3 g1 = grad(hashes.x);
    vec3 g2 = grad(hashes.y);
    vec3 g3 = grad(hashes.z);
    vec3 g4 = grad(hashes.w);
    vec4 extrapolations = vec4(dot(d1, g1), dot(d2, g2), dot(d3, g3), dot(d4, g4));

    vec3 derivative = -8.0 * mat4x3(d1, d2, d3, d4) * (aa * a * extrapolations)
            + mat4x3(g1, g2, g3, g4) * aaaa;
    return vec4(derivative, dot(aaaa, extrapolations));
}

vec4 os2NoiseWithDerivativesImproveXY(vec3 x) {
    mat3 orthonormalMap = mat3(
            0.788675134594813, -0.211324865405187, -0.577350269189626,
            -0.211324865405187, 0.788675134594813, -0.577350269189626,
            0.577350269189626, 0.577350269189626, 0.577350269189626
    );

    x = orthonormalMap * x;
    vec4 result = os2NoiseWithDerivativesPart(x) + os2NoiseWithDerivativesPart(x + 144.5);
    return vec4(result.xyz * orthonormalMap, result.w);
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

    vec2 p = panelPosition * 0.020;
    p += (uv - 0.5) * 2.0;
    vec3 x = vec3(p, mod(1.1 * seconds, 5780.0) * 0.8660254037844386);

    vec4 noiseResult = os2NoiseWithDerivativesImproveXY(x);
    noiseResult = os2NoiseWithDerivativesImproveXY(x - noiseResult.xyz / 16.0);

    float value = clamp(0.54 + 0.72 * noiseResult.w, 0.0, 1.0);
    float derivative = clamp(length(noiseResult.xyz) * 0.18, 0.0, 1.0);
    float veins = smoothstep(0.36, 0.92, value + derivative * 0.18);

    vec3 violet = vec3(0.90, 0.30, 1.00);
    vec3 blue = vec3(0.10, 0.34, 0.78);
    vec3 cyan = vec3(0.18, 0.82, 0.92);
    vec3 flow = mix(blue, violet, value);
    flow += cyan * derivative * 0.22;

    vec3 shaded = color.rgb;
    shaded = mix(shaded, vec3(0.025, 0.014, 0.050), 0.28);
    shaded += flow * veins * 0.24 * edgeFade;
    shaded += violet * pow(value, 5.0) * 0.16 * edgeFade;
    shaded *= 0.92 + edgeFade * 0.08;

    fragColor = vec4(shaded, color.a);
}
