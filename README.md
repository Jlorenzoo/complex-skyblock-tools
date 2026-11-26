<div align="center">

![Complexify Banner](assets/banner.png)

<img src="assets/logo.png" width="85" alt="Complexify Logo" />

# Complexify

**The Precision Quality-of-Life HUD & Telemetry Suite for Complex Gaming Skyblock**

[![Minecraft 1.21.11](https://img.shields.io/badge/Minecraft-1.21.11-ED1C24?style=for-the-badge&logo=minecraft&logoColor=white)](#)
[![Fabric Loader](https://img.shields.io/badge/Fabric-0.18.4+-white?style=for-the-badge&logo=fabric&logoColor=black)](#)
[![Java 21](https://img.shields.io/badge/Java-21-ED1C24?style=for-the-badge&logo=openjdk&logoColor=white)](#)
[![License: GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-white?style=for-the-badge)](#)
[![Server: Complex Gaming](https://img.shields.io/badge/Server-Complex_Gaming-ED1C24?style=for-the-badge)](#)

</div>

---

## Installation

### Option 1: 1-Click Modpack (Recommended)
Pre-configured bundle with **Complexify**, **Simple Voice Chat**, **Litematica**, and Sodium performance optimizations.

1. **Download your launcher bundle** from the [Latest Release](https://github.com/Jlorenzoo/complex-skyblock-tools/releases/latest):
   - **Prism Launcher**: `Full-Pack-Prism-1.21.11.zip`
   - **Modrinth App**: `Full-Pack-Modrinth-1.21.11.mrpack`
2. **Import into your launcher**:
   - In **Prism Launcher**: Click **Add Instance** → **Import from zip** → Select file → **OK**.
   - In **Modrinth App**: Click **Create Instance** → **Import** → Select file.
3. Click **Launch**.

---

### Option 2: Standalone Mod JAR
For existing Minecraft Fabric installations.

1. Download **`skyblock-toolkit-1.1.0+mc1.21.11.jar`** from [Releases](https://github.com/Jlorenzoo/complex-skyblock-tools/releases/latest).
2. Place the `.jar` into your `.minecraft/mods` folder.
3. Launch Minecraft with **Fabric Loader 0.18.4+** on **Java 21**.

---

### In-Game 1-Click Updates
Complexify checks for updates on startup. Click the chat alert or run **`/complexify update`** to automatically download and install updates in 1 click.

---

## Controls & Shortcuts

| Action | Shortcut / Command | Description |
| :--- | :--- | :--- |
| **Open Main Hub** | **`Right Shift`** *(or `Esc` → Complexify)* | Settings dashboard, search, and configuration |
| **HUD Customization** | **`/complexify hud`** | On-screen layout editor to move displays and toggle elements |
| **Toggle Blackout** | **`F6`** | Screen dimming for long AFK, farming, or fishing sessions |
| **Check & Install Updates** | **`/complexify update`** | One-click in-game updater |
| **Direct Shortcuts** | `/complexify farming`<br>`/complexify boosters`<br>`/complexify fishing`<br>`/complexify chat`<br>`/complexify auction` | Quick access to individual modules |

---

## Features

All overlays and telemetry widgets run natively in **Dark Mode** with crisp contrast and Lunar Client–inspired aesthetics. Every HUD element can be moved or hidden at any time via **`/complexify hud`**.

---

### Real-Time Income Tracker ($/hr)
The flagship telemetry engine of Complexify. Continuously evaluates your earnings and displays your live revenue per hour directly on your HUD as you play.

<div align="center">
  <img src="assets/screenshots/income-tracker.gif" width="720" alt="Real-Time Income Tracker" />
</div>

- **Live Rolling Income**: Calculates instant 10-second bursts and hourly averages (**`$/hr`**) in real time from server balance updates.
- **Wand & Auto-Sell Integration**: Accurately accounts for sales from Sell-All wands, chest sells, and passive income.
- **Multi-Resource Tracking**: Also tracks **Minecraft XP per hour** and **Crop harvest rates** (crops/hr and rare Gold Shards) with their own dedicated on-screen HUD counters.

---

### Booster Countdown Timers & Toast Alerts
Independent countdown clocks for server multipliers and consumable buffs.

<div align="center">
  <img src="assets/screenshots/ingame-booster-hud.png" width="720" alt="Booster Countdown Timers & Toast Banner" />
</div>

- **Dual Countdown Bars**: Green **Money (+20%)** and purple **XP (+150%)** bars display exact remaining time and active multipliers.
- **5-Second Reminder**: Subtle visual flash warns you before a booster expires so you can re-activate without losing your boost.
- **Slide-In Alerts**: Floating toast banners slide in when boosters activate or coconuts trigger.

---

### Clear Harvest Crop Visibility
Built-in resource pack that makes fully grown crops immediately distinguishable from immature ones.

<div align="center">
  <img src="assets/screenshots/ingame-clear-harvest.png" width="720" alt="Clear Harvest In-Game Crop Comparison" />
</div>

- **Zero Wasted Clicks**: Immature wheat, carrots, potatoes, nether wart, and beetroots are **90% desaturated & 25% darkened**.
- **Harvest-Ready Pop**: Fully grown crops retain full golden color, letting you spot ripe crops instantly from a distance.
- **1-Click In-Game Toggle**: Switch between Clear Harvest and vanilla textures in **Right Shift → Farming**.

---

### Fishing Signals & Audio Telemetry
Auditory depth cues, bite alerts, and live market catch valuation for Skyblock fishing.

<div align="center">
  <img src="assets/screenshots/ingame-fishing-signals.png" width="720" alt="Fishing Signals & Valuation HUD" />
</div>

- **30-Block Optimal Depth Chime**: Plays a crisp **Note Block Harp (1.6x)** chime the moment your bobber reaches max depth.
- **Custom Bite Splash Tone**: Distinct audible pebble/water-drop sound on hook.
- **Depleted Area Warning**: Descending **Note Block Bass** sequence warns when a fishing spot is exhausted.
- **Line Snap Alert**: Immediate item-break warning sound when a line snaps.
- **Catch Rate Valuation**: Real-time rolling catch revenue calculated from live server fish market prices (**`$/hr`**).

---

### Seasonal Tool History & $/hr Comparison
Automatic session ledgers that track and compare earnings per hour across your tools and seasonal gear.

<div align="center">
  <img src="assets/screenshots/seasonal-history.png" width="720" alt="Seasonal Tool History & Income Comparison" />
</div>

- **Hourly Rate Comparison**: Easily compare how much money each seasonal tool or setup makes per hour (e.g. Oni Rod vs. Void Rod).
- **Session Breakdowns**: Tracks total cash earned, duration, area (Realm vs. Overworld), and Gold Shards collected.
- **Automatic Recording**: Automatically logs sessions when holding trackable tools with no manual start/stop required.

---

### Seasonal Gear: Bone Leggings
Dedicated HUD indicator tracking status and ability cooldowns for custom server equipment.

<div align="center">
  <img src="assets/screenshots/ingame-seasonal-icons.png" width="720" alt="Bone Leggings Status & Cooldown HUD" />
</div>

- **Item & Mode Badge Variant**: Displays the leggings icon overlaid with an active status badge.
- **Speed vs. Regen Modes**: Flame badge for Speed ignition mode; heart badge for non-combat health recovery.
- **Cooldown Readiness Glint**: Dynamic glint effect shows when abilities are off cooldown and ready to trigger.

---

### Native Chat Selection & Clipboard Copy
Highlight and copy text directly from in-game chat like a desktop text editor.

<div align="center">
  <img src="assets/screenshots/ingame-chat-selection.png" width="720" alt="Native Chat Text Selection & Clipboard Copy" />
</div>

- **Drag-to-Select**: Click and drag your mouse cursor across chat lines to highlight text smoothly.
- **Word & Line Expansion**: Double-click to select a word; triple-click to select an entire line.
- **Clipboard Copy (Ctrl+C / Cmd+C)**: Copies clean text directly to your system clipboard while preserving clickable server links and `/commands`.

---

### Treasure Chest Event Countdown & Tracker
Synchronized on-screen countdown timer for server treasure chest events.

<div align="center">
  <img src="assets/screenshots/ingame-treasure-hud.png" width="720" alt="Treasure Chest Event Countdown & Tracker" />
</div>

- **Synchronized Server Countdown**: Displays the exact remaining time until the chest despawns.
- **Final 30-Second Warning**: Flashes alert prompts during the closing window before despawn.
- **Legitimate Event Tracking**: Purely reads official server broadcasts and scoreboard events (not an ESP, radar hack, or memory reader).

---

### Mining & XP Rate Benchmark Telemetry
Live telemetry benchmarks and interactive graph analysis in dark mode.

<div align="center">
  <img src="assets/screenshots/ingame-xp-rates.png" width="720" alt="XP Rate Benchmark Graph" />
</div>

- **Live Performance Curves**: Track XP gain in real-time across 1-minute, 5-minute, or continuous free-run tests.
- **Booster Isolation**: Compares raw base XP against active booster multipliers to measure true farm efficiency.

---

### Blackout Focus Mode (AFK Screen Preservation)
Screen dimming designed for long farming, fishing, and AFK sessions without display wear.

<div align="center">
  <img src="assets/screenshots/ingame-blackout.png" width="720" alt="Blackout AFK Mode" />
</div>

- **One-Touch Dimming (`F6`)**: Dims screen opacity from 0% to 100% to eliminate OLED burn-in and night-time glare during long AFK sessions, while alert audio chimes and chat overlay remain fully active with zero loss of player control.

---

## Building from Source

Requirements: **Python 3** and **Java 21** (or Java 25 for 26.2 legacy profile).

```bash
# Clone the repository
git clone https://github.com/Jlorenzoo/complex-skyblock-tools.git
cd complex-skyblock-tools

# Build the mod JAR and run regression tests
python3 build.py

# Run test suites
python3 tests/test_updater.py
python3 tests/test_installer.py
```

For advanced launcher configurations and development documentation, see [DEVELOPMENT.md](DEVELOPMENT.md).

---

## License & Community

- Distributed under the [GNU General Public License v3.0](LICENSE).
- Built with ❤️ for the **Complex Gaming** community.
