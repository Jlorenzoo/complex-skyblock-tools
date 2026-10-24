# Skyblock Toolkit

This repository is `Jlorenzoo/complex-skyblock-tools`, the Fabric client mod. It is separate from the `mc-afk-oracle` server-control project. Read `DEVELOPMENT.md` for build, test and installation guidance. When present, `.local/WORKSTATION.md` records ignored workstation-specific paths and deployment state.

A unified Minecraft Fabric client mod bringing together booster tracking, fishing signals, chat copying, and a Lunar Client–inspired configuration hub.

## Tech Stack & Environment
- **Primary Minecraft Version:** 1.21.11 (Fabric Loader >= 0.18.4)
- **Java Version:** Java 21 or newer to build; Java 21 to play
- **Dependencies:** Fabric API (`fabric-api-0.141.6+1.21.11.jar`), Fabric Screen API, Mixin
- **Shared sources:** `src/main` retains 26.2 API names; `versions/1.21.11` adapts/remaps them. Keep 1.21.11 the default for future pack updates. The old 26.2 profile is a fallback, not the live install target.

## Modules
1. `com.moneybooster` - Coconut & booster earnings tracker, rolling hourly averages, session history.
2. `com.fishingpings` - Reel depth alert (30 blocks), depleted area audio cues, snapped line detection, sell price earnings estimation.
3. `com.skyblocktoolkit.chat` - Native click-and-drag chat text selection, multi-click expansion (double-click word, triple-click line), and clipboard copy (Ctrl+C / Cmd+C).
4. `com.skyblocktoolkit` - Unified Lunar-style hub (`ToolkitScreen`), layout drag-and-drop HUD editor (`ToolkitLayout`), and modern theme (`ToolkitTheme`, `ToolkitButton`).
5. `com.skyblocktoolkit.ClearHarvest` - Built-in crop visibility resource pack (90% desaturation & 25% darkening for immature stages), in-game Farming tab toggle, and dynamic texture reloading.

## Commands & Workflows
### Standing delivery rule
- For every requested change, complete the implementation, build it, and run the relevant checks. Verify mod changes work in the configured modpack using the available isolated Fabric/runtime checks; visually inspect UI changes in both themes and compact layouts.
- Install each updated build into the live modpack using `python scripts/install.py --wait` (`python3` where available). If Minecraft is running, queue the installer safely and report that installation is pending; never force-close the player's game. Verify the installed jar checksum once installation completes. Do not describe a queued install as completed or claim live gameplay was verified when only isolated checks ran.
- Commit the intended changes and publish them to `Jlorenzoo/complex-skyblock-tools` on GitHub's `main` branch as part of the same task. This is standing user authorization to install and update `main` without asking again. Preserve unrelated work, fetch remote changes first, and never force-push. If branch protection requires a pull request, follow that path through merge where permitted.
- Keep the existing version for routine changes. Bump versions, create release tags, or publish a GitHub release only when the user explicitly requests a new version/release. Routine build, verification, installation and publication to `main` do not require a separate confirmation.

- **Build Mod:**
  ```bash
  python3 build.py
  ```
  Produces `versions/1.21.11/build/libs/skyblock-toolkit-1.1.0+mc1.21.11.jar` and runs the Java regression suites. The explicit `--minecraft 26.2` build remains available with legacy dependency paths.
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
