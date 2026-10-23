# Developing Skyblock Toolkit

This is the source repository for the Minecraft 26.2 Fabric client mod, `skyblocktoolkit`. Open the repository folder as the coding project's root so its source, Git branch and `AGENTS.md` are available together.

The timer model lives in `BoosterTimers.java`. Money and XP have separate clocks and sidebar parsing. A parsed activation's exact percentage and duration stay authoritative until a new activation or disconnect; stale sidebar percentages, minute buckets and persistent labels cannot extend it. Known timers become inactive at their deadline, which also stops money bonus attribution and finishes history. Joining mid-effect still uses sidebar estimates or, without a duration, the sidebar's presence and removal grace. The legacy `BoosterManager` accounting methods refer to money only; XP activations must never replace the money ledger or count as a money boost. Hidden HUDs continue tracking. Visible active timers pack into money-then-XP rows. The five-second reminder is optional and disabled by default.

**Boosters** includes a coconut money sound (enabled by default) and a minimum booster percentage (default 0, meaning every money boost). The threshold is inclusive: 50 alerts for +50% and above. It requires a personal money activation correlated with a recently used coconut; XP, server boosters, other items and sidebar polling do not trigger it. Sound preview respects Minecraft's UI/master volume. **Sell all key** is available under Boosters and Farming, or Minecraft Controls → Skyblock Toolkit → Sell All (/sellall). It starts unbound, persists in Minecraft's `options.txt`, and sends one command for a consumed press during gameplay. Open screens and disconnected clients discard presses. Escape cancels toolkit key capture; Delete/Backspace unbinds it.

Hourly rate tests collect both money and Minecraft XP during the same 1-minute, 5-minute or free-run sample. Open **Boosters → Rate tests**, select **Minecraft XP**, and use **Include XP boosters** to switch between actual gains and estimated base XP. XP uses the vanilla level/progress bar converted to points across level-ups; initial values, losses, death and reconnects establish new baselines rather than income. Base XP divides each gain by the XP booster multiplier active when observed. It removes the detected XP booster only. Spending and withdrawing XP are reflected as bar changes; XP gained from a withdrawal or command cannot be distinguished from earned XP. The optional 10-second XP/hour HUD and its booster preference are available under Farming, with independent visibility and placement in Edit HUD. Money accounting and its shop reconciliation remain independent.

The settings hub groups each activity into named sections, with General collecting keyboard shortcuts and HUD controls. Search accepts multiple words (for example `coconut minimum` or `xp hud`) and opens the matching section. Ctrl+F / Cmd+F focuses search; Escape clears it before closing the menu. Search destinations in `ToolkitSearch` refer to the actual section headings rather than pixel positions; keep the catalog in sync when adding or renaming sections. The offline menu check resolves every destination, exercises search clearing, verifies slider hit geometry and checks visible controls for overlap in full and compact layouts.

`ToolkitPalette` defines independent light/red and dark/red menu palettes. `ToolkitTheme.palette()` selects the saved preference; the menu header switches it immediately. Detail screens and controls use the same palette. Gameplay HUD colors remain independent of the menu theme.

## Build and verify

The additional Fabric **1.21.11 / Java 21** target lives in
[`versions/1.21.11`](versions/1.21.11/README.md). Run its Gradle wrapper with
`build` to compile/remap the shared features and run the Java regression suites.
Its jar has a `+mc1.21.11` filename suffix; the root Python builder continues to
produce the 26.2 jar. Never install the backport into the configured 26.2 profile.
The updater selects release assets by Minecraft version and checks the downloaded
jar metadata before installation. The `Fabric 1.21.11` GitHub workflow publishes
build artifacts and checksums on relevant pushes and pull requests.

Use Python 3 and a Java 25 JDK. `python build.py` creates `build/skyblock-toolkit-1.1.0.jar` and never installs it. Linux Prism defaults and `PRISM_DATA`, `MC_JAR`, `FAPI_JAR`, `LOADER_JAR`, `JAVAC`, and `LIBRARIES_DIR` overrides are supported. For another launcher, configure ignored `local-settings.json` with absolute paths:

- `minecraft_jar`, `fabric_api_jar`, `loader_jar`, `javac`, `libraries_dir`, `mods_dir`
- Optional `library_jars`: the exact dependency paths for the target launcher profile.

Run `python tests/run_java.py`, `python tests/test_installer.py` and `python tests/test_updater.py`. The Java runner excludes the private transaction replay dataset. It covers timer isolation, reminder boundaries, sidebar formats, ledger/history behavior and readable palette contrast.

On a configured Windows workstation, `python tests/run_smoke_windows.py` checks real Fabric mixins and settings migration in a disposable profile. `python tests/run_menu_windows.py` launches an isolated offline client, exercises widgets and captures light/dark menus under `artifacts/menu-preview/`. It never signs in or joins a server. These checks require the installed launcher dependencies; the menu check also requires a graphical desktop. The existing Prism/Linux smoke scripts remain available.

## Install

Run `python scripts/install.py --wait` to wait for clients using the configured profile to close. `--mods PATH` and `--source PATH` override the target and built jar. Check `toolkit-installation-status.txt` before relaunching.

The installer verifies backups and the installed checksum, preserves unrelated mods and settings/history, and rolls back failed replacement. Keep a rollback copy outside `mods` as the in-game updater cleans old `.bak` files. Do not force-close a player's running client to install an update. Never commit launcher tokens, workstation paths, logs, compiled jars, or private game history.

## Voice chat pack dependency

Complex uses Simple Voice Chat, independently of Lunar. `modpack/voicechat.json` pins
the stable Fabric 2.6.22+26.2 release (Minecraft 26.2, Java 25, Loader >=0.19.3;
Fabric API, if installed, must be >=0.152.1+26.2). Optional audio mods are not required.

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
