# For mod developers

Allies are entities of type `better_with_gold:ally` (class `tn.nightbeam.better_with_gold.ally.AllyEntity`).

- They implement vanilla `OwnableEntity`: `getOwnerUUID()` is the leader. Mods that already respect tamed animals
  treat allies the same way.
- Everything Fellowship knows about an ally is the JSON string in the entity's `Fellowship` NBT tag: name, epithet,
  class id, level, experience, traits, morale, order, owner, stats and the Chronicle entries.
- Allies deal damage through normal damage sources (`mob_attack`, arrows, `indirect_magic`), with the ally as the
  attacker, so damage events in other mods see them.
- The aggro label above an engaged enemy is a separate entity, `better_with_gold:aggro_label`, never saved.

Optional integration with RPG Attribute System uses its public `RasApi.getLevel(Player)` and nothing else, by reflection.
