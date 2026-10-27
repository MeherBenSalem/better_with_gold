# Decisions

Every call made while rebuilding Better With Gold into **Remnants | Fellowship** (2.0.0), with the reason. Newest
at the bottom of each section.

## Concept and name

- **Rebuild as a companion, party and warband mod.** The market check (`docs/market-analysis.md`) found no
  Fabric or NeoForge 26.2/26.3 mod that gives hired allies classes, levels, orders and formations, while the
  largest mods in that space are stuck on old versions or one loader (Villager Recruits: Forge, newest 1.20.1;
  Human Companions: Forge; Modern Companions: NeoForge 1.21.1). It is also the one RPG pillar the studio's own
  mods do not cover (stats, mob levels, bosses, skills, weapons, loot and dungeons already exist).
- **Gear itemization (Backup A) was not taken.** It has demand, but it overlaps Runeborn and Mob Loot Bags.
- **Name: "Remnants | Fellowship".** "Fellowship" had no Minecraft mod on Modrinth, CurseForge or GitHub. A Steam
  game called *Fellowship* exists, so the listing always uses the full title, and the copy avoids that game's
  role vocabulary. "Retinue" and "Oathsworn" were dropped (a companion plugin and a 445k-download mod already use
  them).
- **The mod id stays `better_with_gold`**, as do the Java package, the 17 item ids and the Modrinth and CurseForge
  projects. The listings are retitled in place, so worlds, packs and download history carry over.

## Design changes after the market check

- **Tank class renamed from Vanguard to Bulwark.** Modern Companions already has a taunting "Vanguard".
- **Player-facing text says "aggro", not "threat".** Remnants: RPG Mobs already uses "threat" for an area's danger
  level (`/remnants threat`). Code keeps the name `Threat` internally; config keys are `aggroLabels` and
  `aggroSwitchRatio`.
- **Allies implement vanilla `OwnableEntity`.** RPG Mobs, Skills and Powers and Mob Loot Bags already treat
  owned entities as the player's allies, so they work together with no extra code.
- **No "first" or "only" claims anywhere.** Taunting tanks, healing companions, morale and journals exist in other
  companion mods; Fellowship is pitched on what it combines and where it runs, not on inventing those.

## Systems

- **Class files are JSON in `config/better_with_gold/classes/`, not a datapack.** Datapack reload listeners differ
  on every loader and version; a config folder reads the same everywhere. A class picks one of five roles (tank,
  melee, ranged, healer, caster) and numbers, so packs can add classes without code. `/fellowship reload` reloads.
- **Downed, not dead, by default.** At zero health an ally collapses (sleeping pose, mobs lose interest) and bleeds
  out over 30 seconds. Crouching beside it revives it; Menders revive by themselves. When it bleeds out it goes to
  camp to recover for 5 minutes. Permadeath is a config switch; fallen allies then join the Remembrance.
- **Allies that travel with the leader are stowed, not left behind.** On log-out or a dimension change, following
  allies are saved as their full entity data in the leader's warband file and placed back next to them. Allies
  left in unloaded chunks are found by force-loading the chunk where they were last seen (Recall, and
  automatically while following). This is aimed at the most common complaint about companion mods: losing them.
- **Aggro labels are an invisible passenger entity with a name tag**, created and removed by the server. Vanilla
  draws them on every client with no client code, on every version.
- **Orders go to the whole warband.** Per-ally orders were left out of 2.0 to keep the wheel to one gesture.
- **Hiring costs gold ingots.** It ties the mod to its gold heritage. The currency item is configurable.
- **The 17 Gilded Regalia items keep their behaviour.** Their recipes, lost in the 1.2 multi-loader conversion,
  are restored. Reinforced Golden Armour now also adds 3% damage and 5 morale floor per piece to allies near the
  leader; the Golden Fish fully heals an ally and adds 30 morale.
- **RPG Attribute System is optional.** Through its public `RasApi.getLevel`, allies cannot out-level their
  leader's RPG level (minimum 5). Allies' kills credit the leader (`setLastHurtByPlayer`), so vanilla experience
  and player-only drops still happen.

## Scope (cut from the bottom)

- Tier 1 (2.0.0): Warbanner, aggro and taunts, downed and revive, roster with Chronicle, command wheel and
  formations, real gear, gold hiring, five classes, regression tests.
- Tier 2: sellswords at village bells (done in 2.0.0, no new block needed), veteran titles (done). A Tavern or
  Barracks block was not built.
- Tier 3: the Bonecaller class and soft integrations beyond RPG Attribute System were not built.
