# Skyblock Toolkit 1.0.0

One Fabric client mod containing Money Booster HUD, Fishing Pings and Chat Copy for Minecraft 26.2. This is an integrated in-game toolkit for the existing pack. It requires Fabric Loader >=0.19.0, Fabric API and Java >=25.

## Open and customize

Press **Right Shift**, choose **Escape → Skyblock Toolkit** or **Options → Skyblock Toolkit**, or run **/toolkit**. Set another shortcut using **Menu key** in the toolkit or Minecraft's Controls → Key Binds → Miscellaneous. Escape cancels key capture; Delete/Backspace clears it. Existing /boostermenu and /fishping commands open their respective toolkit pages.

The shared navigation includes Overview, Boosters, Fishing, Chat Copy and HUD layout. Panels scroll on small windows, with Done fixed at the bottom. HUD layout opens the placement view within the toolkit: drag the timer or white average text directly. Grey outlines mark hidden elements; toggle the timer/average in the same view. Reset positions restores the default anchors. Positions and settings save on release/close.

Booster tracking/history, shop-transaction filtering, latest bonus, history details and rate tests remain available. Hiding the timer hides its display without deleting accounting data. Farming averages retain their with/without-booster setting and original 10-second window.

Fishing sound controls retain depth-ready, depleted-area and snapped/missing-meter cues, volume, target depth and previews. The area cue uses three descending bass notes. A successful boss-bar scan with no meter for a new cast infers a snapped line after 1.5 seconds and suppresses its later server confirmation. A meter that has appeared then disappears does not trigger that inference.

Chat Copy has selection and double-click-word toggles, highlight color/opacity, and chat size/width/opacity sliders. Chat appearance sliders update Minecraft's own options. Drag text in chat and use Ctrl+C. Existing clickable chat links retain their ordinary single-click behavior.

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

The existing earnings HUD toggle controls both farming and fishing displays. If it was disabled, receiving catches keeps it disabled. If enabled, **Automatic** mode switches to fishing while you have a fishing hook or recent catches, then returns to farming after the fishing window expires. You can also lock the HUD to Farming or Fishing. Fishing value is not added into booster totals or mixed with farming balance changes.

## Installation and preservation

Installation is queued until all clients using the shared Complex Skyblock mods folder close. Close Minecraft, wait a few seconds, then check `toolkit-installation-status.txt` before relaunching. The installer makes byte-identical timestamped backups of the old Money Booster HUD, Fishing Pings and Chat Copy jars, replaces them with the single toolkit jar, and leaves Fabric API in place. Existing config, statistics and history files are preserved. The three Prism profiles share this mods folder.

The standalone Fishing Pings installation queue has been superseded. This toolkit's new entry point, keybind and mixins need **one restart**; they are not loaded into the currently open client. Settings changes apply immediately after installation. Compatible future method edits can still use Java Instrumentation, but the earlier standalone hotreload-fishing.py targets old separate jars and must be adapted before use with the toolkit.

If the 24-hour installer wait expires, close Minecraft and run:

`python3 /home/jeremy/Documents/Codex/2026-10-12/i-ma/outputs/install-skyblock-toolkit.py`

Do not keep the three old active jars alongside the toolkit on another computer: they would register duplicate features/mixins. The `.bak.TIMESTAMP` files are inactive backups.

## Verification and limits

- Full compilation against the installed Minecraft/Fabric jars passed.
- Real Fabric Loader discovery, Chat Copy mixin transformation and booster/fishing callback initialization passed in a separate offline headless smoke process. Sample existing booster/fishing config was loaded without migration loss. Keybinding registration occurs later in the ordinary client initialization lifecycle and was not exercised by that pre-launch smoke probe.
- Tests passed for fish parsing, duplicate/spoof rejection, prices, weight mapping, 60s/5m rates, warmup and expiry, as well as existing timer, ledger, history, rolling average and responsive layout geometry.
- Accounting replay parsed and validated 3,109 existing transaction notifications.
- Installer tests replaced all three mods in a disposable folder, verified byte-identical backups, preserved Fabric API and passed an idempotent reinstall.
- Rendered menus, key capture, HUD dragging, chat selection and live catches still need verification in a running game. Compilation and headless loader checks do not prove their visual behavior.

Source is bundled alongside the jar. The build script references this workstation's installed libraries. Chat Copy is derived from xpncvr's installed GPL-3.0 chattextselect 1.3.0; its license and attribution are preserved, and the combined toolkit is distributed with its source under GPL-3.0. See THIRD-PARTY.md for provenance.
