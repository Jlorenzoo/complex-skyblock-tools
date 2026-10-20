<div align="center">

![Skyblock Toolkit Banner](assets/banner.png)

<img src="assets/logo.png" width="110" alt="Skyblock Toolkit Logo" />

# Skyblock Toolkit

**The Essential Quality-of-Life HUD & Stats Suite for Complex Gaming Skyblock**

[![Minecraft 26.2](https://img.shields.io/badge/Minecraft-26.2-ED1C24?style=for-the-badge&logo=minecraft&logoColor=white)](#)
[![Fabric Loader](https://img.shields.io/badge/Fabric-0.19.5+-white?style=for-the-badge&logo=fabric&logoColor=black)](#)
[![Java 25](https://img.shields.io/badge/Java-25-ED1C24?style=for-the-badge&logo=openjdk&logoColor=white)](#)
[![License: GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-white?style=for-the-badge)](#)
[![Server: Complex Gaming](https://img.shields.io/badge/Server-Complex_Gaming-ED1C24?style=for-the-badge)](#)

</div>

---

## Quick Installation

### Method 1: Fabric Mod (Recommended)
Works seamlessly with your existing Fabric profile alongside **Litematica**, **Sodium**, **Iris**, and other client mods.

1. Ensure you have **Fabric Loader (>= 0.19.0)** and **Fabric API** installed for **Minecraft 26.2**.
2. Download **[Skyblock Toolkit (Latest Release)](https://github.com/Jlorenzoo/complex-skyblock-tools/releases/latest)**.
3. Place the `.jar` directly into your `.minecraft/mods/` folder.
4. Launch Minecraft and press **`Right Shift`** (or type `/toolkit`) to open the settings hub!

### Method 2: Prism Launcher (Pre-configured Pack)
For players who prefer an all-in-one standalone instance with texture packs and configs ready out of the box:

1. Download **[Complex-Skyblock-Prism (Latest Release)](https://github.com/Jlorenzoo/complex-skyblock-tools/releases/latest)**.
2. In Prism Launcher: **Add Instance** → **Import** → select the zip file.
3. Click Launch (Prism handles the Fabric Loader and Java setup automatically).

### ⚡ Automatic Updates
Once installed, you never need to visit GitHub or drag files again:
- The mod automatically checks for updates in the background on startup.
- When an update is ready, an alert appears in chat: `[Skyblock Toolkit] A new update is available! [Click to Update]`
- Click the message or type `/toolkit update` to auto-download and install with one click.

---

## Controls & Shortcuts

| Action | Shortcut / Command |
| :--- | :--- |
| **Open Toolkit Hub** | **`Right Shift`** (or `Escape` → *Skyblock Toolkit*) |
| **Toggle Blackout** | **`F6`** *(customizable in settings)* |
| **Main Menu** | `/toolkit` |
| **Drag & Drop HUD Canvas** | `/toolkit hud` |
| **Check & Install Updates** | `/toolkit update` |
| **Module Shortcuts** | `/toolkit farming`, `/toolkit boosters`, `/toolkit fishing`, `/toolkit chat`, `/toolkit auction`, `/toolkit blackout` |

---

## Features

### 🌾 Farming Hub & Clear Harvest Textures
- **Built-in Crop Visibility**: Immature wheat, carrots, potatoes, nether wart, and beetroots are desaturated and dimmed so fully grown crops are instantly distinguishable.
- **Zero-Setup & Auto-Updating**: Included directly inside the mod JAR as a built-in Fabric resource pack. Updates automatically whenever the mod updates.
- **Quick In-Game Toggle**: Switch between Clear Harvest and vanilla textures with one click under the **Farming** tab (`Right Shift` or `/toolkit farming`).
- **Live Crop Earnings & HUD**: Monitor live rolling harvest rates (`$/hr`), toggle the Crop Earnings HUD overlay, and access rate tests directly.

### ⏱️ Booster & Crop Earnings HUD
- **Consumable Tracker**: Accurately tracks active coconut and booster item durations with countdown bars.
- **Rolling Earnings**: Live rolling window (10-second / hourly) calculation of crop farming revenue (`$/hr`).
- **Session History**: Detailed ledger of your past boosts, earnings, and rate tests.

### 🎣 Fishing Signals & Value Estimates
- **Audio Depth Cue**: Plays a distinct audio chime when your bobber reaches optimal depth (30 blocks).
- **Depleted Area & Snap Alerts**: Warns you with descending audio tones when a fishing spot is depleted or your line snaps.
- **Estimated Hourly Catch Value**: Calculates rolling 60s / 5m catch rates and projected `$/hr` based on your server fish prices.

### 💬 Chat Text Selection & Copy
- **Drag-to-Select**: Click and drag your mouse across in-game chat text to highlight text seamlessly.
- **Multi-Click Selection**: Double-click to select a word; triple-click to select an entire message line.
- **Cross-Platform Copy**: Press **Ctrl + C** (or **Cmd + C** on macOS) to copy text directly to your clipboard. Clickable links and commands remain fully functional.

### 🔔 Auction Alerts & Blackout Mode
- **Auction Sale Chime**: Immediate custom audio notification when an item sells on the Auction House.
- **Blackout Focus**: Dim the screen opacity (0–100%) during long farming or fishing sessions while keeping full controls and game audio active.

---

## Building from Source

Requirements: Python 3 and Java 25.

```bash
# Clone the repository
git clone https://github.com/Jlorenzoo/complex-skyblock-tools.git
cd complex-skyblock-tools

# Build the mod jar
python3 build.py

# Run test suite
python3 tests/test_updater.py
python3 tests/test_installer.py
```

Produces `build/skyblock-toolkit-1.0.0.jar`.

---

## License & Credits

- Distributed under the [MIT License](LICENSE).
- Designed with ❤️ for the **Complex Gaming** community.
