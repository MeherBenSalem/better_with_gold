package tn.nightbeam.better_with_gold.client;

import tn.nightbeam.better_with_gold.net.Net;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * The roster: every ally in the world, at camp or recovering, with class, level, morale, traits, ability, stats
 * and their Chronicle; and the Remembrance of the fallen. Drawn on a Canvas so the screen is shared by every
 * version; each version only wraps it in its own Screen.
 */
public final class RosterView {
	public interface Host {
		void click();

		void close();
	}

	private final Host host;
	private String selectedId;
	private int selected;
	private boolean remembrance;
	private float scroll;
	private float listScroll;
	private long lastRequest = -1000;
	private long lastVersion = -1;
	private String confirmDismiss = "";
	private final List<Button> buttons = new ArrayList<>();
	private float px0;
	private float py0;
	private float px1;
	private float py1;
	private float chronicleTop;
	private float chronicleBottom;
	private float listBottom;

	private record Button(float x0, float y0, float x1, float y1, Runnable action) {
		boolean hit(double x, double y) {
			return x >= x0 && x <= x1 && y >= y0 && y <= y1;
		}
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

	public void render(Canvas c, double mouseX, double mouseY) {
		buttons.clear();
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
		float w = Math.min(430, c.width() - 16);
		float h = Math.min(270, c.height() - 16);
		px0 = (c.width() - w) / 2;
		py0 = (c.height() - h) / 2;
		px1 = px0 + w;
		py1 = py0 + h;
		c.fill(0, 0, c.width(), c.height(), 0x88000000);
		c.panel(px0, py0, px1, py1, 0xFFC8A04A);
		c.text("REMNANTS | FELLOWSHIP", px0 + 8, py0 + 6, 0.75F, 0xFFF2C94C, true);
		int cap = data == null ? ClientFellowship.cap() : Net.integer(data, "cap", 6);
		tab(c, "Warband " + allies().size() + "/" + cap, px1 - 172, py0 + 4, !remembrance, () -> {
			remembrance = false;
			selected = 0;
			scroll = 0;
		});
		tab(c, "Remembrance " + fallen().size(), px1 - 86, py0 + 4, remembrance, () -> {
			remembrance = true;
			selected = 0;
			scroll = 0;
		});
		c.fill(px0 + 6, py0 + 18, px1 - 6, py0 + 19, 0x55C8A04A);
		if (data == null) {
			c.centered("Gathering the roster...", (px0 + px1) / 2, (py0 + py1) / 2, 1, 0xFFAAAAAA, true);
			return;
		}
		float listX1 = px0 + 128;
		listBottom = py1 - 22;
		renderList(c, entries, px0 + 6, py0 + 23, listX1, listBottom, mouseX, mouseY);
		if (entries.isEmpty()) {
			String empty = remembrance ? "No one has fallen under your banner." : "No allies yet. Find sellswords near village bells and pay them in gold.";
			List<String> lines = c.wrap(empty, (int) ((px1 - listX1 - 20) / 0.85F));
			float y = py0 + 40;
			for (String line : lines) {
				c.text(line, listX1 + 10, y, 0.85F, 0xFFBBBBBB, true);
				y += 10;
			}
		} else if (remembrance) {
			renderFallen(c, entries.get(selected), listX1 + 8, py0 + 23, px1 - 8, py1 - 22);
		} else {
			renderAlly(c, entries.get(selected), listX1 + 8, py0 + 23, px1 - 8, py1 - 22);
		}
		String footer = "Order: " + Net.string(data, "order", "Follow") + "  ·  Formation: " + Net.string(data, "formation", "Line")
				+ "  ·  Hold G for the command wheel";
		c.text(footer, px0 + 8, py1 - 13, 0.62F, 0xFF999999, true);
		if (data.has("permadeath") && data.get("permadeath").getAsBoolean()) {
			c.text("PERMADEATH ON", px1 - 8 - c.textWidth("PERMADEATH ON") * 0.62F, py1 - 13, 0.62F, 0xFFFF5555, true);
		}
	}

	private void tab(Canvas c, String label, float x, float y, boolean active, Runnable action) {
		float w = 82;
		c.fill(x, y, x + w, y + 12, active ? 0xFF2A2410 : 0xFF14141C);
		c.rect(x, y, x + w, y + 12, active ? 0xFFF2C94C : 0x66C8A04A);
		c.centered(label, x + w / 2, y + 3, 0.65F, active ? 0xFFF2C94C : 0xFFAAAAAA, true);
		buttons.add(new Button(x, y, x + w, y + 12, action));
	}

	private void renderList(Canvas c, List<JsonObject> entries, float x0, float y0, float x1, float y1, double mx, double my) {
		c.fill(x0, y0, x1, y1, 0x66000000);
		int rowH = 22;
		float maxScroll = Math.max(0, entries.size() * rowH - (y1 - y0));
		listScroll = Math.max(0, Math.min(listScroll, maxScroll));
		c.scissor((int) x0, (int) y0, (int) x1, (int) y1);
		for (int i = 0; i < entries.size(); i++) {
			JsonObject e = entries.get(i);
			float ry = y0 + i * rowH - listScroll;
			if (ry + rowH < y0 || ry > y1) {
				continue;
			}
			boolean sel = i == selected;
			boolean hover = mx >= x0 && mx <= x1 && my >= ry && my < ry + rowH && my >= y0 && my <= y1;
			c.fill(x0 + 1, ry + 1, x1 - 1, ry + rowH - 1, sel ? 0xFF2A2410 : hover ? 0xFF1A1A24 : 0x00000000);
			int color = 0xFF000000 | Net.integer(e, "col", 0xC8A04A);
			c.fill(x0 + 2, ry + 2, x0 + 4, ry + rowH - 2, color);
			c.text(Net.string(e, "name", "?"), x0 + 8, ry + 4, 0.75F, sel ? 0xFFF2C94C : 0xFFFFFFFF, true);
			String sub = Net.string(e, "cls", "") + " " + Net.integer(e, "lv", 1);
			if (!remembrance) {
				String state = Net.string(e, "state", "active");
				int stateColor = switch (state) {
					case "downed" -> 0xFFFF5555;
					case "camp" -> 0xFF88AACC;
					case "recovering" -> 0xFFE0B03A;
					case "away" -> 0xFF999999;
					default -> 0xFF66CC77;
				};
				c.text(state.toUpperCase(), x1 - 4 - c.textWidth(state.toUpperCase()) * 0.5F, ry + 13, 0.5F, stateColor, true);
			}
			c.text(sub, x0 + 8, ry + 13, 0.6F, 0xFF999999, true);
			final int index = i;
			if (ry >= y0 && ry + rowH <= y1 + rowH) {
				buttons.add(new Button(x0, Math.max(y0, ry), x1, Math.min(y1, ry + rowH), () -> {
					selected = index;
					scroll = 0;
					confirmDismiss = "";
					selectedId = Net.string(entries.get(index), "id", "");
				}));
			}
		}
		c.endScissor();
	}

	private void renderAlly(Canvas c, JsonObject e, float x0, float y0, float x1, float y1) {
		int color = 0xFF000000 | Net.integer(e, "col", 0xC8A04A);
		String name = Net.string(e, "name", "?");
		String epithet = Net.string(e, "epithet", "");
		c.text(name + (epithet.isEmpty() ? "" : " " + epithet), x0, y0, 1.15F, color, true);
		String title = Net.string(e, "cls", "") + " · " + Net.string(e, "role", "") + " · Level " + Net.integer(e, "lv", 1) + " " + Net.string(e, "title", "");
		c.text(title, x0, y0 + 13, 0.7F, 0xFFDDDDDD, true);
		c.text("“" + Net.string(e, "headline", "") + "”", x0, y0 + 22, 0.7F, 0xFFF2C94C, true);
		float y = y0 + 33;
		float half = (x1 - x0) / 2 - 4;
		bar(c, x0, y, half, "Health", e.get("hp").getAsDouble(), e.get("mhp").getAsDouble(), 0xFF4CC36B, String.format("%.0f / %.0f", e.get("hp").getAsDouble(), e.get("mhp").getAsDouble()));
		bar(c, x0 + half + 8, y, half, "Experience", Net.integer(e, "xp", 0), Math.max(1, Net.integer(e, "xpNext", 1)), 0xFF5B8DEF,
				Net.integer(e, "xp", 0) + " / " + Net.integer(e, "xpNext", 1));
		y += 15;
		bar(c, x0, y, half, "Morale", Net.integer(e, "morale", 0), 100, 0xFFF2C94C, Net.integer(e, "morale", 0) + " " + Net.string(e, "moraleState", ""));
		c.text(Net.string(e, "where", ""), x0 + half + 8, y + 3, 0.65F, 0xFFAAAAAA, true);
		y += 15;
		StringBuilder traits = new StringBuilder();
		for (JsonElement t : e.getAsJsonArray("traits")) {
			JsonObject to = t.getAsJsonObject();
			traits.append(traits.length() == 0 ? "" : "   ").append(Net.string(to, "n", "")).append(": ").append(Net.string(to, "d", ""));
		}
		y = wrapped(c, traits.toString(), x0, y, x1 - x0, 0.62F, 0xFFC9B7F0);
		y = wrapped(c, Net.string(e, "ability", "") + ": " + Net.string(e, "abilityText", ""), x0, y + 1, x1 - x0, 0.62F, 0xFF9AD1FF);
		JsonObject stats = e.getAsJsonObject("stats");
		String s = Net.integer(stats, "kills", 0) + " kills  ·  " + Net.integer(stats, "bosses", 0) + " bosses slain  ·  "
				+ Net.integer(stats, "bossFights", 0) + " boss fights  ·  downed " + Net.integer(stats, "downs", 0) + "  ·  revived "
				+ Net.integer(stats, "revives", 0) + "  ·  healed " + Net.integer(stats, "healing", 0);
		c.text(s, x0, y + 2, 0.6F, 0xFFBBBBBB, true);
		y += 12;
		c.text("CHRONICLE", x0, y, 0.65F, 0xFFF2C94C, true);
		y += 9;
		chronicleTop = y;
		chronicleBottom = y1 - 16;
		List<String> lines = new ArrayList<>();
		JsonArray arr = e.getAsJsonArray("lines");
		for (int i = arr.size() - 1; i >= 0; i--) {
			lines.addAll(c.wrap(arr.get(i).getAsString(), (int) ((x1 - x0) / 0.66F)));
		}
		scrolled(c, lines, x0, chronicleTop, x1, chronicleBottom, 0.66F, 0xFFDDD6C4);
		String id = Net.string(e, "id", "");
		String state = Net.string(e, "state", "active");
		float by = y1 - 13;
		if (state.equals("camp")) {
			button(c, "Summon", x0, by, () -> send(Net.SUMMON, id));
		} else if (state.equals("active")) {
			button(c, "Send to camp", x0, by, () -> send(Net.STOW, id));
		} else if (state.equals("away")) {
			button(c, "Recall", x0, by, () -> ClientFellowship.send(Net.message(Net.RECALL)));
		}
		boolean confirming = confirmDismiss.equals(id);
		button(c, confirming ? "Click again to dismiss" : "Dismiss", x1 - (confirming ? 110 : 60), by, () -> {
			if (confirmDismiss.equals(id)) {
				send(Net.DISMISS, id);
				confirmDismiss = "";
				selected = 0;
			} else {
				confirmDismiss = id;
			}
		});
	}

	private static String plural(int n, String one, String many) {
		return n + " " + (n == 1 ? one : many);
	}

	private void renderFallen(Canvas c, JsonObject e, float x0, float y0, float x1, float y1) {
		int color = 0xFF000000 | Net.integer(e, "col", 0xC8A04A);
		c.text(Net.string(e, "name", "?"), x0, y0, 1.15F, color, true);
		c.text(Net.string(e, "cls", "") + " · Level " + Net.integer(e, "lv", 1) + " · fell on day " + Net.integer(e, "day", 0)
				+ " to " + Net.string(e, "cause", "an enemy"), x0, y0 + 13, 0.7F, 0xFFDDDDDD, true);
		c.text("“" + Net.string(e, "headline", "") + "”", x0, y0 + 22, 0.7F, 0xFFF2C94C, true);
		List<String> lines = new ArrayList<>();
		JsonArray arr = e.getAsJsonArray("lines");
		for (int i = arr.size() - 1; i >= 0; i--) {
			lines.addAll(c.wrap(arr.get(i).getAsString(), (int) ((x1 - x0) / 0.66F)));
		}
		chronicleTop = y0 + 36;
		chronicleBottom = y1;
		scrolled(c, lines, x0, chronicleTop, x1, chronicleBottom, 0.66F, 0xFFDDD6C4);
	}

	private void scrolled(Canvas c, List<String> lines, float x0, float y0, float x1, float y1, float scale, int color) {
		float lineH = 9;
		float maxScroll = Math.max(0, lines.size() * lineH - (y1 - y0));
		scroll = Math.max(0, Math.min(scroll, maxScroll));
		c.fill(x0 - 2, y0 - 1, x1, y1, 0x44000000);
		c.scissor((int) (x0 - 2), (int) y0, (int) x1, (int) y1);
		float y = y0 + 1 - scroll;
		for (String line : lines) {
			if (y > y0 - lineH && y < y1) {
				c.text(line, x0, y, scale, color, true);
			}
			y += lineH;
		}
		c.endScissor();
	}

	private float wrapped(Canvas c, String text, float x, float y, float width, float scale, int color) {
		for (String line : c.wrap(text, (int) (width / scale))) {
			c.text(line, x, y, scale, color, true);
			y += 8;
		}
		return y;
	}

	private void bar(Canvas c, float x, float y, float w, String label, double value, double max, int color, String text) {
		c.text(label, x, y, 0.6F, 0xFF999999, true);
		c.fill(x, y + 7, x + w, y + 11, 0xFF22222A);
		c.fill(x, y + 7, x + (float) (w * Math.max(0, Math.min(1, value / max))), y + 11, color);
		c.text(text, x + w - c.textWidth(text) * 0.55F, y, 0.55F, 0xFFDDDDDD, true);
	}

	private void button(Canvas c, String label, float x, float y, Runnable action) {
		float w = c.textWidth(label) * 0.7F + 12;
		c.fill(x, y, x + w, y + 12, 0xFF2A2410);
		c.rect(x, y, x + w, y + 12, 0xFFC8A04A);
		c.centered(label, x + w / 2, y + 3, 0.7F, 0xFFF2C94C, true);
		buttons.add(new Button(x, y, x + w, y + 12, action));
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
		if (x < px0 + 134) {
			listScroll -= (float) amount * 22;
		} else {
			scroll -= (float) amount * 18;
		}
		return true;
	}
}
