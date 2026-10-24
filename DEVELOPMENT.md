# Developing Skyblock Toolkit

This is the source repository for the Minecraft 1.21.11 Fabric client mod, `skyblocktoolkit`. Open the repository folder as the coding project's root so its source, Git branch and `AGENTS.md` are available together.

The timer model lives in `BoosterTimers.java`. Money and XP have separate clocks and sidebar parsing. A parsed activation's exact percentage and duration stay authoritative until a new activation or disconnect; stale sidebar percentages, minute buckets and persistent labels cannot extend it. Known timers become inactive at their deadline, which also stops money bonus attribution and finishes history. Joining mid-effect still uses sidebar estimates or, without a duration, the sidebar's presence and removal grace. The legacy `BoosterManager` accounting methods refer to money only; XP activations must never replace the money ledger or count as a money boost. Hidden HUDs continue tracking. Visible active timers pack into money-then-XP rows. The five-second reminder is optional and disabled by default.

**Boosters** includes a coconut money sound (enabled by default) and a minimum booster percentage (default 0, meaning every money boost). The threshold is inclusive: 50 alerts for +50% and above. It requires a personal money activation correlated with a recently used coconut; XP, server boosters, other items and sidebar polling do not trigger it. Sound preview respects Minecraft's UI/master volume. **Sell all key** is available under Boosters and Farming, or Minecraft Controls → Skyblock Toolkit → Sell All (/sellall). It starts unbound, persists in Minecraft's `options.txt`, and sends one command for a consumed press during gameplay. Open screens and disconnected clients discard presses. Escape cancels toolkit key capture; Delete/Backspace unbinds it.

Hourly rate tests collect both money and Minecraft XP during the same 1-minute, 5-minute or free-run sample. Open **Boosters → Rate tests**, select **Minecraft XP**, and use **Include XP boosters** to switch between actual gains and estimated base XP. XP uses the vanilla level/progress bar converted to points across level-ups; initial values, losses, death and reconnects establish new baselines rather than income. Base XP divides each gain by the XP booster multiplier active when observed. It removes the detected XP booster only. Spending and withdrawing XP are reflected as bar changes; XP gained from a withdrawal or command cannot be distinguished from earned XP. The optional 10-second XP/hour HUD and its booster preference are available under Farming, with independent visibility and placement in Edit HUD. Money accounting and its shop reconciliation remain independent.

The settings hub groups each activity into named sections, with General collecting keyboard shortcuts and HUD controls. Search accepts multiple words (for example `coconut minimum` or `xp hud`) and opens the matching section. Ctrl+F / Cmd+F focuses search; Escape clears it before closing the menu. Search destinations in `ToolkitSearch` refer to the actual section headings rather than pixel positions; keep the catalog in sync when adding or renaming sections. The offline menu check resolves every destination, exercises search clearing, verifies slider hit geometry and checks visible controls for overlap in full and compact layouts.

`ToolkitPalette` defines independent light/red and dark/red menu palettes. `ToolkitTheme.palette()` selects the saved preference; the menu header switches it immediately. Detail screens and controls use the same palette. Gameplay HUD colors remain independent of the menu theme.

## Build and verify

**Minecraft 1.21.11 / Java 21 is the maintained pack target.** Run
`python build.py` to compile/remap the shared features and run all Java regression
suites through the Gradle wrapper in [`versions/1.21.11`](versions/1.21.11/README.md).
Output: `versions/1.21.11/build/libs/skyblock-toolkit-1.1.0+mc1.21.11.jar`.
Use a JDK 21 or newer (`JAVA_HOME`, or the configured `javac` JDK).
`python scripts/install.py --wait` installs that target by default into the
configured profile. Installer checks reject jars for a different known launcher
Minecraft version. Routine changes keep version 1.1.0.

The feature sources/assets remain shared in `src/main`. The Gradle target adapts
26.2 API names and remaps the production 1.21.11 jar. Future feature work must
build and exercise the 1.21.11 pack. The updater selects release assets for the
running Minecraft version and checks jar metadata before replacement. The
`Fabric 1.21.11` workflow builds the jar, runs regressions and installer/migration
checks, and publishes a checksummed build artifact on relevant pushes.

The historical Java 25 / 26.2 build is explicit: `python build.py --minecraft 26.2`.
It requires the original 26.2 compiler/dependency paths. Its output remains
`build/skyblock-toolkit-1.1.0.jar`; never put it into a 1.21.11 profile.
Linux Prism defaults and `PRISM_DATA`, `MC_JAR`, `FAPI_JAR`, `LOADER_JAR`, `JAVAC`,
and `LIBRARIES_DIR` overrides remain for this legacy builder.

Workstation paths belong in ignored `local-settings.json`: `mods_dir` selects the
live profile, and `minecraft_version` defaults to `1.21.11`. Legacy build paths
(`minecraft_jar`, `fabric_api_jar`, `loader_jar`, `javac`, `libraries_dir`, optional
`library_jars`) can remain configured independently of the live install target.
Never commit launcher tokens, personal settings or private history.

For the maintained runtime, build the offline smoke mod and run:

```powershell
cd versions/1.21.11
.\gradlew.bat build remapMenuSmoke
cd ../..
python tests/run_1_21_11_windows.py
```

`--profile PATH` copies a complete pack's mods, configs, crop packs and options
into a disposable offline client. It verifies loaded keybindings before exercising
the full/compact light/dark menus and writes the loaded options for comparison.
It never joins a server or modifies the source profile.

An optional `startupServer` address in `config/skyblocktoolkit.json` makes a
profile join that server once after the title screen finishes loading. It defaults
to an empty string. Existing connections, vanilla quick-play and disconnect screens
cancel the startup attempt; returning to the menu never reconnects automatically.
This does not depend on CurseForge forwarding custom game arguments. The configured
Complex workstation profile uses `skyblock.mc-complex.com`. Settings saves retain
the address. The offline runtime's `--startup` check intercepts the real vanilla
connection entry point and verifies host/port and one attempt, without network
access to a game server. Copied profiles with startup enabled are intercepted too.

Run `python tests/test_profile_migration.py`, `python tests/test_installer.py`,
`python tests/test_voicechat.py` and `python tests/test_updater.py`. The Gradle build
runs the Java suites; `python tests/run_java.py` remains a legacy 26.2 check. The Java runner excludes the private transaction replay dataset. It covers timer isolation, reminder boundaries, sidebar formats, ledger/history behavior and readable palette contrast.

On a configured Windows workstation, `python tests/run_smoke_windows.py` checks real Fabric mixins and settings migration in a disposable profile. `python tests/run_menu_windows.py` launches an isolated offline client, exercises widgets and captures light/dark menus under `artifacts/menu-preview/`. It never signs in or joins a server. These checks require the installed launcher dependencies; the menu check also requires a graphical desktop. The existing Prism/Linux smoke scripts remain available.

## Install

Run `python scripts/install.py --wait` to wait for clients using the configured profile to close. `--mods PATH` and `--source PATH` override the target and built jar. Check `toolkit-installation-status.txt` before relaunching.

The installer verifies backups and the installed checksum, preserves unrelated mods and settings/history, and rolls back failed replacement. Keep a rollback copy outside `mods` as the in-game updater cleans old `.bak` files. Do not force-close a player's running client to install an update. Never commit launcher tokens, workstation paths, logs, compiled jars, or private game history.

## Voice chat pack dependency

Complex uses Simple Voice Chat, independently of Lunar. `modpack/voicechat.json` pins
the stable Fabric 1.21.11-2.6.22 release (Minecraft 1.21.11, Java 21, Loader
>=0.18.1). The complete pack pins Fabric API, Litematica, MaLiLib, ChatShot, YACL
and voice chat with upstream SHA-512 hashes in `modpack/fabric-1.21.11.json`.
Optional audio mods are not required. The old 26.2 voice dependency is retained
in `modpack/voicechat-26.2.json` for the historical Prism export.

`python scripts/voicechat.py` downloads to `build/dependencies`, verifies the upstream
SHA-512 and Fabric metadata, and changes no live files. `python scripts/voicechat.py
--install --wait` installs into the configured profile after Minecraft exits. It shares
the toolkit install lock, preserves configurations, replaces only jars with mod ID
`voicechat`, verifies backups outside `mods`, and rolls back failed replacement.
Check `voicechat-installation-status.txt` before relaunching. The installer seeds
only missing voice controls in `options.txt`: V menu, Caps Lock push-to-talk,
Right Alt whisper, F8 mute, N disable, H hide icons; groups/recording stay unbound.
Existing voice bindings are preserved and changed options are backed up outside mods.
The toolkit updater does not update this third-party dependency. `scripts/package_prism.py` includes it
as a separate jar, with attribution and without personal microphone settings.

Run `python tests/test_voicechat.py` for checksum, preservation and rollback tests.
On Windows, `python tests/run_menu_windows.py --voicechat` additionally checks real
Fabric initialization, voice key registration and onboarding rendering in a disposable
offline profile. It does not test server UDP connectivity or your microphone.

## Preserving player data during a version migration

Create a separate profile and keep the original as a fallback. Wait for the source
client to exit before the final snapshot so its last options/history saves are
included. `scripts/profile_migration.py` copies and SHA-256 verifies player data,
including options, mod configuration, voice device settings/volumes, server list,
schematics, screenshots and worlds. It rejects overlapping/nonempty destinations
and linked paths. It changes the options data version to 4671, the known
Ready Crops pack's format to 75, and absolute configuration paths to the copied
profile (including schematic placements); all keybindings and crop texture/model bytes stay
unchanged. Launcher metadata, runtime caches and old mod binaries are separate.
Copied worlds are backups: do not open newer-version worlds in the older client.
Keep personal migration manifests and snapshots local.
