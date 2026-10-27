# Classes

Every recruit has one of these classes. The numbers are the 2.0.0 defaults from the class files the mod writes to
`config/better_with_gold/classes/` on first start; edit those files (or add new ones) and run `/fellowship reload`.

| Class | Role | Health | Damage | Armour | Speed | Hire (gold) | Weapon |
|---|---|---|---|---|---|---|---|
| **Bulwark** | Tank | 30 (+3/level) | 3 (+0.3/level) | 6 (+0.3/level) | 0.28 | 8 (+2/level) | `minecraft:iron_sword` + `minecraft:shield` |
| **Skirmisher** | Melee | 22 (+2/level) | 5 (+0.5/level) | 2 (+0.2/level) | 0.33 | 7 (+2/level) | `better_with_gold:golden_scythe` |
| **Ranger** | Ranged | 20 (+2/level) | 4 (+0.45/level) | 1 (+0.15/level) | 0.31 | 7 (+2/level) | `minecraft:bow` |
| **Mender** | Healer | 20 (+2/level) | 2 (+0.2/level) | 2 (+0.2/level) | 0.3 | 10 (+3/level) | `minecraft:book` |
| **Arcanist** | Caster | 18 (+1.5/level) | 5 (+0.5/level) | 0 (+0.1/level) | 0.3 | 12 (+3/level) | `minecraft:blaze_rod` |

## Abilities

- **Bulwark: Challenge.** Taunts every enemy within 8 blocks that is attacking someone else: they turn on the Bulwark for 6 seconds.
- **Skirmisher: Whirl.** Every 6 seconds, hits every enemy within 3 blocks for 60% damage.
- **Ranger: Volley.** Every 10 seconds, looses 3 arrows at once in a spread.
- **Mender: Mend.** Every 3 seconds, heals the most hurt ally or leader within 12 blocks for 4 health, plus 0.25 per level. Revives the downed.
- **Arcanist: Arc Storm.** Every 12 seconds, strikes every enemy within 5 blocks of its target for 120% damage.

## Writing a class

A class file picks one of five roles and sets numbers; the role decides how the ally fights:

| Role | Behaviour | `abilityPower` means |
|---|---|---|
| `TANK` | Melee; taunts enemies that attack someone else | Taunt length in seconds |
| `MELEE` | Melee; hits every enemy around it | Share of damage dealt to each enemy |
| `RANGED` | Keeps `preferredRange` blocks away and shoots arrows every `attackInterval` ticks | Arrows per volley |
| `HEALER` | Heals the most hurt ally or leader in `abilityRadius`; revives the downed | Health healed (+0.25 per level) |
| `CASTER` | Keeps its distance; magic bolts; a storm on packed enemies | Share of damage for the storm |

Example: a minimal new class (everything else uses safe defaults).

```json
{
  "id": "sentinel",
  "name": "Sentinel",
  "role": "TANK",
  "color": "#8899AA",
  "baseHealth": 40,
  "mainHand": "minecraft:iron_axe",
  "offHand": "minecraft:shield",
  "abilityName": "Hold Fast",
  "abilityDescription": "Taunts nearby enemies.",
  "traits": [
    "steadfast",
    "hardy"
  ]
}
```

Skins for a new class go in a resource pack at `assets/better_with_gold/textures/entity/ally/<id>_<n>.png`
(64x64 player layout, `n` from 0 to `skins - 1`). Without one, the Bulwark skin is used.
