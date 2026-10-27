package tn.nightbeam.better_with_gold.client;

import java.util.List;

/**
 * Party frames in the top-left corner: one bar per ally with class colour, level, health, and a red countdown
 * with revive progress when downed. Drawn on a Canvas, so it is the same on every version.
 */
public final class PartyHud {
	private static final int MAX_FRAMES = 8;

	private PartyHud() {
	}

	public static void render(Canvas c, long gameTime) {
		List<ClientFellowship.Member> party = ClientFellowship.party();
		if (party.isEmpty() && ClientFellowship.roster() == 0) {
			return;
		}
		float x = 4;
		float y = 4;
		float w = 92;
		String header = "WARBAND · " + ClientFellowship.order() + " · " + ClientFellowship.formation();
		c.text(header, x + 1, y, 0.6F, 0xFFF2C94C, true);
		y += 7;
		int shown = 0;
		for (ClientFellowship.Member m : party) {
			if (shown >= MAX_FRAMES) {
				break;
			}
			frame(c, m, x, y, w, gameTime);
			y += 12;
			shown++;
		}
		int more = party.size() - shown;
		String tail = (more > 0 ? "+" + more + " more  " : "") + (ClientFellowship.camp() > 0 ? ClientFellowship.camp() + " at camp  " : "")
				+ ClientFellowship.roster() + "/" + ClientFellowship.cap();
		c.text(tail, x + 1, y + 1, 0.55F, 0xAAFFFFFF, true);
	}

	private static void frame(Canvas c, ClientFellowship.Member m, float x, float y, float w, long gameTime) {
		int alpha = m.far() ? 0x99 : 0xE6;
		c.fill(x, y, x + w, y + 11, (alpha << 24) | 0x0B0C14);
		c.fill(x, y, x + 2, y + 11, 0xFF000000 | m.color());
		String level = String.valueOf(m.level());
		c.text(m.name(), x + 5, y + 1.5F, 0.6F, m.far() ? 0xFFAAAAAA : 0xFFFFFFFF, true);
		c.text(level, x + w - 3 - c.textWidth(level) * 0.55F, y + 1.8F, 0.55F, 0xFF000000 | m.color(), true);
		float barX0 = x + 5;
		float barX1 = x + w - 3;
		float barY = y + 7.5F;
		c.fill(barX0, barY, barX1, barY + 2F, 0xFF2A1416);
		if (m.downedSeconds() > 0) {
			boolean blink = (gameTime / 10) % 2 == 0;
			c.fill(barX0, barY, barX1, barY + 2F, blink ? 0xFFB22222 : 0xFF7A1515);
			c.fill(barX0, barY, barX0 + (barX1 - barX0) * m.revive() / 100F, barY + 2F, 0xFFF2C94C);
			String down = "DOWNED " + m.downedSeconds() + "s";
			c.text(down, x + w - 14 - c.textWidth(down) * 0.5F, y + 2.0F, 0.5F, 0xFFFF5555, true);
			return;
		}
		float ratio = (float) Math.max(0, Math.min(1, m.health() / Math.max(1, m.maxHealth())));
		int color = ratio > 0.5F ? 0xFF4CC36B : ratio > 0.25F ? 0xFFE0B03A : 0xFFE04848;
		c.fill(barX0, barY, barX0 + (barX1 - barX0) * ratio, barY + 2F, color);
	}
}
