# Configuration

`config/better_with_gold/fellowship.json` is written on first server start with every option. Values outside a safe
range are clamped; a broken file falls back to the defaults. Run `/fellowship reload` after editing.

| Option | Default | What it does |
|---|---|---|
| `partySize` | `6` | Allies one leader can have at once (hired and stowed together). |
| `worldAllyCap` | `200` | Most allies of all leaders together that can be loaded at once; hiring and sellswords stop there. |
| `levelCap` | `20` | The highest level an ally can reach. |
| `bleedOutSeconds` | `30` | Seconds a downed ally lasts before it bleeds out. |
| `permadeath` | `false` | When true, an ally that bleeds out is gone for good and joins the Remembrance list. |
| `reviveSeconds` | `3` | Seconds a player must crouch beside a downed ally to revive it. |
| `menderReviveSeconds` | `5` | Seconds a Mender needs to revive a downed ally. |
| `recoverySeconds` | `300` | Seconds a bled-out ally rests at camp before it can be summoned again (permadeath off). |
| `sellswordsSpawn` | `true` | Sellswords for hire turn up near village bells. |
| `sellswordIntervalSeconds` | `180` | How often, per player, the game tries to place a sellsword near a village. |
| `sellswordChance` | `0.5` | Chance (0 to 1) that each try places a sellsword. |
| `maxSellswordsNearVillage` | `3` | Most sellswords that wait at one village at a time. |
| `sellswordStayMinutes` | `40` | Minutes an unhired sellsword waits before moving on. |
| `priceMultiplier` | `1.0` | Multiplies every hiring price. |
| `currency` | `minecraft:gold_ingot` | The item that pays for hiring. |
| `friendlyFire` | `false` | Leaders can hurt their own allies. |
| `allowPvp` | `false` | Allies fight players who attack their leader. |
| `aggroLabels` | `true` | Shows, above each engaged enemy, who holds its aggro. |
| `aggroSwitchRatio` | `1.1` | An enemy switches target only when someone beats its current target's aggro by this much. |
| `teleportDistance` | `40` | Following allies further than this many blocks from the leader teleport to it. |
| `regaliaDamagePerPiece` | `0.03` | Each piece of Reinforced Golden Armour the leader wears adds this to allies' damage, within 24 blocks. |
| `regaliaMoraleFloorPerPiece` | `5` | Each piece also raises the lowest morale allies can fall to. |
| `rpgLevelCapFloor` | `5` | With RPG Attribute System installed, allies cannot out-level the leader's RPG level (at least this). |
| `rpgLevelCap` | `true` | Turns the RPG Attribute System level cap on or off. |

Class files are described in [classes.md](classes.md).
