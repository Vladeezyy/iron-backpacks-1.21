"""Turns the 1.21.1 resources (tools/gen_resources.py on main) into the 26.x format, in place.

python3 -I tools/convert_26.py common/src/main/resources

- recipes: ingredients are strings ("minecraft:x", "#tag") instead of {"item": ...} / {"tag": ...};
- item models: the overrides go, assets/<mod>/items/*.json pick the models by the ironbackpacks:variant and
  ironbackpacks:upgrade select properties and tint the trim with ironbackpacks:backpack_color;
- lang: the key category is key.category.ironbackpacks.main (26.x categories are ids).
Running it again changes nothing.
"""
import json
import pathlib
import sys

root = pathlib.Path(sys.argv[1])
assets = root / "assets" / "ironbackpacks"


def dump(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8", newline="\n")


def ingredient(value):
    if isinstance(value, list):
        return [ingredient(v) for v in value]
    if isinstance(value, dict):
        if "item" in value:
            return value["item"]
        if "tag" in value:
            return "#" + value["tag"]
    return value


for recipe in sorted((root / "data" / "ironbackpacks" / "recipe").glob("*.json")):
    data = json.loads(recipe.read_text(encoding="utf-8"))
    if "key" in data:
        data["key"] = {k: ingredient(v) for k, v in data["key"].items()}
    if "ingredients" in data:
        data["ingredients"] = ingredient(data["ingredients"])
    dump(recipe, data)


def model(name, tinted):
    entry = {"type": "minecraft:model", "model": "ironbackpacks:item/" + name}
    if tinted:
        entry["tints"] = [{"type": "minecraft:constant", "value": -1}, {"type": "ironbackpacks:backpack_color"}]
    return entry


variants = sorted(p.relative_to(assets / "models" / "item" / "backpack").with_suffix("").as_posix()
                  for p in (assets / "models" / "item" / "backpack").glob("*/*.json"))
dump(assets / "items" / "backpack.json", {"model": {
    "type": "minecraft:select",
    "property": "ironbackpacks:variant",
    "cases": [{"when": v, "model": model("backpack/" + v, True)} for v in variants],
    "fallback": model("backpack/null", True),
}})

upgrades = sorted(p.stem for p in (assets / "models" / "item" / "upgrade").glob("*.json") if p.stem != "null")
dump(assets / "items" / "upgrade.json", {"model": {
    "type": "minecraft:select",
    "property": "ironbackpacks:upgrade",
    "cases": [{"when": "ironbackpacks:" + u, "model": model("upgrade/" + u, False)} for u in upgrades],
    "fallback": model("upgrade/null", False),
}})

for name in ("backpack", "upgrade"):
    path = assets / "models" / "item" / (name + ".json")
    data = json.loads(path.read_text(encoding="utf-8"))
    data.pop("overrides", None)
    dump(path, data)

for lang in sorted((assets / "lang").glob("*.json")):
    data = json.loads(lang.read_text(encoding="utf-8"))
    if "key.categories.ironbackpacks" in data:
        out = {}
        for k, v in data.items():
            out["key.category.ironbackpacks.main" if k == "key.categories.ironbackpacks" else k] = v
        dump(lang, out)
