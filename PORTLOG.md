# Port log — Iron Backpacks 1.12.2 → Minecraft 1.21.1 / NeoForge 21.1

Spec: gr8pefish/IronBackpacks branch `dev-1.12`, tag 3.0.8 plus the one later commit 0364d2d ("Stop backpack
nesting without Nesting Upgrade"). The user chose this version over the 1.10.2 v2.2 one (with all 20 upgrades);
1.12.2 is the unfinished rewrite with 4 upgrades.

## 2026-10-09 — first port
Everything of 1.12.2 is ported: the backpack types / upgrade registries, the 7 variants (sizes and points), the
backpack GUI (copygirl's layout up to 17x7), slot blocking, tier / colour / upgrade recipes, anvil upgrades and shears,
latch, damage bar, everlasting, the open key, the blacklist, the equipped-backpack data and its (disabled) equip key,
the JEI tier category and subtypes. 17 GameTests; the DevScene screenshots everything.

### Differences from 1.12.2
- Storage: the "packInfo" / "packInv" NBT tags are the data components `ironbackpacks:pack_info` and
  `ironbackpacks:pack_inventory`, the upgrade item's "upgrade" tag is `ironbackpacks:upgrade`.
- Registries: NeoForge registries `ironbackpacks:types` / `ironbackpacks:upgrade` (default entry `ironbackpacks:null`,
  synced). The config is a NeoForge startup config (`ironbackpacks-startup.toml`); the 1.12 "sizes" and
  "handholding.maxNests" options are left out because 1.12.2 never read them.
- Models: 1.12 chose the item model with custom mesh definitions; 1.21.1 uses overrides on
  `ironbackpacks:variant` / `ironbackpacks:upgrade` item properties (built-in variants only). Item textures moved
  from `textures/items` to `textures/item` (the block atlas only stitches that folder). Unused 1.7.10-era assets
  (`old/`, alternate GUIs, 3D model textures) are not shipped.
- Language files are JSON; only the keys 1.12.2 still read are kept (item / upgrade keys without ".name").
  Translations written for an older argument layout (e.g. German "%d ... %s" for one argument) are dropped, so they
  show English instead of 1.12's "Format error". The key category is the translation key
  `key.categories.ironbackpacks` ("Iron Backpacks").
- Recipes are JSON with custom types (`ironbackpacks:backpack_tier`, `ironbackpacks:backpack_color`), a custom
  ingredient (`ironbackpacks:backpack`: tier + specialty) and a condition (`ironbackpacks:upgrade_enabled`).
  Ore dictionary → tags: leather `c:leathers`, chestWood `c:chests/wooden`, ingots `c:ingots/*`, gemDiamond
  `c:gems/diamond`, string `c:strings`, stickWood `c:rods/wooden`, dye `c:dyes`, wool `#minecraft:wool`, paper the
  item.
- 1.21 can swap a slot with the offhand (F, button 40); that swap gets the same backpack / blacklist checks as the
  number keys.
- Blacklist (`config/ironbackpacks/blacklist.json`): items are listed by id (no metadata in 1.21); the NBT keys are
  looked up in the stack's custom data.
- Not ported: the Inventory Tweaks container annotations (no such mod on 1.21) and the custom backpack inventory
  capability (nothing read it).
- Everlasting: 6000 ticks extra life and an unlimited lifetime (1.12 setNoDespawn + cancelled expiry; 1.21's
  expiry event can't be cancelled).
- The (disabled) Equip/Unequip key is unbound by default: 1.12's H is the "open accessories" key of Accessories, a
  common modpack mod, and the conflict showed in the controls screen.
- Extra tooltip lines are gray, as 1.12's tooltip renderer drew them.

- Every tier can be dyed (user request). 1.12.2 wrote the gold and diamond colour recipes with PACK_BASIC: they asked
  for a basic backpack with a storage / upgrade emphasis, which can't exist, so only basic and iron backpacks could
  be dyed.
- Russian translation (`ru_ru`, user request); 1.12.2 had none. It lives in tools/gen_resources.py like the rest.

### 1.12.2 quirks kept
- Crafting the next tier loses the colour (BackpackInfo.upgradeTo doesn't copy it).
- The Everlasting upgrade has no recipe.
- Any upgrade (not only a blank one) works as the "blank upgrade" ingredient.
- The equip key is registered but does nothing ("Equip/Unequip Backpack (Disabled)"), now unbound by default.

## 2026-10-09 — compatibility with accessory slot mods
Checked with Curios API 9.5.1 and with Accessories 1.1.0-beta.53 + owo-lib + Accessories Compatibility Layer (+ Curios),
the usual 1.21.1 NeoForge modpack setups (`-PcompatMods=curios` / `-PcompatMods=accessories` adds them to the dev
runs only): all GameTests pass, the scene runs, the backpack GUI and the player inventory (with their slot buttons)
look right. The only conflict was the H key above. The GameTests' mock players now get NeoForge's mock connection
(`NetworkRegistry.configureMockConnection`), so other mods' join packets and the menu packet go through.
