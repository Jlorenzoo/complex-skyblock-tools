# Money Booster HUD

Minecraft Fabric mod for the Complex Skyblock profile. Tracks money boosters, estimated extra earnings per activation, consumable history, and a draggable rolling earnings HUD.

## Install at home

1. Sign into GitHub and open [the latest release](https://github.com/Jlorenzoo/money-booster-hud/releases/latest).
2. Download **money-booster-hud-1.3.2.jar** from **Assets**. Keep it as a `.jar` file.
3. Open your Minecraft profile's `mods` folder. With Minecraft closed, remove any older Money Booster HUD jar and copy in the downloaded jar.
4. Start Minecraft. Use `/boostermenu` or `/boosterhud` to open the menu.

Required: Minecraft **26.2**, Fabric Loader **0.19.0 or newer**, **Fabric API**, and **Java 25 or newer**. The jar works across operating systems with those dependencies. In Prism Launcher, right-click the instance and open its folder; in CurseForge, open the profile's folder.

## Features in 1.3.2

- Booster bonus totals and per-activation history with saved item sprites, percentage, announced duration and tracked duration.
- History filter for money boosts only or all recorded consumable effects.
- Plain white rolling ten-second earnings HUD, refreshed once a second, with a two-second reconciliation delay.
- With boosters included by default, plus an optional estimated base rate without them. Drag the white HUD text directly while the menu is open.
- Recognized chest-shop transactions, dollar auction sales, player payments and listing fees are excluded from earnings.
- Sidebar state takes priority over an expired local countdown; below one minute, it can show Active when the server does not provide seconds.
- Compact menus, flat buttons, anchored footers, pagination and scrolling at smaller GUI sizes.

Amounts are estimates from scoreboard samples. Rounded balances, delayed server updates, and unrecognized transactions can affect accuracy. Item sources are matched to recent item use; unknown sources are labeled accordingly. The source zip is included in each release. The included build script uses the original workstation's Minecraft/Fabric library paths and needs adjustment to build elsewhere.

## Optional: move your saved data

To use your work stats/history/settings at home, close Minecraft and copy these files from the work profile's `config` folder into the home profile's `config` folder:

- `moneyboosterstats.json`
- `moneyboosterhistory.json`
- `moneyboosterhud.json`

Back up any existing home copies before replacing them. These files are optional; the mod starts fresh without them. Copy after the work installation has completed its correction of the known contaminated historical entry.

## Validation

The full jar compiled against the installed Minecraft 26.2/Fabric setup. Standalone regression checks passed for accounting, transaction exclusions, activation boundaries, timer/sidebar behavior, history persistence/filtering, rolling rates and layout bounds. Parser replay passed for 3,109 chest-shop and 276 auction/payment/fee notifications. Updated UI dragging and accounting still need live verification.
