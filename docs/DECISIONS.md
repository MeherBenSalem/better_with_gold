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
  and drops that need a player kill still happen.

## Presentation

- **Effects are server-side vanilla particles and sounds**, played by a small scheduler (`ally/Fx.java`) that runs
  an animation one frame per tick. Every client sees them with no resource pack and no client code, on every
  version. Every particle type they use exists unchanged from 1.20.1 to 26.3.
- **GUI art is drawn, not shipped as sprites**, except the command wheel. Icons are 16x16 pixel art authored in
  `tools/gen_icons.py` and drawn as coloured squares on the shared Canvas, so they are crisp at every GUI scale and
  identical on every version. The wheel's slices are smooth 512x512 textures from `tools/gen_gui.py`, drawn
  rotated: 26.x GUI rendering draws nothing but (transformed) rectangles, so a texture is the clean way to get curved
  slices on all four versions.
- **Skins and item icons are generated** by `tools/gen_textures.py` (materials, then details, then one lighting
  pass), so a palette change or a new variant is one edit and the output is reproducible.
- **The roster portrait is the real entity**, drawn with each version's inventory-portrait call; its name tag is
  hidden for the draw. Allies at camp or away show their class emblem instead.

## Testing

- **One play-through for all nine builds.** `tools/qa/fellowship_checks.py` runs the same checks (recruiting,
  following in formation, aggro labels, combat, downed and revived, following again after a fight, the command
  wheel, the Warbanner) on a dev server and a real client under Xvfb, on every version and loader.
- **The QA RCON client reads a reply, or waits for a marker.** Forge 1.21.1's RCON sends no packet at all for a
  command with no output, and vanilla's client-side reader drops the connection when two packets arrive in one read.
  So the client waits briefly for the command's own reply; when none comes, it sends a request of an unknown type,
  which every server answers, and reads up to that answer.
- **Formation, revive and fall-back goals tick every tick.** A goal that does not ask for it runs on alternate ticks,
  chosen by the parity of the server tick plus the entity id, which left some allies standing still after a fight
  and stretched a Mender's 5-second revive. Found while filming the Reel; the after-fight check covers it.
- **Aggro labels draw out to 48 blocks.** The label is a 0.05-block entity, and vanilla skips drawing an entity
  farther than 64 times its size, so labels vanished beyond about 3 blocks. Found while taking the store screenshots.
- **The QA world is cleared of slimes until none are left.** The flat test world fills with slimes while the
  server starts, before the checks can turn spawning off (26.x spells the rules `spawn_mobs` and `spawn_monsters`;
  the checks send both spellings), and `/kill` splits each big slime into smaller ones a second later. Left over,
  they pulled guarding allies away from the Warbanner on 26.2 NeoForge. The checks log what they clear before that
  check.
- **26.3 NeoForge declares the icon with `iconFile`.** With `logoFile`, NeoForge for 26.3 shows a warning screen on
  every launch. 26.2 NeoForge and the older loaders do not know `iconFile`, so they keep `logoFile`.

## Store page and Reel

- **Every image is the mod running.** Screenshots are unedited captures of the 1.21.1 Fabric dev client in one
  staged world; the cards and banner are composed around them in HTML (`marketing/source/render.cjs`) with the mod's
  own pixel art and two OFL fonts. Nothing is AI-generated or retouched.
- **Action stills come from a slowed clock.** The Arc Storm's lightning lasts a few ticks; at normal speed the
  software renderer rarely draws it. Running the game 10× slow and grabbing losslessly keeps every rendered frame, so
  the exact frame can be picked (the same method as the Reel).
- **Formations are a diagram, not a screenshot.** Seen from above on plain grass the four shapes did not read, so the
  command card draws them from the slot offsets in `Formation.java`.
- **The Reel follows the channel's standing rules**: the NightBeam channel voice (Chatterbox, four takes per line),
  real recorded Mixkit sound effects, footage on a slowed clock with no repeated frames. See `marketing/reel/README.md`.

## Scope (cut from the bottom)

- Tier 1 (2.0.0): Warbanner, aggro and taunts, downed and revive, roster with Chronicle, command wheel and
  formations, real gear, gold hiring, five classes, regression tests.
- Tier 2: sellswords at village bells (done in 2.0.0, no new block needed), veteran titles (done). A Tavern or
  Barracks block was not built.
- Tier 3: the Bonecaller class and soft integrations beyond RPG Attribute System were not built.
