# Skyblock Toolkit

A unified Minecraft Fabric client mod bringing together booster tracking, fishing signals, chat copying, and a Lunar Client–inspired configuration hub.

## Tech Stack & Environment
- **Minecraft Version:** 26.2 (Fabric Loader >= 0.19.0)
- **Java Version:** Java 25
- **Dependencies:** Fabric API (`fabric-api-0.160.0+26.2.jar`), Fabric Screen API, Mixin

## Modules
1. `com.moneybooster` - Coconut & booster earnings tracker, rolling hourly averages, session history.
2. `com.fishingpings` - Reel depth alert (30 blocks), depleted area audio cues, snapped line detection, sell price earnings estimation.
3. `chat.text.select` - Click-and-drag chat text selection and clipboard copy (Ctrl+C).
4. `com.skyblocktoolkit` - Unified Lunar-style hub (`ToolkitScreen`), layout drag-and-drop HUD editor (`ToolkitLayout`), and modern theme (`ToolkitTheme`, `ToolkitButton`).

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
- **Theme:** Follow `ToolkitTheme.java`. Use subtle dark panels (`#1B1D21`), sidebar contrast (`#17191D`), muted accents, and smooth pixel-rounded corners.
- **Widgets:** Use `ToolkitButton` for toggle switches (pill sliders), navigation tabs, and cards.
- **Rendering:** Uses Fabric / vanilla `GuiGraphicsExtractor` APIs for screen rendering.
