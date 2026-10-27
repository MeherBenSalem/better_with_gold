package tn.nightbeam.better_with_gold.core;

import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * config/better_with_gold/fellowship.json. Read on server start and on /fellowship reload; unknown or broken
 * values fall back to these defaults and are written back, so the file always shows every option.
 */
public final class FellowshipConfig {
	public static FellowshipConfig INSTANCE = new FellowshipConfig();

	/** Allies one leader can have at once (hired and stowed together). */
	public int partySize = 6;
	/** Most allies of all leaders together that can be loaded at once; hiring and sellswords stop there. */
	public int worldAllyCap = 200;
	/** The highest level an ally can reach. */
	public int levelCap = 20;
	/** Seconds a downed ally lasts before it bleeds out. */
	public int bleedOutSeconds = 30;
	/** When true, an ally that bleeds out is gone for good and joins the Remembrance list. */
	public boolean permadeath = false;
	/** Seconds a player must crouch beside a downed ally to revive it. */
	public int reviveSeconds = 3;
	/** Seconds a Mender needs to revive a downed ally. */
	public int menderReviveSeconds = 5;
	/** Seconds a bled-out ally rests at camp before it can be summoned again (permadeath off). */
	public int recoverySeconds = 300;

	/** Sellswords for hire turn up near village bells. */
	public boolean sellswordsSpawn = true;
	/** How often, per player, the game tries to place a sellsword near a village. */
	public int sellswordIntervalSeconds = 180;
	/** Chance (0 to 1) that each try places a sellsword. */
	public double sellswordChance = 0.5;
	/** Most sellswords that wait at one village at a time. */
	public int maxSellswordsNearVillage = 3;
	/** Minutes an unhired sellsword waits before moving on. */
	public int sellswordStayMinutes = 40;
	/** Multiplies every hiring price. */
	public double priceMultiplier = 1.0;
	/** The item that pays for hiring. */
	public String currency = "minecraft:gold_ingot";

	/** Leaders can hurt their own allies. */
	public boolean friendlyFire = false;
	/** Allies fight players who attack their leader. */
	public boolean allowPvp = false;
	/** Shows, above each engaged enemy, who holds its aggro. */
	public boolean aggroLabels = true;
	/** An enemy switches target only when someone beats its current target's aggro by this much. */
	public double aggroSwitchRatio = 1.1;
	/** Following allies further than this many blocks from the leader teleport to it. */
	public int teleportDistance = 40;
	/** Each piece of Reinforced Golden Armour the leader wears adds this to allies' damage, within 24 blocks. */
	public double regaliaDamagePerPiece = 0.03;
	/** Each piece also raises the lowest morale allies can fall to. */
	public int regaliaMoraleFloorPerPiece = 5;
	/** With RPG Attribute System installed, allies cannot out-level the leader's RPG level (at least this). */
	public int rpgLevelCapFloor = 5;
	/** Turns the RPG Attribute System level cap on or off. */
	public boolean rpgLevelCap = true;

	public FellowshipConfig sanitize() {
		partySize = clamp(partySize, 1, 24);
		worldAllyCap = clamp(worldAllyCap, 1, 2000);
		levelCap = clamp(levelCap, 1, 100);
		bleedOutSeconds = clamp(bleedOutSeconds, 3, 600);
		reviveSeconds = clamp(reviveSeconds, 1, 60);
		menderReviveSeconds = clamp(menderReviveSeconds, 1, 60);
		recoverySeconds = clamp(recoverySeconds, 0, 86400);
		sellswordIntervalSeconds = clamp(sellswordIntervalSeconds, 10, 86400);
		sellswordChance = Math.max(0, Math.min(1, Double.isNaN(sellswordChance) ? 0.5 : sellswordChance));
		maxSellswordsNearVillage = clamp(maxSellswordsNearVillage, 0, 16);
		sellswordStayMinutes = clamp(sellswordStayMinutes, 1, 1440);
		priceMultiplier = Math.max(0, Math.min(100, Double.isNaN(priceMultiplier) ? 1 : priceMultiplier));
		currency = currency == null || currency.isBlank() ? "minecraft:gold_ingot" : currency.trim();
		aggroSwitchRatio = Math.max(1.0, Math.min(3.0, Double.isNaN(aggroSwitchRatio) ? 1.1 : aggroSwitchRatio));
		teleportDistance = clamp(teleportDistance, 12, 128);
		regaliaDamagePerPiece = Math.max(0, Math.min(1, Double.isNaN(regaliaDamagePerPiece) ? 0.03 : regaliaDamagePerPiece));
		regaliaMoraleFloorPerPiece = clamp(regaliaMoraleFloorPerPiece, 0, 25);
		rpgLevelCapFloor = clamp(rpgLevelCapFloor, 1, 100);
		return this;
	}

	private static int clamp(int v, int min, int max) {
		return Math.max(min, Math.min(max, v));
	}

	public static FellowshipConfig load(Path file) {
		FellowshipConfig config = null;
		if (Files.isRegularFile(file)) {
			try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
				config = ClassCatalog.GSON.fromJson(reader, FellowshipConfig.class);
			} catch (IOException | JsonParseException ex) {
				config = null;
			}
		}
		if (config == null) {
			config = new FellowshipConfig();
		}
		config.sanitize();
		try {
			Files.createDirectories(file.getParent());
			try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
				ClassCatalog.GSON.toJson(config, writer);
			}
		} catch (IOException ignored) {
			// A read-only config folder still runs with the values in memory.
		}
		INSTANCE = config;
		return config;
	}
}
