package tn.nightbeam.better_with_gold.core;

import java.util.Locale;

/** What the warband is told to do. Each ally keeps its own order; the wheel and commands set it for the whole band. */
public enum Order {
	FOLLOW("Follow", "Stay in formation around the leader."),
	HOLD("Hold", "Stand where you are and defend this spot."),
	CHARGE("Charge", "Attack every enemy near the leader."),
	FOCUS("Focus", "Everyone attacks the leader's target."),
	FALL_BACK("Fall back", "Break off, regroup on the leader, no attacking for 8 seconds."),
	GUARD_BANNER("Guard banner", "Hold formation around the planted Warbanner.");

	private final String label;
	private final String description;

	Order(String label, String description) {
		this.label = label;
		this.description = description;
	}

	public String label() {
		return label;
	}

	public String description() {
		return description;
	}

	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}

	/** Orders that travel with the leader through portals and log-outs. */
	public boolean travels() {
		return this == FOLLOW || this == CHARGE || this == FOCUS || this == FALL_BACK;
	}

	public static Order parse(String text, Order fallback) {
		if (text == null) {
			return fallback;
		}
		String t = text.trim().replace(' ', '_').replace('-', '_');
		for (Order o : values()) {
			if (o.name().equalsIgnoreCase(t)) {
				return o;
			}
		}
		if (t.equalsIgnoreCase("fallback")) {
			return FALL_BACK;
		}
		if (t.equalsIgnoreCase("guard") || t.equalsIgnoreCase("banner")) {
			return GUARD_BANNER;
		}
		return fallback;
	}
}
