# Skyblock Toolkit — Design System & Style Guide

Welcome to the **Skyblock Toolkit** design guide. This document establishes the visual language, design principles, color tokens, HUD specifications, and interface standards for all Skyblock Toolkit modules.

---

## 1. Brand Identity & Philosophy

Skyblock Toolkit’s visual identity is inspired by high-performance esports HUDs and modern precision instrumentation, as seen in [`assets/logo.png`](file:///home/jeremy/projects/skyblock-toolkit/assets/logo.png) and [`assets/banner.png`](file:///home/jeremy/projects/skyblock-toolkit/assets/banner.png).

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
   - **In-Game Gameplay HUD**: Clean, non-distracting colors (classic booster green `#00E600`, neutral stat pills, crisp white readouts). Red is reserved strictly for expiring warnings (<3s) or emergency cues (snapped line), never persistent status, avoiding eye fatigue or false danger alarms.
   - **Modpack Menu & Hub**: The unified Crimson Red and Crisp White brand theme.
2. **Dark-Room Balance (Anti-Eye Strain)**:
   - Searing, hyper-saturated neon red and blinding 100% white are strictly avoided.
   - Accents use a balanced, rich crimson (`#B82D42`) and warm burgundy fill (`#2D131A`).
   - Text and handles use soft silver-white (`#DDE1E6`, `#E2E6EA`) with gentle luminance to keep late-night sessions comfortable in completely dark rooms.
3. **Official Modpack Branding**:
   - The official emblem from [`assets/logo.png`](file:///home/jeremy/projects/skyblock-toolkit/assets/logo.png) (with transparent background) is embedded directly into the header/sidebar of the toolkit UI.

---

## 2. Color Palette & Tokens

All shared colors are defined centrally in [`ToolkitTheme.java`](file:///home/jeremy/projects/skyblock-toolkit/src/main/java/com/skyblocktoolkit/ToolkitTheme.java).

### Modpack Menu Palette (Dark-Room Balanced)

| Token | Hex | ARGB Hex | Description / Usage |
| :--- | :--- | :--- | :--- |
| `RED_PRIMARY` / `ACCENT` | `#B82D42` | `0xFFB82D42` | Refined brand crimson. Active tab borders, header branding, focus frames. |
| `RED_DARK` | `#2D131A` | `0xD02D131A` | Warm burgundy fill for selected navigation tabs. |
| `RED_ACTIVE_TRACK` | `#962838` | `0xFF962838` | Muted crimson track for active toggle switches and sliders. |
| `WHITE_PURE` | `#E2E6EA` | `0xFFE2E6EA` | Soft silver-white for switch knobs, slider thumbs, and emphasized text. |
| `WHITE_TEXT` / `TEXT` | `#DDE1E6` | `0xFFDDE1E6` | Primary screen typography and modal headers. |
| `MUTED` | `#8E95A0` | `0xFF8E95A0` | Secondary labels, descriptions, and version text. |
| `FAINT` | `#5F6670` | `0xFF5F6670` | Inactive borders, disabled text, and dividers. |

### Neutrals & Surfaces

| Token | Hex | ARGB Hex | Description / Usage |
| :--- | :--- | :--- | :--- |
| `PANEL` | `#1B1D21` | `0xC91B1D21` | Main modal and window background (semi-translucent dark slate). |
| `SIDEBAR` | `#17191D` | `0xD417191D` | Navigation sidebar background with contrasting depth. |
| `SURFACE` | `#23262B` | `0xB823262B` | Card, container, and inactive button background. |
| `HOVER` | `#2C3036` | `0xDA2C3036` | Interactive hover state for cards, fields, and buttons. |

---

## 3. In-Game HUD Architecture

HUD elements remain clean, neutral, and easy on the eyes during active gameplay, farming, and fishing.

### 3.1. The Booster Progress Bar
- **Dimensions**: `104px` wide × `5px` high (with 1px outer black contour).
- **Background Track**: `0xFF052205` deep forest green.
- **Fill Core**: Clean 3-layer booster green:
  - Top highlight (`1px`): `0xFF38FF38`
  - Center core: `0xFF00E600`
  - Bottom shadow (`1px`): `0xFF009900`
- **Expiring Warning (< 3s)**: Alternating amber (`0xFFFFBB00`) and urgency red (`0xFFEE2222`).
- **Segment Dividers**: Quarter notches at 25%, 50%, and 75% (`0x80000000`).
- **Label Typography**: `§a+39% Booster: §f0m 42s` (familiar green prefix + crisp white duration).

### 3.2. Rate Test & Stat Overlays
- **Pill Container**:
  - Background: `0x90000000` (neutral dark translucent pill).
  - Border: `1px` outline `0x30FFFFFF` (subtle white border).
- **Labeling**:
  - Running: `§eRate Test: §f{mm:ss} §7({rate}/h)`
  - Prep Countdown: `§eStarting in §c§l{n}s...`
  - Completed: `§aTest Done: §a§l{rate}/h`

- **Crop Average**: `Crops (10s): ${rate}/h` in clean neutral white `0xFFFFFFFF`.
- **Fishing Estimate**: `Fishing est. ({window}): ${rate}/h` in clean neutral white `0xFFFFFFFF`.
- Both HUD elements support click-and-drag re-positioning via the HUD Layout Canvas (`Shift` menu → **Edit HUD**).

---

## 4. Menu & Lunar-Style Navigation Hub

The toolkit hub (`ToolkitScreen`) uses a modern sidebar-and-card layout.

### 4.1. Navigation Tabs
- **Default State**: Transparent surface, `ToolkitTheme.MUTED` (`#8E95A0`) text.
- **Hover State**: `ToolkitTheme.SURFACE` (`#23262B`) rounded pill.
- **Selected Tab**:
  - Outer border: `ToolkitTheme.RED_PRIMARY` (`#B82D42`).
  - Inner fill: `ToolkitTheme.RED_DARK` (`#2D131A`).
  - Text: `ToolkitTheme.WHITE_PURE` (`#E2E6EA`).

### 4.2. Toggle Switches
- **Track**: `24px` × `12px` rounded capsule.
  - Off: `0xFF353940` soft dark slate.
  - On: `ToolkitTheme.RED_ACTIVE_TRACK` (`#962838`) warm muted crimson.
- **Slider Thumb**: `8px` × `8px` rounded pill.
  - Off: `ToolkitTheme.MUTED` (`#8E95A0`).
  - On: `ToolkitTheme.WHITE_PURE` (`#E2E6EA`) soft silver-white.

### 4.3. Sliders
- **Track Background**: `0xFF353940` (soft dark groove).
- **Filled Track**: `ToolkitTheme.RED_ACTIVE_TRACK` (`#962838`).
- **Slider Knob**: `4px` × `4px` rounded silver-white thumb (`#E2E6EA`).

---

## 5. Minecraft Formatting Codes Reference

When writing in-game messages, tooltips, or HUD labels, adhere to this formatting standard:

| Code | Color Name | Standard Use |
| :--- | :--- | :--- |
| `§c` | Red (Crimson) | Brand tags, active multipliers (`§c+39%`), alert headers. |
| `§f` | White | Quantities, countdown timers, earnings amounts (`§f$1.2M/h`). |
| `§7` | Gray | Unit suffixes, secondary annotations (`§7/h`, `§7(10s)`). |
| `§8` | Dark Gray | Dividers, subtle brackets (`§8|`, `§8[`). |
| `§e` | Yellow | Temporary warnings or pending countdowns. |
| `§l` | Bold | Emphasized numbers or critical alerts. |

---

## 6. Implementation Checklist for New Features

When creating or modifying screens and HUD widgets:
1. **Always use [`ToolkitTheme`](file:///home/jeremy/projects/skyblock-toolkit/src/main/java/com/skyblocktoolkit/ToolkitTheme.java)** for colors rather than raw hex literals.
2. **Keep HUD rendering lightweight**: use native `GuiGraphicsExtractor` primitives (`fill`, `outline`, `text`), avoiding heavy texture loads or allocations during the render loop.
3. **Respect the Red & White hierarchy**: primary interactive accents should use `RED_PRIMARY`, value outputs should use `WHITE_PURE` or `TEXT`, and containers should use `PANEL` / `SURFACE`.
4. **Ensure drag-and-drop support**: any new persistent HUD element should be registered with `ToolkitLayout` / `BoosterHudOverlay` so users can position it freely.
