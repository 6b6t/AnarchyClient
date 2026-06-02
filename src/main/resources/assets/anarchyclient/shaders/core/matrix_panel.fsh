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

float lineSegment(vec2 p, vec2 a, vec2 b, float width) {
    vec2 pa = p - a;
    vec2 ba = b - a;
    float h = clamp(dot(pa, ba) / max(dot(ba, ba), 0.0001), 0.0, 1.0);
    float distanceToLine = length(pa - ba * h);
    return 1.0 - smoothstep(width, width + 0.025, distanceToLine);
}

float glyphMask(vec2 p, float seed) {
    float a = fract(seed * 13.17);
    float b = fract(seed * 29.73);
    float c = fract(seed * 47.41);
    float d = fract(seed * 71.19);

    float mask = 0.0;
    mask = max(mask, lineSegment(p, vec2(0.50, 0.14), vec2(0.50, 0.86), 0.044));
    mask = max(mask, lineSegment(p, vec2(0.22, 0.22 + a * 0.18), vec2(0.78, 0.22 + a * 0.18), 0.040) * step(0.18, a));
    mask = max(mask, lineSegment(p, vec2(0.24, 0.58 + b * 0.16), vec2(0.76, 0.58 + b * 0.16), 0.038) * step(0.24, b));
    mask = max(mask, lineSegment(p, vec2(0.26, 0.18), vec2(0.74, 0.82), 0.034) * step(0.56, c));
    mask = max(mask, lineSegment(p, vec2(0.74, 0.20), vec2(0.30, 0.82), 0.034) * step(0.68, d));
    mask = max(mask, lineSegment(p, vec2(0.30 + c * 0.24, 0.18), vec2(0.30 + c * 0.24, 0.84), 0.032) * step(0.74, a));

    float dotMask = 1.0 - smoothstep(0.035, 0.082, length(p - vec2(0.50 + (a - 0.5) * 0.34, 0.50 + (b - 0.5) * 0.46)));
    mask = max(mask, dotMask * step(0.78, c));

    float inset = smoothstep(0.04, 0.14, p.x)
            * smoothstep(0.04, 0.14, p.y)
            * smoothstep(0.04, 0.14, 1.0 - p.x)
            * smoothstep(0.04, 0.14, 1.0 - p.y);
    return clamp(mask * inset, 0.0, 1.0);
}

void main() {
    vec4 color = vertexColor * ColorModulator;
    if (color.a <= 0.0) {
        discard;
    }

    vec2 uv = clamp(panelUv, vec2(0.0), vec2(1.0));
    float edge = min(min(uv.x, 1.0 - uv.x), min(uv.y, 1.0 - uv.y));
    float edgeFade = smoothstep(0.0, 0.055, edge);
    float rim = 1.0 - smoothstep(0.0, 0.040, edge);

    float seconds = GameTime * 1200.0;
    vec2 cellSize = vec2(4.0, 7.5);
    vec2 gridPosition = panelPosition / cellSize;
    vec2 cell = floor(gridPosition);
    vec2 cellUv = fract(gridPosition);

    float columnSeed = hash(vec2(cell.x, 19.71));
    float columnEnabled = step(0.13, columnSeed);
    float speed = 4.8 + columnSeed * 6.4;
    float period = 15.0 + columnSeed * 21.0;
    float trailLength = 7.0 + columnSeed * 10.0;
    float phase = mod(seconds * speed - cell.y + columnSeed * period, period);
    float trail = (1.0 - smoothstep(0.0, trailLength, phase)) * step(phase, trailLength);
    float head = 1.0 - smoothstep(0.0, 0.86, phase);

    float changeFrame = floor(seconds * (4.0 + columnSeed * 6.0));
    float glyphSeed = hash(cell + vec2(changeFrame, columnSeed * 41.0));
    float blinkFrame = floor(seconds * (9.0 + columnSeed * 9.0));
    float blink = 0.55 + 0.45 * step(0.38, hash(cell + vec2(blinkFrame, 7.0)));
    float dropouts = step(0.22, hash(cell + vec2(changeFrame * 0.37, 31.0)));
    float glyph = glyphMask(cellUv, glyphSeed);

    float rain = glyph * trail * blink * dropouts * columnEnabled * edgeFade;
    float headRain = glyph * head * columnEnabled * edgeFade;
    float columnMist = trail * columnEnabled * edgeFade * (0.035 + hash(cell + vec2(3.0, changeFrame)) * 0.045);

    vec3 matrixDark = vec3(0.0, 0.045, 0.018);
    vec3 matrixGreen = vec3(0.02, 0.92, 0.27);
    vec3 headGreen = vec3(0.74, 1.0, 0.72);

    vec3 shaded = color.rgb;
    shaded = mix(shaded, matrixDark, 0.28);
    shaded += matrixGreen * columnMist;
    shaded += matrixGreen * rain * 0.58;
    shaded += headGreen * headRain * 0.72;
    shaded += matrixGreen * rim * 0.08;
    shaded *= 0.92 + edgeFade * 0.08;

    fragColor = vec4(shaded, color.a);
}
