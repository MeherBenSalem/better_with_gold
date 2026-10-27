package tn.nightbeam.better_with_gold.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything Fellowship remembers about one ally, apart from the vanilla entity data (health, gear, position).
 * Stored as one JSON string on the entity and in the leader's roster file, so it is identical on every version.
 */
public final class AllyRecord {
	private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

	public String name = "";
	public String epithet = "";
	public String classId = "bulwark";
	public int level = 1;
	public int xp = 0;
	public List<String> traits = new ArrayList<>();
	public int morale = Progression.MORALE_START;
	public int variant = 0;
	public String order = "follow";
	/** Leader UUID as text; empty while the ally is a sellsword for hire. */
	public String owner = "";
	public String ownerName = "";
	public long swornDay = -1;
	/** Game time after which an unhired sellsword leaves. */
	public long leaveAt = 0;
	public Stats stats = new Stats();
	public List<Chronicle.Entry> chronicle = new ArrayList<>();
	public List<String> visited = new ArrayList<>();

	public static final class Stats {
		public int kills;
		public int bossKills;
		public int bossFights;
		public int downs;
		public int revivesGiven;
		public int revivesReceived;
		public double healing;
		public double damage;
	}

	public boolean hired() {
		return owner != null && !owner.isEmpty();
	}

	public AllyClass allyClass() {
		return ClassCatalog.get(classId);
	}

	public List<Trait> traitList() {
		List<Trait> list = new ArrayList<>();
		for (String id : traits) {
			Trait t = Trait.parse(id);
			if (t != null && !list.contains(t)) {
				list.add(t);
			}
		}
		return list;
	}

	public boolean has(Trait trait) {
		return traitList().contains(trait);
	}

	public Order orderValue() {
		return Order.parse(order, Order.FOLLOW);
	}

	public String fullName() {
		return epithet == null || epithet.isEmpty() ? name : name + " " + epithet;
	}

	public String toJson() {
		return GSON.toJson(this);
	}

	public static AllyRecord fromJson(String json) {
		if (json == null || json.isEmpty()) {
			return new AllyRecord();
		}
		try {
			AllyRecord r = GSON.fromJson(json, AllyRecord.class);
			return r == null ? new AllyRecord() : r.repair();
		} catch (JsonParseException ex) {
			return new AllyRecord();
		}
	}

	/** Fills nulls left by older or hand-edited data. */
	public AllyRecord repair() {
		name = name == null ? "" : name;
		epithet = epithet == null ? "" : epithet;
		classId = classId == null ? "bulwark" : classId;
		level = Math.max(1, level);
		xp = Math.max(0, xp);
		traits = traits == null ? new ArrayList<>() : new ArrayList<>(traits);
		order = order == null ? "follow" : order;
		owner = owner == null ? "" : owner;
		ownerName = ownerName == null ? "" : ownerName;
		stats = stats == null ? new Stats() : stats;
		chronicle = chronicle == null ? new ArrayList<>() : new ArrayList<>(chronicle);
		visited = visited == null ? new ArrayList<>() : new ArrayList<>(visited);
		morale = Math.max(0, Math.min(100, morale));
		return this;
	}

	/**
	 * Adds experience; returns how many levels were gained (0 when capped).
	 */
	public int addXp(int amount, int levelCap) {
		if (amount <= 0 || level >= levelCap) {
			if (level >= levelCap) {
				xp = 0;
			}
			return 0;
		}
		xp += amount;
		int gained = 0;
		while (level < levelCap && xp >= Progression.xpToNext(level)) {
			xp -= Progression.xpToNext(level);
			level++;
			gained++;
		}
		if (level >= levelCap) {
			xp = 0;
		}
		return gained;
	}
}
