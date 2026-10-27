package tn.nightbeam.better_with_gold.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * The classes recruits can have. The five built-in classes are written to config/better_with_gold/classes
 * on first start; packs edit those files or add new ones. A class file only picks a role and numbers, so a new
 * class works without code.
 */
public final class ClassCatalog {
	public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static final Map<String, AllyClass> CLASSES = new LinkedHashMap<>();
	private static final List<String> PROBLEMS = new ArrayList<>();

	static {
		reset();
	}

	private ClassCatalog() {
	}

	/** Back to the built-in classes. */
	public static synchronized void reset() {
		CLASSES.clear();
		for (AllyClass c : defaults()) {
			CLASSES.put(c.id, c);
		}
	}

	public static List<AllyClass> defaults() {
		List<AllyClass> list = new ArrayList<>();

		AllyClass bulwark = new AllyClass();
		bulwark.id = "bulwark";
		bulwark.name = "Bulwark";
		bulwark.role = "TANK";
		bulwark.color = "#4F8FE0";
		bulwark.description = "Shield and sword. Stands in front and pulls enemies off everyone else.";
		bulwark.baseHealth = 30;
		bulwark.healthPerLevel = 3;
		bulwark.baseDamage = 3;
		bulwark.damagePerLevel = 0.3;
		bulwark.armor = 6;
		bulwark.armorPerLevel = 0.3;
		bulwark.speed = 0.28;
		bulwark.knockbackResistance = 0.4;
		bulwark.threatMultiplier = 2.5;
		bulwark.price = 8;
		bulwark.pricePerLevel = 2;
		bulwark.mainHand = "minecraft:iron_sword";
		bulwark.offHand = "minecraft:shield";
		bulwark.abilityName = "Challenge";
		bulwark.abilityDescription = "Taunts every enemy within 8 blocks that is attacking someone else: they turn on the Bulwark for 6 seconds.";
		bulwark.abilityCooldown = 160;
		bulwark.abilityRadius = 8;
		bulwark.abilityPower = 6;
		bulwark.traits = List.of("steadfast", "swift", "brutal", "stalwart", "devoted", "lucky", "greedy", "hardy", "fearless");
		list.add(bulwark);

		AllyClass skirmisher = new AllyClass();
		skirmisher.id = "skirmisher";
		skirmisher.name = "Skirmisher";
		skirmisher.role = "MELEE";
		skirmisher.color = "#D9483B";
		skirmisher.description = "Fast blade work. Swings through every enemy around it.";
		skirmisher.baseHealth = 22;
		skirmisher.healthPerLevel = 2;
		skirmisher.baseDamage = 5;
		skirmisher.damagePerLevel = 0.5;
		skirmisher.armor = 2;
		skirmisher.armorPerLevel = 0.2;
		skirmisher.speed = 0.33;
		skirmisher.threatMultiplier = 1.0;
		skirmisher.price = 7;
		skirmisher.pricePerLevel = 2;
		skirmisher.mainHand = "better_with_gold:golden_scythe";
		skirmisher.abilityName = "Whirl";
		skirmisher.abilityDescription = "Every 6 seconds, hits every enemy within 3 blocks for 60% damage.";
		skirmisher.abilityCooldown = 120;
		skirmisher.abilityRadius = 3;
		skirmisher.abilityPower = 0.6;
		skirmisher.traits = List.of("steadfast", "swift", "brutal", "stalwart", "devoted", "lucky", "greedy", "hardy", "fearless");
		list.add(skirmisher);

		AllyClass ranger = new AllyClass();
		ranger.id = "ranger";
		ranger.name = "Ranger";
		ranger.role = "RANGED";
		ranger.color = "#5DBB63";
		ranger.description = "Bow from a distance. Steps back when enemies get close.";
		ranger.baseHealth = 20;
		ranger.healthPerLevel = 2;
		ranger.baseDamage = 4;
		ranger.damagePerLevel = 0.45;
		ranger.armor = 1;
		ranger.armorPerLevel = 0.15;
		ranger.speed = 0.31;
		ranger.threatMultiplier = 0.8;
		ranger.price = 7;
		ranger.pricePerLevel = 2;
		ranger.mainHand = "minecraft:bow";
		ranger.abilityName = "Volley";
		ranger.abilityDescription = "Every 10 seconds, looses 3 arrows at once in a spread.";
		ranger.abilityCooldown = 200;
		ranger.abilityRadius = 0;
		ranger.abilityPower = 3;
		ranger.preferredRange = 11;
		ranger.attackInterval = 30;
		ranger.traits = List.of("swift", "brutal", "keen", "stalwart", "devoted", "lucky", "greedy", "hardy", "fearless");
		list.add(ranger);

		AllyClass mender = new AllyClass();
		mender.id = "mender";
		mender.name = "Mender";
		mender.role = "HEALER";
		mender.color = "#F2C94C";
		mender.description = "Keeps everyone standing. Heals the most hurt ally and pulls the downed back up.";
		mender.baseHealth = 20;
		mender.healthPerLevel = 2;
		mender.baseDamage = 2;
		mender.damagePerLevel = 0.2;
		mender.armor = 2;
		mender.armorPerLevel = 0.2;
		mender.speed = 0.30;
		mender.threatMultiplier = 1.0;
		mender.price = 10;
		mender.pricePerLevel = 3;
		mender.mainHand = "minecraft:book";
		mender.abilityName = "Mend";
		mender.abilityDescription = "Every 3 seconds, heals the most hurt ally or leader within 12 blocks for 4 health, plus 0.25 per level. Revives the downed.";
		mender.abilityCooldown = 60;
		mender.abilityRadius = 12;
		mender.abilityPower = 4;
		mender.traits = List.of("steadfast", "swift", "stalwart", "devoted", "lucky", "greedy", "hardy", "fearless", "merciful");
		list.add(mender);

		AllyClass arcanist = new AllyClass();
		arcanist.id = "arcanist";
		arcanist.name = "Arcanist";
		arcanist.role = "CASTER";
		arcanist.color = "#A06BE0";
		arcanist.description = "Throws arc bolts from range. Calls down a storm on packed enemies.";
		arcanist.baseHealth = 18;
		arcanist.healthPerLevel = 1.5;
		arcanist.baseDamage = 5;
		arcanist.damagePerLevel = 0.5;
		arcanist.armor = 0;
		arcanist.armorPerLevel = 0.1;
		arcanist.speed = 0.30;
		arcanist.threatMultiplier = 1.0;
		arcanist.price = 12;
		arcanist.pricePerLevel = 3;
		arcanist.mainHand = "minecraft:blaze_rod";
		arcanist.abilityName = "Arc Storm";
		arcanist.abilityDescription = "Every 12 seconds, strikes every enemy within 5 blocks of its target for 120% damage.";
		arcanist.abilityCooldown = 240;
		arcanist.abilityRadius = 5;
		arcanist.abilityPower = 1.2;
		arcanist.preferredRange = 10;
		arcanist.attackInterval = 40;
		arcanist.traits = List.of("swift", "brutal", "keen", "stalwart", "devoted", "lucky", "greedy", "hardy", "fearless");
		list.add(arcanist);
		return list;
	}

	public static synchronized AllyClass get(String id) {
		AllyClass c = id == null ? null : CLASSES.get(id);
		return c != null ? c : CLASSES.getOrDefault("bulwark", CLASSES.values().iterator().next());
	}

	public static synchronized boolean has(String id) {
		return id != null && CLASSES.containsKey(id);
	}

	public static synchronized Collection<AllyClass> all() {
		return Collections.unmodifiableCollection(new ArrayList<>(CLASSES.values()));
	}

	public static synchronized List<AllyClass> enabled() {
		List<AllyClass> list = new ArrayList<>();
		for (AllyClass c : CLASSES.values()) {
			if (c.enabled) {
				list.add(c);
			}
		}
		return list;
	}

	public static synchronized List<String> problems() {
		return List.copyOf(PROBLEMS);
	}

	/**
	 * Writes the built-in files when the folder has no class files, then loads every *.json in it. A broken file is
	 * skipped and reported; the built-in class with the same id stays.
	 */
	public static synchronized void load(Path dir) {
		reset();
		PROBLEMS.clear();
		try {
			Files.createDirectories(dir);
			boolean empty;
			try (Stream<Path> existing = Files.list(dir)) {
				empty = existing.noneMatch(p -> p.getFileName().toString().endsWith(".json"));
			}
			if (empty) {
				for (AllyClass c : defaults()) {
					try (Writer writer = Files.newBufferedWriter(dir.resolve(c.id + ".json"), StandardCharsets.UTF_8)) {
						GSON.toJson(c, writer);
					}
				}
			}
			try (Stream<Path> files = Files.list(dir)) {
				for (Path file : files.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList()) {
					try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
						AllyClass c = GSON.fromJson(reader, AllyClass.class);
						if (c == null || !c.sanitize()) {
							PROBLEMS.add(file.getFileName() + ": needs an \"id\" of lowercase letters, digits or _");
							continue;
						}
						CLASSES.put(c.id, c);
					} catch (IOException | JsonParseException ex) {
						PROBLEMS.add(file.getFileName() + ": " + ex.getMessage());
					}
				}
			}
		} catch (IOException ex) {
			PROBLEMS.add(dir + ": " + ex.getMessage());
		}
		if (enabled().isEmpty()) {
			PROBLEMS.add("every class is disabled; the built-in classes are used instead");
			reset();
		}
	}
}
