#!/usr/bin/env python3
"""Builds src/main/resources/{assets,data} for the 1.21.1 port from the original Iron Backpacks 1.12.2 resources.

    python3 -I tools/gen_resources.py <IronBackpacks 1.12 checkout>/src/main/resources/assets/ironbackpacks common/src/main/resources

Copies the textures, sounds and logo the 1.12.2 mod used (textures/items -> textures/item for the item atlas),
converts the .lang files to .json (only the keys 1.12.2 still used; item and upgrade keys lose their ".name"),
writes the item models (one model per backpack variant / upgrade, chosen by the overrides of backpack.json and
upgrade.json) and the recipes of core/RecipesIronBackpacks.java (ore dictionary -> common tags).
"""
import gzip
import json
import re
import shutil
import struct
import sys
from pathlib import Path

MOD = "ironbackpacks"
src = Path(sys.argv[1])
res = Path(sys.argv[2])
A = res / "assets" / MOD
D = res / "data" / MOD


def write(p, obj):
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def copy(rel_from, rel_to):
    (A / rel_to).parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(src / rel_from, A / rel_to)


# --- textures, sounds, logo ------------------------------------------------------------------------------------
TYPES = ["basic", "iron", "gold", "diamond"]
for t in TYPES:
    for part in ("base", "border", "latch_dark", "latch_light"):
        copy(f"textures/items/backpack_{t}_{part}.png", f"textures/item/backpack_{t}_{part}.png")
UPGRADE_TEX = {"null": "upgrade_blank", "damage_bar": "upgrade_damage_bar", "lock": "upgrade_latch",
               "extra_upgrade": "upgrade_extra_upgrade", "everlasting": "upgrade_everlasting"}
for tex in UPGRADE_TEX.values():
    copy(f"textures/items/{tex}.png", f"textures/item/{tex}.png")
copy("textures/gui/backpack_gui.png", "textures/gui/backpack_gui.png")
copy("textures/jei/crafting_grid.png", "textures/jei/crafting_grid.png")
for s in ("open_backpack_sound", "close_backpack_sound"):
    copy(f"sounds/{s}.ogg", f"sounds/{s}.ogg")
shutil.copyfile(src / "sounds.json", A / "sounds.json")
res.mkdir(parents=True, exist_ok=True)
shutil.copyfile(src / "logo.png", res / f"{MOD}_logo.png")
# the square icon (Fabric's mod list): art/logoOverlappingPacks.png of the 1.12 repo
shutil.copyfile(src.parents[4] / "art/logoOverlappingPacks.png", res / f"{MOD}_icon.png")

# --- lang ---------------------------------------------------------------------------------------------------------
LANGS = {"en_US": "en_us", "de_DE": "de_de", "es_SP": "es_es", "sv_SE": "sv_se", "zh_CN": "zh_cn"}


def parse_lang(path):
    out, key = {}, None
    lines = path.read_text(encoding="utf-8").splitlines()
    i = 0
    while i < len(lines):
        line = lines[i]
        i += 1
        if not line.strip() or line.startswith("#") or "=" not in line:
            continue
        k, v = line.split("=", 1)
        while v.endswith("\\") and i < len(lines):  # PARSE_ESCAPES line continuation
            v = v[:-1] + lines[i]
            i += 1
        out[k.strip()] = v
    return out


def key_of(k):
    # 1.12: item.<id>.name / upgrade.<mod>.<id>.name; 1.21: item.<id> / upgrade.<mod>.<id>
    if (k.startswith("item.") or k.startswith("upgrade.")) and k.endswith(".name"):
        return k[:-len(".name")]
    return k


def top_section(path):
    # the keys above the "OLD" banner, i.e. the ones written for the 1.12 rewrite
    text = path.read_text(encoding="utf-8")
    return parse_lang_text(text.split("################ OLD", 1)[0])


def parse_lang_text(text):
    tmp = res / ".lang_tmp"
    tmp.write_text(text, encoding="utf-8")
    try:
        return parse_lang(tmp)
    finally:
        tmp.unlink()


# the keys the 1.12 code still reads: the new section plus a few old ones (ItemBackpack tooltips, the JEI category)
used = {key_of(k) for k in top_section(src / "lang/en_US.lang")} | {
    "tooltip.ironbackpacks.backpack.emphasis.storage", "tooltip.ironbackpacks.backpack.emphasis.upgrade",
    "tooltip.ironbackpacks.backpack.tier", "tooltip.ironbackpacks.backpack.upgrade.used", "jei.description.shapedCrafting",
    "key.categories.ironbackpacks"}
english = {key_of(k): v for k, v in parse_lang(src / "lang/en_US.lang").items()}


def placeholders(v):
    return sorted(re.findall(r"%[ds]", v))
for old, new in LANGS.items():
    entries = {key_of(k): v for k, v in parse_lang(src / f"lang/{old}.lang").items()}
    if old == "en_US":
        entries["key.categories.ironbackpacks"] = "Iron Backpacks"   # 1.12 used the mod name as key category
    # translations written for the old argument layout (e.g. "%d ... %s" for one argument) are left out, like a
    # missing translation they fall back to English instead of the 1.12 "Format error" text
    entries = {k: v for k, v in entries.items() if k in used and placeholders(v) == placeholders(english.get(k, v))}
    write(A / f"lang/{new}.json", dict(sorted(entries.items())))

# Russian: not in 1.12.2, written for the port (user request)
RUSSIAN = {
    "container.ironbackpacks.backpack": "Открыть рюкзак",
    "item.ironbackpacks.backpack": "Порванный рюкзак",
    "item.ironbackpacks.backpack.ironbackpacks.basic": "Базовый рюкзак",
    "item.ironbackpacks.backpack.ironbackpacks.iron": "Железный рюкзак",
    "item.ironbackpacks.backpack.ironbackpacks.gold": "Золотой рюкзак",
    "item.ironbackpacks.backpack.ironbackpacks.diamond": "Алмазный рюкзак",
    "item.ironbackpacks.upgrade": "Пустое улучшение",
    "itemGroup.ironbackpacks": "Iron Backpacks",
    "jei.description.shapedCrafting": "Крафт по форме",
    "jei.ironbackpacks.increaseTier.name": "Повышение тира рюкзака",
    "jei.ironbackpacks.increaseTier.desc": "Чтобы повысить тир рюкзака, просто следуйте нужному §2рецепту крафта§r. При улучшении, возможно, "
                                           "придётся выбирать между разными видами рюкзаков. Выбирайте с умом!",
    "jei.ironbackpacks.increaseTier.desc2": "При переходе на новый тир все предметы, улучшения, настройки и т. д. прежнего рюкзака сохраняются!",
    "key.categories.ironbackpacks": "Iron Backpacks",
    "key.ironbackpacks.open": "Открыть рюкзак",
    "key.ironbackpacks.equip": "Надеть/снять рюкзак (отключено)",
    "tooltip.ironbackpacks.backpack.emphasis.storage": "Упор на хранение",
    "tooltip.ironbackpacks.backpack.emphasis.upgrade": "Упор на улучшения",
    "tooltip.ironbackpacks.backpack.tier": "Тир: %d",
    "tooltip.ironbackpacks.backpack.upgrade.list": "Установленные улучшения:",
    "tooltip.ironbackpacks.backpack.upgrade.used": "Использовано очков улучшений: %d/%d.",
    "tooltip.ironbackpacks.shift": "Зажмите Shift, чтобы узнать больше",
    "tooltip.ironbackpacks.upgrade.cost": "Стоимость: %d очк. улучшений",
    "tooltip.ironbackpacks.upgrade.minimum_tier": "Минимальный тир: %d",
    "upgrade.ironbackpacks.damage_bar": "Улучшение «Шкала заполненности»",
    "upgrade.ironbackpacks.damage_bar.desc": "Добавляет полоску, по которой с первого взгляда видно, насколько заполнен рюкзак.",
    "upgrade.ironbackpacks.lock": "Улучшение «Защёлка»",
    "upgrade.ironbackpacks.lock.desc": "Открыть рюкзак может только его владелец.",
    "upgrade.ironbackpacks.extra_upgrade": "Дополнительное очко улучшений",
    "upgrade.ironbackpacks.extra_upgrade.desc": "Добавляет ещё одно очко улучшений.",
    "upgrade.ironbackpacks.everlasting": "Улучшение «Вечность»",
    "upgrade.ironbackpacks.everlasting.desc": "Выброшенный рюкзак не исчезает со временем.",
}
assert set(RUSSIAN) == set(english) & used | {"key.categories.ironbackpacks"}, set(english) & used ^ set(RUSSIAN)
for k, v in RUSSIAN.items():
    assert placeholders(v) == placeholders(english.get(k, v)), k
write(A / "lang/ru_ru.json", dict(sorted(RUSSIAN.items())))

# --- item models --------------------------------------------------------------------------------------------------
# property ironbackpacks:variant (client/ClientEventHandler.VARIANT_MODELS): 0 = no / unknown variant ("Torn Backpack"),
# then 0.1, 0.2, ... (item properties are clamped to 0..1)
VARIANTS = [("basic", "none"), ("iron", "storage"), ("iron", "upgrade"), ("gold", "storage"), ("gold", "upgrade"),
            ("diamond", "storage"), ("diamond", "upgrade")]


def layered(t, latch):
    return {"parent": "minecraft:item/generated", "textures": {
        "layer0": f"{MOD}:item/backpack_{t}_base", "layer1": f"{MOD}:item/backpack_{t}_border",
        "layer2": f"{MOD}:item/backpack_{t}_latch_{latch}"}}


write(A / "models/item/backpack/null.json", layered("basic", "dark"))
for t, spec in VARIANTS:
    write(A / f"models/item/backpack/{t}/{spec}.json", layered(t, "dark" if spec == "upgrade" else "light"))
write(A / "models/item/backpack.json", dict(layered("basic", "dark"), overrides=[
    {"predicate": {f"{MOD}:variant": (i + 1) / 10}, "model": f"{MOD}:item/backpack/{t}/{spec}"} for i, (t, spec) in enumerate(VARIANTS)]))

# property ironbackpacks:upgrade (client/ClientEventHandler.UPGRADE_MODELS): 0 = blank / unknown upgrade, then 0.1, 0.2, ...
UPGRADES = ["damage_bar", "lock", "extra_upgrade", "everlasting"]


def flat(tex):
    return {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{tex}"}}


write(A / "models/item/upgrade/null.json", flat(UPGRADE_TEX["null"]))
for u in UPGRADES:
    write(A / f"models/item/upgrade/{u}.json", flat(UPGRADE_TEX[u]))
write(A / "models/item/upgrade.json", dict(flat(UPGRADE_TEX["null"]), overrides=[
    {"predicate": {f"{MOD}:upgrade": (i + 1) / 10}, "model": f"{MOD}:item/upgrade/{u}"} for i, u in enumerate(UPGRADES)]))

# --- recipes (core/RecipesIronBackpacks.java) ---------------------------------------------------------------------
TAG = {"wool": {"tag": "minecraft:wool"}, "leather": {"tag": "c:leathers"}, "chestWood": {"tag": "c:chests/wooden"},
       "ingotIron": {"tag": "c:ingots/iron"}, "ingotGold": {"tag": "c:ingots/gold"}, "gemDiamond": {"tag": "c:gems/diamond"},
       "string": {"tag": "c:strings"}, "stickWood": {"tag": "c:rods/wooden"}, "paper": {"item": "minecraft:paper"},
       "bowl": {"item": "minecraft:bowl"}, "upgrade": {"item": f"{MOD}:upgrade"}, "dye": {"tag": "c:dyes"},
       "water_bucket": {"item": "minecraft:water_bucket"}}


# The backpack slot holds the plain backpack item; the recipe's "input_backpack" says which tier / specialty it must be
# (checked by the recipe, so the JSON is the same on NeoForge and Fabric).
BACKPACK = {"item": f"{MOD}:backpack"}


def backpack(t, spec):
    return {"backpack_type": f"{MOD}:{t}", "specialty": spec}


def upgrade_enabled(u):
    # both loaders' recipe conditions; each one ignores the other's key
    return {"neoforge:conditions": [{"type": f"{MOD}:upgrade_enabled", "upgrade": f"{MOD}:{u}"}],
            "fabric:load_conditions": [{"condition": f"{MOD}:upgrade_enabled", "upgrade": f"{MOD}:{u}"}]}


R = D / "recipe"
for f in R.glob("*.json") if R.exists() else []:
    f.unlink()


def tier(name, t, spec, pattern, key, input_backpack=None):
    r = {"type": f"{MOD}:backpack_tier", "category": "misc", "pattern": pattern, "key": key, "backpack_type": f"{MOD}:{t}", "specialty": spec}
    if input_backpack:
        r["input_backpack"] = input_backpack
    write(R / f"{name}.json", r)


tier("pack_basic", "basic", "none", ["WLW", "LCL", "WLW"], {"W": TAG["wool"], "L": TAG["leather"], "C": TAG["chestWood"]})
for t, prev, metal in (("iron", None, "ingotIron"), ("gold", "iron", "ingotGold")):
    for spec, c in (("storage", TAG["chestWood"]), ("upgrade", TAG["upgrade"])):
        b = backpack("basic", "none") if prev is None else backpack(prev, spec)
        tier(f"pack_{t}_{spec}", t, spec, ["ICI", "IBI", "III"], {"I": TAG[metal], "B": BACKPACK, "C": c}, b)
for spec, c in (("storage", TAG["chestWood"]), ("upgrade", TAG["upgrade"])):
    tier(f"pack_diamond_{spec}", "diamond", spec, ["DDD", "CBC", "DDD"], {"D": TAG["gemDiamond"], "B": BACKPACK, "C": c}, backpack("gold", spec))


def shaped(name, result, pattern, key, conditions=None):
    r = {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern, "key": key, "result": result}
    if conditions:
        r = {**conditions, **r}
    write(R / f"{name}.json", r)


shaped("upgrade_blank", {"id": f"{MOD}:upgrade", "count": 1}, ["SPS", "PWP", "SPS"],
       {"S": TAG["string"], "W": TAG["stickWood"], "P": TAG["paper"]})
for name, u, m in (("upgrade_damage_bar", "damage_bar", TAG["bowl"]), ("upgrade_latch", "lock", TAG["ingotGold"]),
                   ("upgrade_extra_upgrade", "extra_upgrade", TAG["leather"])):
    shaped(name, {"id": f"{MOD}:upgrade", "count": 1, "components": {f"{MOD}:upgrade": f"{MOD}:{u}"}}, ["MSM", "SCS", "MSM"],
           {"M": m, "S": TAG["string"], "C": TAG["upgrade"]}, upgrade_enabled(u))

# Color and decolor. 1.12.2 wrote the gold and diamond entries with PACK_BASIC (a basic backpack with a storage /
# upgrade emphasis, which can't exist), so only basic and iron backpacks could be dyed; fixed for the port (user
# request): every tier can be dyed.
COLOR = [("pack_basic", ("basic", "none"), ("basic", "none")),
         ("pack_iron_storage", ("iron", "storage"), ("iron", "storage")), ("pack_iron_upgrade", ("iron", "upgrade"), ("iron", "upgrade")),
         ("pack_gold_storage", ("gold", "storage"), ("gold", "storage")), ("pack_gold_upgrade", ("gold", "upgrade"), ("gold", "upgrade")),
         ("pack_diamond_storage", ("diamond", "storage"), ("diamond", "storage")), ("pack_diamond_upgrade", ("diamond", "upgrade"), ("diamond", "upgrade"))]
for operation, d in (("color", TAG["dye"]), ("decolor", TAG["water_bucket"])):
    for name, (rt, rs), (it, isp) in COLOR:
        write(R / f"{name}_{operation}.json", {"type": f"{MOD}:backpack_color", "category": "misc", "pattern": ["BD"],
                                               "key": {"B": BACKPACK, "D": d}, "backpack_type": f"{MOD}:{rt}", "specialty": rs,
                                               "input_backpack": backpack(it, isp)})


# --- game test area ---------------------------------------------------------------------------------------------
# vanilla's empty 1x1x1 structure, 8x8x8 (data version 3955 = 1.21.1)
EMPTY_STRUCTURE = bytes.fromhex(
    "0a0000090004" "73697a65" "0300000003" "00000001" "00000001" "00000001"
    "090008" "656e746974696573" "0000000000"
    "090006" "626c6f636b73" "0a00000001" "090003" "706f73" "0300000003" "000000000000000000000000"
    "030005" "7374617465" "00000000" "00"
    "090007" "70616c65747465" "0a00000001" "080004" "4e616d65" "000d" "6d696e6563726166743a616972" "00"
    "03000b" "4461746156657273696f6e" "00000f73" "00")
size_at = EMPTY_STRUCTURE.index(bytes.fromhex("0300000003")) + 5
area = EMPTY_STRUCTURE[:size_at] + struct.pack(">3i", 8, 8, 8) + EMPTY_STRUCTURE[size_at + 12:]
(D / "structure").mkdir(parents=True, exist_ok=True)
with gzip.GzipFile(D / "structure/gametest_area.nbt", "wb", mtime=0) as f:
    f.write(area)

print("resources written to", res)
