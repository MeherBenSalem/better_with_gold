package tn.nightbeam.better_with_gold.core;

import java.util.Locale;

/**
 * A personality trait. Every recruit rolls one or two. Traits change numbers only, so they work the same
 * on every class and every Minecraft version.
 */
public enum Trait {
	STEADFAST("Steadfast", "+10% health and +20% knockback resistance.", new String[]{"the Steadfast", "Ironroot"}),
	SWIFT("Swift", "+12% movement speed.", new String[]{"the Swift", "Quickstep"}),
	BRUTAL("Brutal", "+15% damage.", new String[]{"the Brutal", "Bonebreaker"}),
	KEEN("Keen-eyed", "+15% damage and +2 blocks of range with bows and spells.", new String[]{"Hawkeye", "the Keen"}),
	STALWART("Stalwart", "Bleeds out 50% slower when downed.", new String[]{"the Unbroken", "Last-Stand"}),
	DEVOTED("Devoted", "Morale never drops below 40.", new String[]{"the Faithful", "Oathkeeper"}),
	LUCKY("Lucky", "10% chance to shrug off a hit.", new String[]{"the Lucky", "Fortune's Own"}),
	GREEDY("Greedy", "Costs 30% more gold, deals 10% more damage.", new String[]{"Goldgrasp", "the Greedy"}),
	HARDY("Hardy", "Regains 1 health every 4 seconds out of combat.", new String[]{"the Hardy", "Stonehide"}),
	FEARLESS("Fearless", "Loses no morale when allies fall.", new String[]{"the Fearless", "Dauntless"}),
	MERCIFUL("Merciful", "+25% healing done.", new String[]{"the Kind", "Mercy"});

	private final String label;
	private final String description;
	private final String[] epithets;

	Trait(String label, String description, String[] epithets) {
		this.label = label;
		this.description = description;
		this.epithets = epithets;
	}

	public String label() {
		return label;
	}

	public String description() {
		return description;
	}

	public String epithet(int pick) {
		return epithets[Math.floorMod(pick, epithets.length)];
	}

	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}

	public double healthMultiplier() {
		return this == STEADFAST ? 1.10 : 1.0;
	}

	public double damageMultiplier(Role role) {
		return switch (this) {
			case BRUTAL -> 1.15;
			case GREEDY -> 1.10;
			case KEEN -> role == Role.RANGED || role == Role.CASTER ? 1.15 : 1.0;
			default -> 1.0;
		};
	}

	public double speedMultiplier() {
		return this == SWIFT ? 1.12 : 1.0;
	}

	public double priceMultiplier() {
		return this == GREEDY ? 1.30 : 1.0;
	}

	public double knockbackBonus() {
		return this == STEADFAST ? 0.2 : 0.0;
	}

	public double rangeBonus() {
		return this == KEEN ? 2.0 : 0.0;
	}

	public double bleedOutMultiplier() {
		return this == STALWART ? 1.5 : 1.0;
	}

	public double dodgeChance() {
		return this == LUCKY ? 0.10 : 0.0;
	}

	public double healingMultiplier() {
		return this == MERCIFUL ? 1.25 : 1.0;
	}

	public int moraleFloor() {
		return this == DEVOTED ? 40 : 0;
	}

	public static Trait parse(String id) {
		if (id == null) {
			return null;
		}
		for (Trait trait : values()) {
			if (trait.name().equalsIgnoreCase(id.trim())) {
				return trait;
			}
		}
		return null;
	}
}
