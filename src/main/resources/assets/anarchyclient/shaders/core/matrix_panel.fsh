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
    vec2 cellSize = vec2(3.7, 7.1);
    vec2 gridPosition = panelPosition / cellSize;
    vec2 cell = floor(gridPosition);
    vec2 cellUv = fract(gridPosition);

    float columnSeed = hash(vec2(cell.x, 19.71));
    float laneSeed = hash(vec2(floor(cell.x * 0.37), 77.13));
    float speed = mix(2.6, 10.8, pow(columnSeed, 1.35));
    float period = mix(17.0, 49.0, hash(vec2(cell.x, 5.29)));
    float rowLag = (hash(vec2(cell.x * 2.31, floor(cell.y * 0.23))) - 0.5) * 3.6;
    float flow = seconds * speed + columnSeed * period + laneSeed * 11.0 + rowLag - cell.y;
    float streamIndex = floor(flow / period);
    float phase = flow - streamIndex * period;

    float streamSeed = hash(vec2(cell.x * 1.73 + 7.0, streamIndex * 11.13));
    float streamEnabled = step(0.20, columnSeed) * step(0.26, streamSeed);
    float trailLength = mix(4.5, 18.0, hash(vec2(streamSeed, columnSeed * 3.17)));
    float trailFade = exp(-phase / max(1.0, trailLength * 0.43));
    float trailCutoff = 1.0 - smoothstep(trailLength, trailLength + 5.0, phase);
    float trail = trailFade * trailCutoff;
    float head = pow(1.0 - smoothstep(0.0, 0.95, phase), 2.2);

    float glyphFrame = floor(seconds * mix(1.8, 5.8, hash(vec2(cell.x, streamSeed))) + streamIndex * 7.0);
    float glyphSeed = hash(cell * vec2(1.0, 1.37) + vec2(glyphFrame, streamSeed * 41.0));
    float blinkFrame = floor(seconds * mix(6.0, 17.0, hash(vec2(streamSeed, 9.0))) + cell.y * 0.11);
    float blink = 0.42 + 0.58 * step(0.30, hash(cell + vec2(blinkFrame, streamIndex * 2.0)));
    float dropout = step(0.18 + streamSeed * 0.22, hash(cell + vec2(glyphFrame * 0.41, 31.0)));
    float glyph = glyphMask(cellUv, glyphSeed);

    float ghostFrame = floor(seconds * 1.35 + laneSeed * 19.0);
    float ghost = glyph * step(0.92, hash(cell + vec2(ghostFrame, 93.0))) * edgeFade;
    float rain = glyph * trail * blink * dropout * streamEnabled * edgeFade;
    float headRain = glyph * head * streamEnabled * edgeFade;
    float laneCore = 1.0 - smoothstep(0.23, 0.48, abs(cellUv.x - 0.50));
    float columnMist = (trail * streamEnabled * laneCore * 0.045 + ghost * 0.12) * edgeFade;

    vec3 matrixDark = vec3(0.0, 0.045, 0.018);
    vec3 matrixGreen = mix(vec3(0.00, 0.62, 0.18), vec3(0.05, 1.0, 0.34), columnSeed);
    vec3 headGreen = mix(vec3(0.55, 1.0, 0.55), vec3(0.88, 1.0, 0.78), streamSeed);

    vec3 shaded = color.rgb;
    shaded = mix(shaded, matrixDark, 0.32);
    shaded += matrixGreen * columnMist;
    shaded += matrixGreen * rain * (0.42 + streamSeed * 0.30);
    shaded += matrixGreen * ghost * 0.10;
    shaded += headGreen * headRain * 0.96;
    shaded += matrixGreen * rim * 0.055;
    shaded *= 0.92 + edgeFade * 0.08;

    fragColor = vec4(shaded, color.a);
}
