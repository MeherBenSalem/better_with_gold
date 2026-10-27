package tn.nightbeam.better_with_gold.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The threat one enemy holds against each of its attackers. Damage and healing add threat; the enemy attacks
 * whoever holds the most, but only switches away from its current target when someone beats it by a margin
 * (the switch ratio), so it does not flip every hit. A taunt forces the target for a while. Plain Java.
 */
public final class ThreatTable {
	private final Map<UUID, Double> threat = new HashMap<>();
	private UUID holder;
	private UUID tauntedBy;
	private long tauntUntil;
	private long lastUpdate;

	public void add(UUID who, double amount, long now) {
		if (who == null || !(amount > 0)) {
			return;
		}
		threat.merge(who, amount, Double::sum);
		lastUpdate = now;
	}

	public double get(UUID who) {
		return threat.getOrDefault(who, 0.0);
	}

	public void remove(UUID who) {
		threat.remove(who);
		if (who != null && who.equals(holder)) {
			holder = null;
		}
		if (who != null && who.equals(tauntedBy)) {
			tauntedBy = null;
		}
	}

	public boolean isEmpty() {
		return threat.isEmpty();
	}

	public long lastUpdate() {
		return lastUpdate;
	}

	public UUID holder() {
		return holder;
	}

	/** Multiplies every value; drops the ones that fall under half a point. */
	public void decay(double factor) {
		threat.replaceAll((k, v) -> v * factor);
		threat.values().removeIf(v -> v < 0.5);
		if (holder != null && !threat.containsKey(holder)) {
			holder = null;
		}
	}

	public double top() {
		double best = 0;
		for (double v : threat.values()) {
			best = Math.max(best, v);
		}
		return best;
	}

	/** The taunter jumps to the top with a lead, and holds the enemy until {@code until}. */
	public void taunt(UUID who, long now, long until) {
		double lead = Math.max(top() * 1.5, get(who) + 10);
		threat.put(who, lead);
		tauntedBy = who;
		tauntUntil = until;
		holder = who;
		lastUpdate = now;
	}

	public boolean taunted(long now) {
		return tauntedBy != null && now < tauntUntil;
	}

	public UUID taunter(long now) {
		return taunted(now) ? tauntedBy : null;
	}

	/**
	 * Picks who the enemy should attack. Keeps {@code current} unless the top value beats it by
	 * {@code switchRatio} (1.1 = 10% more). Returns null when nobody has threat.
	 */
	public UUID choose(UUID current, double switchRatio, long now) {
		if (taunted(now) && threat.containsKey(tauntedBy)) {
			holder = tauntedBy;
			return holder;
		}
		UUID best = null;
		double bestValue = 0;
		for (Map.Entry<UUID, Double> e : threat.entrySet()) {
			if (e.getValue() > bestValue) {
				best = e.getKey();
				bestValue = e.getValue();
			}
		}
		if (best == null) {
			holder = null;
			return null;
		}
		UUID keep = current != null && threat.containsKey(current) ? current : holder;
		if (keep != null && threat.containsKey(keep) && !keep.equals(best) && bestValue < get(keep) * switchRatio) {
			holder = keep;
			return keep;
		}
		holder = best;
		return best;
	}

	/** This attacker's share of all threat, 0..1. */
	public double share(UUID who) {
		double total = 0;
		for (double v : threat.values()) {
			total += v;
		}
		return total <= 0 ? 0 : get(who) / total;
	}

	/** Attackers by threat, highest first. */
	public List<Map.Entry<UUID, Double>> ranked() {
		List<Map.Entry<UUID, Double>> list = new ArrayList<>(threat.entrySet());
		list.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
		return list;
	}
}
