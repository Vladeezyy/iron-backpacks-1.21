#!/usr/bin/env python3
"""Builds src/main/resources/{assets,data} for the 1.21.1 port from the original Iron Backpacks 1.12.2 resources.

    python3 -I tools/gen_resources.py <IronBackpacks 1.12 checkout>/src/main/resources/assets/ironbackpacks src/main/resources

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

# --- item models --------------------------------------------------------------------------------------------------
# property ironbackpacks:variant (client/ClientEventHandler.VARIANT_MODELS): 0 = no / unknown variant ("Torn Backpack")
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
    {"predicate": {f"{MOD}:variant": i + 1}, "model": f"{MOD}:item/backpack/{t}/{spec}"} for i, (t, spec) in enumerate(VARIANTS)]))

# property ironbackpacks:upgrade (client/ClientEventHandler.UPGRADE_MODELS): 0 = blank / unknown upgrade
UPGRADES = ["damage_bar", "lock", "extra_upgrade", "everlasting"]


def flat(tex):
    return {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{tex}"}}


write(A / "models/item/upgrade/null.json", flat(UPGRADE_TEX["null"]))
for u in UPGRADES:
    write(A / f"models/item/upgrade/{u}.json", flat(UPGRADE_TEX[u]))
write(A / "models/item/upgrade.json", dict(flat(UPGRADE_TEX["null"]), overrides=[
    {"predicate": {f"{MOD}:upgrade": i + 1}, "model": f"{MOD}:item/upgrade/{u}"} for i, u in enumerate(UPGRADES)]))

# --- recipes (core/RecipesIronBackpacks.java) ---------------------------------------------------------------------
TAG = {"wool": {"tag": "minecraft:wool"}, "leather": {"tag": "c:leathers"}, "chestWood": {"tag": "c:chests/wooden"},
       "ingotIron": {"tag": "c:ingots/iron"}, "ingotGold": {"tag": "c:ingots/gold"}, "gemDiamond": {"tag": "c:gems/diamond"},
       "string": {"tag": "c:strings"}, "stickWood": {"tag": "c:rods/wooden"}, "paper": {"item": "minecraft:paper"},
       "bowl": {"item": "minecraft:bowl"}, "upgrade": {"item": f"{MOD}:upgrade"}, "dye": {"tag": "c:dyes"},
       "water_bucket": {"item": "minecraft:water_bucket"}}


def backpack(t, spec):
    return {"type": f"{MOD}:backpack", "backpack_type": f"{MOD}:{t}", "specialty": spec}


def upgrade_enabled(u):
    return [{"type": f"{MOD}:upgrade_enabled", "upgrade": f"{MOD}:{u}"}]


R = D / "recipe"
for f in R.glob("*.json") if R.exists() else []:
    f.unlink()


def tier(name, t, spec, pattern, key):
    write(R / f"{name}.json", {"type": f"{MOD}:backpack_tier", "category": "misc", "pattern": pattern,
                               "key": key, "backpack_type": f"{MOD}:{t}", "specialty": spec})


tier("pack_basic", "basic", "none", ["WLW", "LCL", "WLW"], {"W": TAG["wool"], "L": TAG["leather"], "C": TAG["chestWood"]})
for t, prev, metal in (("iron", None, "ingotIron"), ("gold", "iron", "ingotGold")):
    for spec, c in (("storage", TAG["chestWood"]), ("upgrade", TAG["upgrade"])):
        b = backpack("basic", "none") if prev is None else backpack(prev, spec)
        tier(f"pack_{t}_{spec}", t, spec, ["ICI", "IBI", "III"], {"I": TAG[metal], "B": b, "C": c})
for spec, c in (("storage", TAG["chestWood"]), ("upgrade", TAG["upgrade"])):
    tier(f"pack_diamond_{spec}", "diamond", spec, ["DDD", "CBC", "DDD"], {"D": TAG["gemDiamond"], "B": backpack("gold", spec), "C": c})


def shaped(name, result, pattern, key, conditions=None):
    r = {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern, "key": key, "result": result}
    if conditions:
        r = {"neoforge:conditions": conditions, **r}
    write(R / f"{name}.json", r)


shaped("upgrade_blank", {"id": f"{MOD}:upgrade", "count": 1}, ["SPS", "PWP", "SPS"],
       {"S": TAG["string"], "W": TAG["stickWood"], "P": TAG["paper"]})
for name, u, m in (("upgrade_damage_bar", "damage_bar", TAG["bowl"]), ("upgrade_latch", "lock", TAG["ingotGold"]),
                   ("upgrade_extra_upgrade", "extra_upgrade", TAG["leather"])):
    shaped(name, {"id": f"{MOD}:upgrade", "count": 1, "components": {f"{MOD}:upgrade": f"{MOD}:{u}"}}, ["MSM", "SCS", "MSM"],
           {"M": m, "S": TAG["string"], "C": TAG["upgrade"]}, upgrade_enabled(u))

# Color and decolor. 1.12.2 quirk kept: the gold and diamond entries were written with PACK_BASIC, so they ask for a
# basic backpack with a storage / upgrade emphasis (which can't exist) and never match.
COLOR = [("pack_basic", ("basic", "none"), ("basic", "none")),
         ("pack_iron_storage", ("iron", "storage"), ("iron", "storage")), ("pack_iron_upgrade", ("iron", "upgrade"), ("iron", "upgrade")),
         ("pack_gold_storage", ("basic", "storage"), ("basic", "storage")), ("pack_gold_upgrade", ("basic", "upgrade"), ("basic", "upgrade")),
         ("pack_diamond_storage", ("basic", "storage"), ("basic", "storage")), ("pack_diamond_upgrade", ("basic", "upgrade"), ("basic", "upgrade"))]
for operation, d in (("color", TAG["dye"]), ("decolor", TAG["water_bucket"])):
    for name, (rt, rs), (it, isp) in COLOR:
        write(R / f"{name}_{operation}.json", {"type": f"{MOD}:backpack_color", "category": "misc", "pattern": ["BD"],
                                               "key": {"B": backpack(it, isp), "D": d}, "backpack_type": f"{MOD}:{rt}", "specialty": rs})


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
