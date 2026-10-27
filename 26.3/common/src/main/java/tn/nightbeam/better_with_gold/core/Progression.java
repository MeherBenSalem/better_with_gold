package tn.nightbeam.better_with_gold.core;

import java.util.List;

/** Experience, levels, titles, hiring prices and morale for allies. Plain Java. */
public final class Progression {
	private Progression() {
	}

	/** Experience needed to go from {@code level} to the next one. */
	public static int xpToNext(int level) {
		return 12 + 8 * Math.max(1, level);
	}

	/** Total experience from level 1 to the start of {@code level}. */
	public static int totalXpFor(int level) {
		int sum = 0;
		for (int l = 1; l < level; l++) {
			sum += xpToNext(l);
		}
		return sum;
	}

	/** Experience for helping to kill something with this much maximum health. */
	public static int killXp(double maxHealth) {
		return (int) Math.max(1, Math.min(100, Math.round(maxHealth / 2.0)));
	}

	public static String title(int level) {
		if (level >= 20) {
			return "Legend";
		}
		if (level >= 15) {
			return "Hero";
		}
		if (level >= 10) {
			return "Champion";
		}
		if (level >= 5) {
			return "Veteran";
		}
		return "Recruit";
	}

	/** True when reaching {@code level} earns a new title. */
	public static boolean titleLevel(int level) {
		return level == 5 || level == 10 || level == 15 || level == 20;
	}

	public static int price(AllyClass c, int level, List<Trait> traits, double multiplier) {
		double p = c.price + (double) c.pricePerLevel * (Math.max(1, level) - 1);
		for (Trait t : traits) {
			p *= t.priceMultiplier();
		}
		return (int) Math.max(0, Math.round(p * multiplier));
	}

	public static double healthMultiplier(List<Trait> traits) {
		double m = 1;
		for (Trait t : traits) {
			m *= t.healthMultiplier();
		}
		return m;
	}

	public static double damageMultiplier(List<Trait> traits, Role role) {
		double m = 1;
		for (Trait t : traits) {
			m *= t.damageMultiplier(role);
		}
		return m;
	}

	public static double speedMultiplier(List<Trait> traits) {
		double m = 1;
		for (Trait t : traits) {
			m *= t.speedMultiplier();
		}
		return m;
	}

	// Morale: 0..100. High morale hits harder, low morale hits softer.

	public static final int MORALE_START = 70;

	public static double moraleDamageMultiplier(int morale) {
		if (morale >= 80) {
			return 1.10;
		}
		if (morale <= 25) {
			return 0.85;
		}
		return 1.0;
	}

	public static String moraleState(int morale) {
		if (morale >= 80) {
			return "Inspired";
		}
		if (morale <= 25) {
			return "Shaken";
		}
		return "Steady";
	}

	public static int clampMorale(int morale, List<Trait> traits, int regaliaFloor) {
		int floor = regaliaFloor;
		for (Trait t : traits) {
			floor = Math.max(floor, t.moraleFloor());
		}
		return Math.max(Math.min(floor, 100), Math.min(100, morale));
	}

	/** Morale drifts one point a minute towards 60. */
	public static int drift(int morale) {
		if (morale > 60) {
			return morale - 1;
		}
		if (morale < 60) {
			return morale + 1;
		}
		return morale;
	}
}
