# FAQ

**Where do I find allies?** Near village bells, as sellswords for hire. Pay them in gold ingots. See
[getting started](getting-started.md).

**My ally died. Is it gone?** Not by default. It was downed, bled out and went back to camp to recover for
5 minutes; summon it from the roster. With `permadeath` on it is gone for good (and remembered).

**An ally is missing.** Open the wheel and pick Recall. It loads the chunk where the ally was last seen and brings it
to you. If it still cannot be found (for example the area was deleted), it is struck from the roll.

**Can I give orders to one ally?** Orders go to the whole warband in 2.0. You can send single allies to camp or
dismiss them from the roster.

**Do allies follow me through portals?** Yes, when they are following (Follow, Charge, Focus, Fall back). Allies
holding a spot or guarding the banner stay.

**Can allies hurt me or my friends?** No. Friendly fire is off by default (`friendlyFire`), and allies fight
players just when `allowPvp` is on.

**How many allies can I have?** 6 by default (`partySize`, up to 24). A world-wide cap (`worldAllyCap`, 200) keeps
large servers playable.

**Do allies need food?** No. Gold nuggets and the Golden Fish raise their morale; they heal over time if Hardy, and
a Mender heals everyone.

**Can I add my own classes?** Yes: drop a JSON file in `config/better_with_gold/classes/` and run
`/fellowship reload`. See [classes](classes.md).
