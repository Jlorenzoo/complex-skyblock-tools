# Skyblock Toolkit

This repository is `Jlorenzoo/complex-skyblock-tools`, the Fabric client mod. It is separate from the `mc-afk-oracle` server-control project. Read `DEVELOPMENT.md` for build, test and installation guidance. When present, `.local/WORKSTATION.md` records ignored workstation-specific paths and deployment state.

A unified Minecraft Fabric client mod bringing together booster tracking, fishing signals, chat copying, and a Lunar Client–inspired configuration hub.

## Tech Stack & Environment
- **Minecraft Version:** 26.2 (Fabric Loader >= 0.19.0)
- **Java Version:** Java 25
- **Dependencies:** Fabric API (`fabric-api-0.160.0+26.2.jar`), Fabric Screen API, Mixin

## Modules
1. `com.moneybooster` - Coconut & booster earnings tracker, rolling hourly averages, session history.
2. `com.fishingpings` - Reel depth alert (30 blocks), depleted area audio cues, snapped line detection, sell price earnings estimation.
3. `com.skyblocktoolkit.chat` - Native click-and-drag chat text selection, multi-click expansion (double-click word, triple-click line), and clipboard copy (Ctrl+C / Cmd+C).
4. `com.skyblocktoolkit` - Unified Lunar-style hub (`ToolkitScreen`), layout drag-and-drop HUD editor (`ToolkitLayout`), and modern theme (`ToolkitTheme`, `ToolkitButton`).
5. `com.skyblocktoolkit.ClearHarvest` - Built-in crop visibility resource pack (90% desaturation & 25% darkening for immature stages), in-game Farming tab toggle, and dynamic texture reloading.

## Commands & Workflows
- **Build Mod:**
  ```bash
  python3 build.py
  ```
  Produces `build/skyblock-toolkit-1.0.0.jar`.
- **Run Tests:**
  ```bash
  python3 tests/test_installer.py
  ```
- **Install to Live Client:**
  ```bash
  python3 scripts/install.py --wait
  ```
  Safely replaces individual mod jars with byte-verified backups once Minecraft exits.

## UI & Design Conventions
- **Theme & Guidelines:** Follow [`DESIGN.md`](DESIGN.md) and [`ToolkitTheme.java`](src/main/java/com/skyblocktoolkit/ToolkitTheme.java). Follow the brand Crimson Red (`#E5254A`) and Crisp White (`#FFFFFF`) aesthetic inspired by `assets/logo.png` and `assets/banner.png`. Use subtle dark panels (`#1B1D21`), sidebar contrast (`#17191D`), and smooth pixel-rounded corners.
- Use `ToolkitTheme.palette()` for every menu surface, text, control and error state; both light/red and dark/red modes must remain readable. Preserve the HUD's independent money/XP colors. Theme changes apply immediately and save in `skyblocktoolkit.json`.
- **Widgets:** Use `ToolkitButton` for toggle switches (pill sliders), navigation tabs, and cards.
- **Rendering:** Uses Fabric / vanilla `GuiGraphicsExtractor` APIs for screen rendering.
