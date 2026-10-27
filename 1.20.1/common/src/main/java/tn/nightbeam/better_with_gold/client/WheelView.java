package tn.nightbeam.better_with_gold.client;

import tn.nightbeam.better_with_gold.net.Net;

import com.google.gson.JsonObject;

/**
 * The command wheel: hold the key, point at an order, let go. Eight orders around a ring; the centre shows the
 * current order, the formation and what the highlighted order does.
 */
public final class WheelView {
	public record Option(String label, String hint, String type, String value) {
	}

	public static final Option[] OPTIONS = {
			new Option("Follow", "Formation around you", Net.ORDER, "follow"),
			new Option("Charge", "Attack every enemy near you", Net.ORDER, "charge"),
			new Option("Focus", "Everyone hits your target", Net.ORDER, "focus"),
			new Option("Hold", "Defend where they stand", Net.ORDER, "hold"),
			new Option("Fall back", "Regroup on you, no attacking", Net.ORDER, "fall_back"),
			new Option("Guard banner", "Hold formation at the Warbanner", Net.ORDER, "guard_banner"),
			new Option("Formation", "Line, Wedge, Shield Wall, Circle", Net.FORMATION, "next"),
			new Option("Recall", "Bring stray allies to you", Net.RECALL, ""),
	};

	private final int focusId;
	private final String focusName;
	private int hovered = -1;
	private float cx;
	private float cy;

	public WheelView(int focusId, String focusName) {
		this.focusId = focusId;
		this.focusName = focusName;
	}

	public void render(Canvas c, double mouseX, double mouseY) {
		cx = c.width() / 2f;
		cy = c.height() / 2f;
		double dx = mouseX - cx;
		double dy = mouseY - cy;
		hovered = Math.hypot(dx, dy) < 16 ? -1 : index(dx, dy);
		float radius = Math.min(92, Math.min(c.width(), c.height()) * 0.32f);
		c.fill(0, 0, c.width(), c.height(), 0x55000000);
		for (int i = 0; i < OPTIONS.length; i++) {
			double a = angle(i);
			float ox = cx + (float) Math.sin(a) * radius;
			float oy = cy - (float) Math.cos(a) * radius;
			boolean on = i == hovered;
			if (on) {
				c.line(cx, cy, ox, oy, 1.5F, 0xCCF2C94C);
			}
			Option o = OPTIONS[i];
			float w = Math.max(54, c.textWidth(o.label()) * 0.9F + 12);
			c.fill(ox - w / 2, oy - 9, ox + w / 2, oy + 9, on ? 0xF01E1A10 : 0xE00B0C14);
			c.rect(ox - w / 2, oy - 9, ox + w / 2, oy + 9, on ? 0xFFF2C94C : 0x88F2C94C);
			c.centered(o.label(), ox, oy - 3.5F, 0.9F, on ? 0xFFF2C94C : 0xFFE8E8E8, true);
		}
		float r = 30;
		c.fill(cx - r, cy - r * 0.75F, cx + r, cy + r * 0.75F, 0xE00B0C14);
		c.rect(cx - r, cy - r * 0.75F, cx + r, cy + r * 0.75F, 0xAAF2C94C);
		c.centered("WARBAND", cx, cy - 17, 0.65F, 0xFFF2C94C, true);
		c.centered(ClientFellowship.order(), cx, cy - 8, 0.85F, 0xFFFFFFFF, true);
		c.centered(ClientFellowship.formation(), cx, cy + 2, 0.7F, 0xFFBBBBBB, true);
		c.centered(ClientFellowship.party().size() + " in the field", cx, cy + 11, 0.55F, 0xFF999999, true);
		String hint = hovered < 0 ? "Point at an order and let go" : OPTIONS[hovered].hint();
		if (hovered >= 0 && OPTIONS[hovered].value().equals("focus")) {
			hint = focusName.isEmpty() ? "Look at an enemy first" : "Focus: " + focusName;
		}
		c.centered(hint, cx, cy + radius + 20, 0.85F, 0xFFFFFFFF, true);
	}

	private static double angle(int i) {
		return 2 * Math.PI * i / OPTIONS.length;
	}

	private static int index(double dx, double dy) {
		double a = Math.atan2(dx, -dy);
		if (a < 0) {
			a += 2 * Math.PI;
		}
		return (int) Math.round(a / (2 * Math.PI / OPTIONS.length)) % OPTIONS.length;
	}

	/** Sends the highlighted order. Returns false when nothing was highlighted. */
	public boolean commit() {
		if (hovered < 0) {
			return false;
		}
		Option o = OPTIONS[hovered];
		JsonObject msg = Net.message(o.type());
		if (o.type().equals(Net.ORDER)) {
			msg.addProperty("o", o.value());
			if (o.value().equals("focus")) {
				msg.addProperty("id", focusId);
			}
		} else if (o.type().equals(Net.FORMATION)) {
			msg.addProperty("f", o.value());
		}
		ClientFellowship.send(msg);
		return true;
	}
}
