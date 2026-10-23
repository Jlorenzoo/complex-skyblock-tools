# Skyblock Toolkit for Fabric 1.21.11

[Download the 1.21.11 build](https://github.com/Jlorenzoo/complex-skyblock-tools/releases/tag/v1.1.0-mc1.21.11).

## Install

1. Create or select a **Minecraft Java 1.21.11** profile using **Fabric Loader 0.18.4 or newer** and **Java 21**.
2. Install **Fabric API for 1.21.11** (tested with `0.141.6+1.21.11`).
3. Close that Minecraft instance. Download `skyblock-toolkit-1.1.0+mc1.21.11.jar` and put it in that profile's `mods` folder. Remove any other Skyblock Toolkit jar from that folder first.
4. Launch and press **Right Shift**, or run **`/toolkit`**.

The jar includes the current toolkit features: money/XP booster timers, earnings
and rate tests, fishing alerts, chat selection/copy, Clear Harvest, auction/coconut
sounds, sell-all binding, blackout and the light/dark configuration hub.

This download is for **Fabric 1.21.11**. The existing 26.2 jar and Prism pack are
separate downloads. Install other mods, including Simple Voice Chat if desired,
using their own **1.21.11 Fabric** builds. This jar does not bundle Fabric API,
voice chat, Minecraft, launcher accounts or personal settings.

Updates are selected for the running Minecraft version. The mod checks the
downloaded jar's Minecraft requirement before replacing the installed toolkit.

## Build

From a checkout of the full repository, use a JDK 21 or newer:

```powershell
cd versions/1.21.11
.\gradlew.bat build
```

On Linux/macOS: `bash gradlew build` from the same directory.

Output: `build/libs/skyblock-toolkit-1.1.0+mc1.21.11.jar`.
Use the remapped jar from `build/libs`, not a development jar from `build/devlibs`.
Gradle downloads the pinned Minecraft/Fabric dependencies and runs the shared
Java regression suites. GitHub Actions also builds and uploads the jar plus a
SHA-256 checksum after relevant changes to `main`.

The feature sources and assets remain in the root `src/main` tree. The Gradle
build generates 1.21.11 sources using the explicit API substitutions in
`build.gradle`; `ClientMessages.java` adapts chat/action-bar feedback. Mixin
accessors keep boss-bar access valid after intermediary remapping.

## Offline runtime verification

On a configured Windows workstation with vanilla 1.21.11 and its assets installed:

```powershell
.\gradlew.bat build remapMenuSmoke
cd ../..
python tests/run_1_21_11_windows.py
```

`MINECRAFT_INSTALL` and `JAVA_21` can override the launcher installation folder
and Java 21 executable. The check launches a disposable offline production client,
exercises the real widgets and captures full/compact menus in both themes under
`artifacts/fabric-1.21.11`. It never joins a server or changes a live modpack.
