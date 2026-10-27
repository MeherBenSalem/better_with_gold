![Remnants | Fellowship 2.0](https://raw.githubusercontent.com/MeherBenSalem/better_with_gold/main/marketing/images/banner.png)

## Lead a warband

**Remnants | Fellowship** lets you hire named sellswords with gold and lead them as a warband. Five classes that each
fight their own way, a command wheel, four formations, aggro you can steer with a taunting Bulwark, allies that are
**downed instead of killed** so you can drag them back into the fight, a Warbanner to rally on, and a Chronicle that
writes every ally's story from what they actually did.

**2.0** is a full rework of *Better With Gold*. Same mod id, so 1.x worlds keep their gold gear, and the gold items
are still here.

![The command wheel in use](https://raw.githubusercontent.com/MeherBenSalem/better_with_gold/main/marketing/images/gifs/command-wheel.gif)

![divider](https://raw.githubusercontent.com/MeherBenSalem/better_with_gold/main/marketing/images/divider.png)

![Your Warband](https://raw.githubusercontent.com/MeherBenSalem/better_with_gold/main/marketing/images/header-warband.png)

![Hire your warband](https://raw.githubusercontent.com/MeherBenSalem/better_with_gold/main/marketing/images/card-warband.png)

- **Sellswords for hire turn up near village bells.** Each one shows its name, class, level and price above its head.
  Use one while holding gold ingots to hire it. The price is 7 to 12 gold at level 1, plus 2 or 3 per level.
- **Up to 6 allies** per leader by default (`partySize`, up to 24).
- Every ally rolls **one or two of eleven traits**: Steadfast, Swift, Brutal, Keen-eyed, Stalwart, Devoted, Lucky,
  Greedy, Hardy, Fearless and Merciful. The leading trait gives its epithet: *Brenna the Steadfast*, *Corwin Bonebreaker*.
- Allies earn experience for every kill they help with and reach **level 20**, with titles at 5, 10, 15 and 20
  (Veteran, Champion, Hero, Legend). **Morale** rises with victories and falls when friends go down; high morale hits
  harder.
- **Real gear:** crouch and use an ally while holding a weapon, shield or armour piece to hand it over. Their stats
  are plain attributes, so gear from any mod works.

![Five Classes](https://raw.githubusercontent.com/MeherBenSalem/better_with_gold/main/marketing/images/header-classes.png)

![Five classes](https://raw.githubusercontent.com/MeherBenSalem/better_with_gold/main/marketing/images/card-classes.png)

| Class | Role | Fights by | Ability |
| --- | --- | --- | --- |
| **Bulwark** | Tank | Sword and shield in the front line | **Challenge:** taunts every enemy within 8 blocks that is attacking someone else, for 6 seconds |
| **Skirmisher** | Melee | Golden scythe, in the thick of it | **Whirl:** every 6 seconds, hits every enemy within 3 blocks for 60% damage |
| **Ranger** | Ranged | Keeps its distance with a bow | **Volley:** every 10 seconds, three arrows at once |
| **Mender** | Healer | Stays behind the line | **Mend:** heals the most hurt ally or leader every 3 seconds, and revives the downed |
| **Arcanist** | Caster | Arc bolts from range | **Arc Storm:** lightning on packed enemies, 120% damage to everyone within 5 blocks of the target |

Classes are JSON files in `config/better_with_gold/classes/`: change their numbers or add your own.

![Command](https://raw.githubusercontent.com/MeherBenSalem/better_with_gold/main/marketing/images/header-command.png)

![Command the band](https://raw.githubusercontent.com/MeherBenSalem/better_with_gold/main/marketing/images/card-command.png)

- **Hold G** for the command wheel, point, let go: **Follow, Charge, Focus** (your crosshair target), **Hold, Fall
  back, Guard banner, Formation** and **Recall**.
- **Four formations:** Line, Wedge, Shield Wall (tanks and melee in front) and Circle around you.
- **Party frames** in the corner show every ally's class, health and level, with a red countdown when one is down.

![Aggro & Taunts](https://raw.githubusercontent.com/MeherBenSalem/better_with_gold/main/marketing/images/header-aggro.png)

![Aggro you can steer](https://raw.githubusercontent.com/MeherBenSalem/better_with_gold/main/marketing/images/card-aggro.png)

- Enemies remember who hurt them and who healed their targets, and attack whoever holds the most **aggro**. They
  keep their target until someone beats it by 10%, so a fight has a shape.
- The **Bulwark's Challenge** pulls enemies off everyone else.
- A label over every engaged enemy shows who it is after.

![Downed, Not Dead](https://raw.githubusercontent.com/MeherBenSalem/better_with_gold/main/marketing/images/header-downed.png)

![Pull them back up](https://raw.githubusercontent.com/MeherBenSalem/better_with_gold/main/marketing/images/card-downed.png)

![Reviving an ally under fire](https://raw.githubusercontent.com/MeherBenSalem/better_with_gold/main/marketing/images/gifs/revive.gif)

- At zero health an ally **collapses instead of dying**, enemies lose interest, and it bleeds out over **30 seconds**.
- **Crouch beside it for 3 seconds** to revive it (any friendly player can). A Mender does it in 5.
- An ally that bleeds out is carried back to camp and can be summoned again after **5 minutes**.
- Want stakes? Turn on `permadeath` and the fallen join the **Remembrance**.

![The Warbanner](https://raw.githubusercontent.com/MeherBenSalem/better_with_gold/main/marketing/images/header-banner.png)

![Plant the Warbanner](https://raw.githubusercontent.com/MeherBenSalem/better_with_gold/main/marketing/images/card-banner.png)

- Craft it from **3 gold ingots, a black banner and a stick**.
- **Use it on the ground** to plant it: the warband forms up around it in your formation and guards it.
- **Use it in the air** to lift it, and they follow you again.

![Roster & Chronicle](https://raw.githubusercontent.com/MeherBenSalem/better_with_gold/main/marketing/images/header-roster.png)

![Every ally has a story](https://raw.githubusercontent.com/MeherBenSalem/better_with_gold/main/marketing/images/card-roster.png)

- **Press U** for the roster: every ally with a live 3D portrait, health, experience and morale, kills, bosses,
  revives and healing, traits and ability, and buttons to send them to camp, summon or dismiss them.
- The **Chronicle** is written from what actually happened: where they swore the oath, the kill that blooded them, kill
  milestones, the bosses they fought, the times they went down and who pulled them back, the dimensions they crossed.

![Never Lost](https://raw.githubusercontent.com/MeherBenSalem/better_with_gold/main/marketing/images/header-travel.png)

- Allies following you **come along** when you log out and back in, and when you change dimension.
- If one is left behind in an unloaded area, choose **Recall**: the chunk where it was last seen is loaded and it is
  brought to you. Allies told to Hold or Guard banner stay where they are.

![Gold & Gilded Regalia](https://raw.githubusercontent.com/MeherBenSalem/better_with_gold/main/marketing/images/header-gold.png)

- Gold pays your sellswords.
- Each piece of **Reinforced Golden Armour** you wear adds **3% damage** and **5 points of minimum morale** to allies
  within 24 blocks.
- The **Golden Fish** fully heals an ally and adds **30 morale**; a gold nugget adds 5.
- The 17 gold items of *Better With Gold* stay, in the Gilded Regalia tab, with their crafting and smithing recipes.

![Server Owners](https://raw.githubusercontent.com/MeherBenSalem/better_with_gold/main/marketing/images/header-config.png)

![Your rules](https://raw.githubusercontent.com/MeherBenSalem/better_with_gold/main/marketing/images/card-config.png)

- `config/better_with_gold/fellowship.json`: party size, a world-wide ally cap, bleed-out and revive times,
  permadeath, sellsword spawning and prices, the hiring currency, friendly fire, PvP, aggro labels.
- Class files in `config/better_with_gold/classes/`. `/fellowship reload` applies changes.
- Admin commands: `/fellowship recruit`, `/fellowship sellsword`, `/fellowship setlevel`.

![Downloads & Compatibility](https://raw.githubusercontent.com/MeherBenSalem/better_with_gold/main/marketing/images/header-compatibility.png)

| Minecraft | Loaders |
| --- | --- |
| 1.20.1 | Fabric, Forge |
| 1.21.1 | Fabric, NeoForge, Forge |
| 26.2 | Fabric, NeoForge |
| 26.3 | Fabric, NeoForge |

- Fabric builds need **Fabric API**. Nothing else is required, on any loader.
- Install on the server **and** the client (screens, keys and party frames are client side).
- **Remnants | RPG Attribute System**, optional: allies cannot out-level your RPG level (at least level 5).
- Allies are vanilla owned entities, which other Remnants mods treat as yours: Remnants: RPG Mobs, Remnants | Skills
  and Powers, Mob Loot Bags.

![Questions](https://raw.githubusercontent.com/MeherBenSalem/better_with_gold/main/marketing/images/header-faq.png)

**Do my allies die for good?** Not by default: they are downed, then carried back to camp. Turn on `permadeath` if
you want that.

**Can I lose them?** Allies following you travel with you through log-outs and dimension changes, and Recall finds
the ones left behind.

**Do allies hurt players?** No. Allies never attack players unless the server turns on `allowPvp`, and then
just the players who attack their leader.

**Does it need a resource pack?** No. Every effect is vanilla particles and sounds.

**I had Better With Gold.** Update in place: the mod id is the same, so your gold gear stays.

![Gallery](https://raw.githubusercontent.com/MeherBenSalem/better_with_gold/main/marketing/images/header-gallery.png)

See the gallery for screenshots of every class, the wheel, the roster and the Warbanner.
