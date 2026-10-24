# Kiln

Kiln is a matte claymorphism system for web apps, dashboards and mobile UI. Surfaces are made of the same clay as the background: they swell up out of it, rest on top of it, or are pressed into it. There is no gloss, no outlines and no plastic shine, only soft light from the top-left and a fine, unglazed grain.

## Principles

1. **Matte, never glossy.** Highlights stay low and there are no specular dots. One grain layer covers the whole ground.
2. **Molded, not stacked.** The ground (`bg`), molded surfaces (`surface`) and wells (`well`) share one colour, so depth comes only from light: `shadow-dark` falls bottom-right and `shadow-light` top-left.
3. **Pressed means active.** Raised means you can touch it; pressed into the clay means selected, in use or editable (inputs, tracks, the pressed state).

## The stacking rule

Light halos pile up and turn muddy when molded surfaces sit on molded surfaces. So:

- **Mold only from the ground.** `mold-1`, `mold-2` and `mold-3` are for surfaces that sit directly on the background, like cards, panels and list rows.
- **Inside a raised surface, float or dip.** Children either rest on top (`float-1`…`float-3`: the `surface-2` fill, a shadow cast straight down, no light halo) or are pressed in (`dip-sm`, `dip`). A molded `.k-clay` nested in another switches to `float-1` automatically.
- **Highlights may float anywhere.** The active nav item, overlays, sheets, the cover and illustration pieces always float.
- All depth tokens share the same four shadow layers in the same order, so any state animates smoothly into any other: from flat to molded, from floating to dipped.

## Colour

Four clay bodies, each fired light and dark. They are switchable themes: **Slate** is the default, **Terracotta** has a heavier grain for a rougher, earthier feel, **Sage** is quiet celadon, and **Ochre** is a warm, sandy buff.

- `ink` on `bg`, `surface`, `surface-2` and `well` is the text colour; `ink-2` is for secondary text. Both pass 4.5:1 in every theme.
- `accent` is the one primary colour: the main button, selected states, progress and the focus ring. Text on it is `accent-ink`. Use one accent-filled element per view.
- `pop` is the complementary colour, used rarely: destructive actions and one illustration piece. Text on it is `pop-ink`.
- `ok`, `warn` and `err` are for status text and badges. Always pair them with a word or icon, never colour alone.
- `line` is for hairlines and dashed outlines only, never card borders.

## Type

Three pairings share one scale; **B is the default**. To switch, add a class to the `.kiln` ancestor:

| Option | Display | Body | Feel | Class |
| --- | --- | --- | --- | --- |
| A | Fredoka 600 | Nunito | soft and friendly | `type-a` |
| B | Baloo 2 700 | Figtree | chunky and clear | (default) |
| C | Young Serif 400 | Instrument Sans | crafted, studio | `type-c` |

Scale: `k-display` 72–168, `k-h1` 44, `k-h2` 30, `k-h3` 21, `k-body` 16/1.55, `k-small` 14, `k-label` 12 uppercase with +8% tracking. Headings use the display face; everything else uses the body face. Sentence case everywhere.

## Shape and space

- Radii: `radius-md` 18 for inputs, tiles and rows; `radius-lg` 26 for cards; `radius-xl` 38 for panels and sheets; `radius-pill` for buttons, chips, badges, toggles and tab bars. Illustration blobs use irregular radii, so no two corners match.
- Spacing is 4-based (`space-1` 4 to `space-8` 64). Card padding is `space-5`, panel padding `space-6`.
- Touch targets are at least 44 px. Focus is a 3 px `accent` ring, offset 3 px from the clay.

## Motion

Clay moves like clay. Nothing slides in from off-screen; motion happens in depth.

- **Rise:** entrances grow from `flat` to their resting depth in `duration-slow` (520 ms) with `ease-settle`. Stagger list items 80 ms apart, at most 6 steps (`.k-rise` with `--i`).
- **Press:** surfaces sink to `dip-sm` in `duration-fast` (140 ms) and spring back in `duration-base` (260 ms) with `ease-clay`, a soft overshoot.
- **Toggle:** the knob travels in 520 ms with `ease-clay`.
- **Fill:** progress fills in from the left with its rounded ends kept.
- **Squish** (`.k-squish`) is a one-off squash and stretch for success moments. **Drift** is a 7 s idle float, for illustrations only.
- Use one motion per moment, and keep translations at 8 px or less. With `prefers-reduced-motion`, everything is instant.

## Iconography

24 line icons on a 24 px grid: 2 px stroke, round caps and joins, drawn in `currentColor` by the `Icon` component (Assets › Icons has the same set as SVG files). Icons stay flat; give them volume by placing them on a Tile or Button. Never use emoji as UI.

## Illustration

Build scenes from 3–6 floating `Blob`s (pebble, slab, coil, bowl) that rest on a slab or overlap. Use at most one `pop` piece, and `drift` only when nothing else on screen moves.

## Voice

Plain, warm and short. Labels are verbs in sentence case ("Start firing", "Check answer"). Feedback is kind and specific ("Not quite: it's Jupiter"). No exclamation marks except to celebrate a real success, and no emoji.

## Using this system

Load `tokens.css`, then `components/bundle.css` (it imports the Google Fonts for all three pairings), then React 18 and `components/bundle.js`, which exposes `window.Kiln`. Wrap the app in an element with class `kiln`. This element is the ground: it paints `bg`, sets `ink` and the body font, and lays the grain over everything. Set the palette with `data-theme` on `<html>`: `light`, `dark`, `terracotta`, `terracotta-dark`, `sage`, `sage-dark`, `ochre` or `ochre-dark`.

Without React, use the classes directly: `k-clay` (+ `mold-1`, `mold-3`, `float-1/2/3`, `dip`, `dip-sm`, `accent`, `pop`, `raised`), `k-well`, `k-btn` (+ `primary`, `pop`, `ghost`, `sm`, `lg`, `icon`, `is-pressed`), `k-chip`, `k-badge`, `k-tabs`/`k-tab`, `k-field`, `k-toggle`, `k-check`, `k-progress`, `k-tile`, `k-avatar`, `k-nav-item`, `k-blob`, plus the type classes `k-display`…`k-label`.

The reference canvas with example screens (a mobile home screen, a dashboard and a playable quiz app) is the Kiln design canvas this system was made from.
