# AnarchyClient — Menu Design Specification

This document describes the AnarchyClient client menu ("liquid glass" UI) precisely enough that a
human or an AI can recreate the look and behaviour from scratch. It is a *design* spec: it states
the intent, the exact tokens, the layout rules, and the rendering techniques. It is not a line-by-line
code dump — where an algorithm matters, the algorithm is described.

Stack it was built on: Minecraft 26.2, Fabric, Java 25, the **Rivet** UI layout library
(`com.github.Lenni0451.rivet`), and **Blaze3D** for all rendering. Nothing is drawn with a legacy
immediate-mode path; every pixel goes through Blaze3D render pipelines.

---

## 1. Design language (the "wow" in one paragraph)

Floating panels of **frosted liquid glass** hover over the *live, still-visible game*. The game
behind each panel is blurred and gently refracted at the panel's rounded edges, with a soft specular
rim on the top edge and a diffuse drop shadow beneath — the panels read as real slabs of glass lit
from above. A slim vertical **icon dock** on the left is the only persistent chrome; everything else
(module grid, inspector, side drawer, Friends/Profiles/Theme tabs) appears as separate floating glass
islands that show and hide instantly. The palette is near-monochrome white-on-dark-glass with a single
configurable **accent** color. It must never look like a Meteor/Vape-style opaque click-GUI: the game
is always the backdrop, corners are always round, shapes are always anti-aliased, motion is always a
soft fade.

Non-negotiables:
- The game stays visible and blurred behind panels (glassmorphism), never a solid or dimmed sheet.
- All shapes are SDF-anti-aliased: circles are round, rounded rects have clean corners, shadows fall
  off as a continuous penumbra (never banded or "lines").
- One accent color drives all "active/selected/focused" affordances; everything else is white at
  varying alpha over dark glass.
- Motion is minimal: only small controls ease between their own states (hover glow, toggle thumb).
  No whole-panel/menu fades, no scale-pop, no slide.

---

## 2. Color tokens

All colors are RGBA with 0–255 channels. "White @ N" means `rgba(255,255,255,N)`.

### Glass substrate
| Token | Value | Use |
|---|---|---|
| `GLASS_BASE` | `rgb(13,17,26)` | Base tint mixed over the blurred scene |
| `glass()` | `GLASS_BASE` @ `round(255 * glassOpacity)` | Standard panel fill |
| `glassDeep()` | `GLASS_BASE` @ `round(255 * (glassOpacity + 0.14))` | Inspector / drawer (denser, keeps text legible) |
| `glassOpacity` | default **0.55**, range 0.15–0.95 | User-editable density |

### Neutral text / surface tokens (fixed)
| Token | Value | Use |
|---|---|---|
| `TEXT` | White @ 236 | Primary text |
| `MUTED` | White @ 152 | Secondary text, inactive labels |
| `FAINT` | White @ 96 | Tertiary text, chevrons |
| `DIVIDER` | White @ 26 | Hairline separators |
| `CARD` | White @ 14 | Resting card/row fill |
| `CARD_HOVER` | White @ 32 | Hovered card/row fill |
| `FIELD` | White @ 15 | Text-field background |
| `TRACK` | White @ 44 | Toggle/slider track |
| `WARNING` | `rgb(240,173,78)` | Warnings |
| Shadow | `rgba(0,0,0,110)` | Panel drop shadow color |

### Accent presets (user-selectable, one active at a time)
| Preset | Active accent |
|---|---|
| **Emerald** (default) | `rgb(0,186,148)` |
| Amber | `rgb(222,148,54)` |
| Rose | `rgb(218,82,116)` |
| Cyan | `rgb(64,165,208)` |

Derived: `accentSoft = accent × 0.42 alpha` (soft glows/fills), `selection = accent × 0.40 alpha`
(text selection). The accent is applied to: active toggle fill, focused text-field outline, selected
dock item stripe/glow, slider active bar + thumb-click, checkbox check, scrollbar-drag, "Apply"/count
labels.

The accent is the **only** hue in the UI. Do not introduce a second color; state is communicated by
alpha and by the accent, not by a rainbow.

---

## 3. Geometry & layout tokens

Distances are in **virtual pixels** (see §7, auto-scale). Panels are laid out with Rivet's absolute
and grid layouts; do not hand-place with raw coordinates where a layout anchor will do.

| Token | Value |
|---|---|
| Corner radius (panels) | default **14**, range 0–24 (user-editable) |
| Dock width | 46 |
| Dock button | 30 × 30, gap 2, divider block 7 |
| Panel outer margin (top/bottom) | 20 |
| Gap between islands | 12 |
| Modules panel width | 336 |
| Tab panel width (Friends/Profiles/Theme) | 420 |
| Inspector width | 288 (wide mode 324) |
| Side drawer width | 280 |
| Inner content padding | 12 |
| Search field height | 28 |
| Section header height | 36 |
| Card gap | 8 |
| Setting row height | 46 (number 64, text 54, option 34) |
| Toggle switch | 24 × 14 track, painted track 12 (box is 2px taller so the AA edge never clips) |
| Icon glyph box | 20 |
| Icon button | 28 |
| Friend head | 20 |

### Drop shadow
Each floating island casts one soft shadow: `spread = 10`, `offsetY = 3`, color `rgba(0,0,0,110)`,
corner radius matching the panel. It must be a **single continuous penumbra** (one SDF shadow draw),
never stacked rectangles.

---

## 4. Screen composition

```
┌───────────────────────────────────────────────────────────────┐
│                                                    brand label  │  ← top-right, faint
│  ┌──┐   ┌───────────────────┐   ┌───────────────┐   ┌────────┐  │
│  │  │   │  MODULES PANEL     │   │  INSPECTOR     │   │ DRAWER │  │
│  │D │   │  ┌─ search ─────┐  │   │  (opened       │   │ (UI    │  │
│  │O │   │  │ card  card   │  │   │   module's     │   │  prefs)│  │
│  │C │   │  │ card  card   │  │   │   settings)    │   │        │  │
│  │K │   │  └──────────────┘  │   │                │   │        │  │
│  └──┘   └───────────────────┘   └───────────────┘   └────────┘  │
└───────────────────────────────────────────────────────────────┘
   fixed    ← these are separate floating glass islands that show/hide →
```

- **Dock** (left, always present): vertical stack of 30px icon buttons. One button per module
  *category*, then a divider, then Friends / Profiles / Theme tab buttons. The dock is vertically
  centered. The **selected** item shows an accent stripe + soft accent glow; hover shows a faint
  white glow. Clicking the already-open item **closes** it (toggle-to-close) → the whole content area
  disappears and only the dock floats over the game.
- **Modules panel**: glass island with a title, a live module count (accent), a search field, and a
  scrolling **card grid**. Each card = one module (icon, name, a toggle). Compact cards are the
  default. Left-click toggles the module; right-click opens/closes its **inspector**.
- **Inspector**: a second glass island to the right of the modules panel showing the opened module's
  header (icon, name, description) and its settings rows. Right-clicking the module again closes it.
- **Tabs** (Friends / Profiles / Theme): replace the modules+inspector area with a single 420-wide
  glass island. Friends lists friends with their rendered **player head** (see §8). Theme edits the
  live design tokens (accent, glass opacity, blur, background design, layout toggles).
- **Drawer** (optional, right edge): a narrow glass island for UI preferences / secondary options.
- **Brand label**: small faint text, top-right.

Everything except the dock is a floating island with its own rounded rect, glass fill, and shadow.

---

## 5. The glass effect (how the blur/refraction works)

This is the signature of the design. Reproduce it faithfully.

1. **Capture a blurred copy of the frame.** Minecraft's menu-background blur pass
   (`processBlurEffect`, strength = the "Menu Background Blur" video setting) is hooked. Around it, a
   backdrop helper:
   - snapshots the *sharp* frame,
   - lets vanilla's single blur pass run,
   - copies the blurred result into a sampleable texture,
   - restores the sharp frame so the game itself stays crisp.
   Net effect: the game is fully visible and sharp, but the menu has a blurred copy of it to sample.
   The **"Blur" slider** sets vanilla's *menu-background blur strength* (0–10, the value the vanilla
   blur pass already uses), so a stronger setting frosts the glass more. Do **not** try to stack
   passes by re-invoking `processBlurEffect` within the frame — the blur post-chain's ring buffer
   cannot be re-submitted mid-frame and it hard-crashes on a GPU fence assert.

2. **Render each panel as a distance-field mesh.** A panel is a 3×3 grid of quads (4 corners, 4
   edges, 1 center). Each vertex carries a UV equal to its **distance to the nearest panel edge, in
   corner-radius units**. In the fragment shader (`glass_panel.fsh`) this reconstructs an exact
   rounded-rect SDF, so corners are analytically anti-aliased at any scale. One draw call per panel.

3. **Per-fragment glass shading** (all in `glass_panel.fsh`):
   - `alpha = clamp(distPx + 0.5, 0, 1)` — crisp AA rounded-rect coverage.
   - **Edge refraction**: within a band along the contour, bend the sampled blurred-scene UV *inward*
     (toward panel center) by a falloff `pow(1 - smoothstep(0, band, distPx), 1.8)`. This makes the
     glass "grab" the background at its rim.
   - **Chromatic dispersion**: sample R/G/B of the background at slightly different offsets
     (×1.05 / ×1.0 / ×0.95) so the refracted edge splits color faintly, like real glass.
   - **Frost**: lift saturation ~1.12× so the glass reads dense, not muddy.
   - **Tint**: `mix(background, tint.rgb, tint.a)` — the glass token over the refracted scene.
   - **Specular rim**: `rim = 1 - smoothstep(0.4, 2.2, distPx)`, brightened where the edge faces "up"
     (`topness = clamp(-inwardDir.y, 0, 1)`), added as white. Lit-from-above highlight on the top
     contour.

4. **Everything else** (fills, rounded rects, circles, shadows) also renders through SDF shaders
   (`sdf_fill.fsh`, `sdf_shadow.fsh`) so nothing is tessellated/jagged. Fill coverage
   `= clamp(sdf * radiusPx + 0.5, 0, 1)`; shadow fade `= smoothstep(0,1,sdf)²` for a soft penumbra.

5. **Animated backgrounds** (optional, "Background design"): extra fragment shaders can play a subtle
   animated pattern *inside* the glass at low opacity (e.g. deep gradient, caustics, fire, matrix,
   retro grid, simplex flow). Off by default; purely decorative, layered under the glass tint.

---

## 6. Motion — micro-transitions only

The menu deliberately has **no open/close or tab fade** and no slide/scale reveals — panels appear
and disappear instantly. Whole-panel fades and reveals were tried and removed for looking cheap; the
menu should feel immediate and solid, not animated.

The only motion is small, local state easing on individual controls, so state changes read smoothly
without the UI feeling "animated":

- Dock item hover glow, dock selection glow.
- Toggle-switch thumb sliding between off/on.
- Card / row hover highlight.

These run through one exponential-smoothing helper (frame-rate independent easing toward a target;
the menu re-renders every frame). Suggested easing speeds (higher = snappier): 14–16.

Rules:
- No whole-panel or whole-menu fades. No scale, slide, or bounce on position/size.
- Motion is confined to a control easing between its own visual states.

---

## 7. Auto-scale (fit any GUI scale)

The menu is authored for **GUI Scale 2** and must look identical regardless of the user's Minecraft
GUI-scale setting (like a BleachHack-style auto-fit).

- `uiScale = clamp(REFERENCE_GUI_SCALE / actualGuiScaleFactor, 0.5, 2)` with
  `REFERENCE_GUI_SCALE = 2`.
- Rivet lays out and hit-tests in **virtual space** = `realPixels / uiScale`.
- Render is wrapped in a pose `scale(uiScale, uiScale)`; mouse coordinates are divided by `uiScale`
  before being handed to Rivet. (Scissors are pose-transformed in 26.2, so the pose scale handles
  clipping too.)

Result: the menu keeps a constant on-screen size and always fits the screen; changing GUI scale
never resizes or clips it.

---

## 8. Friend heads

Friends are stored by name. In the Friends tab, each row renders the player's **face** (head + hat
overlay) to the left of the name:

- Build a resolvable profile from the name (`ResolvableProfile.createUnresolved(name)`), look it up in
  vanilla's player-skin render cache (async; falls back to the default Steve/Alex skin until loaded),
  and blit the face via vanilla's `PlayerFaceExtractor` at 20×20.
- Because the menu re-renders every frame, the head upgrades from the default to the real skin within
  a moment of resolving.

---

## 9. Global-token architecture (the Theme tab is live)

All editable design values are **global variables read live by every component**, so edits in the
Theme tab restyle the entire UI on the next frame with no rebuild:

- Editable globals: accent preset, glass opacity, corner radius, blur strength, background design,
  plus layout toggles (compact cards, hover descriptions, wide inspector).
- These persist to the client config (`UiPreferences`) and load back on startup.
- Fixed tokens (the neutral White-@-N scale, glass base, shadow) are constants, not user-editable.

To recreate: keep one central token holder that components query each frame; never bake a token into a
component at construction time if it should respond to Theme edits.

---

## 10. Recreation checklist

1. Capture a blurred, sampleable copy of the game frame; keep the real frame sharp.
2. Render panels as rounded-rect SDF meshes that sample that blurred texture with edge refraction,
   chromatic dispersion, frost, tint, and a top specular rim.
3. Draw every other shape (fills, circles, shadows) through SDF shaders — no tessellation.
4. Compose the screen as a fixed left dock + separate floating glass islands (modules grid,
   inspector, tabs, drawer) with single soft drop shadows.
5. Use the exact color tokens (§2) and geometry (§3); one accent hue, everything else white-alpha.
6. Auto-scale to reference GUI scale 2 (§7).
7. Motion is micro-transitions only (hover glows, toggle thumb); no whole-panel or whole-menu
   fades (§6).
8. Keep every editable value as a live global read each frame so the Theme tab restyles instantly.
9. Render friend heads from resolved skins with a Steve/Alex fallback (§8).

If it looks like an opaque click-GUI, has jagged shapes, moves by sliding/scaling, or shows a second
hue — it's wrong. Frosted glass over the live game, round and soft, white + one accent, immediate.
