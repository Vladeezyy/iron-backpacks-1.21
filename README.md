# Iron Backpacks for Minecraft 1.21.1 and 26.x (NeoForge and Fabric)

An unofficial port of [Iron Backpacks](https://github.com/gr8pefish/IronBackpacks) by gr8pefish and contributors
from Minecraft 1.12.2 (version 3.0.8) to **Minecraft 1.21.1, 26.1.2, 26.2 and 26.3** on **NeoForge** and **Fabric**.

The port keeps the 1.12.2 behaviour, numbers, textures, sounds and translations (plus Russian, Ukrainian and Portuguese (Brazil) ones, and
the German and Spanish strings 1.12.2 lacked). The 1.12.2 source is the
specification; every difference is listed in [PORTLOG.md](PORTLOG.md).

**Download:** [Modrinth](https://modrinth.com/mod/iron-backpacks) ·
[GitHub releases](https://github.com/Vladeezyy/iron-backpacks-1.21/releases)

| Version | Loader | Requires | Notes |
|---|---|---|---|
| 1.0.3+26.3 | NeoForge / Fabric | NeoForge 26.3.0.94+ / Fabric Loader 0.19.5+ and Fabric API 0.164+, Java 25 | first 26.x versions |
| 1.0.3+26.2 | NeoForge / Fabric | NeoForge 26.2.0.93+ / Fabric Loader 0.19.5+ and Fabric API 0.161+, Java 25 | |
| 1.0.3+26.1.2 | NeoForge / Fabric | NeoForge 26.1.2.122+ / Fabric Loader 0.19.5+ and Fabric API 0.155.3+, Java 25 | |
| 1.0.3+1.21.1 | NeoForge / Fabric | the same as 1.0.2 | fixes of the original's dupes and lost contents (see below) |
| 1.0.2+1.21.1 | NeoForge / Fabric | NeoForge 21.1.197+ / Fabric Loader 0.16.14+ and Fabric API | faster backpack rendering, lighter models |
| 1.0.1+1.21.1 | NeoForge / Fabric | the same | the Fabric version |
| 1.0.0+1.21.1 | NeoForge | NeoForge 21.1.197+ | first release |

## Content
| | |
|---|---|
| Backpacks | Basic (18 slots, 4 upgrade points); Iron, Gold and Diamond, each with a storage emphasis (more slots) or an upgrade emphasis (+5 points). Diamond storage holds 77 items (11x7). |
| Tiers | Craft the next tier around the previous backpack: items, upgrades and owner carry over. |
| Upgrades | Damage Bar (fill level), Latch (only the owner can open it), Extra Upgrade Point, Everlasting (a dropped backpack never despawns). Added on an anvil, the last one taken off with shears. |
| Colours | Backpack + dye (every tier); a water bucket washes the colour out. |
| Key | Open Backpack (default I): the held, offhand or first backpack in the inventory. |
| Config | `config/ironbackpacks-startup.toml` on NeoForge / `config/ironbackpacks-startup.json` on Fabric (enable upgrades), `config/ironbackpacks/blacklist.json` (items that can't go in). |
| JEI | "Increase Backpack Tier" category; backpack variants and upgrades are separate entries. |

## Bugs of the original fixed in this port
Report bugs of this port [here](https://github.com/Vladeezyy/iron-backpacks-1.21/issues), not to the original author.

| Original issue | Fix |
|---|---|
| [#234](https://github.com/gr8pefish/IronBackpacks/issues/234), [#268](https://github.com/gr8pefish/IronBackpacks/issues/268) item dupe: the backpack dropped or moved out while its GUI is open | the GUI closes as soon as that backpack leaves the player, clicks after that do nothing |
| [#255](https://github.com/gr8pefish/IronBackpacks/issues/255), [#221](https://github.com/gr8pefish/IronBackpacks/issues/221) contents lost or reverted when an inventory sorter moves the open backpack | every change is saved into the backpack at once, not only on close |
| [#243](https://github.com/gr8pefish/IronBackpacks/issues/243) no sound when the open key opens a backpack | the key plays the opening sound |
| open key ignored the Latch upgrade (anyone could open someone else's latched backpack with the key) | the key checks the owner like a right click |
| [#223](https://github.com/gr8pefish/IronBackpacks/issues/223), [#245](https://github.com/gr8pefish/IronBackpacks/issues/245), [#271](https://github.com/gr8pefish/IronBackpacks/issues/271) dyeing crashes on a server and dupes the dye | the colour recipes are data recipes without client code (tested on a dedicated server) |
| [#244](https://github.com/gr8pefish/IronBackpacks/issues/244), [#264](https://github.com/gr8pefish/IronBackpacks/issues/264) backpacks shift-clicked into each other | backpacks can't go into backpacks in any way (shift, number keys, offhand swap) |
| gold and diamond backpacks couldn't be dyed (the recipes asked for a basic backpack) | every tier can be dyed |

## Branches
| Branch | Minecraft | Build JDK |
|---|---|---|
| `main` | 1.21.1 (NeoForge 21.1, Fabric) | 21 |
| `mc/26.1.2` | 26.1.2 | 25 (also for Gradle: Fabric Loom 1.18) |
| `mc/26.2` | 26.2 | 25 |
| `mc/26.3` | 26.3 | 25 |

Each `mc/*` branch is the port commit on top of the previous one (`main` → `mc/26.1.2` → `mc/26.2` → `mc/26.3`); fixes
for every version go into `main` first. What changed for 26.x is in [PORTLOG.md](PORTLOG.md).

## Building
Requires JDK 21 (`main`) or JDK 25 (`mc/*`, `JAVA_HOME` pointing to it). The game code is in `common`; `neoforge` and
`fabric` are thin loader layers over it (after the MultiLoader template), behind
`gr8pefish.ironbackpacks.platform.IPlatform`.
```bash
./gradlew build                          # jars in neoforge/build/libs and fabric/build/libs
./gradlew :neoforge:runClient            # dev clients (with JEI)
./gradlew :fabric:runClient
./gradlew :neoforge:runGameTestServer    # headless in-game tests
./gradlew :fabric:runGametest
./gradlew :fabric:prodGametest           # the tests with the release Fabric jar on a real Fabric server
./gradlew :neoforge:runScene             # scripted scene: screenshots to <loader>/run/screenshots/ (1.21.1: needs run/saves/ib_scene; 26.x: makes a new world)
./gradlew :fabric:runScene
```
Resources are generated from the original 1.12.2 assets:
```bash
rm -rf common/src/main/resources/{assets,data} && python3 -I tools/gen_resources.py <IronBackpacks dev-1.12>/src/main/resources/assets/ironbackpacks common/src/main/resources
```
On the `mc/*` branches they are then turned into the 26.x format: `python3 -I tools/convert_26.py common/src/main/resources`.

## License and credits
GPL-3.0, the same license as the original — see [LICENSE](LICENSE).
Original mod, art, sounds and translations: gr8pefish, TehNut and the Iron Backpacks contributors; backpack GUI code
by copygirl. Original credits: cpw, sapient, #minecraftforge (especially diesieben07), TehNut, tterrag.
