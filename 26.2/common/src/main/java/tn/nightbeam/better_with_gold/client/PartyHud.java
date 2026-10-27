package tn.nightbeam.better_with_gold.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Party frames in the top-left corner: one frame per ally with its class emblem, name, level and a segmented health
 * bar whose recent damage drains away behind it; a downed ally's frame pulses red with its bleed-out countdown and
 * revive progress. Drawn on a Canvas, so it is the same on every version.
 */
public final class PartyHud {
	private static final int MAX_FRAMES = 8;
	private static final float W = 112;
	private static final float H = 17;
	/** Per ally: shown health fraction, trailing (damage) fraction. */
	private static final Map<Integer, float[]> BARS = new HashMap<>();
	private static long lastFrame;

	private PartyHud() {
	}

	public static void render(Canvas c, long gameTime) {
		List<ClientFellowship.Member> party = ClientFellowship.party();
		if (ClientFellowship.rosterOpen()) {
			return;
		}
		if (party.isEmpty() && ClientFellowship.roster() == 0) {
			BARS.clear();
			return;
		}
		long now = System.nanoTime();
		float dt = lastFrame == 0 ? 0 : Math.min(0.1F, (now - lastFrame) / 1e9F);
		lastFrame = now;
		float x = 5;
		float y = 5;
		header(c, x, y);
		y += 11;
		int shown = 0;
		for (ClientFellowship.Member m : party) {
			if (shown >= MAX_FRAMES) {
				break;
			}
			frame(c, m, x, y, dt);
			y += H + 2;
			shown++;
		}
		int more = party.size() - shown;
		StringBuilder tail = new StringBuilder();
		if (more > 0) {
			tail.append('+').append(more).append(" more  ·  ");
		}
		if (ClientFellowship.camp() > 0) {
			tail.append(ClientFellowship.camp()).append(" at camp  ·  ");
		}
		tail.append(ClientFellowship.roster()).append('/').append(ClientFellowship.cap());
		c.text(tail.toString(), x + 1, y + 0.5F, 0.5F, 0xC0A6A2B4, true);
	}

	private static void header(Canvas c, float x, float y) {
		Icons.draw(c, Icons.GUARD_BANNER, x, y - 0.5F, 9 / 16F, 0, 1);
		c.text("WARBAND", x + 11, y + 1.5F, 0.6F, Ui.GOLD, true);
		float ox = x + 11 + c.textWidth("WARBAND") * 0.6F + 5;
		Icons.draw(c, Icons.order(ClientFellowship.orderId()), ox, y, 8 / 16F, 0, 1);
		String text = ClientFellowship.order() + "  ·  " + ClientFellowship.formation() + (ClientFellowship.banner() ? "  ·  Banner" : "");
		c.text(text, ox + 10, y + 1.8F, 0.52F, 0xFFD8D2C2, true);
	}

	private static void frame(Canvas c, ClientFellowship.Member m, float x, float y, float dt) {
		boolean downed = m.downedSeconds() > 0;
		float alpha = m.far() ? 0.55F : 1F;
		int color = m.color();
		float pulse = downed ? Ui.pulse(0.8F) : 0;
		// body
		c.fill(x - 0.5F, y - 0.5F, x + W + 0.5F, y + H + 0.5F, Ui.fade(0xD0050507, alpha));
		c.gradient(x, y, x + W, y + H, Ui.fade(downed ? 0xD83A1014 : 0xD0161A2A, alpha), Ui.fade(downed ? 0xD8200A0C : 0xD00A0B13, alpha));
		int edge = downed ? Ui.opaque(Canvas.mix(0xB8352A, 0xFF6B5A, pulse)) : Ui.opaque(Canvas.mix(color, 0x000000, 0.35F));
		c.rect(x, y, x + W, y + H, Ui.fade(edge, alpha));
		c.fill(x + 1, y + 1, x + W - 1, y + 1.5F, Ui.fade(0x18FFFFFF, alpha));
		// emblem
		float es = H - 4;
		if (downed) {
			c.fill(x + 2, y + 2, x + 2 + es, y + 2 + es, Ui.fade(0xFF1A0608, alpha));
			c.rect(x + 2, y + 2, x + 2 + es, y + 2 + es, Ui.fade(edge, alpha));
			Icons.draw(c, Icons.DOWN, x + 2.5F, y + 2.5F + pulse * 0.8F, (es - 1) / 16F, 0, alpha);
		} else {
			Ui.emblem(c, x + 2, y + 2, es, m.role(), color, alpha);
		}
		float tx = x + es + 5;
		float barX1 = x + W - 3;
		// name and level
		c.text(m.name(), tx, y + 2.4F, 0.6F, Ui.fade(downed ? 0xFFFF8A80 : m.far() ? 0xFFBBBBBB : Ui.TEXT, alpha), true);
		String level = String.valueOf(m.level());
		float lw = c.textWidth(level) * 0.5F;
		Ui.diamond(c, barX1 - lw / 2 - 1, y + 4.7F, 3, 1.1F, Ui.fade(0xFF0C0C12, alpha));
		c.text(level, barX1 - lw - 1, y + 3.2F, 0.5F, Ui.fade(Ui.opaque(Canvas.mix(color, 0xFFFFFF, 0.3F)), alpha), true);
		if (m.far()) {
			c.text("far", barX1 - lw - 16, y + 3.4F, 0.45F, Ui.fade(Ui.MUTED, 1), true);
		}
		float barY = y + 10.5F;
		float bw = barX1 - tx;
		float[] bar = BARS.computeIfAbsent(m.id(), k -> new float[] {-1, -1});
		if (downed) {
			Ui.bar(c, tx, barY, bw, 3.5F, m.revive() / 100F, 0, Ui.GOLD, 1, alpha);
			String down = m.downedSeconds() + "s";
			c.text(down, barX1 - c.textWidth(down) * 0.5F - 0.5F, barY - 6.6F + 0, 0.5F, Ui.fade(0xFFFF6B5A, alpha), true);
			c.text(m.revive() > 0 ? "REVIVING" : "DOWNED", tx + c.textWidth(m.name()) * 0.6F + 4, y + 3.2F, 0.45F,
					Ui.fade(m.revive() > 0 ? Ui.GOLD : 0xFFFF6B5A, alpha), true);
			bar[0] = 0;
			bar[1] = 0;
			return;
		}
		float target = (float) Math.max(0, Math.min(1, m.health() / Math.max(1, m.maxHealth())));
		if (bar[0] < 0) {
			bar[0] = target;
			bar[1] = target;
		}
		bar[0] += (target - bar[0]) * Math.min(1, dt * 14);
		if (bar[1] < bar[0]) {
			bar[1] = bar[0];
		} else {
			bar[1] = Math.max(bar[0], bar[1] - dt * 0.35F);
		}
		int hp = target > 0.5F ? Ui.GREEN : target > 0.25F ? Ui.AMBER : Ui.RED;
		int segments = (int) Math.max(1, Math.min(12, Math.round(m.maxHealth() / 10)));
		Ui.bar(c, tx, barY, bw, 3.5F, bar[0], bar[1], hp, segments, alpha);
		if (target <= 0.25F) {
			float p = Ui.pulse(0.9F);
			c.rect(x, y, x + W, y + H, Ui.fade(0xFFE5534B, 0.35F + 0.4F * p));
		}
	}
}
