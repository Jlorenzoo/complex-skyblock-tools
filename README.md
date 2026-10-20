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

## Features

### ⏱️ Booster & Crop Earnings HUD
- **Consumable Tracker**: Accurately tracks active coconut and booster item durations with countdown bars.
- **Rolling Earnings**: Live rolling window (10-second / hourly) calculation of crop farming revenue (`$/hr`).
- **Session History**: Detailed ledger of your past boosts, earnings, and rate tests.

### 🎣 Fishing Signals & Value Estimates
- **Audio Depth Cue**: Plays a distinct audio chime when your bobber reaches optimal depth (30 blocks).
- **Depleted Area & Snap Alerts**: Warns you with descending audio tones when a fishing spot is depleted or your line snaps.
- **Estimated Hourly Catch Value**: Calculates rolling 60s / 5m catch rates and projected `$/hr` based on your server fish prices.

### 💬 Chat Text Selection & Copy
- **Drag-to-Select**: Click and drag your mouse across in-game chat text to highlight lines or words.
- **Clipboard Copy**: Press **Ctrl + C** to copy text directly to your OS clipboard. Clickable links remain functional.

### 🔔 Auction Alerts & Blackout Mode
- **Auction Sale Chime**: Immediate custom audio notification when an item sells on the Auction House.
- **Blackout Focus**: Dim the screen opacity (0–100%) during long farming or fishing sessions while keeping full controls and game audio active.

### ⚡ Built-in 1-Click Auto-Updater
- **In-Game Alerts**: Receive a chat notification whenever a new update is released.
- **1-Click Download**: Click `[Click to Update]` or run `/toolkit update` to automatically download the new jar to your mods folder. No manual browser downloads or file dragging required!

---

## Controls & Shortcuts

| Action | Shortcut / Command |
| :--- | :--- |
| **Open Toolkit Hub** | **`Right Shift`** (or `Escape` → *Skyblock Toolkit*) |
| **Toggle Blackout** | **`F6`** *(customizable in settings)* |
| **Main Menu** | `/toolkit` |
| **Drag & Drop HUD Canvas** | `/toolkit hud` |
| **Check & Install Updates** | `/toolkit update` |
| **Module Shortcuts** | `/toolkit boosters`, `/toolkit fishing`, `/toolkit chat`, `/toolkit auction`, `/toolkit blackout` |

---

## Installation

### Option A: Prism Launcher (Recommended 1-Click Pack)
1. Download **`Complex-Skyblock-Prism-1.0.0.zip`** from [Releases](https://github.com/Jlorenzoo/complex-skyblock-tools/releases).
2. Open Prism Launcher → **Add Instance** → **Import** → select the downloaded zip.
3. Launch and play! (Downloads Minecraft 26.2, Fabric Loader, and dependencies automatically).

### Option B: Standalone Fabric Mod
1. Install **Fabric Loader (>= 0.19.0)** for **Minecraft 26.2** with **Fabric API**.
2. Download **`skyblock-toolkit-1.0.0.jar`** from [Releases](https://github.com/Jlorenzoo/complex-skyblock-tools/releases).
3. Place the `.jar` inside your `.minecraft/mods/` directory.
4. Launch Minecraft and press **`Right Shift`** to configure.

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

- Distributed under the [GNU General Public License v3.0](LICENSE).
- Chat Copy derived from `chattextselect` 1.3.0 by **xpncvr** (GPL-3.0). See [THIRD-PARTY.md](THIRD-PARTY.md).
- Designed with ❤️ for the **Complex Gaming** community.
