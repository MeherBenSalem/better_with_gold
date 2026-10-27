package tn.nightbeam.better_with_gold.core;

import java.util.ArrayList;
import java.util.List;

/**
 * One class file from config/better_with_gold/classes/*.json. Every field has a safe default, so a pack
 * can write a new class with only an id, a name and a role. Read with Gson; plain Java.
 */
public final class AllyClass {
	public String id = "";
	public String name = "";
	public String role = "MELEE";
	/** Banner and HUD colour, "#RRGGBB". */
	public String color = "#C8A04A";
	public String description = "";
	public boolean enabled = true;

	public double baseHealth = 24;
	public double healthPerLevel = 2;
	public double baseDamage = 3;
	public double damagePerLevel = 0.35;
	public double armor = 2;
	public double armorPerLevel = 0.2;
	public double speed = 0.30;
	public double knockbackResistance = 0;
	/** Multiplies the threat this class makes with damage. Tanks make more. */
	public double threatMultiplier = 1.0;

	/** Gold ingots to hire at level 1, plus this much per level above 1. */
	public int price = 8;
	public int pricePerLevel = 2;

	public String mainHand = "minecraft:iron_sword";
	public String offHand = "";

	public String abilityName = "";
	public String abilityDescription = "";
	/** Ticks between ability uses. */
	public int abilityCooldown = 160;
	public double abilityRadius = 8;
	/** Meaning depends on the role: taunt seconds, heal amount, arrows per volley, splash share. */
	public double abilityPower = 1;

	/** Ranged and caster classes keep this many blocks from their target. */
	public double preferredRange = 0;
	/** Ticks between ranged attacks. */
	public int attackInterval = 30;

	public List<String> traits = new ArrayList<>();
	/** Number of skin textures: textures/entity/ally/{id}_{n}.png for n in 0..skins-1. */
	public int skins = 4;

	public Role role() {
		return Role.parse(role, Role.MELEE);
	}

	public int colorRgb() {
		String c = color == null ? "" : color.trim();
		if (c.startsWith("#")) {
			c = c.substring(1);
		}
		try {
			return Integer.parseInt(c, 16) & 0xFFFFFF;
		} catch (NumberFormatException ex) {
			return 0xC8A04A;
		}
	}

	public List<Trait> allowedTraits() {
		List<Trait> list = new ArrayList<>();
		if (traits != null) {
			for (String id : traits) {
				Trait trait = Trait.parse(id);
				if (trait != null && !list.contains(trait)) {
					list.add(trait);
				}
			}
		}
		return list;
	}

	public double health(int level) {
		return baseHealth + healthPerLevel * (Math.max(1, level) - 1);
	}

	public double damage(int level) {
		return baseDamage + damagePerLevel * (Math.max(1, level) - 1);
	}

	public double armor(int level) {
		return armor + armorPerLevel * (Math.max(1, level) - 1);
	}

	/** Clamps every number into a range that cannot break the game. Returns false when the file is unusable. */
	public boolean sanitize() {
		if (id == null || id.isBlank() || !id.matches("[a-z0-9_]+")) {
			return false;
		}
		if (name == null || name.isBlank()) {
			name = id;
		}
		description = description == null ? "" : description;
		abilityName = abilityName == null ? "" : abilityName;
		abilityDescription = abilityDescription == null ? "" : abilityDescription;
		mainHand = mainHand == null ? "" : mainHand;
		offHand = offHand == null ? "" : offHand;
		baseHealth = clamp(baseHealth, 1, 1024);
		healthPerLevel = clamp(healthPerLevel, 0, 64);
		baseDamage = clamp(baseDamage, 0, 256);
		damagePerLevel = clamp(damagePerLevel, 0, 16);
		armor = clamp(armor, 0, 30);
		armorPerLevel = clamp(armorPerLevel, 0, 4);
		speed = clamp(speed, 0.05, 0.6);
		knockbackResistance = clamp(knockbackResistance, 0, 1);
		threatMultiplier = clamp(threatMultiplier, 0, 10);
		price = (int) clamp(price, 0, 4096);
		pricePerLevel = (int) clamp(pricePerLevel, 0, 1024);
		abilityCooldown = (int) clamp(abilityCooldown, 10, 72000);
		abilityRadius = clamp(abilityRadius, 0, 32);
		abilityPower = clamp(abilityPower, 0, 1000);
		preferredRange = clamp(preferredRange, 0, 32);
		attackInterval = (int) clamp(attackInterval, 5, 1200);
		skins = (int) clamp(skins, 1, 16);
		if (traits == null) {
			traits = new ArrayList<>();
		}
		return true;
	}

	private static double clamp(double value, double min, double max) {
		if (Double.isNaN(value)) {
			return min;
		}
		return Math.max(min, Math.min(max, value));
	}
}
