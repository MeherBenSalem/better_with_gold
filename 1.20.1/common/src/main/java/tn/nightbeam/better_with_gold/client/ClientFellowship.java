package tn.nightbeam.better_with_gold.client;

import tn.nightbeam.better_with_gold.net.Net;
import tn.nightbeam.better_with_gold.platform.Services;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/** What the client knows about its warband: party frames for the HUD and the last roster. */
public final class ClientFellowship {
	public record Member(int id, String name, String classId, String className, String role, int color, int level, double health,
						 double maxHealth, int downedSeconds, int revive, boolean far) {
	}

	private static List<Member> party = List.of();
	private static String order = "Follow";
	private static String orderId = "follow";
	private static String formation = "Line";
	private static boolean banner;
	private static int roster;
	private static int cap = 6;
	private static int camp;
	private static JsonObject rosterData;
	private static long rosterVersion;
	private static long rosterShownAt;

	private ClientFellowship() {
	}

	public static void handle(String text) {
		JsonObject msg = Net.read(text);
		switch (Net.type(msg)) {
			case Net.PARTY -> readParty(msg);
			case Net.ROSTER -> {
				rosterData = msg;
				rosterVersion++;
			}
			case Net.OPEN -> ScreenOpener.openRoster(Net.string(msg, "id", ""));
			default -> {
			}
		}
	}

	private static void readParty(JsonObject msg) {
		order = Net.string(msg, "order", "Follow");
		orderId = Net.string(msg, "oid", "follow");
		formation = Net.string(msg, "formation", "Line");
		banner = msg.has("banner") && msg.get("banner").getAsBoolean();
		roster = Net.integer(msg, "roster", 0);
		cap = Net.integer(msg, "cap", 6);
		camp = Net.integer(msg, "camp", 0);
		List<Member> list = new ArrayList<>();
		JsonArray allies = msg.has("a") ? msg.getAsJsonArray("a") : new JsonArray();
		for (JsonElement e : allies) {
			JsonObject o = e.getAsJsonObject();
			list.add(new Member(Net.integer(o, "id", -1), Net.string(o, "n", "?"), Net.string(o, "c", ""), Net.string(o, "cn", ""),
					Net.string(o, "ro", "TANK"), Net.integer(o, "col", 0xC8A04A), Net.integer(o, "lv", 1), o.has("hp") ? o.get("hp").getAsDouble() : 0,
					o.has("mhp") ? o.get("mhp").getAsDouble() : 1, Net.integer(o, "d", 0), Net.integer(o, "r", 0),
					o.has("far") && o.get("far").getAsBoolean()));
		}
		party = list;
	}

	public static void reset() {
		party = List.of();
		rosterData = null;
		order = "Follow";
		orderId = "follow";
		formation = "Line";
		banner = false;
	}

	public static void send(JsonObject message) {
		Services.PLATFORM.sendToServer(Net.write(message));
	}

	public static List<Member> party() {
		return party;
	}

	public static String order() {
		return order;
	}

	/** The current order's id (follow, charge, focus, hold, fall_back, guard_banner), for its icon. */
	public static String orderId() {
		return orderId;
	}

	public static String formation() {
		return formation;
	}

	public static boolean banner() {
		return banner;
	}

	public static int roster() {
		return roster;
	}

	public static int cap() {
		return cap;
	}

	public static int camp() {
		return camp;
	}

	public static JsonObject rosterData() {
		return rosterData;
	}

	/** Called every frame the roster is drawn; the party frames step aside while it is open. */
	public static void rosterShown() {
		rosterShownAt = System.nanoTime();
	}

	public static boolean rosterOpen() {
		return System.nanoTime() - rosterShownAt < 150_000_000L;
	}

	public static long rosterVersion() {
		return rosterVersion;
	}
}
