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
- Ukrainian (`uk_ua`) and Portuguese (Brazil) (`pt_br`) translations; 1.12.2 had none. German and Spanish get the
  keys 1.12.2 lacked; the Spanish shift tooltip and tab name (machine-translated nonsense in 1.12.2) are replaced.
  Written for the port, not by native speakers. Also in tools/gen_resources.py.

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

## 2026-10-09 — Fabric (1.0.1)
The project is split like the MultiLoader template: `common` (all game code, compiled against vanilla through
NeoForm), `neoforge` and `fabric` (entry points and `IPlatform`: registration, the custom registries, the menu type
and opening, networking, the equipped-backpack data, the config, the mock connection for tests).
- Fabric has no anvil or item-expiry events: mixins into AnvilMenu (createResult, onTake: no anvil damage, like the
  1.12 AnvilRepairEvent's break chance 0) and ItemEntity.tick call the same logic as the NeoForge events.
- Fabric's config is `config/ironbackpacks-startup.json` (the upgrade switches); NeoForge keeps its startup toml.
- The recipes no longer use a NeoForge custom ingredient: the backpack slot is the plain backpack item and the
  recipe's `input_backpack` (tier + specialty) is checked in `matches`, so the JSON is the same on both loaders; the
  upgrade recipes carry both loaders' conditions (`neoforge:conditions`, `fabric:load_conditions`).
- The backpack inventory is a vanilla container (BackpackInventory) instead of NeoForge's ItemStackHandler; dye
  colours come from dye items or `c:dyes/<colour>` tags (NeoForge's DyeColor.getColor did the same).
- The item model properties are index / 10 (0.1, 0.2, ...): item properties are clamped to 0..1.
- JEI 19.56 on Fabric (19.57 is built with a Loom that needs Java 25 for Gradle).
- Tests: 17 GameTests on both loaders, plus `:fabric:prodGametest`, which runs them with the release jar on a real
  Fabric server (with the fabric-gametest module, which the Fabric API bundle doesn't include).

## 2026-10-17 — performance pass (branch `perf`)
- Tool: `tools/perf.sh [-Pjfr]` runs `client/PerfScene` on NeoForge (world `neoforge/run/saves/ib_scene`, a sky platform):
  inventory open with 36 full shulker boxes / 36 full diamond backpacks (without and with the damage bar), then 400 full
  shulker boxes / 400 full backpacks lying on the ground; average FPS and server tick. The laptop is noisy (FPS swings
  ~20 % between runs), so each case is compared with the shulker boxes of the same run.
- Found (flight recording): the server does nothing per tick (no change needed); the client rebuilt the whole backpack
  (data + a copy of every stack inside) every frame: for the item name (vanilla asks a dropped item's name twice per
  frame for its name tag), for the durability bar (isBarVisible on every backpack, width and colour on those with the
  upgrade) and the model property built a string key per frame.
- Fixed: name, bar and model choice read only the backpack's header (`BackpackInfo.headerOf`: variant and upgrades),
  the bar counts the stored contents directly; the border and latch textures (layers 1 and 2, always the same tint) are
  merged into one `trim` texture by gen_resources.py, so the item/generated model has two layers instead of three
  (pixel-identical with and without tint, a third fewer vertices per backpack drawn).
- Results, FPS as a share of the shulker boxes' in the same run (average of two runs): 36 backpacks in the open
  inventory 69 % -> 76 %, with the damage bar 52 % -> 62 %, 400 backpacks on the ground 17 % -> 27 % (140 -> 190 FPS).
  The rest of the gap is vanilla's per-pixel item/generated geometry of a flat item; the server tick is unchanged.
- 17 GameTests pass on NeoForge and Fabric; the DevScene screenshots match the 2026-10-09 ones (lineup pixel-identical).
- Released as 1.0.2 (2026-10-17): GitHub release v1.0.2 (Latest, NeoForge and Fabric jars; 17 GameTests on both
  loaders and `:fabric:prodGametest` with the release jar) and Modrinth versions 1.0.2+1.21.1-neoforge / -fabric (JEI
  optional, Fabric API required on Fabric).

## 2026-10-19 — bugs of the original (1.0.3)
Open issues of gr8pefish/IronBackpacks (42) read; those of 1.12.2 that the port still had are fixed in common code:
- #234, #268 (dupe): `ContainerBackpack.stillValid` was always true, so the GUI of a dropped / moved backpack stayed
  open and its slots could be emptied while the dropped copy kept the items. Now the opened stack (by reference) must
  still be in the player's inventory (or equipped); clicks of the same tick are ignored, the server closes the GUI.
- #255, #221 (lost / reverted contents with inventory sorters): the contents were written on close into the old stack.
  Now every slot change is written into the backpack at once (a listener on the backpack container).
- #243: the open key plays the opening sound (to that player only); the key also checks the Latch owner, which 1.12
  checked on right click only.
- Already fine in the port: #223 / #245 / #271 (the colour recipe called client code on a server), #244 / #264
  (nesting, the later 1.12 commit 0364d2d), #261 (the size config, never read in 1.12, left out).
- 3 new GameTests (20 in all) on NeoForge and Fabric.

## 2026-10-19 — Minecraft 26.1.2, 26.2 and 26.3 (1.0.3, branches mc/*)
One port commit per version on top of the previous one (main → mc/26.1.2 → mc/26.2 → mc/26.3), the same gameplay and
data as 1.21.1. Build: Gradle 9.8.1, Java 25, Fabric Loom 1.18 without mappings, no Parchment.
- Items: ids in the properties (`Properties.setId`), names by `getName(stack)` (the backpack name still depends on its
  type), `appendHoverText` with a consumer; `use` returns `InteractionResult` (`ResultHolder` replaces 1.21.1's
  InteractionResultHolder in the API helpers).
- Item models: the overrides of models/item/*.json are client item definitions `assets/ironbackpacks/items/*.json`
  with two select properties (`ironbackpacks:variant` "type/specialty", `ironbackpacks:upgrade`) and a tint source
  (`ironbackpacks:backpack_color`, the trim layer); NeoForge registers them with its events, Fabric through the
  mappers its access wideners open. `tools/convert_26.py` writes them and the 26.x recipes (string ingredients).
- Recipes: `ShapedRecipe` subclasses with `Recipe.CommonInfo` / `CraftingBookInfo`, the serializer is a record
  (`getSerializer` cast, ShapedRecipe fixes its type); the result is an `ItemStackTemplate` built from the component
  patch, because 26.x reads recipes before item components are bound (an ItemStack there crashes:
  "Components not bound yet"). The backpack slot shows the fitting backpacks through `display()`.
- JEI 29-31: the client has no recipe manager; the tier recipes come with the loaders' recipe sync (NeoForge
  `OnDatapackSyncEvent.sendRecipes` + `RecipesReceivedEvent`, Fabric `RecipeSynchronization` +
  `ClientRecipeSynchronizedEvent`), slots are built from the recipe's `SlotDisplay`s.
- Menu: `ContainerInput` (ClickType), `Inventory.getSelectedSlot()`; the opening sound by the open key is a
  `ClientboundSoundPacket` to that player (no `playNotifySound`); 26.x `SimpleContainer` has no listeners, so
  `BackpackInventory` calls back itself (the same on main).
- GUI: the extract API (`extractLabels`, `extractBackground`, `GuiGraphicsExtractor.blit(RenderPipelines.GUI_TEXTURED, ...)`),
  the size given to the screen constructor, ARGB text colours. Keys: `KeyMapping.Category` with the id
  `ironbackpacks:main` (lang `key.category.ironbackpacks.main`); 26.3 uses `InputConstants.Type.KEYBOARD` and scancodes.
- Anvil on NeoForge: `AnvilRepairEvent`'s break chance is gone; `AnvilCraftEvent.Pre` is cancelled and the vanilla take
  done without the damage roll (as the Fabric mixin does).
- GameTests: a registry of test functions plus test instances (NeoForge `RegisterGameTestsEvent`), Fabric
  `@GameTest` methods (`FabricGameTests`, one line per common test). The mock player is a `ServerPlayer` subclass:
  NeoForge's config sync throws for a plain one that never had a configuration phase.
- 26.2: `Minecraft.gui` holds the screen and overlay, `GameRenderer.mainRenderTarget()`, colour collections
  (`Items.DYE.red()`), no `BlockPos.getCenter()`; neoforge.mods.toml `bannerFile`/`iconFile` instead of `logoFile`.
- 26.3: `Inventory.placeItemBackInInventory(stack, Prediction)`, `TestData` with a dimension.
- Tests on every version: 20 GameTests on NeoForge and Fabric, `:fabric:prodGametest` with the release jar, the
  DevScene (17 screenshots, the same as 1.21.1; Russian checked on 26.1.2) on both loaders.
- PerfScene on 26.3 (NeoForge; 26.x also caps the frame rate to 30 after 30 s without input, now switched off in the
  scene): 36 full backpacks in the open inventory 1514 FPS against 1645 for shulker boxes (92 %, 1.21.1: 76 %), with the
  damage bar 68 %; 400 on the ground 148 against 1006 FPS (15 %). A flight recording of the last case: the mod's code
  is 2 % of the render thread, the rest is vanilla emitting the quads of the flat item/generated model (a shulker box
  item is a few cuboids). Server tick the same as with shulker boxes (1.9 / 2.1 ms).
- Known: the open key's default (I) is also the key of the 26.x debug combination F3+I (copy recreate command); they
  don't interfere, F3 has to be held for the debug one.
