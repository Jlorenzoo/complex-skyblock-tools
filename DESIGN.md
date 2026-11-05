# Complexify — Design System & Style Guide

Welcome to the **Complexify** design guide. This document establishes the visual language, design principles, color tokens, HUD specifications, and interface standards for all Complexify modules.

---

## 1. Brand Identity & Philosophy

Complexify’s visual identity is inspired by high-performance esports HUDs and modern precision instrumentation, as seen in [`assets/logo.png`](assets/logo.png) and [`assets/banner.png`](assets/banner.png).

```
   ┌─────────────────────────────────────────────────────────────┐
   │                     [ TELEMETRY ]                           │
   │                          ▲                                  │
   │                    /───────────\                            │
   │                   /  COMPLEXIFY \   ◄── Bold Crimson Red    │
   │                  │  CLIENT SUITE │  ◄── Crisp Pure White    │
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
   - Keep large surfaces neutral; reserve vivid crimson and white for small controls and branding.
   - Structural rails and selected markers use artwork crimson (`#E5254A`); text accents use contrast-adjusted crimson and rose.
   - Text uses crisp silver-white (`#F4F5F7`) and handles use white with gentle luminance to keep late-night sessions comfortable in completely dark rooms.
3. **Official Modpack Branding**:
   - The official emblem from [`assets/logo.png`](assets/logo.png) (with transparent background) is embedded directly into the header/sidebar of the toolkit UI.

---

## 2. Color Palette & Tokens

All menu colors are defined in [`ToolkitPalette.java`](src/main/java/com/skyblocktoolkit/ToolkitPalette.java), selected through `ToolkitTheme.palette()`. Avoid fixed menu colors so both modes remain consistent.

The menu header switches between **Dark mode** and **Light mode** using a compact pill with a sun on the left and a crescent moon on the right. Both icons remain visible; a crimson segment with a white icon marks the active mode. The control retains its current mode label for narration and a tooltip describing the next mode. Dark remains the default; the choice persists as `lightMode` in `skyblocktoolkit.json`. Changing it rebuilds the current page immediately without changing its scroll position or other module settings.

| Palette accessor | Dark / red | Light / red | Purpose |
| :--- | :--- | :--- | :--- |
| `panel()` | `#1B1D21` | `#FAFAFC` | Main panel |
| `sidebar()` | `#101216` | `#EEEFF3` | Sidebar |
| `surface()` | `#24272D` | `#E8E9F0` | Controls and fields |
| `hover()` | `#30343B` | `#DDDFE8` | Hover / focus fill |
| `text()` | `#F4F5F7` | `#232632` | Primary text |
| `muted()` | `#ADB4BF` | `#596171` | Secondary text |
| `accent()` | `#FF7089` | `#AE1939` | Brand, selection, focus |
| `accentSurface()` | `#361923` | `#F8DDE4` | Selected navigation fill |
| `accentTrack()` | `#E5254A` | `#D51F43` | Enabled switch / slider |
| `border()` | `#373B44` | `#CCD0DB` | Subtle panel / control edge |
| `error()` | `#FF9EAD` | `#AA2140` | Invalid input |

The hub echoes the artwork with a larger shield emblem, broken crimson edge rails, filled crystal facets, and a faint carbon weave confined to the sidebar. A burgundy / pale rose header carries a small crystal constellation. Topic rows use 40px surfaces with native item icons on burgundy / pale rose badges, angular end details, and a crimson hover rail; the settings body stays neutral. Compact layouts retain a reduced header crystal cluster, the emblem and edge rails. Decorations stay outside the scrolling settings and use palette-backed colors. Selected tabs have a crimson marker as well as a tinted fill.

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

### 3.2. Booster Notifications

Activation, coconut and ending banners use a compact 30px-high toast near the top edge, away from the crosshair. Use regular-size sentence-case text, a muted detail line, and a tiny sage/purple status mark. The neutral background is 40% opaque with soft corners; no colored frame, stripe, enlarged text or uppercase headline. Keep the same independent gameplay colors in either menu theme. Fit long source names to the viewport with an ellipsis and fade text together with the surface. Existing banner/flash preferences and timing remain independent.

### 3.3. Rate Test & Stat Overlays
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

The toolkit hub (`ToolkitScreen`) uses a streamlined sidebar structured around **6 primary pillars**:
1. **Boosters**: Booster timers, ending banner & flash alerts, 5-second reminders, coconut triggers and audio alerts, and session stats/history.
2. **Skills**: Gathering activities (Farming crop visibility/rates, Fishing catch/depleted cues and inline fish sell prices, and Mining XP tracking).
3. **Seasonals**: Item overview with real in-game icons, names and subtitles. Selecting Void Rod or Bone Leggings opens its dedicated settings page; the fixed breadcrumb returns to the item list.
4. **HUD & Overlays**: Centralized on-screen displays matrix for all HUD element toggles, the interactive drag-and-drop HUD placement editor (`go(5)`), and restore defaults.
5. **Utilities**: Quality-of-life tools including chat text selection & copy, auction house sale chime notifications, blackout (AFK) mode, and global keyboard shortcuts.
6. **Stats**: Earning tools appear automatically after their first cash, shard or catch result, with automatic sessions and inactivity pauses, weighted cash/shard rates and history across restarts. Unused inventory, named storage and passive gear stay out of the library; held-tool income is never copied to armor. There are no registration or session controls. Cash and item-value estimates remain distinct.

Toggle descriptions are part of the owning widget: align both lines at a 6px inset, use 75% muted helper text 3px below the title, and keep the switch level with the title. Described toggles use a 28px target and scroll as one unit, with a consistent gap before the next setting. Keep field hints at the same inset and helper scale.

Use 20px control targets with a 30px row rhythm and extra space before section headings. Menu labels and buttons use the shared 85% text scale; editable values retain native text for legibility. Section headings use neutral primary text so crimson stays focused on selected navigation and active controls. Most settings have a left-aligned label and a right-aligned switch, short value field or slider. Seasonal overview cards use 32px targets with native item rendering. Actions use small, flat surfaces without an outline; paired actions have equal widths. Do not stretch numeric fields, key bindings or sliders across the entire content area. Input text has no shadow in either theme. The hub is at most 600 × 400 GUI pixels, with a 132px sidebar and generous content insets, and moves navigation to a compact top row on narrow or short screens where 5 readable tabs (`Boost`, `Skills`, `Season`, `HUD`, `Utils`) fit comfortably without clipping.

Category landing views show a short list of topics with a title, a one-line scope description and a chevron. Each entire row is clickable and keyboard accessible. Open one topic at a time, with a fixed breadcrumb above the scrolling settings; Escape returns to the topic list before closing. Returning restores keyboard focus and scrolls the originating topic row into view. Clicking the selected category also returns to its list. Switching categories, theme and window size retain the selected topic during the menu session. Utilities puts Keyboard shortcuts first. Seasonal item cards open a single focused item page using the same fixed navigation.

Search stays above the scrolling content. It searches individual setting labels, categories and setting keywords across every category, ignoring case and matching all entered words within one setting. Results show the setting name above its category / topic path and bring that control into view, automatically opening any named details group that owns it. Search matches topic names as well as labels and keywords. Use descriptive, independently expandable detail groups for occasional appearance, valuation and calibration controls; keep the primary toggles visible. Expanders retain their keyboard focus and state through rebuilds. Shared settings have one search destination; the sell-all binding lives in Utilities → Keyboard shortcuts. Ctrl+F / Cmd+F focuses search; Escape clears the query first, restoring the previous scroll position; a second Escape returns from a topic to its category. Theme changes retain the query. Page Up / Page Down scroll the body when an input is not focused. Close and Edit HUD stay in the footer; leaving the HUD canvas returns to its originating category.

### 4.1. Navigation Tabs
- **Default State**: Transparent surface and `palette().muted()` text.
- **Hover State**: `palette().surface()` rounded pill.
- **Selected Tab**:
  - Text: `palette().accent()`, without an outer outline.
  - Inner fill: `palette().accentSurface()`.

### 4.2. Toggle Switches
- **Track**: `20px` × `10px` rounded capsule.
  - Off: `palette().track()`.
  - On: `palette().accentTrack()`.
- **Slider Thumb**: `6px` × `6px` rounded pill.
  - Both states: `palette().onAccent()`, with position indicating off/on.

### 4.3. Sliders
- **Track Background**: `palette().track()`.
- **Filled Track**: `palette().accentTrack()`.
- **Slider Knob**: `4px` × `6px` thumb using `palette().accent()`. Keep the track and percentage on the right, aligned with other setting values; mouse input must map to the visible track.

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
