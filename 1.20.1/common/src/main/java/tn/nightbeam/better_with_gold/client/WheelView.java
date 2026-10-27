package tn.nightbeam.better_with_gold.client;

import tn.nightbeam.better_with_gold.net.Net;

import com.google.gson.JsonObject;

import java.util.List;

/**
 * The command wheel: hold the key, point at an order, let go. Eight orders on a ring of gold-rimmed slices; the
 * highlighted slice lifts out and glows, and the hub in the middle shows the current order and formation or what the
 * highlighted order does. Opens with a short ease-out.
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
	/** Fraction of the texture's half size covered by the slices (see tools/gen_gui.py). */
	private static final float SLICE_OUTER = 0.90F;
	private static final float SLICE_INNER = 0.36F;
	private static final float HUB = 0.33F;

	private final int focusId;
	private final String focusName;
	private final long opened = System.nanoTime();
	private final float[] lift = new float[OPTIONS.length];
	private int hovered = -1;
	private long lastFrame;

	public WheelView(int focusId, String focusName) {
		this.focusId = focusId;
		this.focusName = focusName;
	}

	public void render(Canvas c, double mouseX, double mouseY) {
		long now = System.nanoTime();
		float dt = lastFrame == 0 ? 0 : Math.min(0.1F, (now - lastFrame) / 1e9F);
		lastFrame = now;
		float open = Ui.ease((now - opened) / 1e9F / 0.16F);
		float cx = c.width() / 2f;
		float cy = c.height() / 2f;
		double dx = mouseX - cx;
		double dy = mouseY - cy;
		float radius = Math.min(104, Math.min(c.width(), c.height()) * 0.36f);
		float half = radius / SLICE_OUTER * (0.86F + 0.14F * open);
		hovered = Math.hypot(dx, dy) < half * SLICE_INNER * 0.8F ? -1 : index(dx, dy);

		c.fill(0, 0, c.width(), c.height(), Ui.fade(0x66000000, open));
		c.gradient(0, 0, c.width(), c.height() * 0.5F, Ui.fade(0x40000000, open), 0x00000000);
		c.gradient(0, c.height() * 0.5F, c.width(), c.height(), 0x00000000, Ui.fade(0x40000000, open));
		c.textureRotated("wheel_ring", cx, cy, half * 2, half * 2, 0, Ui.fade(0xFFFFFFFF, open));
		String current = ClientFellowship.orderId();
		for (int i = 0; i < OPTIONS.length; i++) {
			boolean on = i == hovered;
			lift[i] += ((on ? 1 : 0) - lift[i]) * Math.min(1, dt * 16);
			double a = angle(i);
			float push = 4 * lift[i];
			float sx = cx + (float) Math.sin(a) * push;
			float sy = cy - (float) Math.cos(a) * push;
			float deg = (float) Math.toDegrees(a);
			c.textureRotated("wheel_sector", sx, sy, half * 2, half * 2, deg, Ui.fade(0xFFFFFFFF, open * (1 - lift[i])));
			if (lift[i] > 0.01F) {
				c.textureRotated("wheel_sector_hover", sx, sy, half * 2, half * 2, deg, Ui.fade(0xFFFFFFFF, open * lift[i]));
			}
		}
		for (int i = 0; i < OPTIONS.length; i++) {
			Option o = OPTIONS[i];
			boolean on = i == hovered;
			double a = angle(i);
			float mid = half * (SLICE_INNER + SLICE_OUTER) / 2 + 4 * lift[i];
			float ox = cx + (float) Math.sin(a) * mid;
			float oy = cy - (float) Math.cos(a) * mid;
			float size = 16 + 3 * lift[i];
			boolean active = o.type().equals(Net.ORDER) && o.value().equals(current);
			Icons.draw(c, Icons.order(o.type().equals(Net.ORDER) ? o.value() : o.type().equals(Net.FORMATION) ? "formation" : "recall"),
					ox - size / 2, oy - size / 2 - 5, size / 16F, 0, open);
			int labelColor = on ? Ui.GOLD_HI : active ? Ui.GOLD : 0xFFE6E2D6;
			c.centered(o.label(), ox, oy + 5.5F + 1.5F * lift[i], 0.62F, Ui.fade(labelColor, open), true);
			if (o.type().equals(Net.FORMATION)) {
				c.centered(ClientFellowship.formation(), ox, oy + 12.5F + 1.5F * lift[i], 0.48F, Ui.fade(Ui.DIM, open), true);
			} else if (active) {
				Ui.diamond(c, ox, oy + 14 + 1.5F * lift[i], 2, 0.8F, Ui.fade(Ui.GOLD, open));
			}
		}
		float hub = half * HUB;
		c.textureRotated("wheel_hub", cx, cy, half * 2, half * 2, 0, Ui.fade(0xFFFFFFFF, open));
		if (hovered >= 0 || Math.hypot(dx, dy) > hub * 0.6F) {
			float deg = (float) Math.toDegrees(Math.atan2(dx, -dy));
			c.textureRotated("wheel_pointer", cx, cy, half * 2, half * 2, deg, Ui.fade(0xFFFFFFFF, open));
		}
		hubText(c, cx, cy, hub, open);
		c.centered("Let go to give the order  ·  click works too", cx, Math.max(4, cy - half * 0.98F - 12), 0.55F, Ui.fade(0xC0D8D2C2, open), true);
	}

	private void hubText(Canvas c, float cx, float cy, float hub, float alpha) {
		if (hovered < 0) {
			c.centered("WARBAND", cx, cy - hub * 0.62F, 0.5F, Ui.fade(Ui.GOLD, alpha), true);
			Icons.draw(c, Icons.order(ClientFellowship.orderId()), cx - 7, cy - hub * 0.42F, 14 / 16F, 0, alpha);
			c.centered(ClientFellowship.order(), cx, cy + hub * 0.06F, 0.78F, Ui.fade(Ui.TEXT, alpha), true);
			c.centered(ClientFellowship.formation(), cx, cy + hub * 0.32F, 0.55F, Ui.fade(Ui.DIM, alpha), true);
			int n = ClientFellowship.party().size();
			c.centered(n + (n == 1 ? " ally" : " allies") + " in the field", cx, cy + hub * 0.52F, 0.45F, Ui.fade(Ui.MUTED, alpha), true);
			return;
		}
		Option o = OPTIONS[hovered];
		c.centered(o.label().toUpperCase(), cx, cy - hub * 0.5F, 0.7F, Ui.fade(Ui.GOLD_HI, alpha), true);
		String hint = o.hint();
		if (o.value().equals("focus")) {
			hint = focusName.isEmpty() ? "Look at an enemy first" : "Everyone attacks " + focusName;
		}
		List<String> lines = c.wrap(hint, (int) (hub * 1.55F / 0.55F));
		float y = cy - hub * 0.18F;
		for (String line : lines) {
			c.centered(line, cx, y, 0.55F, Ui.fade(0xFFDDD8CA, alpha), true);
			y += 7;
		}
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
