package tn.nightbeam.better_with_gold.client;

import tn.nightbeam.better_with_gold.net.Net;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * The roster: every ally in the world, at camp or recovering, and the Remembrance of the fallen. The selected ally
 * gets a live 3D portrait (when loaded nearby), its bars, six stat tiles, trait and ability chips with tooltips and
 * its Chronicle as a timeline. Drawn on a Canvas so the screen is shared by every version; each version only wraps
 * it in its own Screen.
 */
public final class RosterView {
	public interface Host {
		void click();

		void close();
	}

	private final Host host;
	private final long opened = System.nanoTime();
	private String selectedId;
	private int selected;
	private boolean remembrance;
	private float scroll;
	private float listScroll;
	private long lastRequest = -1000;
	private long lastVersion = -1;
	private long selectedAt = System.nanoTime();
	private float highlightY = -1;
	private String confirmDismiss = "";
	private final List<Button> buttons = new ArrayList<>();
	private Tip tip;
	private float px0;
	private float py0;
	private float px1;
	private float py1;
	private float listX1;

	private record Button(float x0, float y0, float x1, float y1, Runnable action) {
		boolean hit(double x, double y) {
			return x >= x0 && x <= x1 && y >= y0 && y <= y1;
		}
	}

	private record Tip(String title, String body, int color) {
	}

	public RosterView(Host host, String selectedId) {
		this.host = host;
		this.selectedId = selectedId == null ? "" : selectedId;
	}

	public void tick(long gameTime) {
		if (gameTime - lastRequest >= 40 || gameTime < lastRequest) {
			lastRequest = gameTime;
			ClientFellowship.send(Net.message(Net.ROSTER_REQUEST));
		}
	}

	private List<JsonObject> allies() {
		return list("allies");
	}

	private List<JsonObject> fallen() {
		return list("fallen");
	}

	private List<JsonObject> list(String key) {
		List<JsonObject> out = new ArrayList<>();
		JsonObject data = ClientFellowship.rosterData();
		if (data == null || !data.has(key)) {
			return out;
		}
		for (JsonElement e : data.getAsJsonArray(key)) {
			out.add(e.getAsJsonObject());
		}
		return out;
	}

	private static boolean in(double mx, double my, float x0, float y0, float x1, float y1) {
		return mx >= x0 && mx <= x1 && my >= y0 && my <= y1;
	}

	public void render(Canvas c, double mouseX, double mouseY) {
		buttons.clear();
		tip = null;
		ClientFellowship.rosterShown();
		float open = Ui.ease((System.nanoTime() - opened) / 1e9F / 0.18F);
		JsonObject data = ClientFellowship.rosterData();
		List<JsonObject> entries = remembrance ? fallen() : allies();
		if (ClientFellowship.rosterVersion() != lastVersion) {
			lastVersion = ClientFellowship.rosterVersion();
			if (!selectedId.isEmpty() && !remembrance) {
				for (int i = 0; i < entries.size(); i++) {
					if (Net.string(entries.get(i), "id", "").equals(selectedId)) {
						selected = i;
					}
				}
			}
		}
		selected = Math.max(0, Math.min(selected, entries.size() - 1));
		float w = Math.min(468, c.width() - 20);
		float h = Math.min(286, c.height() - 20);
		float rise = 8 * (1 - open);
		px0 = (c.width() - w) / 2;
		py0 = (c.height() - h) / 2 + rise;
		px1 = px0 + w;
		py1 = py0 + h;
		c.fill(0, 0, c.width(), c.height(), Ui.fade(0x99000000, open));
		Ui.frame(c, px0, py0, px1, py1, open);
		Icons.draw(c, Icons.GUARD_BANNER, px0 + 8, py0 + 6, 11 / 16F, 0, open);
		c.text("REMNANTS | FELLOWSHIP", px0 + 22, py0 + 8.5F, 0.8F, Ui.fade(Ui.GOLD, open), true);
		int cap = data == null ? ClientFellowship.cap() : Net.integer(data, "cap", 6);
		float tx = px1 - 8;
		tx = tab(c, "Remembrance  " + fallen().size(), Icons.SKULL, tx, py0 + 5, remembrance, mouseX, mouseY, open, () -> {
			remembrance = true;
			select(0, entries);
		});
		tab(c, "Warband  " + allies().size() + "/" + cap, Icons.GUARD_BANNER, tx - 4, py0 + 5, !remembrance, mouseX, mouseY, open, () -> {
			remembrance = false;
			select(0, entries);
		});
		Ui.rule(c, px0 + 8, px1 - 8, py0 + 22, open);
		if (data == null) {
			c.centered("Gathering the roster...", (px0 + px1) / 2, (py0 + py1) / 2, 0.9F, Ui.fade(Ui.DIM, open), true);
			return;
		}
		listX1 = px0 + 146;
		renderList(c, entries, px0 + 8, py0 + 27, listX1, py1 - 22, mouseX, mouseY, open);
		float dx0 = listX1 + 8;
		if (entries.isEmpty()) {
			String empty = remembrance ? "No one has fallen under your banner." : "No allies yet. Find sellswords near village bells and pay them in gold.";
			Icons.draw(c, remembrance ? Icons.SKULL : Icons.COIN, (dx0 + px1 - 8) / 2 - 16, py0 + 60, 2, 0, 0.5F * open);
			float y = py0 + 100;
			for (String line : c.wrap(empty, (int) ((px1 - dx0 - 30) / 0.8F))) {
				c.centered(line, (dx0 + px1 - 8) / 2, y, 0.8F, Ui.fade(Ui.DIM, open), true);
				y += 10;
			}
		} else {
			float fade = open * Ui.ease((System.nanoTime() - selectedAt) / 1e9F / 0.15F);
			if (remembrance) {
				renderFallen(c, entries.get(selected), dx0, py0 + 27, px1 - 8, py1 - 22, fade);
			} else {
				renderAlly(c, entries.get(selected), dx0, py0 + 27, px1 - 8, py1 - 22, mouseX, mouseY, fade);
			}
		}
		footer(c, data, open);
		if (tip != null) {
			Ui.tooltip(c, (float) mouseX, (float) mouseY, tip.title(), tip.body(), tip.color());
		}
	}

	private void select(int index, List<JsonObject> entries) {
		selected = index;
		scroll = 0;
		confirmDismiss = "";
		selectedAt = System.nanoTime();
	}

	private float tab(Canvas c, String label, Icons.Icon icon, float right, float y, boolean active, double mx, double my, float alpha, Runnable action) {
		float w = c.textWidth(label) * 0.62F + 22;
		float x = right - w;
		boolean hover = in(mx, my, x, y, right, y + 13);
		c.fill(x - 0.5F, y - 0.5F, right + 0.5F, y + 13.5F, Ui.fade(0xFF050507, alpha));
		c.gradient(x, y, right, y + 13, Ui.fade(active ? 0xFF3A2C0E : hover ? 0xFF22202C : 0xFF16151E, alpha),
				Ui.fade(active ? 0xFF1E1606 : 0xFF0E0D14, alpha));
		c.rect(x, y, right, y + 13, Ui.fade(active ? Ui.GOLD : hover ? 0xAAE8B63E : 0x55C8A04A, alpha));
		if (active) {
			c.fill(x + 1, y + 12, right - 1, y + 13, Ui.fade(Ui.GOLD_HI, alpha));
		}
		Icons.draw(c, icon, x + 4, y + 2.5F, 8 / 16F, 0, alpha * (active ? 1 : 0.6F));
		c.text(label, x + 15, y + 3.6F, 0.62F, Ui.fade(active ? Ui.GOLD_HI : hover ? Ui.TEXT : Ui.DIM, alpha), true);
		buttons.add(new Button(x, y, right, y + 13, action));
		return x;
	}

	private void renderList(Canvas c, List<JsonObject> entries, float x0, float y0, float x1, float y1, double mx, double my, float alpha) {
		Ui.inset(c, x0, y0, x1, y1, 0x44C8A04A, alpha);
		int rowH = 26;
		float maxScroll = Math.max(0, entries.size() * rowH - (y1 - y0 - 2));
		listScroll = Math.max(0, Math.min(listScroll, maxScroll));
		float targetY = y0 + 1 + selected * rowH - listScroll;
		highlightY = highlightY < 0 ? targetY : highlightY + (targetY - highlightY) * 0.35F;
		c.scissor((int) x0 + 1, (int) y0 + 1, (int) x1 - 1, (int) y1 - 1);
		if (!entries.isEmpty()) {
			c.gradient(x0 + 1, highlightY, x1 - 1, highlightY + rowH - 1, Ui.fade(0x55E8B63E, alpha), Ui.fade(0x18E8B63E, alpha));
			c.fill(x0 + 1, highlightY, x0 + 2.5F, highlightY + rowH - 1, Ui.fade(Ui.GOLD_HI, alpha));
		}
		for (int i = 0; i < entries.size(); i++) {
			JsonObject e = entries.get(i);
			float ry = y0 + 1 + i * rowH - listScroll;
			if (ry + rowH < y0 || ry > y1) {
				continue;
			}
			boolean sel = i == selected;
			boolean hover = in(mx, my, x0, Math.max(y0, ry), x1, Math.min(y1, ry + rowH));
			if (hover && !sel) {
				c.fill(x0 + 1, ry, x1 - 1, ry + rowH - 1, Ui.fade(0x14FFFFFF, alpha));
			}
			int color = Net.integer(e, "col", 0xC8A04A);
			String state = remembrance ? "fallen" : Net.string(e, "state", "active");
			float ex = x0 + 5;
			if (remembrance) {
				Ui.emblem(c, ex, ry + 4, 18, Net.string(e, "rid", "TANK"), 0x777480, alpha * 0.8F);
			} else {
				Ui.emblem(c, ex, ry + 4, 18, Net.string(e, "rid", "TANK"), color, alpha);
			}
			float nx = ex + 23;
			c.text(Net.string(e, "name", "?"), nx, ry + 4.5F, 0.72F, Ui.fade(sel ? Ui.GOLD_HI : remembrance ? 0xFFC8C4D0 : Ui.TEXT, alpha), true);
			String sub = Net.string(e, "cls", "") + "  ·  Lv " + Net.integer(e, "lv", 1);
			c.text(sub, nx, ry + 13.5F, 0.55F, Ui.fade(Ui.DIM, alpha), true);
			int stateColor = switch (state) {
				case "downed" -> 0xFFFF5A50;
				case "camp" -> 0xFF8AB4E0;
				case "recovering" -> Ui.AMBER;
				case "away", "fallen" -> 0xFF8A8698;
				default -> Ui.GREEN;
			};
			String label = state.toUpperCase();
			float lw = c.textWidth(label) * 0.45F;
			float dot = state.equals("downed") ? 0.4F + 0.6F * Ui.pulse(0.8F) : 1;
			Ui.diamond(c, x1 - 7 - lw - 4, ry + 6.6F, 1, 1, Ui.fade(stateColor, alpha * dot));
			c.text(label, x1 - 5 - lw, ry + 5, 0.45F, Ui.fade(stateColor, alpha), true);
			if (!remembrance && (state.equals("active") || state.equals("downed"))) {
				double hp = e.has("hp") ? e.get("hp").getAsDouble() : 0;
				double mhp = e.has("mhp") ? e.get("mhp").getAsDouble() : 1;
				float frac = state.equals("downed") ? 0 : (float) (hp / Math.max(1, mhp));
				c.fill(nx, ry + 21, x1 - 5, ry + 22.5F, Ui.fade(0xFF1A1C24, alpha));
				c.fill(nx, ry + 21, nx + (x1 - 5 - nx) * frac, ry + 22.5F, Ui.fade(frac > 0.5F ? Ui.GREEN : frac > 0.25F ? Ui.AMBER : Ui.RED, alpha));
			}
			final int index = i;
			buttons.add(new Button(x0, Math.max(y0, ry), x1, Math.min(y1, ry + rowH), () -> {
				select(index, entries);
				selectedId = Net.string(entries.get(index), "id", "");
			}));
		}
		c.endScissor();
		if (maxScroll > 0) {
			float track = y1 - y0 - 4;
			float thumb = Math.max(12, track * (y1 - y0) / (entries.size() * rowH));
			float ty = y0 + 2 + (track - thumb) * (listScroll / maxScroll);
			c.fill(x1 - 2.5F, y0 + 2, x1 - 1.5F, y1 - 2, Ui.fade(0x33FFFFFF, alpha));
			c.fill(x1 - 2.5F, ty, x1 - 1.5F, ty + thumb, Ui.fade(Ui.GOLD, alpha));
		}
	}

	private void renderAlly(Canvas c, JsonObject e, float x0, float y0, float x1, float y1, double mx, double my, float alpha) {
		int color = Net.integer(e, "col", 0xC8A04A);
		String role = Net.string(e, "rid", "TANK");
		String state = Net.string(e, "state", "active");
		// portrait
		float pw = 76;
		float ph = 98;
		portrait(c, e, x0, y0, x0 + pw, y0 + ph, color, role, state, mx, my, alpha);
		// name block
		float nx = x0 + pw + 8;
		String name = Net.string(e, "name", "?");
		c.text(name, nx, y0 + 1, 1.25F, Ui.fade(Ui.opaque(Canvas.mix(color, 0xFFFFFF, 0.2F)), alpha), true);
		String epithet = Net.string(e, "epithet", "");
		if (!epithet.isEmpty()) {
			c.text(epithet, nx + c.textWidth(name) * 1.25F + 5, y0 + 4.2F, 0.75F, Ui.fade(Ui.GOLD, alpha), true);
		}
		Icons.draw(c, Icons.role(role), nx, y0 + 13, 8 / 16F, color, alpha);
		String cls = Net.string(e, "cls", "") + "  ·  " + Net.string(e, "role", "") + "  ·  ";
		c.text(cls, nx + 10, y0 + 14.4F, 0.62F, Ui.fade(Ui.DIM, alpha), true);
		String title = Net.string(e, "title", "");
		if (!title.isEmpty()) {
			c.text(title, nx + 10 + c.textWidth(cls) * 0.62F, y0 + 14.4F, 0.62F, Ui.fade(Ui.GOLD, alpha), true);
		}
		c.text("“" + Net.string(e, "headline", "") + "”", nx, y0 + 23.5F, 0.6F, Ui.fade(0xFFD8C690, alpha), true);
		// bars
		float bw = x1 - nx;
		double hp = e.get("hp").getAsDouble();
		double mhp = Math.max(1, e.get("mhp").getAsDouble());
		float by = y0 + 33;
		statBar(c, Icons.HEART, "Health", String.format("%.0f / %.0f", hp, mhp), nx, by, bw, (float) (hp / mhp), Ui.GREEN,
				(int) Math.max(1, Math.min(16, Math.round(mhp / 10))), alpha);
		int xp = Net.integer(e, "xp", 0);
		int next = Math.max(1, Net.integer(e, "xpNext", 1));
		int lv = Net.integer(e, "lv", 1);
		statBar(c, Icons.XP, "Level " + lv, xp + " / " + next + " xp", nx, by + 13, bw, xp / (float) next, Ui.BLUE, 1, alpha);
		int morale = Net.integer(e, "morale", 0);
		int moraleColor = morale >= 80 ? Ui.GOLD : morale > 25 ? 0xFFD49A3A : Ui.RED;
		statBar(c, Icons.FLAME, "Morale", morale + "  " + Net.string(e, "moraleState", ""), nx, by + 26, bw, morale / 100F, moraleColor, 4, alpha);
		// stat tiles
		JsonObject stats = e.getAsJsonObject("stats");
		Object[][] tiles = {
				{Icons.SKULL, Net.integer(stats, "kills", 0), "Kills", "Enemies this ally finished or helped finish."},
				{Icons.CROWN, Net.integer(stats, "bosses", 0), "Bosses", "Bosses slain."},
				{Icons.MELEE, Net.integer(stats, "bossFights", 0), "Boss fights", "Boss fights survived."},
				{Icons.DOWN, Net.integer(stats, "downs", 0), "Downed", "Times this ally went down."},
				{Icons.REVIVE, Net.integer(stats, "revives", 0), "Revives", "Band-mates this ally pulled back up."},
				{Icons.HEART, Net.integer(stats, "healing", 0), "Healed", "Health restored to the band."},
		};
		float ty = by + 40;
		float tw = (bw - 5 * 3) / 6;
		for (int i = 0; i < tiles.length; i++) {
			float tx0 = nx + i * (tw + 3);
			boolean hover = in(mx, my, tx0, ty, tx0 + tw, ty + 21);
			c.fill(tx0, ty, tx0 + tw, ty + 21, Ui.fade(hover ? 0xFF262434 : 0xFF14131C, alpha));
			c.rect(tx0, ty, tx0 + tw, ty + 21, Ui.fade(hover ? 0xAAE8B63E : 0x40C8A04A, alpha));
			Icons.draw(c, (Icons.Icon) tiles[i][0], tx0 + 3, ty + 3, 8 / 16F, Ui.GOLD, alpha);
			String v = String.valueOf(tiles[i][1]);
			c.text(v, tx0 + tw - 3 - c.textWidth(v) * 0.75F, ty + 3.2F, 0.75F, Ui.fade(Ui.TEXT, alpha), true);
			c.centered((String) tiles[i][2], tx0 + tw / 2, ty + 13.5F, 0.45F, Ui.fade(Ui.DIM, alpha), true);
			if (hover) {
				tip = new Tip((String) tiles[i][2], (String) tiles[i][3], Ui.GOLD);
			}
		}
		// traits and ability chips
		float cy = y0 + ph + 6;
		float cx = x0;
		for (JsonElement t : e.getAsJsonArray("traits")) {
			JsonObject to = t.getAsJsonObject();
			String tn = Net.string(to, "n", "");
			float w = c.textWidth(tn) * 0.58F + 8;
			boolean hover = in(mx, my, cx, cy, cx + w, cy + 10);
			cx = Ui.chip(c, cx, cy, tn, 0xB89AF0, hover, alpha) + 3;
			if (hover) {
				tip = new Tip("Trait: " + tn, Net.string(to, "d", ""), 0xFFC9B7F0);
			}
		}
		String ability = Net.string(e, "ability", "");
		if (!ability.isEmpty()) {
			float w = c.textWidth(ability) * 0.58F + 8 + 10;
			boolean hover = in(mx, my, cx, cy, cx + w, cy + 10);
			c.fill(cx, cy, cx + w, cy + 10, Ui.fade(hover ? 0xFF1E2A3C : 0xFF141A26, alpha));
			c.rect(cx, cy, cx + w, cy + 10, Ui.fade(hover ? 0xFF9AD1FF : 0xFF5A86B8, alpha));
			Icons.draw(c, Icons.role(role), cx + 2, cy + 1, 8 / 16F, color, alpha);
			c.text(ability, cx + 12, cy + 2.6F, 0.58F, Ui.fade(0xFFBFE2FF, alpha), true);
			if (hover) {
				tip = new Tip("Ability: " + ability, Net.string(e, "abilityText", ""), 0xFF9AD1FF);
			}
		}
		c.text(Net.string(e, "where", ""), x1 - c.textWidth(Net.string(e, "where", "")) * 0.55F, cy + 2.8F, 0.55F, Ui.fade(Ui.MUTED, alpha), true);
		// chronicle
		float chTop = cy + 14;
		chronicle(c, e.getAsJsonArray("lines"), x0, chTop, x1, y1 - 17, alpha);
		// actions
		String id = Net.string(e, "id", "");
		float by2 = y1 - 13;
		if (state.equals("camp")) {
			action(c, "Summon", x0, by2, false, mx, my, alpha, () -> send(Net.SUMMON, id));
		} else if (state.equals("active")) {
			action(c, "Send to camp", x0, by2, false, mx, my, alpha, () -> send(Net.STOW, id));
		} else if (state.equals("away")) {
			action(c, "Recall", x0, by2, false, mx, my, alpha, () -> ClientFellowship.send(Net.message(Net.RECALL)));
		}
		boolean confirming = confirmDismiss.equals(id);
		String label = confirming ? "Click again to dismiss" : "Dismiss";
		float dw = c.textWidth(label) * 0.7F + 16;
		action(c, label, x1 - dw, by2, true, mx, my, alpha, () -> {
			if (confirmDismiss.equals(id)) {
				send(Net.DISMISS, id);
				confirmDismiss = "";
				selected = 0;
			} else {
				confirmDismiss = id;
			}
		});
	}

	private void portrait(Canvas c, JsonObject e, float x0, float y0, float x1, float y1, int color, String role, String state,
						  double mx, double my, float alpha) {
		c.fill(x0 - 0.5F, y0 - 0.5F, x1 + 0.5F, y1 + 0.5F, Ui.fade(0xFF050507, alpha));
		c.gradient(x0, y0, x1, y1, Ui.fade(Ui.opaque(Canvas.mix(color, 0x0A0B13, 0.68F)), alpha), Ui.fade(0xFF07070C, alpha));
		// a lit floor under the figure
		float fx = (x0 + x1) / 2;
		for (int k = 0; k < 6; k++) {
			float rw = (x1 - x0) * (0.42F - k * 0.05F);
			c.fill(fx - rw, y1 - 13 + k * 0.6F, fx + rw, y1 - 12.4F + k * 0.6F, Ui.fade(Ui.opaque(Canvas.mix(color, 0xFFFFFF, 0.2F)), alpha * 0.05F * (k + 1)));
		}
		int eid = Net.integer(e, "eid", -1);
		boolean drawn = false;
		if (eid >= 0 && alpha > 0.9F) {
			c.scissor((int) x0 + 1, (int) y0 + 1, (int) x1 - 1, (int) y1 - 1);
			drawn = c.entity(eid, (int) x0 + 1, (int) y0 + 4, (int) x1 - 1, (int) y1 - 6, (float) mx, (float) my);
			c.endScissor();
		}
		if (!drawn) {
			float s = 40;
			Icons.draw(c, Icons.role(role), fx - s / 2, (y0 + y1) / 2 - s / 2 - 8, s / 16, color, alpha * 0.85F);
			String caption = switch (state) {
				case "camp" -> "AT CAMP";
				case "recovering" -> "RECOVERING";
				case "away" -> "AWAY";
				default -> "";
			};
			if (!caption.isEmpty()) {
				Icons.draw(c, Icons.CAMP, fx - 6, y1 - 30, 12 / 16F, 0, alpha);
				c.centered(caption, fx, y1 - 18, 0.5F, Ui.fade(Ui.DIM, alpha), true);
			}
		}
		c.rect(x0, y0, x1, y1, Ui.fade(Ui.opaque(color), alpha));
		c.rect(x0 + 1.5F, y0 + 1.5F, x1 - 1.5F, y1 - 1.5F, Ui.fade(0x30FFFFFF, alpha));
		// level badge on the bottom edge
		String lv = String.valueOf(Net.integer(e, "lv", 1));
		float bw = c.textWidth(lv) * 0.7F + 12;
		float bx = fx - bw / 2;
		float by = y1 - 6;
		c.fill(bx - 0.5F, by - 0.5F, bx + bw + 0.5F, by + 11.5F, Ui.fade(0xFF050507, alpha));
		c.gradient(bx, by, bx + bw, by + 11, Ui.fade(0xFF4A3812, alpha), Ui.fade(0xFF221806, alpha));
		c.rect(bx, by, bx + bw, by + 11, Ui.fade(Ui.GOLD, alpha));
		c.centered(lv, fx, by + 2.4F, 0.7F, Ui.fade(Ui.GOLD_HI, alpha), true);
		if (state.equals("downed")) {
			float p = Ui.pulse(0.8F);
			c.rect(x0, y0, x1, y1, Ui.fade(0xFFFF5A50, 0.4F + 0.5F * p));
			c.centered("DOWNED", fx, y0 + 5, 0.6F, Ui.fade(0xFFFF8A80, alpha), true);
		}
	}

	private static void statBar(Canvas c, Icons.Icon icon, String label, String value, float x, float y, float w, float frac, int color,
								int segments, float alpha) {
		Icons.draw(c, icon, x, y - 0.5F, 7 / 16F, 0, alpha);
		c.text(label, x + 9, y, 0.55F, Ui.fade(Ui.DIM, alpha), true);
		c.text(value, x + w - c.textWidth(value) * 0.55F, y, 0.55F, Ui.fade(Ui.TEXT, alpha), true);
		Ui.bar(c, x, y + 6.5F, w, 4, frac, 0, color, segments, alpha);
	}

	private void chronicle(Canvas c, JsonArray arr, float x0, float y0, float x1, float y1, float alpha) {
		c.text("CHRONICLE", x0, y0, 0.6F, Ui.fade(Ui.GOLD, alpha), true);
		Ui.rule(c, x0 + c.textWidth("CHRONICLE") * 0.6F + 6, x1, y0 + 2.5F, alpha * 0.8F);
		float top = y0 + 8;
		Ui.inset(c, x0, top, x1, y1, 0x30C8A04A, alpha);
		float scale = 0.6F;
		float lineH = 7.5F;
		float textX = x0 + 16;
		List<String[]> rows = new ArrayList<>();
		for (int i = arr.size() - 1; i >= 0; i--) {
			String line = arr.get(i).getAsString();
			String day = "";
			String text = line;
			int colon = line.indexOf(':');
			if (line.startsWith("Day ") && colon > 0) {
				day = line.substring(4, colon);
				text = line.substring(colon + 1).trim();
			}
			List<String> wrapped = c.wrap(text, (int) ((x1 - textX - 4) / scale));
			for (int k = 0; k < wrapped.size(); k++) {
				rows.add(new String[] {k == 0 ? day : null, wrapped.get(k)});
			}
		}
		float inner = y1 - top - 4;
		float maxScroll = Math.max(0, rows.size() * lineH - inner);
		scroll = Math.max(0, Math.min(scroll, maxScroll));
		c.scissor((int) x0 + 1, (int) top + 1, (int) x1 - 1, (int) y1 - 1);
		float lineX = x0 + 7;
		c.fill(lineX - 0.25F, top + 2, lineX + 0.25F, y1 - 2, Ui.fade(0x55C8A04A, alpha));
		float y = top + 3 - scroll;
		for (String[] row : rows) {
			if (y > top - lineH && y < y1) {
				if (row[0] != null) {
					Ui.diamond(c, lineX, y + 2.6F, 1, 1.2F, Ui.fade(Ui.GOLD, alpha));
					if (!row[0].isEmpty()) {
						String d = "Day " + row[0];
						c.text(d, textX, y, scale, Ui.fade(Ui.GOLD, alpha), true);
						c.text(row[1], textX + c.textWidth(d + "  ") * scale, y, scale, Ui.fade(0xFFDDD6C4, alpha), true);
					} else {
						c.text(row[1], textX, y, scale, Ui.fade(0xFFDDD6C4, alpha), true);
					}
				} else {
					c.text(row[1], textX, y, scale, Ui.fade(0xFFDDD6C4, alpha), true);
				}
			}
			y += lineH;
		}
		c.endScissor();
		if (rows.isEmpty()) {
			c.text("Nothing written yet. Go and make history.", textX, top + 4, scale, Ui.fade(Ui.MUTED, alpha), true);
		}
	}

	private void renderFallen(Canvas c, JsonObject e, float x0, float y0, float x1, float y1, float alpha) {
		int color = Net.integer(e, "col", 0xC8A04A);
		String role = Net.string(e, "rid", "TANK");
		float pw = 76;
		float ph = 70;
		c.fill(x0 - 0.5F, y0 - 0.5F, x0 + pw + 0.5F, y0 + ph + 0.5F, Ui.fade(0xFF050507, alpha));
		c.gradient(x0, y0, x0 + pw, y0 + ph, Ui.fade(0xFF1A1A22, alpha), Ui.fade(0xFF07070C, alpha));
		Icons.draw(c, Icons.role(role), x0 + pw / 2 - 18, y0 + 10, 36 / 16F, 0x8A8698, alpha * 0.7F);
		Icons.draw(c, Icons.SKULL, x0 + pw / 2 - 6, y0 + ph - 18, 12 / 16F, 0, alpha);
		c.rect(x0, y0, x0 + pw, y0 + ph, Ui.fade(0xFF5A566A, alpha));
		float nx = x0 + pw + 8;
		c.text(Net.string(e, "name", "?"), nx, y0 + 1, 1.25F, Ui.fade(Ui.opaque(Canvas.mix(color, 0xFFFFFF, 0.3F)), alpha), true);
		c.text(Net.string(e, "cls", "") + "  ·  Level " + Net.integer(e, "lv", 1), nx, y0 + 15, 0.62F, Ui.fade(Ui.DIM, alpha), true);
		c.text("Fell on day " + Net.integer(e, "day", 0) + " to " + Net.string(e, "cause", "an enemy") + ".", nx, y0 + 25, 0.62F,
				Ui.fade(0xFFFF9A8E, alpha), true);
		c.text("“" + Net.string(e, "headline", "") + "”", nx, y0 + 35, 0.6F, Ui.fade(0xFFD8C690, alpha), true);
		c.text("Remembered.", nx, y0 + 50, 0.6F, Ui.fade(Ui.MUTED, alpha), true);
		chronicle(c, e.getAsJsonArray("lines"), x0, y0 + ph + 8, x1, y1, alpha);
	}

	private void footer(Canvas c, JsonObject data, float alpha) {
		float y = py1 - 14;
		Icons.draw(c, Icons.order(Net.string(data, "oid", "follow")), px0 + 8, y - 0.5F, 9 / 16F, 0, alpha);
		String text = Net.string(data, "order", "Follow") + "  ·  " + Net.string(data, "formation", "Line");
		c.text(text, px0 + 20, y + 1.5F, 0.6F, Ui.fade(0xFFD8D2C2, alpha), true);
		String keys = "Hold G: command wheel  ·  U: roster";
		c.text(keys, px0 + 20 + c.textWidth(text) * 0.6F + 12, y + 1.5F, 0.55F, Ui.fade(Ui.MUTED, alpha), true);
		int regalia = Net.integer(data, "regalia", 0);
		float right = px1 - 8;
		if (data.has("permadeath") && data.get("permadeath").getAsBoolean()) {
			String p = "PERMADEATH";
			float w = c.textWidth(p) * 0.55F + 8;
			c.fill(right - w, y - 0.5F, right, y + 9, Ui.fade(0xFF3A1010, alpha));
			c.rect(right - w, y - 0.5F, right, y + 9, Ui.fade(0xFFE5534B, alpha));
			c.text(p, right - w + 4, y + 1.8F, 0.55F, Ui.fade(0xFFFF8A80, alpha), true);
			right -= w + 6;
		}
		if (regalia > 0) {
			String r = "Gilded Regalia " + regalia + "/4";
			c.text(r, right - c.textWidth(r) * 0.55F, y + 1.8F, 0.55F, Ui.fade(Ui.GOLD, alpha), true);
		}
	}

	private void action(Canvas c, String label, float x, float y, boolean danger, double mx, double my, float alpha, Runnable run) {
		float w = c.textWidth(label) * 0.7F + 16;
		boolean hover = in(mx, my, x, y, x + w, y + 13);
		float[] box = Ui.button(c, x, y, label, hover, danger, alpha);
		buttons.add(new Button(box[0], box[1], box[2], box[3], run));
	}

	private void send(String type, String id) {
		JsonObject msg = Net.message(type);
		msg.addProperty("id", id);
		ClientFellowship.send(msg);
		lastRequest = -1000;
	}

	public boolean mouseClicked(double x, double y, int button) {
		if (button != 0) {
			return false;
		}
		for (int i = buttons.size() - 1; i >= 0; i--) {
			if (buttons.get(i).hit(x, y)) {
				host.click();
				buttons.get(i).action().run();
				return true;
			}
		}
		if (x < px0 || x > px1 || y < py0 || y > py1) {
			host.close();
			return true;
		}
		return false;
	}

	public boolean mouseScrolled(double x, double y, double amount) {
		if (x < listX1) {
			listScroll -= (float) amount * 26;
		} else {
			scroll -= (float) amount * 15;
		}
		return true;
	}
}
