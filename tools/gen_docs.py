#!/usr/bin/env python3
"""Generates the reference pages in docs/ from the mod itself, so the numbers in the docs and on the store page
always match the defaults:

- docs/classes.md from the class files the mod writes on first start (config/better_with_gold/classes/*.json),
- docs/configuration.md from FellowshipConfig.java (defaults and the comment above each field),
- docs/traits.md from Trait.java,
- docs/reference/ copies of the generated config files.

    python3 tools/gen_docs.py [path/to/run/config/better_with_gold]

Start a dev server once (tools/qa/devrun.sh) to have the config folder; the default path is the 1.21.1 NeoForge one.
"""
import json
import re
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "1.21.1/common/src/main/java/tn/nightbeam/better_with_gold/core"
DOCS = ROOT / "docs"
ORDER = ["bulwark", "skirmisher", "ranger", "mender", "arcanist"]


def fmt(v):
    if isinstance(v, float):
        return ("%.2f" % v).rstrip("0").rstrip(".")
    return str(v)


def classes(config_dir):
    files = {p.stem: json.loads(p.read_text()) for p in (config_dir / "classes").glob("*.json")}
    ids = [i for i in ORDER if i in files] + sorted(i for i in files if i not in ORDER)
    lines = ["# Classes", "",
             "Every recruit has one of these classes. The numbers are the 2.0.0 defaults from the class files the mod writes to",
             "`config/better_with_gold/classes/` on first start; edit those files (or add new ones) and run `/fellowship reload`.",
             "", "| Class | Role | Health | Damage | Armour | Speed | Hire (gold) | Weapon |", "|---|---|---|---|---|---|---|---|"]
    for i in ids:
        c = files[i]
        health = f"{fmt(c['baseHealth'])} (+{fmt(c['healthPerLevel'])}/level)"
        damage = f"{fmt(c['baseDamage'])} (+{fmt(c['damagePerLevel'])}/level)"
        armor = f"{fmt(c['armor'])} (+{fmt(c['armorPerLevel'])}/level)"
        hire = f"{c['price']} (+{c['pricePerLevel']}/level)"
        lines.append(f"| **{c['name']}** | {c['role'].title()} | {health} | {damage} | {armor} | {fmt(c['speed'])} | {hire} | `{c['mainHand']}`"
                     + (f" + `{c['offHand']}`" if c.get("offHand") else "") + " |")
    lines += ["", "## Abilities", ""]
    for i in ids:
        c = files[i]
        lines.append(f"- **{c['name']}: {c['abilityName']}.** {c['abilityDescription']}")
    lines += ["", "## Writing a class", "",
              "A class file picks one of five roles and sets numbers; the role decides how the ally fights:", "",
              "| Role | Behaviour | `abilityPower` means |", "|---|---|---|",
              "| `TANK` | Melee; taunts enemies that attack someone else | Taunt length in seconds |",
              "| `MELEE` | Melee; hits every enemy around it | Share of damage dealt to each enemy |",
              "| `RANGED` | Keeps `preferredRange` blocks away and shoots arrows every `attackInterval` ticks | Arrows per volley |",
              "| `HEALER` | Heals the most hurt ally or leader in `abilityRadius`; revives the downed | Health healed (+0.25 per level) |",
              "| `CASTER` | Keeps its distance; magic bolts; a storm on packed enemies | Share of damage for the storm |",
              "", "Example: a minimal new class (everything else uses safe defaults).", "", "```json",
              json.dumps({"id": "sentinel", "name": "Sentinel", "role": "TANK", "color": "#8899AA", "baseHealth": 40,
                          "mainHand": "minecraft:iron_axe", "offHand": "minecraft:shield", "abilityName": "Hold Fast",
                          "abilityDescription": "Taunts nearby enemies.", "traits": ["steadfast", "hardy"]}, indent=2), "```", "",
              "Skins for a new class go in a resource pack at `assets/better_with_gold/textures/entity/ally/<id>_<n>.png`",
              "(64x64 player layout, `n` from 0 to `skins - 1`). Without one, the Bulwark skin is used.", ""]
    (DOCS / "classes.md").write_text("\n".join(lines))


def configuration():
    src = (SRC / "FellowshipConfig.java").read_text()
    rows = []
    comment = ""
    for line in src.splitlines():
        line = line.strip()
        c = re.fullmatch(r"/\*\*\s*(.*?)\s*\*/", line)
        f = re.fullmatch(r"public (int|double|boolean|String) (\w+) = ([^;]+);", line)
        if c:
            comment = c.group(1)
        elif f:
            rows.append((f.group(2), f.group(3).strip().strip('"'), comment))
            comment = ""
        else:
            comment = ""
    lines = ["# Configuration", "",
             "`config/better_with_gold/fellowship.json` is written on first server start with every option. Values outside a safe",
             "range are clamped; a broken file falls back to the defaults. Run `/fellowship reload` after editing.", "",
             "| Option | Default | What it does |", "|---|---|---|"]
    for name, value, comment in rows:
        lines.append(f"| `{name}` | `{value}` | {comment} |")
    lines += ["", "Class files are described in [classes.md](classes.md).", ""]
    (DOCS / "configuration.md").write_text("\n".join(lines))


def traits():
    src = (SRC / "Trait.java").read_text()
    lines = ["# Traits", "", "Every recruit rolls one or two traits from its class's list; the first one gives its epithet.", "",
             "| Trait | Effect | Epithets |", "|---|---|---|"]
    for m in re.finditer(r'\n\t(\w+)\("([^"]+)", "([^"]+)", new String\[\]\{([^}]*)\}\)', src):
        _, label, desc, epithets = m.groups()
        lines.append(f"| **{label}** | {desc} | {', '.join(e.strip().strip(chr(34)) for e in epithets.split(','))} |")
    lines.append("")
    (DOCS / "traits.md").write_text("\n".join(lines))


def main():
    config_dir = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "1.21.1/neoforge/runs/server/config/better_with_gold"
    if not (config_dir / "fellowship.json").exists():
        sys.exit(f"no generated config in {config_dir}: start a dev server once")
    ref = DOCS / "reference"
    if ref.exists():
        shutil.rmtree(ref)
    shutil.copytree(config_dir, ref)
    classes(config_dir)
    configuration()
    traits()
    print("docs written")


if __name__ == "__main__":
    main()
