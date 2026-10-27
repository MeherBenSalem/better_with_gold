package tn.nightbeam.better_with_gold.ally;

import tn.nightbeam.better_with_gold.core.Formation;
import tn.nightbeam.better_with_gold.core.Order;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One leader's warband as saved in world/better_with_gold/fellowship/{uuid}.json: the standing order and
 * formation, the planted banner, allies resting at camp, where each ally in the world was last seen, and the
 * fallen. Plain data read with Gson.
 */
public final class WarbandState {
	public String owner = "";
	public String ownerName = "";
	public String order = "follow";
	public String formation = "line";
	public Banner banner;
	public List<Stowed> stowed = new ArrayList<>();
	public Map<String, Seen> seen = new HashMap<>();
	public List<Fallen> fallen = new ArrayList<>();

	/** Not saved: the enemy the leader told everyone to focus, and when Fall back ends. */
	public transient UUID focus;
	public transient long fallBackUntil;
	public transient long lastSaveRecord;
	public transient String lastParty = "";
	public transient boolean dirty;

	public static final class Banner {
		public String dimension = "";
		public int x;
		public int y;
		public int z;
		public float yaw;
	}

	/** An ally resting at camp: the full entity, saved as text. */
	public static final class Stowed {
		public String id = "";
		public String entity = "";
		public String record = "";
		/** Game time when a bled-out ally may fight again; 0 when ready. */
		public long recoverAt;
		/** True when it went to camp only to travel with the leader (log out, portal); comes back by itself. */
		public boolean travelling;
	}

	public static final class Seen {
		public String dimension = "";
		public double x;
		public double y;
		public double z;
		public String name = "";
		public String classId = "";
		public int level = 1;
	}

	public static final class Fallen {
		public String name = "";
		public String classId = "";
		public int level;
		public long day;
		public String cause = "";
		public String headline = "";
		public List<String> chronicle = new ArrayList<>();
	}

	public Order orderValue() {
		return Order.parse(order, Order.FOLLOW);
	}

	public Formation formationValue() {
		return Formation.parse(formation, Formation.LINE);
	}

	public WarbandState repair() {
		order = order == null ? "follow" : order;
		formation = formation == null ? "line" : formation;
		stowed = stowed == null ? new ArrayList<>() : stowed;
		seen = seen == null ? new HashMap<>() : seen;
		fallen = fallen == null ? new ArrayList<>() : fallen;
		stowed.removeIf(s -> s == null || s.entity == null || s.entity.isEmpty());
		return this;
	}

	public int rosterSize() {
		return seen.size() + stowed.size();
	}
}
