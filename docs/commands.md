# Commands

All commands start with `/fellowship`. Leader commands work for any player on their own warband; admin commands
need permission level 2 (operators, cheats on).

## Leader

| Command | What it does |
|---|---|
| `/fellowship` | One-line summary: allies in the world, at camp and fallen; order, formation, banner |
| `/fellowship roster` | Opens the roster screen (same as the **U** key) |
| `/fellowship order <follow\|hold\|charge\|focus\|fall_back\|guard_banner>` | Gives the whole warband an order. `focus` targets what you look at |
| `/fellowship formation <line\|wedge\|shield_wall\|circle>` | Sets the formation used when following or guarding the banner |
| `/fellowship recall` | Brings every ally to you, loading the chunk where a missing one was last seen, and summons everyone ready at camp |
| `/fellowship summon [name\|all]` | Brings allies back from camp |
| `/fellowship stow <name\|all>` | Sends allies to camp (they keep everything) |
| `/fellowship dismiss <name>` | Releases an ally from service for good |
| `/fellowship chronicle <name>` | Prints an ally's Chronicle in chat |
| `/fellowship remembrance` | Lists the fallen (permadeath) |
| `/fellowship banner lift` | Lifts your planted Warbanner from anywhere |

## Admin (permission level 2)

| Command | What it does |
|---|---|
| `/fellowship recruit <class> [level]` | Adds a new ally of that class to your warband |
| `/fellowship sellsword [class] [level]` | Without a class: places a sellsword at the nearest village bell, as natural spawning would. With a class: places one for hire in front of you |
| `/fellowship setlevel <level>` | Sets the level of the ally you look at (or the nearest of yours) |
| `/fellowship reload` | Reloads `fellowship.json` and the class files |

Allies are entities of type `better_with_gold:ally`; vanilla commands work on them, for example
`/damage @e[type=better_with_gold:ally,limit=1,sort=nearest] 100` to see the downed state. A bare
`/summon better_with_gold:ally` makes a random sellsword for hire.
