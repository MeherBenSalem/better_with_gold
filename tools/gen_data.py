#!/usr/bin/env python3
"""Writes the data files that differ in format between Minecraft versions: crafting and smithing recipes for the
Gilded Regalia and the Warbanner, the vanilla tool tags, and the 1.21.4+ item model definitions.

The recipes restore the original Better With Gold 1.0 recipes, lost in the 1.2 multi-loader conversion.

    python3 tools/gen_data.py
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
NS = "better_with_gold"

SHAPED = {
    "upgrade_template": (["aba", "aca", "aaa"], {"a": "minecraft:gold_ingot", "b": "minecraft:obsidian", "c": "minecraft:amethyst_shard"}, 1),
    "golden_fish": (["aaa", "aba", "aaa"], {"a": "minecraft:gold_ingot", "b": "minecraft:cooked_cod"}, 1),
    "golden_scythe": (["aaa", "ab ", " b "], {"a": "minecraft:gold_ingot", "b": "minecraft:stick"}, 1),
    "greater_golden_hammer": (["a a", "aba", " b "], {"a": "minecraft:gold_block", "b": "minecraft:stick"}, 1),
    "greater_golden_axe": (["aa", "ab", " b"], {"a": "minecraft:gold_block", "b": "minecraft:iron_ingot"}, 1),
    "warbanner": ([" g ", "gbg", " s "], {"g": "minecraft:gold_ingot", "b": "minecraft:black_banner", "s": "minecraft:stick"}, 1),
}

# Smithing: upgrade template + base + amethyst block.
SMITHING = {
    "reinforced_golden_sword": "minecraft:golden_sword",
    "reinforced_golden_pick_axe": "minecraft:golden_pickaxe",
    "reinforced_golden_axe": "minecraft:golden_axe",
    "reinforced_golden_shovel": "minecraft:golden_shovel",
    "reinforced_golden_hoe": "minecraft:golden_hoe",
    "reinforced_golden_armor_helmet": "minecraft:golden_helmet",
    "reinforced_golden_armor_chestplate": "minecraft:golden_chestplate",
    "reinforced_golden_armor_leggings": "minecraft:golden_leggings",
    "reinforced_golden_armor_boots": "minecraft:golden_boots",
    "reinforced_golden_scythe": NS + ":golden_scythe",
    "reinforced_greater_golden_axe": NS + ":greater_golden_axe",
    "reinforced_greater_golden_hammer": NS + ":greater_golden_hammer",
}

TAGS = {
    "swords": ["golden_scythe", "reinforced_golden_scythe", "reinforced_golden_sword"],
    "axes": ["reinforced_golden_axe", "greater_golden_axe", "reinforced_greater_golden_axe", "greater_golden_hammer", "reinforced_greater_golden_hammer"],
    "pickaxes": ["reinforced_golden_pick_axe"],
    "shovels": ["reinforced_golden_shovel"],
    "hoes": ["reinforced_golden_hoe"],
    "head_armor": ["reinforced_golden_armor_helmet"],
    "chest_armor": ["reinforced_golden_armor_chestplate"],
    "leg_armor": ["reinforced_golden_armor_leggings"],
    "foot_armor": ["reinforced_golden_armor_boots"],
}

ITEMS = list(SHAPED) + list(SMITHING)

# version root -> (recipe folder, tag folder, ingredient style, result key)
VERSIONS = {
    "1.20.1": ("recipes", "items", "object", "item"),
    "1.21.1": ("recipe", "item", "object", "id"),
    "26.2": ("recipe", "item", "string", "id"),
    "26.3": ("recipe", "item", "string", "id"),
}


def ingredient(item, style):
    return {"item": item} if style == "object" else item


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n")


def main():
    for root, (recipe_dir, tag_dir, style, result_key) in VERSIONS.items():
        data = ROOT / root / "common/src/main/resources/data"
        recipes = data / NS / recipe_dir
        for name, (pattern, key, count) in SHAPED.items():
            write(recipes / f"{name}.json", {
                "type": "minecraft:crafting_shaped",
                "category": "equipment" if name != "golden_fish" else "misc",
                "pattern": pattern,
                "key": {k: ingredient(v, style) for k, v in key.items()},
                "result": {result_key: f"{NS}:{name}", "count": count},
            })
        for name, base in SMITHING.items():
            write(recipes / f"{name}.json", {
                "type": "minecraft:smithing_transform",
                "template": ingredient(f"{NS}:upgrade_template", style),
                "base": ingredient(base, style),
                "addition": ingredient("minecraft:amethyst_block", style),
                "result": {result_key: f"{NS}:{name}"} if root != "1.20.1" else {"item": f"{NS}:{name}"},
            })
        for tag, items in TAGS.items():
            write(data / "minecraft/tags" / tag_dir / f"{tag}.json", {"replace": False, "values": [f"{NS}:{i}" for i in items]})
        if root in ("26.2", "26.3", "1.21.1"):
            items_dir = ROOT / root / "common/src/main/resources/assets" / NS / "items"
            write(items_dir / "warbanner.json", {"model": {"type": "minecraft:model", "model": f"{NS}:item/warbanner"}})
    write(ROOT / "shared-resources/assets" / NS / "models/item/warbanner.json",
          {"parent": "item/generated", "textures": {"layer0": f"{NS}:item/warbanner"}})
    print("data written for", ", ".join(VERSIONS))


if __name__ == "__main__":
    main()
