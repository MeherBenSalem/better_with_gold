# Getting started

## 1. Find a sellsword

Sellswords for hire turn up near village bells: every 3 minutes the game may place one (half the time) at a village
within 96 blocks of you, up to 3 waiting at once. Each one shows their name, class, level and price above their head:

> **Brenna the Steadfast · Bulwark 2 · 10 gold**

Use them with an empty hand to hear their traits, or **use them while holding gold ingots** to hire them. The price
is 7 to 12 gold at level 1, plus 2 or 3 per level; the Greedy trait costs 30% more. You can lead up to **6 allies**
(config `partySize`, up to 24). Sellswords that nobody hires move on after 40 minutes.

Creative and server admins can also use `/fellowship recruit <class> [level]` and `/fellowship sellsword [class] [level]`.

## 2. Lead them

- **Command wheel:** hold **G**, point at an order, let go: Follow, Charge, Focus (your crosshair target), Hold,
  Fall back, Guard banner, Formation (Line, Wedge, Shield Wall, Circle) and Recall.
- **Roster:** press **U** (or use an ally with an empty hand) for every ally's level, health, morale, traits,
  ability, stats and Chronicle, plus the Remembrance of the fallen.
- **Party frames** in the top-left corner show each ally's health, and a red countdown when someone is down.

## 3. Keep them standing

At zero health an ally is **downed, not dead**: it collapses, enemies lose interest, and it bleeds out over 30
seconds. **Crouch beside it for 3 seconds** to revive it (any friendly player can); a Mender revives it in 5.
An ally that bleeds out is carried back to camp and can be summoned again after 5 minutes. With `permadeath` on, it
is gone for good and its name joins the Remembrance.

## 4. The Warbanner

Craft it from 3 gold ingots, a black banner and a stick (a gold ingot either side of the banner, one on top, the
stick below). **Use it on the ground** to plant it: the warband forms up around it in your formation and guards
it. **Use it in the air** to lift it; the band follows you again.

## 5. Gear, gold and the Golden Fish

- **Gear:** crouch and use an ally while holding a weapon, shield or armour piece to give it to them; they hand back
  what they wore. Their stats are plain attributes, so any mod's gear works.
- **Golden Fish:** feeding one to an ally restores all its health and adds 30 morale. A gold nugget adds 5 morale.
- **Gilded Regalia:** each piece of Reinforced Golden Armour you wear adds 3% damage and 5 points of minimum morale
  to allies within 24 blocks.
- **Name tag:** use a named name tag on an ally to rename it.

## 6. Levels, morale and the Chronicle

Allies earn experience for every kill they help with (half the enemy's health, at most 100; helpers get half) and
reach level 20 (titles at 5, 10, 15 and 20: Veteran, Champion, Hero, Legend). Morale (0 to 100) goes up with kills
and revives and down when allies fall; at 80 or more allies deal 10% more damage, at 25 or less 15% less.

Every ally keeps a **Chronicle** written from what actually happened: where it swore the oath, the kill that blooded it, kill
milestones, bosses it fought or slew, the times it went down and who pulled it back, the dimensions it crossed into.

## 7. Travelling

Allies following you come along when you log out and back in, and when you change dimension. If one is left behind
in an unloaded area, choose **Recall** on the wheel (or `/fellowship recall`): the chunk where it was last seen is
loaded and it is brought to you. Allies told to Hold or Guard banner stay where they are.
