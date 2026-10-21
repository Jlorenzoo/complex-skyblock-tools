# Skyblock Toolkit — Design System & Style Guide

Welcome to the **Skyblock Toolkit** design guide. This document establishes the visual language, design principles, color tokens, HUD specifications, and interface standards for all Skyblock Toolkit modules.

---

## 1. Brand Identity & Philosophy

Skyblock Toolkit’s visual identity is inspired by high-performance esports HUDs and modern precision instrumentation, as seen in [`assets/logo.png`](assets/logo.png) and [`assets/banner.png`](assets/banner.png).

```
   ┌─────────────────────────────────────────────────────────────┐
   │                       [ HUD ]                               │
   │                          ▲                                  │
   │                    /───────────\                            │
   │                   /  SKYBLOCK   \   ◄── Bold Crimson Red    │
   │                  │   TOOLKIT     │  ◄── Crisp Pure White    │
   │                   \─────────────/                           │
   │                          ▼                                  │
   │               Carbon Slate & Crimson Glow                   │
   └─────────────────────────────────────────────────────────────┘
```

### Core Principles
1. **Separation of HUD and Menu**:
   - **In-Game Gameplay HUD**: Green money and purple XP timers, neutral stat pills and white readouts. The optional final-five-second cue keeps the timer's identity and uses a soft outline rather than an urgent red flash.
   - **Modpack Menu & Hub**: The unified Crimson Red and Crisp White brand theme.
2. **Dark-Room Balance (Anti-Eye Strain)**:
   - Searing, hyper-saturated neon red and blinding 100% white are strictly avoided.
   - Accents use a balanced, rich crimson (`#B82D42`) and warm burgundy fill (`#2D131A`).
   - Text and handles use soft silver-white (`#DDE1E6`, `#E2E6EA`) with gentle luminance to keep late-night sessions comfortable in completely dark rooms.
3. **Official Modpack Branding**:
   - The official emblem from [`assets/logo.png`](assets/logo.png) (with transparent background) is embedded directly into the header/sidebar of the toolkit UI.

---

## 2. Color Palette & Tokens

All menu colors are defined in [`ToolkitPalette.java`](src/main/java/com/skyblocktoolkit/ToolkitPalette.java), selected through `ToolkitTheme.palette()`. Avoid fixed menu colors so both modes remain consistent.

The menu header switches between **Dark mode** and **Light mode** using a compact pill with a sun on the left and a crescent moon on the right. Both icons remain visible; a crimson segment with a white icon marks the active mode. The control retains its current mode label for narration and a tooltip describing the next mode. Dark remains the default; the choice persists as `lightMode` in `skyblocktoolkit.json`. Changing it rebuilds the current page immediately without changing its scroll position or other module settings.

| Palette accessor | Dark / red | Light / red | Purpose |
| :--- | :--- | :--- | :--- |
| `panel()` | `#1B1D22` | `#F9F9FC` | Main panel |
| `sidebar()` | `#15171B` | `#F0F0F5` | Sidebar |
| `surface()` | `#25282E` | `#E8E9F0` | Controls and fields |
| `hover()` | `#30343B` | `#DDDFE8` | Hover / focus fill |
| `text()` | `#E4E7EC` | `#232632` | Primary text |
| `muted()` | `#A3AAB5` | `#596171` | Secondary text |
| `accent()` | `#E6647A` | `#B41F42` | Brand, selection, focus |
| `accentSurface()` | `#391E28` | `#F8DDE4` | Selected navigation fill |
| `accentTrack()` | `#C33D56` | `#BF2949` | Enabled switch / slider |
| `border()` | `#373B44` | `#CCD0DB` | Subtle panel / control edge |
| `error()` | `#FF9EAD` | `#AA2140` | Invalid input |

Both themes use deliberate surface contrast, restrained red accents, matching subtle borders, a small header accent, and soft shadows. Light mode uses an off-white canvas and deep ink text, rather than reversing the colors mechanically. Toggles and slider thumbs stay warm white against their tracks. History, rate tests, inputs and detail screens use the same shared palette.

`ToolkitPaletteTest` checks at least 4.5:1 contrast for primary and secondary text across menu surfaces, selected navigation, invalid input and colored timer status. Money/XP status text uses darker semantic colors in light mode; the gameplay HUD keeps its existing green/purple identity.

---

## 3. In-Game HUD Architecture

HUD elements remain clean, neutral, and easy on the eyes during active gameplay, farming, and fishing.

### 3.1. The Booster Progress Bars
- Money and XP have independent clocks and visibility settings. Render only enabled, active effects, money first and XP beneath it with a 24px row spacing. A single visible effect occupies the first row.
- Money uses `ToolkitTheme.MONEY_TIMER` (sage green), XP uses `ToolkitTheme.XP_TIMER` (soft purple); each has matching track/highlight/shadow tokens.
- The optional five-second reminder starts disabled. When enabled, append a muted `· ending` cue and a slowly pulsing, low-opacity outline to that timer only. Keep its identifying color; do not flash the screen or play audio.
- The HUD editor previews both effects, with separate eye controls and a shared draggable anchor.
- **Dimensions**: `104px` wide × `5px` high (with 1px outer black contour).
- **Segment Dividers**: Quarter notches at 25%, 50%, and 75% (`0x80000000`).
- **Label Typography**: `Money +39%: §f0m 42s` / `XP +150%: §f2m 10s`, with a type-colored prefix and white duration.

### 3.2. Rate Test & Stat Overlays
- **Pill Container**:
  - Background: `0x90000000` (neutral dark translucent pill).
  - Border: `1px` outline `0x30FFFFFF` (subtle white border).
- **Labeling**:
  - Running: `§eRate Test: §f{mm:ss} §7({rate}/h)`
  - Prep Countdown: `§eStarting in §c§l{n}s...`
  - Completed: `§aTest Done: §a§l{rate}/h`

- **Crop Average**: `Crops (60s): ${rate}/h` in clean neutral white `0xFFFFFFFF`, adaptively calculating over active duration up to 60s.
- **Fishing Estimate**: `Fishing est. ({window}): ${rate}/h` in clean neutral white `0xFFFFFFFF`.
- **XP Average**: `XP (60s): {rate} XP/h` or `XP base (60s): {rate} XP/h`, with independent visibility and position, throttled to 1-second display updates and adaptively calculating over active duration up to 60s. It can appear alongside either money HUD; the XP booster preference is shared with the Minecraft XP rate-test view.
- All three HUD elements support click-and-drag re-positioning via the HUD Layout Canvas (`Shift` menu → **Edit HUD**).
- Rate tests offer Money and Minecraft XP tabs, the existing duration choices, and a palette-backed **Include XP boosters** pill for XP. Both measures share the same sample clock. The XP view shows actual or base XP/hour, XP gained and booster bonus. Compact screens scroll the body while keeping Start/Stop and Done fixed. HUD previews stay outside the rate-test controls.

---

## 4. Menu & Lunar-Style Navigation Hub

The toolkit hub (`ToolkitScreen`) uses a modern sidebar-and-card layout.

### 4.1. Navigation Tabs
- **Default State**: Transparent surface and `palette().muted()` text.
- **Hover State**: `palette().surface()` rounded pill.
- **Selected Tab**:
  - Outer border and text: `palette().accent()`.
  - Inner fill: `palette().accentSurface()`.

### 4.2. Toggle Switches
- **Track**: `24px` × `12px` rounded capsule.
  - Off: `palette().track()`.
  - On: `palette().accentTrack()`.
- **Slider Thumb**: `8px` × `8px` rounded pill.
  - Both states: `palette().onAccent()`, with position indicating off/on.

### 4.3. Sliders
- **Track Background**: `palette().track()`.
- **Filled Track**: `palette().accentTrack()`.
- **Slider Knob**: `4px` × `4px` thumb using `palette().onAccent()`.

---

## 5. Minecraft Formatting Codes Reference

When writing in-game messages, tooltips, or HUD labels, adhere to this formatting standard:

| Code | Color Name | Standard Use |
| :--- | :--- | :--- |
| `§c` | Red (Crimson) | Brand tags, active multipliers (`§c+39%`), alert headers. |
| `§f` | White | Quantities, countdown timers, earnings amounts (`§f$1.2M/h`). |
| `§7` | Gray | Unit suffixes, secondary annotations (`§7/h`, `§7(60s)`). |
| `§8` | Dark Gray | Dividers, subtle brackets (`§8|`, `§8[`). |
| `§e` | Yellow | Temporary warnings or pending countdowns. |
| `§l` | Bold | Emphasized numbers or critical alerts. |

---

## 6. Implementation Checklist for New Features

When creating or modifying screens and HUD widgets:
1. **Always use [`ToolkitTheme`](src/main/java/com/skyblocktoolkit/ToolkitTheme.java)** for colors rather than raw hex literals.
2. **Keep HUD rendering lightweight**: use native `GuiGraphicsExtractor` primitives (`fill`, `outline`, `text`), avoiding heavy texture loads or allocations during the render loop.
3. **Respect both themes**: use `palette().accent()` for interactive accents, `text()` / `muted()` for content, and the corresponding surface tokens for containers. Avoid Minecraft's dark text shadows on light menu buttons.
4. **Ensure drag-and-drop support**: any new persistent HUD element should be registered with `ToolkitLayout` / `BoosterHudOverlay` so users can position it freely.
