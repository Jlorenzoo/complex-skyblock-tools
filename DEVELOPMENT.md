# Developing Skyblock Toolkit

This is the source repository for the Minecraft 1.21.11 Fabric client mod, `skyblocktoolkit`. Open the repository folder as the coding project's root so its source, Git branch and `AGENTS.md` are available together.

The timer model lives in `BoosterTimers.java`. Money and XP have separate clocks and sidebar parsing. A parsed activation's exact percentage and duration stay authoritative until a new activation or disconnect; stale sidebar percentages, minute buckets and persistent labels cannot extend it. Known timers become inactive at their deadline, which also stops money bonus attribution and finishes history. Joining mid-effect still uses sidebar estimates or, without a duration, the sidebar's presence and removal grace. The legacy `BoosterManager` accounting methods refer to money only; XP activations must never replace the money ledger or count as a money boost. Hidden HUDs continue tracking. Visible active timers pack into money-then-XP rows. The five-second reminder is optional and disabled by default.

**Boosters** includes a coconut money sound (enabled by default) and a minimum booster percentage (default 0, meaning every money boost). The threshold is inclusive: 50 alerts for +50% and above. It requires a personal money activation correlated with a recently used coconut; XP, server boosters, other items and sidebar polling do not trigger it. Sound preview respects Minecraft's UI/master volume. **Sell all key** is available under General → Keyboard shortcuts, or Minecraft Controls → Skyblock Toolkit → Sell All (/sellall). It starts unbound, persists in Minecraft's `options.txt`, and sends one command for a consumed press during gameplay. Open screens and disconnected clients discard presses. Escape cancels toolkit key capture; Delete/Backspace unbinds it.

Hourly rate tests collect both money and Minecraft XP during the same 1-minute, 5-minute or free-run sample. Open **Boosters → Rate tests**, select **Minecraft XP**, and use **Include XP boosters** to switch between actual gains and estimated base XP. XP uses the vanilla level/progress bar converted to points across level-ups; initial values, losses, death and reconnects establish new baselines rather than income. Base XP divides each gain by the XP booster multiplier active when observed. It removes the detected XP booster only. Spending and withdrawing XP are reflected as bar changes; XP gained from a withdrawal or command cannot be distinguished from earned XP. The optional 10-second XP/hour HUD and its booster preference are available under Mining, with independent visibility and placement in Edit HUD. Money accounting and its shop reconciliation remain independent.

Bone Leggings shows one server-item sprite with one mode badge: Minecraft's original Speed or Regeneration effect sprite, overlapping the bottom-right corner of the leggings. Badges render the native effect textures at 16×16 with their original colors and respect resource-pack overrides. A small rounded slate backing and a thin cyan (Speed) or rose (Regen) rim keep the mode visible against dark or busy scenery; the rest of the HUD remains transparent by default. The item has two mutually exclusive modes: detecting Speed VI selects speed, otherwise the visible item's mode is regeneration. There are no idle or combined states, and no duplicate leggings icons. The optional glass background remains off by default. Alternate display styles provide the mode name or just the mode badge. HUD editor bounds reserve the same space for both modes. The complete named server item stack is captured even while the HUD is hidden, retaining model components, leather dye and enchantment glint; the Seasonal card shares that same stack. Gray chainmail is only the fallback before a matching item has been observed in the current client session.

Farming keeps crop visibility and its HUD toggle together. Utilities → Keyboard shortcuts owns the single sell-all shortcut selector. Mining owns Minecraft XP settings. Seasonal owns seasonal items: Void Rod (ready-to-reel and line-snap cues with target depth controls) and Bone Leggings. Occasional settings use named details groups within their topic. The XP HUD appears only after a measured gain and hides after five seconds without another gain. Snapped-line and ready-to-reel depth cues require a held fishing rod named Void Rod.

Farming → **Include gold shards in income** is off by default. Set **Gold shard value ($ each)** (accepts `125k`, `2m`) to add estimated item value to the crop HUD and Money rate tests. The Farming section shows the current connection's shard count and a rolling shard/hour rate; each rate test counts its own shards, so matching tests in normal farming and realms can be compared. Holy Cultivation and Withering Plague reward messages are parsed from observed server formats. Silent drops are detected from increases in the player's own inventory while holding a hoe and harvesting (sidebar activity or attack held), with menus excluded. Chat and inventory observations are reconciled within three seconds. Existing inventory, spending, chest transfers, fishing/mining reward messages and treasure broadcasts are not income. Picked-up shards during active farming can be included; inventory changes do not identify their source. Shard estimates never enter cash balances, booster bonus/history or XP; the cash booster preference does not multiply shard value. Tracking continues with inclusion off, and toggling/repricing recomputes the estimate. Connection changes clear inventory baselines and the rolling shard sample.

The settings hub opens Boosters, Skills, HUD & Overlays and Utilities as short topic lists. Selecting a topic shows only that group of controls. A fixed breadcrumb keeps the category and topic visible while the settings scroll; clicking it, pressing Escape or clicking the active category returns to the topic list. Back restores focus to the originating topic row. Switching categories remembers each selected topic for the lifetime of the menu; theme changes and resizing retain it too. Keyboard shortcuts comes first in Utilities. Seasonals uses item cards to open one focused item page at a time. Search accepts multiple words (for example `coconut minimum` or `xp hud`) and returns individual settings across every category. Results show the setting name with its category/topic path. Selecting a result opens its topic, expands the owning details group when needed, and reveals its control. Topic names also match search. Ctrl+F / Cmd+F focuses search; Escape clears it before closing the menu. Search destinations in `ToolkitSearch` refer to actual control labels and section headings rather than pixel positions; keep the catalog in sync when adding or renaming controls. Named details groups keep alert appearance/previews, gold shard valuation, chat appearance, extra volume boost, other HUD displays, treasure appearance/timing and Bone Leggings appearance out of the everyday control list. These view choices survive category/theme/size changes within the menu session. Shortcuts have one editor per key in Utilities; the crop booster preference lives in Farming. Fishing links directly to fish prices and Void Rod settings, while Farming and Mining link to Money/XP rate tests respectively. The offline menu check resolves every destination, exercises search clearing, verifies slider hit geometry and checks visible controls for overlap in full and compact layouts.

**HUD & Overlays → Treasurechest timer** is an opt-in, passive treasure module. Open the server's treasure menu once per connection to read the **Next Chest Spawn** clock's lore. The received countdown is authoritative; subsequent two-hour cycles show `~` and `estimated` until refreshed. A “was discovered” broadcast identifies a chest, not the global spawn time, so it never resets the clock. Disconnecting clears the clock and observations. Tracking continues while the HUD is hidden and never sends commands or clicks.

Discovered chests appear by rarity and Tomb/Realm location, with a two-open capacity. Details disappear 120 seconds after each discovery by default (adjustable 30–300 seconds); reward messages do not extend that deadline. **Reward pool estimates** uses the observed inclusive shard pools: Rare 64–192, Epic 96–256, Legendary 128–320, Mythic 192–384. Overlapping rewards and shared item rewards stay unassigned. `0–1/2 observed` means zero confirmed opens and at most one possible observed open for that chest; counts do not claim that unseen opens or unseen chests are known. Only a message explicitly identifying a location and an unambiguous rarity confirms an individual open. The module also has independent discovery details, background, 60–150% size, and draggable HUD placement with an eye control. Offline tests exercise real clock item components, frozen lore, persistence, dragging, both themes and compact layouts; they do not verify a live server event.

`ToolkitPalette` defines independent light/red and dark/red menu palettes. `ToolkitTheme.palette()` selects the saved preference; the menu header switches it immediately. Detail screens and controls use the same palette. Gameplay HUD colors remain independent of the menu theme.

Blackout supports a tap to toggle and hold + mouse wheel to adjust opacity during gameplay (up darkens, down clears, 5% per notch). Scrolling activates the overlay without toggling it off, consumes the hotbar scroll, and saves opacity when the key is released. Open menus cancel pending taps.

## Build and verify

**Minecraft 1.21.11 / Java 21 is the maintained pack target.** Run
`python build.py` to compile/remap the shared features and run all Java regression
suites through the Gradle wrapper in [`versions/1.21.11`](versions/1.21.11/README.md).
Output: `versions/1.21.11/build/libs/skyblock-toolkit-<patch>+mc1.21.11.jar`.
Use a JDK 21 or newer (`JAVA_HOME`, or the configured `javac` JDK).
`python scripts/install.py --wait` installs that target by default into the
configured profile. Installer checks reject jars for a different known launcher
Minecraft version. Every commit receives a numeric patch version (`1.1.<full Git commit count>`).
`python scripts/build_version.py` prints the exact version. Commit before the final
build/install; the same source commit produces the same version on every machine.
Shallow clones are rejected; CI checks out full history. Keep the major/minor line
in `src/main/resources/fabric.mod.json`; change that line for an intentional larger update.
The patch component in that source template is replaced when building.

The feature sources/assets remain shared in `src/main`. The Gradle target adapts
26.2 API names and remaps the production 1.21.11 jar. Future feature work must
build and exercise the 1.21.11 pack. The updater selects release assets for the
running Minecraft version and checks jar metadata before replacement. The
`Fabric 1.21.11` workflow builds the jar, runs regressions and installer/migration
checks, and publishes a checksummed public GitHub Release on relevant main pushes.
The 26.2 workflow publishes its own compatible patch too. Releases stay drafts
until the jar and checksum are uploaded. Existing installed updaters already
understand these numeric versions and Minecraft-specific asset names. Only the
26.2 patch is marked GitHub Latest, preserving the original 26.2 updater that
used `/releases/latest` without a Minecraft compatibility filter; 1.21.11
clients select their own asset from the release list.
After pushing, wait for both build/publication jobs and verify their public assets;
a green local build alone does not deliver an update to another PC.
The updater checks on startup, every 15 minutes while running, and retries failed
checks after one minute. It notifies in chat after entering a world. `/toolkit update`
refreshes a previously up-to-date result; available updates still require clicking
to download and a restart to activate.

The additional Java 25 / 26.2 build is explicit: `python build.py --minecraft 26.2`.
It downloads public dependencies into ignored `.local/fabric-26.2`, verifies their
upstream checksums, compiles the shared sources and runs the Java regressions.
Set `JAVA_HOME` or `JAVAC` to JDK 25; configured launcher files are reused only
when their bytes match the pinned upstream dependencies. No installed pack is
needed. Its output remains `build/skyblock-toolkit-<patch>.jar`; never put it into
a 1.21.11 profile. `python scripts/package_26_2.py` creates the clearly named
`skyblock-toolkit-<patch>+mc26.2.jar`, a complete Prism ZIP, a Modrinth `.mrpack`,
installation instructions and SHA-256 checksums under `dist/fabric-26.2`.
The `Fabric 26.2` GitHub workflow runs these steps on every relevant main push
and uploads downloadable artifacts. Successful main builds also publish the compatible jar as an in-game patch release.

The full 26.2 dependency set is pinned in `modpack/fabric-26.2.json`. Pack exports
use fresh defaults and the built-in crop pack, without reading any live player
profile. Run `python tests/test_pack_26_2.py` for pack isolation/integrity checks.
The historical `python tests/run_menu_windows.py --pack` graphical check is
disabled on Windows until a private display backend is available. Linux graphical
checks exercise the full/compact light/dark menus on Xvfb. `python tests/run_smoke_windows.py` verifies
26.2 Fabric mixins and native audio without opening a game window (also used on
Linux CI). The active 1.21.11 profile is unchanged by these 26.2 checks.

The 26.2 jar requires Fabric Loader >=0.19.5. Older loaders bundle MixinExtras
that cannot read the array-form `@Redirect.at` annotations emitted by the newer
Mixin compiler dependency, causing an `ArrayList` / `AnnotationNode` cast failure
in `SoundEngine`. Check the real pre-mixin dependency rejection with
`python tests/run_smoke_windows.py --loader 0.19.3 --expect-loader-rejection`.
The 1.21.11 adaptation retains its own >=0.18.4 loader requirement.

Workstation paths belong in ignored `local-settings.json`: `mods_dir` selects the
live profile, and `minecraft_version` defaults to `1.21.11`. Legacy build paths
(`minecraft_jar`, `fabric_api_jar`, `loader_jar`, `javac`, `libraries_dir`, optional
`library_jars`) can remain configured independently of the live install target.
Never commit launcher tokens, personal settings or private history.

For the maintained Linux runtime, build the offline smoke mod and run:

```bash
(cd versions/1.21.11 && bash gradlew build remapMenuSmoke)
python3 tests/run_menu_linux.py
```

All graphical runners use `scripts/test_display.py` to create a fresh private
Xvfb display. Minecraft renders with software OpenGL there, and captures the
same light/dark, full/compact menu screenshots without opening a window on the
player's desktop or taking keyboard/pointer focus. Sound uses the null backend.
Inspect the PNG files under `artifacts/menu-preview-linux/screenshots` directly;
do not automate the player's window. `DISPLAY` is replaced and inherited Wayland
handles are removed only for the test subprocess. The virtual server is cleaned
up on success, failure and timeout. Each run gets its own display.

Install the Linux `xvfb` package before running, or extract its package under
`.local/xvfb` so `.local/xvfb/usr/bin/Xvfb` exists. If isolation is missing or fails,
the test stops; it never falls back to the active desktop. Windows graphical
entry points now fail closed too; use a Linux test host instead. Windows
`CREATE_NO_WINDOW` hides a console, but does not isolate an OpenGL game window.
Non-graphical Java/Fabric smoke checks remain available on Windows.
Run `python3 tests/test_display_isolation.py` to check display separation,
Wayland exclusion, missing-backend rejection and timeout cleanup.

`--profile PATH` copies a complete pack's mods, configs and crop packs into a
disposable offline Linux client. The test never joins a server or modifies the
source profile. Screenshots and assertions still come from the real Fabric runtime.

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

On a configured Windows workstation, `python tests/run_smoke_windows.py` checks real Fabric mixins and settings migration in a disposable profile. `python3 tests/run_menu_linux.py` exercises widgets and captures menus on a private Xvfb display. It never signs in or joins a server. These checks require the installed launcher dependencies. Graphical Windows runners are disabled to protect the active desktop.

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
The historical Windows `--voicechat` graphical runner is disabled. On Linux,
use `tests/run_menu_linux.py --profile PATH` with the configured pack to include
voice chat initialization on the private display. These offline checks do not
test server UDP connectivity or your microphone.

## Preserving player data during a version migration

Create a separate profile and keep the original as a fallback. Wait for the source
client to exit before the final snapshot so its last options/history saves are
included. `scripts/profile_migration.py` copies and SHA-256 verifies player data,
including options, mod configuration, voice device settings/volumes, server list,
schematics, screenshots and worlds. It rejects overlapping/nonempty destinations
and linked paths. It changes the options data version and future schematic data versions to 4671, the known
Ready Crops pack's format to 75, and absolute configuration paths to the copied
profile (including schematic placements); all keybindings and crop texture/model bytes stay
unchanged. Launcher metadata, runtime caches and old mod binaries are separate.
Copied worlds are backups: do not open newer-version worlds in the older client.
Keep personal migration manifests and snapshots local.
