# Market Analysis: Remnants | Fellowship

Competitive research for rebuilding **Better With Gold** (current listing: a small gold-gear mod) into
**Remnants | Fellowship**: a companions, party and warband RPG mod. You hire named sellswords with gold ingots near
villages. They level to 20, take one of five classes (Vanguard, Skirmisher, Ranger, Mender, Arcanist), carry 1–2
traits and a morale value, and keep a **Chronicle** written from what they did. Mobs choose targets by an aggro
(threat) table. Allies go down instead of dying (revive ring, bleed-out timer; optional permadeath with a
Remembrance list). You lead them with a command wheel and a planted **Warbanner**. Gold is loyalty: legacy gold
armour boosts allies. The mod id stays `better_with_gold`. Target platforms: 1.20.1 (Fabric, Forge), 1.21.1 (Fabric,
NeoForge, Forge), 26.2 and 26.3 (Fabric, NeoForge). No dependencies except Fabric API on Fabric.

- **Research date:** 2026-10-27.
- **Download counts:** Modrinth numbers come from the public API (`https://api.modrinth.com/v2/project/<slug>`).
  CurseForge numbers come from the CFWidget mirror (`https://api.cfwidget.com/minecraft/mc-mods/<slug>`, or
  `https://api.cfwidget.com/<id>` for the studio's own projects). Both were fetched on 2026-10-27. CFWidget can lag
  CurseForge a little, and it returned 404 for some slugs (Taterzens, Aggro Indicator); those cells say "not found".
  Numbers are rounded in the prose and exact in the tables.
- **Versions and loaders** come from each project's version or file list, not from the description, unless noted.
  "26.x search" means a Modrinth search filtered to mods tagged 26.1.2, 26.2 or 26.3.
- **Complaints** come from GitHub issue and pull-request pages, mod descriptions and FAQs, and from add-ons that exist
  just to patch a gap (an add-on's downloads are used as a demand signal). The GitHub REST API was blocked for this
  session, so issue pages were read on github.com directly. **Villager Recruits has GitHub Issues turned off**, so its
  evidence comes from its pull requests and add-ons. CurseForge comment threads load through JavaScript and are **not**
  covered.
- Anything that could not be checked against a primary source is marked **unverified**.

---

## TL;DR

1. **The ally category is large, and its leaders stop before 26.x.**
   - Human Companions: 17.4M CurseForge downloads, Forge alone, newest 1.20.1, last file 2024-12-15.
   - Villager Recruits: 2.89M CurseForge and 467k Modrinth, Forge alone, newest 1.20.1. Three NeoForge 1.21.1 port pull
     requests are unmerged; the latest was closed on 2026-10-24.
   - MineColonies (89.8M CF, up to 1.21.1), Modern Companions (Human Companions' NeoForge 1.21.1 port), Taterzens (up
     to 1.21.6) and Mebahel's Villager Quests & Companions (up to 1.21.1) have no 26.x build either.

   On 26.2/26.3 we found village guards (Guard Villagers), a village army (Millager), tamed mobs (Petting, Tame Every
   Mob), a single romance companion (Girlfriend Mod) and two tiny Fabric recruit mods (Champion Companions, 104
   downloads; Mercenaries, 84). As of 2026-10-27 **we found no 26.2/26.3 mod where you recruit, level and command a
   party of classed adventurers**, and none on Fabric and NeoForge together.
2. **Roles exist, but in pieces.** Modern Companions (NeoForge 1.21.1) already has a Vanguard with "monster
   taunts", a healing Cleric, a Necromancer, 14 traits, Morale, Bond and a stats Journal. Millager (26.2) has a taunting
   Rioter and a Lancer with a healing aura, but they are a village army, not your party. Fellowship must not be
   pitched as a new tank/healer setup. Its distinct parts are: an aggro table you can see over every engaged mob,
   a downed state for allies, a command wheel with formations and a Warbanner, and one dependency-free feature set
   on nine loader and version combinations.
3. **The same complaints repeat across mods** (section 2):
   - Followers get lost through portals, boats and chunk unloads.
   - Death is either cheap or final, with nothing in between.
   - Targeting ignores modded mobs or hits the wrong side.
   - Many NPCs cost TPS and save time.
   - Mods are stuck on old versions.
   - Commands are buried in inventory screens.

   Demand signals: Companion (2.14M MR) and Followers Teleport Too (490k MR) exist just to keep pets with you. Aggro
   Indicator (336k MR) shows which mobs target you. The revive mods (Hardcore Revival 10.2M MR, PlayerRevive 4.80M
   MR) cover players and nobody else.
4. **It fits the studio without code coupling.** Remnants | RPG Mobs, Remnants | Skills and Powers and Mob Loot Bags
   all recognise "your ally" through vanilla `OwnableEntity#getOwnerUUID` (checked in their source). If Fellowship
   allies implement it, they skip RPG Mobs levelling, are spared and buffed by Skills and Powers, and give Loot Bag
   kill credit, all with no new code in those mods. One term clashes: RPG Mobs already uses "threat" for an area's
   danger level (`/remnants threat`). Call Fellowship's system **aggro** in the UI.
5. **Name:** **Remnants | Fellowship**, with conditions (section 5). It is clean as a Minecraft mod title: 0 mods on
   Modrinth, a few small modpacks on either site, and no Minecraft mod repository on GitHub. Outside Minecraft,
   *Fellowship* is a 2025 Steam game (Chief Rebel) built on DPS/Tank/Healer heroes. "Retinue" collides inside the
   niche (a paid Minecraft companion plugin and a Hytale companion mod). "Oathsworn" collides with *Oathsworn
   Paladins* (445k CF).

---

## 1. Comparison tables

### 1a. Reach, platforms and dependencies

"26.x" means a published build for 26.1, 26.2 or 26.3. **Bold** marks a 26.2 or 26.3 build.

| Mod (author) | Loaders | MC versions (from file lists) | 26.x? | Downloads CF / MR | Required dependencies |
|---|---|---|---|---|---|
| **Human Companions** (justinwon777) | Forge | 1.16.5, 1.18.2, 1.19.2, 1.20.1. Last file 2024-12-15. | No | 17,429,599 / not on MR (issue [#57](https://github.com/justinwon777/HumanCompanions/issues/57) asks) | None found (?) ([CF](https://www.curseforge.com/minecraft/mc-mods/human-companions)) |
| **Villager Recruits** (talhanation) | Forge | CF 1.16.3 → 1.20.1; MR 1.18.2 → 1.20.1. Latest 1.15.2 (2026-06-29). | No. NeoForge 1.21.1 port PRs [#68](https://github.com/talhanation/recruits/pull/68), [#77](https://github.com/talhanation/recruits/pull/77), [#78](https://github.com/talhanation/recruits/pull/78) are unmerged | 2,887,572 / 467,047 | None listed on MR. PR #77 says "corelib bundled", so 1.20.1 may need talhanation's CoreLib (**unverified**) ([MR](https://modrinth.com/mod/villager-recruits)) |
| **Modern Companions** (Modern-Mods), a port and rebrand of Human Companions | NeoForge | 1.21.1 alone. Latest 4.60 on CF (2026-08-25). | No | 24,678 / 10,504 | None. Optional: Iron's Spells, Ars Nouveau, TacZ, Curios, Sophisticated Backpacks ([MR](https://modrinth.com/mod/modern-companions)) |
| **MineColonies** (LDTTeam), a colony builder | Forge, NeoForge | Up to 1.21.1 (latest file 2026-10-21) | No | 89,778,894 / 34,507 (an old 1.18.2 MR listing) | Not checked ([CF](https://www.curseforge.com/minecraft/mc-mods/minecolonies)) |
| **Guard Villagers** (seymourimadeit) | Forge, NeoForge | 1.14.4 → **26.3**; 26.x is NeoForge alone | Yes (NeoForge) | 93,142,609 / 13,971,414 | None ([MR](https://modrinth.com/mod/guard-villagers)) |
| Guard Villagers (Fabric/Quilt), a separate port | Fabric, Quilt | 1.18.2 → **26.2** | Yes (Fabric) | – / 5,630,754 | None ([MR](https://modrinth.com/mod/guard-villagers-fabricquilt)) |
| **Taterzen NPCs** (samolego), a server NPC tool | Fabric (Forge to 1.19, Quilt to 1.20.1) | 1.16.5 → 1.21.6. Last update 2025-06-26. | No | not found / 136,206 | Fabric API. Entity-type editing depends on the abandoned DisguiseLib ([#171](https://github.com/samolego/Taterzens/issues/171)) |
| **Easy NPC** (MarkusBordihn), an NPC authoring tool | Fabric, Forge, NeoForge, Quilt | 1.18.2 → **26.3** | Yes | 8,735,480 / 1,290,493 | Easy NPC: Core and Easy NPC: Config UI ([MR](https://modrinth.com/mod/easy-npc)) |
| Mebahel's RPG: Villager Quests & Companions | Fabric, Quilt; Forge and NeoForge through Sinytra Connector, per its page | 1.20 → 1.21.1 | No | 23,887 / 16,343 | Connector and Forgified Fabric API on Forge/NeoForge ([MR](https://modrinth.com/mod/mebahels-rpg-villager-quests-and-companions)) |
| **Millager (Village Defenders)** (LZYZL6) | Fabric, NeoForge (Forge to 1.20.1) | 1.20.1 → **26.2** | Yes | 101,151 / 29,828 | Fabric API ([MR](https://modrinth.com/mod/millager)) |
| **Petting - Tame any mob!** (yigitguven) | NeoForge, Forge (Fabric to 1.20.1) | 1.19.4 → **26.3** | Yes (NeoForge, Forge) | 137,486 / 33,966 | None ([MR](https://modrinth.com/mod/petting)) |
| **Girlfriend Mod**, one companion NPC | Fabric, NeoForge | 1.21.9 → **26.3** | Yes | CF not checked / 44,649 | Fabric API ([MR](https://modrinth.com/mod/girlfriend)) |
| Champion Companions | Fabric | **26.2** alone | Yes | – / 104 | Fabric API, GeckoLib ([MR](https://modrinth.com/mod/champion-companions)) |
| Mercenaries (Kniumm), "alpha quality" | Fabric | **26.2** alone | Yes | – / 84 | Fabric API ([MR](https://modrinth.com/mod/mercenaries)) |
| Companion 🐕 (snownee), keeps pets safe | Fabric, Forge, NeoForge, Quilt | 1.18.2 → 26.1.2 | 26.1.2 (no 26.2/26.3) | – / 2,139,681 | Kiwi ([MR](https://modrinth.com/mod/companion)) |
| **Followers Teleport Too** (Serilum) | Fabric, Forge, NeoForge, Quilt | 1.16.5 → **26.3** | Yes | not checked / 489,841 | Collective ([MR](https://modrinth.com/mod/followers-teleport-too)) |
| **Aggro Indicator** (Raverbury) | Fabric, NeoForge (Forge to 1.20.6) | 1.16.5 → **26.2** | Yes (no 26.3) | not found / 336,243 | Fabric API ([MR](https://modrinth.com/mod/aggroindicator)) |
| **Better Party** (MrStilu2k6), player parties | Fabric, Forge, NeoForge | 1.20.1 → **26.3** | Yes | 16,565 / 11,039 | Fabric API ([MR](https://modrinth.com/mod/better-party)) |
| Other player-party mods: Epic RPG: Party Hud; Odyssey Allies | Forge/NeoForge; all three loaders | 1.20.1 → 26.1.2; 1.20 → 1.21 | 26.1.2; No | – / 16,299; 1,032,800 / 1,169,059 | None; Resourceful Lib |
| **Hardcore Revival** (BlayTheNinth), players alone | Fabric, NeoForge (Forge to 1.21.11) | 1.12 → **26.3** | Yes | 7,540,065 / 10,213,685 | Balm, plus Fabric API on Fabric ([MR](https://modrinth.com/mod/hardcore-revival)) |
| **PlayerRevive** (CreativeMD), players alone | Forge, NeoForge | 1.18.2 → **26.3** (NeoForge) | Yes | not checked / 4,797,964 | CreativeCore ([MR](https://modrinth.com/mod/playerrevive)) |
| Incapacitated, players alone | Fabric, Forge, NeoForge, Quilt | 1.16.5 → 1.21.8 | No | 733,003 / 199,046 | None |
| *Ours today:* Better With Gold | Fabric, Forge, NeoForge | 1.16.5 → **26.3** | Yes | 6,916 / 1,889 | – ([CF](https://www.curseforge.com/minecraft/mc-mods/better-with-gold), [MR](https://modrinth.com/mod/better-with-gold)) |

Recruits add-ons (demand signals, all Forge 1.20.1):

| Add-on | Downloads | What it adds |
|---|---|---|
| [Villager Recruits & Workers: Advanced Grouping + Controls](https://www.curseforge.com/minecraft/mc-mods/villager-recruits-workers-advanced-grouping) | 8,249 CF | Collapsible group icons ("Large armies ... can quickly clutter the command screen"), drag-to-move deployment and army formations |
| [Recruits Morale Addon](https://www.curseforge.com/minecraft/mc-mods/recruits-morale-addon) | 6,271 CF | 0–100 morale, panic that ignores orders and breaks formation, and a morale bar |
| [No More Fool Recruits](https://modrinth.com/mod/no-more-fool-recruits) | 4,231 MR | Hazard-aware pathing, creeper panic, a fix for "Follow Me" indoors sending recruits "to the roof", and anti-stuck teleport |
| [Recruits Warium](https://modrinth.com/mod/recruits-warium) / [Superb Recruits](https://modrinth.com/mod/superb-recruits) | 9,106 / 11,270 MR | Let recruits use two gun mods' weapons |

### 1b. Features: recruiting, roles, death and command

Key: ✔ yes · ✖ no · ~ partial or through an add-on · ? not found or **unverified**

| Mod | How you get allies | Classes / roles | Progression | Identity and record | Death and revive | Commands | Aggro / taunt | Keeps up with you | Server notes |
|---|---|---|---|---|---|---|---|---|---|
| Human Companions | Give the food a wild companion asks for, at its house | ~ Knight, Archer, Axeguard, Arbalist. No tank or healer | ✔ Levels from kills; +1 health every 3 levels | ~ Random names; rename with tags | ✖ "they die easily"; dupes and lost items on death (listed known issue, [#26](https://github.com/justinwon777/HumanCompanions/issues/26), [#37](https://github.com/justinwon777/HumanCompanions/issues/37)) | ~ Toggles inside the inventory: follow, patrol, guard, hunt, alert; shift-click to sit | ✖ Attacks what hurts you or what you hit; runs from creepers | ✖ [#13](https://github.com/justinwon777/HumanCompanions/issues/13) portals, [#14](https://github.com/justinwon777/HumanCompanions/issues/14) boats. The page suggests a separate Pet Tracker mod | [#31](https://github.com/justinwon777/HumanCompanions/issues/31) one companion pushed the server to 90–120% CPU (fixed) |
| Villager Recruits | Emeralds; a Noble Villager recruits nearby villagers ([wiki](https://github.com/talhanation/wiki/wiki/Villager-Recruits-Wiki)) | ~ Recruit, Shieldman, Bowman, Crossbowman, Horseman, Nomad. They block with shields and eat food or potions | ? Not on the listing or wiki | ? | ? | ✔ Command screen: Move, Follow me, Hold, Back to position, Protect this, Hail of arrows, Shields; stances; groups. ~ Formations and morale come from add-ons | ✖ | ~ An add-on adds anti-stuck teleport | Async pathfinding ([PR #23](https://github.com/talhanation/recruits/pull/23)); authority-hardening PRs [#62](https://github.com/talhanation/recruits/pull/62)–#66 still open |
| Modern Companions | Bring the 2 requested stacks to a companion house | ✔ 14 roles, including Vanguard ("monster taunts"), Cleric (heals allies), Alchemist and Necromancer | ✔ STR/DEX/INT/END; "No Level Cap" | ✔ 14 traits, backstory, Morale, Bond, and a Journal of kills, resurrections and distance | ~ A Resurrection Scroll, activated with a Nether Star | ~ Companion screen: Follow, Patrol, Guard, Alert, Hunting, Sprint, Pickup, Radius | ~ Vanguard taunts; we found no aggro display (?) | ~ Summoning Wand recalls companions "in the current dimension"; Soul Gems store them | NeoForge 1.21.1 alone; friendly fire and PvP are configurable |
| Guard Villagers | 6 spawn per village; convert a nitwit with a sword or crossbow | ~ Sword or crossbow; clerics heal guards | ✖ | ✖ | ✖ | ~ Patrol. **Follow needs Hero of the Village** (or ring the bell) | ✖ | ? [#248](https://github.com/seymourimadeit/guardvillagers/issues/248) a following guard vanished | The FAQ answers "How do I make the mod less laggy?"; [#170](https://github.com/seymourimadeit/guardvillagers/issues/170) TPS buildup with about 8 guards (fixed) |
| Mebahel's Quests & Companions | Story outcomes (Rusk, Merrin) | ✖ Named characters | ? | ✔ Named, story-driven | ? Heals 10 s after combat | ✔ Follow, wait, return home, guard area, 3 stances | ✖ | ? | Story progress can be per player or shared |
| Millager | Muster Orders summon a 3–5 unit squad; Profession Orders convert a villager | ✔ 8 units, including Rioter (taunts), Lancer (healing aura) and Doctor (builds golems) | ? | ✖ | ✖ | ~ Items and orders; raid squads advance in formation behind a banner leader | ~ Rioter taunts | ? | Village side, not a personal party |
| Petting | Golden Treats tame any mob | ✖ Mob types, not classes | ? | ✖ | ✔ A collar bound to a bed cancels death and respawns the pet there | ✔ 5 collar modes plus toggles | ✖ | ✔ Teleport threshold | Uses gold for taming (see 4a) |
| Girlfriend Mod | One companion | ✖ | ✔ Relationship tiers | ✔ Chat, emotes, gifts | ~ At the top tier she "pulls you back from death" once a day (the player, not her) | ~ Chat: stay, follow | ✖ | ✔ "runs back to you" | One NPC per player |
| Champion Companions | Defeat a wild champion to recruit it | ✔ 4 champions: support, kiter, melee, mage | ✔ To level 18, abilities rank up | ~ Codex | ? | ? | ? | ? | 26.2 Fabric, 104 downloads |
| Easy NPC | You author NPCs | ~ Any, by configuration | ✖ | ✔ Dialogue | ? | ~ Objectives (follow, attack); requests for command control ([#694](https://github.com/MarkusBordihn/BOs-Easy-NPC/issues/694), [#761](https://github.com/MarkusBordihn/BOs-Easy-NPC/issues/761)) | ✖ | ? | [#857](https://github.com/MarkusBordihn/BOs-Easy-NPC/issues/857) save stalls with 327+ NPCs (fixed 2026-10-23) |
| Better Party / Party Hud / Odyssey Allies | Players alone | ~ Leader, Officer, Member (permissions) | ~ Nearby XP sharing | – | – | – | – | – | Better Party has friendly-fire protection for players |
| Aggro Indicator | – | – | – | – | – | – | ✔ An icon over mobs targeting **you**, plus an alert sound | – | Needed on both sides; [#4](https://github.com/Raverbury/AggroIndicator/issues/4) missed modded mobs |
| Hardcore Revival / PlayerRevive / Incapacitated | Players alone | – | – | – | ✔ Downed state, bleed-out timer, revive by a friend | – | – | – | Hardcore Revival [#105](https://github.com/TwelveIterations/HardcoreRevival/issues/105) "make mobs ignore you when downed"; [#38](https://github.com/TwelveIterations/HardcoreRevival/issues/38) mobs cancel the revive timer |

### 1c. The 26.2 / 26.3 landscape

| Need | What exists on 26.x (2026-10-27) | Gap |
|---|---|---|
| Hire and lead a party of named, classed adventurers | Guard Villagers (NeoForge 26.3; Fabric port 26.2): follow just with Hero of the Village. Millager (26.2): village squads summoned by Muster Orders. Girlfriend Mod: one companion. Champion Companions (Fabric 26.2, 104 downloads, needs GeckoLib). Mercenaries (Fabric 26.2, 84, alpha). Easy NPC: an authoring tool. 26.x searches for "hireling", "mercenary", "bodyguard", "knight companion", "barracks" and "adventurer guild" return 0 relevant mods | We found no Fabric + NeoForge 26.2/26.3 mod with recruitable allies who level and have classes. Recruits, Human Companions, Modern Companions, MineColonies, Taterzens and Mebahel's are all absent |
| Tank/healer/damage roles and aggro | Aggro Indicator (26.2, just your own aggro); Radial Aggro Indicator (26.1.2); Millager's taunting Rioter and healing Lancer; Champion Companions' support and kiter. Searches for "taunt", "tank healer" and "healer" return 0 relevant mods | We found no party-wide aggro table with a visible label over mobs |
| Downed instead of dead | Hardcore Revival, PlayerRevive and Simple Revive (all 26.3): players alone. Petting's collar respawn: a pet teleports home, with no downed phase. "revived pet" (7.5k): a dropped collar revives a wolf or cat | We found no downed state for NPC allies |
| Keeping followers with you | Followers Teleport Too and GUI Followers (26.3; need Collective); Wolf Whistle (Fabric 26.2, 462 downloads) recalls a wolf "even across dimensions". Companion stops at 26.1.2 | These cover tamed animals; coverage of modded NPC allies is **unverified** |
| Commanding allies | Wolf Whistle: a radial menu for one wolf. Petting: collar modes. Millager: order items. "formation" on 26.x finds structure mods and nothing else; "command wheel" finds client macro wheels (Orbit, Easy Macros) | We found no command wheel or formations for a party of allies |
| Player parties | Better Party (26.3, all loaders); Party Hud (NeoForge 26.1.2) | Players alone. Complementary: a party member's allies should count as friendly (section 3) |

---

## 2. Top recurring player complaints and gaps

Ranked by how often the problem appears across mods and how strong the evidence is.

1. **Followers get lost or vanish.**
   - Human Companions: "Using a boat for long distances causes companions to permenantly despawn", and the reporter
     "lost 5 companions worth of highly valuable inventory" ([#14](https://github.com/justinwon777/HumanCompanions/issues/14));
     companions follow "trough the portal, no matter what" ([#13](https://github.com/justinwon777/HumanCompanions/issues/13)).
     The listing itself recommends a separate Pet Tracker mod.
   - Guard Villagers: "Disappearing permanent guards" ([#248](https://github.com/seymourimadeit/guardvillagers/issues/248)).
   - Villager Recruits: an add-on fixes "Follow Me" indoors sending recruits "to the roof or the top of nearby trees"
     and teleports recruits stuck for 7 seconds ([No More Fool Recruits](https://modrinth.com/mod/no-more-fool-recruits)).
   - Demand: [Companion](https://modrinth.com/mod/companion) (2,139,681 MR) forces pets to "teleport to you if it is
     about to be unloaded"; [Followers Teleport Too](https://modrinth.com/mod/followers-teleport-too) (489,841 MR) and
     [GUI Followers](https://modrinth.com/mod/gui-followers) (143,126 MR) exist just to keep followers with you.
2. **Death is cheap or final, with nothing in between.**
   - Human Companions: "don't recommend storing valuables in them as they die easily" (listing); "set their respawn to
     beds" ([#9](https://github.com/justinwon777/HumanCompanions/issues/9)); "companions will lose AI after
     resurrection" ([#35](https://github.com/justinwon777/HumanCompanions/issues/35)).
   - Modern Companions brings an ally back through nothing but a Resurrection Scroll plus a Nether Star.
   - Players clearly like a downed phase (Hardcore Revival 10.2M MR / 7.5M CF; PlayerRevive 4.8M MR), but every
     revive mod we found is for players.
   - Lessons from those mods: "make mobs ignore you when downed" ([#105](https://github.com/TwelveIterations/HardcoreRevival/issues/105))
     and "Mobs cancelling res timer when punched and already down" ([#38](https://github.com/TwelveIterations/HardcoreRevival/issues/38)).
3. **Targeting and combat AI.**
   - Human Companions: "make them target hostile mobs rather than just the 3 vanilla monsters ... I hate having to use
     external mods to add aggro values to every single enemy in a modpack" ([#60](https://github.com/justinwon777/HumanCompanions/issues/60),
     also [#20](https://github.com/justinwon777/HumanCompanions/issues/20)). Known issues: "Archers sometimes shoot at
     nothing", "Knights sometimes hesitate"; the arbalist does not dodge melee ([#23](https://github.com/justinwon777/HumanCompanions/issues/23)).
   - Guard Villagers: "Guards kill me no matter what, even after I die" ([#142](https://github.com/seymourimadeit/guardvillagers/issues/142));
     guards never target other guards or golems ([#277](https://github.com/seymourimadeit/guardvillagers/issues/277)).
   - Aggro Indicator: did not work with some modded mobs ([#4](https://github.com/Raverbury/AggroIndicator/issues/4)).
4. **Performance with many NPCs.**
   - Easy NPC: with 327–348 NPCs, unchanged NPCs were rewritten on every chunk unload; saves stalled and one tick ran
     past the 60-second watchdog. Fixed in 7.17.0 on 2026-10-23 ([#857](https://github.com/MarkusBordihn/BOs-Easy-NPC/issues/857)).
   - Guard Villagers: "Guard Villager Tasks Causing TPS Buildup" with about 8 guards ([#170](https://github.com/seymourimadeit/guardvillagers/issues/170)),
     "Lagging like crazy" ([#150](https://github.com/seymourimadeit/guardvillagers/issues/150)), and an FAQ entry on lag.
   - Human Companions: one old companion re-applied a health modifier every tick ([#31](https://github.com/justinwon777/HumanCompanions/issues/31)).
   - Villager Recruits: pathfinding and target finding moved to a thread pool, and line-of-sight results cached
     ([PR #23](https://github.com/talhanation/recruits/pull/23)).
5. **Stuck on old versions and loaders.**
   - Villager Recruits: NeoForge 1.21.1 port PRs [#68](https://github.com/talhanation/recruits/pull/68) (open),
     [#77](https://github.com/talhanation/recruits/pull/77) (draft) and [#78](https://github.com/talhanation/recruits/pull/78)
     (closed unmerged on 2026-10-24).
   - Taterzens: "Any chance of a 1.21.11 port?" ([#205](https://github.com/samolego/Taterzens/issues/205)), plus
     [#190](https://github.com/samolego/Taterzens/issues/190), #184, #179 and #170.
   - Human Companions: "Any plans to add a mod to Modrinth?" ([#57](https://github.com/justinwon777/HumanCompanions/issues/57)),
     last file 2024-12-15. Modern Companions exists because of that gap.
   - Guard Villagers FAQ: "Can you port to an older version/fabric? No."
   - Aggro Indicator: port and backport requests ([#8](https://github.com/Raverbury/AggroIndicator/issues/8), [#9](https://github.com/Raverbury/AggroIndicator/issues/9)).
6. **Commanding is clunky or buried in screens.**
   - Human Companions: every mode is a toggle in the inventory screen.
   - Guard Villagers: "I want to get a follow button in the GUI" ([#182](https://github.com/seymourimadeit/guardvillagers/issues/182));
     following needs Hero of the Village.
   - Easy NPC: requests to change objectives by command ([#694](https://github.com/MarkusBordihn/BOs-Easy-NPC/issues/694), [#761](https://github.com/MarkusBordihn/BOs-Easy-NPC/issues/761)).
   - Villager Recruits: formations and cleaner army control come from add-ons (Advanced Grouping, 8,249 CF). A
     community "Recruits Addon" with LINE/SQUARE/CIRCLE/WEDGE formations is described on ModpackIndex (**unverified**).
     Another add-on fixes the Move command breaking when pointed at a wall.
7. **No real roles in most mods.** Human Companions, Guard Villagers and Recruits have fighter types and nothing else. The demand
   shows in Modern Companions adding a Vanguard and a Cleric, in the [Recruits Morale Addon](https://www.curseforge.com/minecraft/mc-mods/recruits-morale-addon)
   (6,271 CF), and in [Taunt RPG](https://modrinth.com/mod/taunt-rpg) (385 MR), a horn that exists just to pull aggro
   for tanks.
8. **Friendly fire and safety.**
   - Human Companions: "Friendly fire = off doesn't work?" ([#27](https://github.com/justinwon777/HumanCompanions/issues/27)).
   - Companion ships "Sweeping Edge effect won't hurt your pets" and a pet friendly-fire game rule.
   - Better Party ships friendly-fire protection between players. Modern Companions defaults its villager and PvP
     protection to safe.
9. **Server authority and permissions.**
   - Villager Recruits: five open "Harden ... authority" PRs. #62 rewires commands "through server-resolved owned
     targets instead of trusting client UUIDs" ([#62](https://github.com/talhanation/recruits/pull/62)). #85 asks for
     operator rights on debug tools.
   - Taterzens: "Add way to disable NPC editor GUI for OPs" (#201).
   - Easy NPC: "Security config is not intuitive" (#733).
10. **Dependency chains and bridges.**
    - Taterzens loses entity-type editing to the abandoned DisguiseLib ([#171](https://github.com/samolego/Taterzens/issues/171)).
    - Mebahel's runs on Forge/NeoForge through Sinytra Connector and no other way.
    - Followers Teleport Too needs Collective, Companion needs Kiwi, Champion Companions needs GeckoLib, and [Companions!](https://modrinth.com/mod/companions-mod)
      (tamable creatures; 2,535,692 CF / 210,486 MR) needs GeckoLib and Knight Lib.
    - The Minions datapack-mod warns that TimeLib, its core dependency, was archived on 2025-09-27.
11. **Item loss and duplication.** Human Companions dupes ([#26](https://github.com/justinwon777/HumanCompanions/issues/26), [#37](https://github.com/justinwon777/HumanCompanions/issues/37)) and lost inventories (#14). Taterzens' editor deletes the player's inventory (#154, #163), and capturing an NPC corrupts player data (#185).

---

## 3. What this means for the design

Each row names the gap it answers. Claim a row in marketing once it ships, never before.

| Gap | Design response |
|---|---|
| 1. Lost followers | Allies travel through portals with you. Stranded or unloaded allies teleport back, with no item loss. Roster data lives with the owner, so allies survive relogs, restarts and chunk unloads. Write roster data when it changes, never every tick (the Easy NPC #857 lesson) |
| 2. Death | Downed state with a revive ring and a bleed-out timer. Mobs drop downed allies from their aggro table, and a hit does not reset revive progress (Hardcore Revival #105, #38). Permadeath and the Remembrance list are off by default |
| 3. Targeting | The aggro table covers any `Enemy` or any mob targeting the party, so modded mobs are included. Never attack the owner, party members, their allies or villagers. Clear stale targets (Human Companions' "shoot at nothing") |
| 4. Performance | Hard per-owner and per-world caps. Stagger target scans and path recalculation over a tick budget. Cache line of sight |
| 5. Versions | One feature set on all nine jars (1.20.1 Fabric/Forge, 1.21.1 Fabric/NeoForge/Forge, 26.2 and 26.3 Fabric/NeoForge) on day one |
| 6. Commands | A hold-key command wheel: Follow, Hold, Charge, Focus (aim at a target), Fall back, Guard banner, formations, Recall. Every command also works from `/fellowship` |
| 7. Roles | Vanguard taunt, Mender heal and revive, Ranger kite, and an aggro label over each engaged mob |
| 8. Friendly fire | Allies never damage the owner, party members, other allies or villagers by default; a config controls it |
| 9. Authority | Every command packet is resolved on the server against owned allies. Admin tools need operator permission |
| 10. Dependencies | Fabric API on Fabric, nothing else |
| 11. Items | Ally equipment is stored once on the server and dropped or returned exactly once |

---

## 4. Positioning

### 4a. How Fellowship differs from the nearest mods

| Versus | What they already do well | Where Fellowship differs (once shipped) |
|---|---|---|
| **Modern Companions** (closest in features) | 14 roles including a taunting Vanguard and a healing Cleric; traits, Morale, Bond and a stats Journal; deep gear and backpack support | Runs on Fabric and on 26.2/26.3, not just NeoForge 1.21.1. Downed state instead of a Nether-Star scroll. A visible aggro label. A command wheel with formations and a Warbanner. A Chronicle of sentences from events, not just counters. **Do not** claim the Vanguard or taunt as new; "Vanguard" is also their class name |
| **Villager Recruits** | Armies, factions, claims, sieges, diplomacy, groups | A small RPG party, not an army sim. Roles, aggro and revives. Multi-loader and 26.x. Do not compete on sieges or diplomacy |
| **Human Companions** | Simple charm: find a house, feed, recruit | Allies that do not get lost and do not die outright, with roles and a performance budget |
| **Guard Villagers / Millager** | Village defence and village armies | A personal party that you hire and that levels with you |
| **Petting / Tame Every Mob** | Any mob as a companion. Petting tames with Golden Treats and gold collars | Named people with classes. Note that Petting already uses gold for taming, so lead with **Gold is loyalty: wages and regalia**, not "gold tames" |
| **Better Party / Party Hud / Odyssey Allies** | Player-to-player parties | NPC allies. Complementary: treat a party member's allies as friendly |
| **Revive mods** | Downed players | Downed allies. Complementary: Fellowship leaves player death alone |
| **Aggro Indicator** | Shows mobs targeting you | Shows which party member each mob is targeting. Complementary |

### 4b. Overlap with the studio's own mods, and how Fellowship plugs in

Modrinth and CurseForge downloads fetched on 2026-10-27.

| Mod | Downloads CF / MR | What it covers | Overlap with Fellowship | Hook (checked in source unless marked) |
|---|---|---|---|---|
| Remnants \| RPG Stats, Skills & Progression | 444,966 / 95,822 | Player levels and attributes, shared XP, public API | Both have "levels". RAS levels the player; Fellowship levels allies | `RasApi` (`getLevel`, `getCombatSnapshot`): optionally scale ally level or XP from the owner's RAS level |
| Remnants \| RPG Mobs | 132,695 / 36,518 | Monster levels, ranks, traits and **area threat** (`/remnants threat`, Hunter's Lens) | **Term clash: "threat".** Use "aggro" in Fellowship's UI and commands | `MobLeveler` skips any `OwnableEntity` with an owner, so allies are never levelled as monsters. `RemnantMobsApi.getRank`/`getTraits`/`getLevel` can weight aggro and focus targets |
| Remnants \| The Eclipsed Realm | 812,315 / 43,078 | Bosses and a dimension | None | Allies follow through its portals (gap 1); boss kills feed the Chronicle (**unverified**: needs a boss tag or event) |
| Remnants \| Skills and Powers | 5,490 (CF title still "The Elemental - RPG") / 3,342 | Passive web and elemental powers | "Skills" words; Arcanist and Mender cast spells | `Targets.canHit` spares the caster's own `OwnableEntity` (`Compat.ownedBy`), and `Targets.allies` includes them, so area heals and buffs reach Fellowship allies with no new code |
| Runeborn | 8,840 / 3,461 | Soulslike weapons, champion summons | Gear | Allies equip any item; stats are plain attributes |
| Mob Loot Bags | 314,175 / 36,912 | Loot bags from kills | None | `DropEngine` credits kills by an `OwnableEntity` to its owner (config `creditPetKills`), so ally kills earn bags |
| Much More Dungeons | 48,986 / 21,037 | Dungeons with named guardians | None | Guardian kills feed the Chronicle (**unverified**) |
| Remnants: Ballistics | 58 / not on MR | Firearms (1.21.1) | None | Whether allies can fire its guns is **unverified** |
| Smart Villagers AI | 9,095 / 7,191 | Talking villagers, requests paid in emeralds, reputation | Both live in villages; both name NPCs | No public API package found in its source; skip integration. Fellowship's Chronicle needs no AI |
| NightMMO (Paper plugin) | not on Modrinth | Player classes (Warrior, Mage, Rogue, **Ranger**, Paladin, Berserker), parties, guilds | Player classes, not NPC allies; "Ranger" appears in both | None. Different platform |
| Better With Gold (legacy) | 6,916 / 1,889 | 17 gold items | Becomes Fellowship's "Gilded Regalia" | Gold armour adds morale in the banner aura |

One-line split for every listing: "RPG Stats levels **you**. RPG Mobs levels **them**. Fellowship levels **the people who
fight beside you**."

### 4c. Angles and wording

| Angle | Why it works (gap) | Wording ideas |
|---|---|---|
| **Ready for 26.2 and 26.3** | The category leaders stop at 1.20.1 or 1.21.1 (1c) | "Hire a party on 26.3 today, and on 1.20.1 and 1.21.1." |
| **Nobody gets left behind** | Gaps 1 and 2 | "Down, not dead. Pick them up under fire." "Through portals, across relogs, never lost." |
| **Roles you can see** | Gap 7 | "Watch the aggro label switch to your Vanguard." Never "the trinity Minecraft never had", which is both a banned "first" claim and contradicted by Modern Companions |
| **Lead, don't babysit** | Gap 6 | "Hold a key. Charge, hold, focus, fall back." "Plant the Warbanner and they form up." |
| **Their story, from what they did** | Modern Companions keeps counters; Fellowship writes events | "Every ally keeps a Chronicle of what they actually did." "No AI required." |
| **Server-checked and light** | Gaps 4 and 9 | "Every order is checked by the server. Caps and a tick budget keep big warbands playable." |
| **Part of Remnants** | Studio continuity | "Works with RPG Stats, RPG Mobs and Skills and Powers, and needs none of them." |

---

## 5. Naming check

Checked on 2026-10-27: Modrinth search (all project types), CurseForge (CFWidget slug lookups plus web search), GitHub
repository search, and a plain web search outside Minecraft. **This is not legal advice.**

| Name | Modrinth | CurseForge | GitHub | Outside Minecraft | Verdict |
|---|---|---|---|---|---|
| **Fellowship** | 0 mods. 2 modpacks: [The Fellowship](https://modrinth.com/modpack/the-fellowship) (3,605, Fabric 1.21.x) and [Fellowship Adventures](https://modrinth.com/modpack/fellowship-adventures) (233) | No mod found (slug `fellowship` 404). Modpacks: [The Fellowship 67](https://www.curseforge.com/minecraft/modpacks/the-fellowship-67) (137) and [Fellowships of Middle Earth (LOTR)](https://www.curseforge.com/minecraft/modpacks/fellowships-of-middle-earth-lotr) (610). Near names: [Fellow Traveler](https://www.curseforge.com/minecraft/mc-mods/fellow-traveler) (a companion mod, 4,001, Forge 1.20.1) and "Fellowship Tweaks" (a modpack helper, **unverified**) | No Minecraft mod repo ("fellowship minecraft mod": 0; "fellowship minecraft": one modpack archive) | **[Fellowship](https://store.steampowered.com/app/2352620/)** (Chief Rebel; Coffee Stain Publishing and Arc Games; Early Access since 2025-10-16): a co-op dungeon game whose heroes come in "three archetypes: DPS, Tank, and Healer". A web search returned a US application for FELLOWSHIP covering game software (serial 97629701, filed 2022-10-12); the record page returned 403, so its owner and status are **unverified**. The word is also strongly tied to *The Fellowship of the Ring* | **Recommended, with conditions** |
| Retinue | 0 | No mod found (slug 404). Hytale: [Retinue](https://www.curseforge.com/hytale/mods/retinue) (84 downloads), an NPC follower and roster framework | 0 Minecraft repos. "bannerlord-retinues" is a troop mod for another game | "Retinue \| Combat & Gathering Companion", a paid Minecraft Paper companion plugin on BuiltByBit (published July 2026 per search results; the page returned 403, so details are **unverified**) | Rejected: collides inside the niche |
| Oathsworn | 0 (Paladin's Oath, 7,698, is a near name) | [Oathsworn Paladins (RPG Series Plus)](https://www.curseforge.com/minecraft/mc-mods/oathsworn-paladins): **445,014** CF, Fabric 1.21.1 | No Minecraft repos; many repos for the board game | *Oathsworn: Into the Deepwood* (Shadowborne Games), a board game | Rejected: a big RPG mod in the same audience |
| "Remnants \| Fellowship" (exact) | 0 | 0 found | 0 | 0 found | Clean |

**Recommendation: Remnants | Fellowship.**

- Among Minecraft mods it is the cleanest of the three: no mod, plugin or Hytale project uses it, while "Retinue"
  already names a Minecraft companion plugin and "Oathsworn" a 445k-download RPG add-on.
- It says "party" in one word, in any language players are likely to know, and it reads well in a voiceover.
- Conditions, because of the Steam game and the Tolkien link:
  1. Always use the full title **"Remnants | Fellowship"**. Never use "Fellowship" alone as a title, logo wordmark or
     hashtag; use `#RemnantsFellowship`.
  2. Do not borrow that game's vocabulary ("MODA", "Heroes", "Multiplayer Online Dungeon Adventure") in copy.
  3. **Rethink the planned "banner and gold ring" icon:** a gold ring next to the word "Fellowship" points straight at
     *The Lord of the Rings*. Use a banner with a gold coin, signet or laurel instead.
  4. Before any paid promotion, have the owner check the USPTO/EUIPO records for FELLOWSHIP in classes 9/41.

Related terms:

- **"Warband"** is a Modrinth mod ([Warband](https://modrinth.com/mod/warband), 6,772, Fabric 26.1–26.2, a hostile mob
  AI overhaul) and part of another game's title. Keep it out of the title and summary; generic use in body text is fine.
- **"Threat"** clashes with RPG Mobs (see 4b). Use "aggro".
- **"Vanguard"** is also a Modern Companions class with the same job. It is a generic word, but do not present it as
  new.

---

## 6. Alternatives considered

| Option | Evidence (2026-10-27) | Why not chosen |
|---|---|---|
| **A. Gear itemization** (affixes, tiers, modular tools) | [Tetra](https://modrinth.com/mod/tetra) 1,944,343 MR (Forge/NeoForge, newest 1.20.1); [TieredZ](https://modrinth.com/mod/tieredz) 1,740,194 (Fabric, up to 1.21.1); [Mine and Slash](https://modrinth.com/mod/mine-and-slash) 65,269 (Forge 1.20.x); [Apotheosis](https://modrinth.com/mod/apotheosis) 412,588 (Forge/NeoForge, up to 26.1.2, so its 26.x gap may close). "affix" and "legendary items" return 0 on 26.x | It overlaps the studio's own Runeborn (74 armaments, reinforcing and infusing) and Mob Loot Bags (rewards). It keeps the "gold gear" identity the owner wants to leave. Apotheosis is already on 26.1.2 |
| **B. Summoner / necromancer mod** | "necromancer" and "summoner minions" return 0 on 26.x, but [Occultism](https://modrinth.com/mod/occultism) (920,701 MR, summoned spirits) is on 26.x, and Modern Companions has a Necromancer | A narrower fantasy with the same AI and performance costs as allies. Folded in as the stretch Bonecaller class |
| **C. NPC authoring tool** | Easy NPC (8.7M CF, 1.29M MR, 26.3 on every loader) | Already well served |
| **Chosen: allies, parties and warbands** | Section 1c: we found no 26.2/26.3 multi-loader party mod with classes, levels and commands; the leaders are stuck on 1.20.1/1.21.1 | It fills the studio's missing pillar (who fights next to you) and connects to RAS, RPG Mobs, Skills and Powers and Loot Bags through vanilla ownership |

---

## Appendix: sources checked, and unverified items

- **Modrinth API:** project and version endpoints for every mod in 1a and 4b; about 45 searches filtered to 26.x (the
  role, recruit, companion, party, revive, aggro, command and name terms quoted in sections 1c and 5); the
  `nightbeam` organization for the studio list.
- **CFWidget:** every CurseForge number in 1a, 4b and 5. Slugs that returned 404: taterzens, aggro-indicator,
  fellowship, retinue, oathsworn, remnants-fellowship.
- **GitHub pages read:** HumanCompanions issues (both pages, plus #13, #14, #31, #60); talhanation/recruits (Issues
  turned off; pull requests page plus #23, #62, #78); talhanation/wiki (Recruits wiki); Taterzens issues (pages 1–2);
  guardvillagers issue searches plus #170, #248, #305; AggroIndicator issues; BOs-Easy-NPC issue search plus #857;
  HardcoreRevival issue search; ModernCompanions issues (1 issue). GitHub repository search for fellowship, retinue and
  oathsworn.
- **Studio source checked:** `Ascendant-Mobs/.../MobLeveler.java` (OwnableEntity skip) and `RemnantMobsApi.java`;
  `Elementals/.../Targets.java` and `Compat.java` (`ownedBy`); `Mob-Loot-Bags/.../DropEngine.java` (`creditPetKills`);
  `RPG-Attribute-System/.../RasApi.java`; no `api` package in `CivilizationAI`.
- **Unverified:**
  - Villager Recruits: levels, death, portal behaviour and the CoreLib requirement.
  - Whether Modern Companions uses an aggro table behind its taunt.
  - The "Recruits Addon" formations (seen in a ModpackIndex summary, not the mod itself).
  - The BuiltByBit "Retinue" plugin details (page returned 403).
  - The owner and status of US trademark application 97629701 (seen in a search-result snippet, nowhere else).
  - "Fellowship Tweaks" downloads.
  - Whether Followers Teleport Too moves modded NPC allies.
  - Eclipsed Realm and Much More Dungeons kill hooks.
  - Ballistics compatibility.
  - CurseForge counts for Taterzens, Aggro Indicator, Girlfriend Mod, Followers Teleport Too and PlayerRevive (not
    found or not checked).
- Download figures are cumulative and differ between platforms. CurseForge counts include launcher auto-installs, so
  compare mods within one platform rather than across platforms.
