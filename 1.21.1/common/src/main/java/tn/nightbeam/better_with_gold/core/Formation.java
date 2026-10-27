package tn.nightbeam.better_with_gold.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Where each ally stands relative to an anchor (the leader, or a planted Warbanner). Offsets use a local frame
 * where +z is the way the anchor faces and +x is its left. Allies are ordered by role rank (tanks first), so the
 * same warband always lands in the same slots.
 */
public enum Formation {
	LINE("Line"),
	WEDGE("Wedge"),
	SHIELD_WALL("Shield Wall"),
	CIRCLE("Circle");

	public static final double SPACING = 1.6;

	private final String label;

	Formation(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}

	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}

	public Formation next() {
		return values()[(ordinal() + 1) % values().length];
	}

	public static Formation parse(String text, Formation fallback) {
		if (text == null) {
			return fallback;
		}
		String t = text.trim().replace(' ', '_').replace('-', '_');
		for (Formation f : values()) {
			if (f.name().equalsIgnoreCase(t)) {
				return f;
			}
		}
		return fallback;
	}

	/** A slot in the local frame. */
	public record Offset(double x, double z) {
		/** Rotates the local offset into world x/z for an anchor with Minecraft yaw {@code yawDegrees}. */
		public double[] toWorld(double yawDegrees) {
			double yaw = Math.toRadians(yawDegrees);
			// Minecraft: facing vector for yaw is (-sin, cos). Left of that is (cos, sin).
			double fx = -Math.sin(yaw);
			double fz = Math.cos(yaw);
			double lx = fz;
			double lz = -fx;
			return new double[]{lx * x + fx * z, lz * x + fz * z};
		}
	}

	/**
	 * Offsets for {@code roles.size()} allies, in the order given. Call {@link #order(List)} first so the same
	 * roles always get the same slots.
	 */
	public List<Offset> offsets(List<Role> roles) {
		int n = roles.size();
		List<Offset> out = new ArrayList<>(n);
		switch (this) {
			case LINE -> {
				for (int i = 0; i < n; i++) {
					out.add(new Offset((i - (n - 1) / 2.0) * SPACING, -2.0));
				}
			}
			case WEDGE -> {
				for (int i = 0; i < n; i++) {
					if (i == 0) {
						out.add(new Offset(0, -2.0));
						continue;
					}
					int step = (i + 1) / 2;
					double side = i % 2 == 1 ? 1 : -1;
					out.add(new Offset(side * step * SPACING * 0.9, -2.0 - step * SPACING * 0.9));
				}
			}
			case SHIELD_WALL -> {
				List<Integer> front = new ArrayList<>();
				List<Integer> back = new ArrayList<>();
				for (int i = 0; i < n; i++) {
					Role r = roles.get(i);
					if (r == Role.TANK || r == Role.MELEE) {
						front.add(i);
					} else {
						back.add(i);
					}
				}
				if (front.isEmpty() && !back.isEmpty()) {
					front.add(back.remove(0));
				}
				Offset[] slots = new Offset[n];
				for (int k = 0; k < front.size(); k++) {
					slots[front.get(k)] = new Offset((k - (front.size() - 1) / 2.0) * 1.2, 2.5);
				}
				for (int k = 0; k < back.size(); k++) {
					slots[back.get(k)] = new Offset((k - (back.size() - 1) / 2.0) * SPACING, -1.8);
				}
				for (Offset slot : slots) {
					out.add(slot);
				}
			}
			case CIRCLE -> {
				double radius = Math.max(2.5, n * SPACING / (2 * Math.PI));
				for (int i = 0; i < n; i++) {
					double a = 2 * Math.PI * i / Math.max(1, n);
					out.add(new Offset(Math.sin(a) * radius, Math.cos(a) * radius));
				}
			}
		}
		return out;
	}

	/** Sorts by role rank, then by a stable key, and returns the sorted keys. */
	public static <K extends Comparable<K>> List<K> order(List<Member<K>> members) {
		List<Member<K>> sorted = new ArrayList<>(members);
		sorted.sort(Comparator.<Member<K>>comparingInt(m -> m.role().rank()).thenComparing(Member::key));
		List<K> keys = new ArrayList<>();
		for (Member<K> m : sorted) {
			keys.add(m.key());
		}
		return keys;
	}

	public record Member<K>(K key, Role role) {
	}
}
