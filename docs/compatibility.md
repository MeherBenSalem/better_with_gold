# Compatibility

## Versions and loaders

| Minecraft | Loaders | Java | Needs |
|---|---|---|---|
| 1.20.1 | Fabric, Forge | 17 | Fabric API on Fabric |
| 1.21.1 | Fabric, NeoForge, Forge | 21 | Fabric API on Fabric |
| 26.2 | Fabric, NeoForge | 25 | Fabric API on Fabric |
| 26.3 | Fabric, NeoForge | 25 | Fabric API on Fabric |

Install it on the server and on every client (it adds an entity, a key binding, the HUD and two screens). Nothing
else is required.

## Upgrading from Better With Gold 1.x

The mod id is still `better_with_gold`, so 1.x worlds keep every Gilded Regalia item (the 17 gold tools, weapons,
armour pieces, the upgrade template and the Golden Fish). Their crafting and smithing recipes, missing since 1.2,
are back.

## Other mods

- **Remnants | RPG Stats, Skills & Progression (RPG Attribute System):** allies cannot out-level your RPG level
  (at least level 5). Turn it off with `rpgLevelCap`.
- **Remnants: RPG Mobs, Remnants | Skills and Powers, Mob Loot Bags:** allies are vanilla "owned" entities, which
  these mods already treat as your allies.
- **Bosses:** anything tagged `c:bosses`, the vanilla bosses, and anything with 150 or more maximum health counts
  as a boss for the Chronicle and for the Arcanist's Arc Storm.
- **Gear mods:** allies wear and wield real items and use plain attributes, so modded weapons and armour work.
- **Modded hostile mobs:** allies fight anything hostile, and anything that targets you or your band.
