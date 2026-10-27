package tn.nightbeam.better_with_gold.core;

/**
 * How an ally fights. A class picks one role; the role decides which goals the ally runs, and the
 * class file tunes the numbers. Plain Java, shared by every Minecraft version.
 */
public enum Role {
	/** Holds the front, taunts enemies off weaker allies. */
	TANK("Tank", true),
	/** Close-range damage. */
	MELEE("Melee", true),
	/** Bow damage from a distance, steps back when enemies close in. */
	RANGED("Ranged", false),
	/** Heals the lowest ally and revives the downed. */
	HEALER("Healer", true),
	/** Ranged spell damage that splashes around the target. */
	CASTER("Caster", false);

	private final String label;
	private final boolean frontLine;

	Role(String label, boolean frontLine) {
		this.label = label;
		this.frontLine = frontLine;
	}

	public String label() {
		return label;
	}

	/** Front-line roles stand in the first rank of a formation. Healers stand second but count here for spacing. */
	public boolean frontLine() {
		return frontLine;
	}

	/** Lower numbers stand closer to the front of a formation. */
	public int rank() {
		return switch (this) {
			case TANK -> 0;
			case MELEE -> 1;
			case HEALER -> 2;
			case CASTER, RANGED -> 3;
		};
	}

	public static Role parse(String text, Role fallback) {
		if (text == null) {
			return fallback;
		}
		for (Role role : values()) {
			if (role.name().equalsIgnoreCase(text.trim())) {
				return role;
			}
		}
		return fallback;
	}
}
