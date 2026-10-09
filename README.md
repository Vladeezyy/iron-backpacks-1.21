# Iron Backpacks for Minecraft 1.21.1 (NeoForge)

An unofficial port of [Iron Backpacks](https://github.com/gr8pefish/IronBackpacks) by gr8pefish and contributors
from Minecraft 1.12.2 (version 3.0.8) to **Minecraft 1.21.1** on **NeoForge 21.1**.

The port keeps the 1.12.2 behaviour, numbers, textures, sounds and translations (plus a Russian one). The 1.12.2 source is the
specification; every difference is listed in [PORTLOG.md](PORTLOG.md).

**Download:** [Modrinth](https://modrinth.com/mod/iron-backpacks) ·
[GitHub releases](https://github.com/Vladeezyy/iron-backpacks-1.21/releases)

## Content
| | |
|---|---|
| Backpacks | Basic (18 slots, 4 upgrade points); Iron, Gold and Diamond, each with a storage emphasis (more slots) or an upgrade emphasis (+5 points). Diamond storage holds 77 items (11x7). |
| Tiers | Craft the next tier around the previous backpack: items, upgrades and owner carry over. |
| Upgrades | Damage Bar (fill level), Latch (only the owner can open it), Extra Upgrade Point, Everlasting (a dropped backpack never despawns). Added on an anvil, the last one taken off with shears. |
| Colours | Backpack + dye (every tier); a water bucket washes the colour out. |
| Key | Open Backpack (default I): the held, offhand or first backpack in the inventory. |
| Config | `config/ironbackpacks-startup.toml` (enable upgrades), `config/ironbackpacks/blacklist.json` (items that can't go in). |
| JEI | "Increase Backpack Tier" category; backpack variants and upgrades are separate entries. |

## Building
Requires JDK 21.
```bash
./gradlew build              # jar in build/libs/
./gradlew runClient          # dev client (with JEI)
./gradlew runGameTestServer  # headless in-game tests
./gradlew runScene           # scripted scene: screenshots to run/screenshots/ (needs run/saves/ib_scene)
```
Resources are generated from the original 1.12.2 assets:
```bash
rm -rf src/main/resources/{assets,data} && python3 -I tools/gen_resources.py <IronBackpacks dev-1.12>/src/main/resources/assets/ironbackpacks src/main/resources
```

## License and credits
GPL-3.0, the same license as the original — see [LICENSE](LICENSE).
Original mod, art, sounds and translations: gr8pefish, TehNut and the Iron Backpacks contributors; backpack GUI code
by copygirl. Original credits: cpw, sapient, #minecraftforge (especially diesieben07), TehNut, tterrag.
