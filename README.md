# Skyblock Toolkit 1.0.0

One Fabric client mod containing Money Booster HUD, Fishing Pings and Chat Copy for Minecraft 26.2. This is an integrated in-game toolkit for the existing pack. It requires Fabric Loader >=0.19.0, Fabric API and Java >=25.

## Open and customize

Press **Right Shift**, choose **Escape → Skyblock Toolkit** or **Options → Skyblock Toolkit**, or run **/toolkit**. The module shortcuts are `/toolkit boosters`, `/toolkit fishing`, `/toolkit chat`, `/toolkit auction`, `/toolkit blackout` and `/toolkit hud`. The old `/boostermenu`, `/boosterhud` and `/fishping` aliases have been removed.

The smaller translucent menu opens directly to Boosters. Its sidebar separates Boosters, Fishing, Chat Copy, Auction and Blackout. Related switches share two-column rows, actions sit next to each other, and the selected sidebar item has a full outline. HUD eyes are small unboxed icons. Each module keeps its common controls visible and puts detailed controls under **Advanced**. Menu-key rebinding is in **Blackout → Advanced**, or Minecraft's Controls → Key Binds → Miscellaneous. Escape cancels key capture; Delete/Backspace clears it.

Choose **Advanced → HUD elements** for a small menu with separate switches for **Booster timer**, **Crop earnings**, **Fishing earnings** and **Rate test info**. Crop and fishing earnings can appear together or independently, with separate saved positions. Existing earnings visibility is used as the initial setting when migrating older configurations. Hiding a HUD element does not stop accounting.

**Edit HUD** opens a clear placement canvas with no toolbar. Drag the timer, crop earnings or fishing earnings directly; use each element's adjacent eye to show or hide it. Escape returns to the settings. Reset positions is in **Blackout → Advanced**.

Booster history, shop-transaction filtering, latest bonus and rate tests remain available. Crop earnings use a 10-second rolling window; **Boosters → Advanced** selects base earnings or earnings including booster bonuses. The timer preserves exact coconut chat durations over coarse minute-only sidebar displays, and repeated copies of a server countdown sample no longer extend it. Sidebar-confirmed removal still ends the booster after a short grace period. Server latency and rounded sidebar data still limit precision.

Fishing retains ready-to-reel, depleted-area and snapped-line cues. **Fishing volume** now ranges from 0–300%. **Advanced → Individual alert sounds** provides separate 0–300% gains and previews. The main fishing volume multiplies each alert's individual gain; 0% mutes it. Existing volume settings are preserved. Minecraft UI/master volume still applies. The area cue uses three descending bass notes. Missing depth-meter detection and its 1.5-second guard are unchanged.

Chat Copy keeps chat selection and double-click-word switches in its main page. Highlight color/opacity and Minecraft chat size/width/opacity are under Advanced. Drag text in chat and use Ctrl+C; existing clickable links retain their ordinary single-click behavior.

**Auction** contains its sale alert, volume, sound choice and preview. **Blackout** contains activation, wake-key binding and opacity. Only the configured wake key dismisses blackout; repeated Escape presses are consumed by its dedicated screen. Blackout does not pause the game, and audio remains active.

## Fishing value estimate

Open **Fishing → Fish earnings**. Enter the **sell price of one fish** for cod, salmon, pufferfish and tropical fish. Dollar values accept commas and k/m/b suffixes, such as 1,250 or 1.5k. For a 64-item stack price, divide it by 64 first. Prices start at zero; no prices are invented. Invalid input stays red and does not replace the last valid value.

Weights were verified directly in the installed Minecraft 26.2 client's `data/minecraft/loot_table/gameplay/fishing/fish.json`:

- Cod: 60%
- Salmon: 25%
- Pufferfish: 13%
- Tropical fish: 2%

Expected dollars per caught item = 0.60 × cod price + 0.25 × salmon price + 0.13 × pufferfish price + 0.02 × tropical price.

The parser accepts the server's `What Lurks Below VIII/IX/... ┃ ✔ Caught 13 items!` system messages and accumulates the actual announced item count. It excludes unrelated catches, player-prefixed quotes, snap messages and immediate duplicate notifications. The estimate assumes these rod catches consist of fish using the requested vanilla mix: it cannot identify actual species from a count-only message. It is estimated inventory sell value, not confirmed money received. No booster multiplier or extra rod multiplier is added.

Choose **60 seconds** or **5 minutes**. The page displays fish count and estimated total value in that rolling window; the HUD displays its equivalent dollars/hour. For 60s, hourly value = window value × 60; for 5m, hourly value = window value × 12. The denominator always stays the full selected window, so the first catch cannot produce a huge estimate by being divided by one second. An asterisk marks the initial warm-up; early readings understate the eventual steady rate. Old catches roll out of the window. Counts reset on disconnect/world change, and changing prices recalculates retained catches immediately.

Crop and fishing displays have independent visibility and positions in **Advanced → HUD elements**. Fishing value is not added to booster totals or mixed with farming balance changes.


## Installation and preservation

Build with `python3 build.py`, then install with `python3 scripts/install.py --wait`. Building only creates the jar and never overwrites a running client. Installation is queued until all clients using the shared Complex Skyblock mods folder close. Close Minecraft, wait a few seconds, then check `toolkit-installation-status.txt` before relaunching. The installer makes byte-identical timestamped backups of the old Money Booster HUD, Fishing Pings and Chat Copy jars, replaces them with the single toolkit jar, and leaves Fabric API in place. Existing config, statistics and history files are preserved. The three Prism profiles share this mods folder.

The standalone Fishing Pings installation queue has been superseded. This toolkit's new entry point, keybind and mixins need **one restart**; they are not loaded into the currently open client. Settings changes apply immediately after installation. Compatible future method edits can still use Java Instrumentation, but the earlier standalone hotreload-fishing.py targets old separate jars and must be adapted before use with the toolkit.

If the 24-hour installer wait expires, close Minecraft and run:

`python3 scripts/install.py --wait`

Do not keep the three old active jars alongside the toolkit on another computer: they would register duplicate features/mixins. The `.bak.TIMESTAMP` files are inactive backups.

## Verification and limits

- Full compilation against the installed Minecraft/Fabric jars passed.
- Real Fabric Loader discovery, Chat Copy mixin transformation and booster/fishing callback initialization passed in a separate offline headless smoke process. Sample existing booster/fishing config was loaded without migration loss. Keybinding registration occurs later in the ordinary client initialization lifecycle and was not exercised by that pre-launch smoke probe.
- Tests passed for fish parsing, duplicate/spoof rejection, prices, weight mapping, 60s/5m rates, warmup and expiry, as well as existing timer, ledger, history, rolling average and responsive layout geometry.
- Accounting replay parsed and validated 3,109 existing transaction notifications.
- Installer tests replaced all three mods in a disposable folder, verified byte-identical backups, preserved Fabric API and passed an idempotent reinstall.
- `python3 tests/test_menu_runtime.py` renders menus in a disposable offline Fabric client and checks HUD eyes, independent crop/fish switches and persistence, dragging, Escape navigation, 25 repeated blackout Escape presses and wake-key restoration. Screenshots are saved under `artifacts/menu-preview/screenshots`. This requires a desktop display.
- `python3 tests/test_sound_controls.py` checks real Fabric audio injection, 300% volume persistence, amplified native gain, mute and ordinary-sound isolation.
- Live server timer/catch synchronization and the complete installed modpack still need an in-game check after restart; offline checks do not establish server timing accuracy.

Source is bundled alongside the jar. The build script references this workstation's installed libraries. Chat Copy is derived from xpncvr's installed GPL-3.0 chattextselect 1.3.0; its license and attribution are preserved, and the combined toolkit is distributed with its source under GPL-3.0. See THIRD-PARTY.md for provenance.
