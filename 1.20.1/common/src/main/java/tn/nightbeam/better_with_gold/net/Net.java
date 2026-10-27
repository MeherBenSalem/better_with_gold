package tn.nightbeam.better_with_gold.net;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Fellowship sends small JSON messages over one channel per direction. Each message is an object with a "t"
 * (type) field. The largest, the roster with every chronicle, stays well under the payload limit.
 */
public final class Net {
	public static final int MAX_LENGTH = 1_000_000;
	public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

	// server to client
	/** Party frames for the HUD: allies near the leader, health, downed timers, order. */
	public static final String PARTY = "party";
	/** The whole roster: allies in the world, at camp and fallen, with chronicles. */
	public static final String ROSTER = "roster";
	/** Opens the roster screen on one ally. */
	public static final String OPEN = "open";

	// client to server
	public static final String ORDER = "order";
	public static final String FORMATION = "formation";
	public static final String RECALL = "recall";
	public static final String SUMMON = "summon";
	public static final String STOW = "stow";
	public static final String DISMISS = "dismiss";
	public static final String ROSTER_REQUEST = "roster?";

	private Net() {
	}

	public static JsonObject message(String type) {
		JsonObject o = new JsonObject();
		o.addProperty("t", type);
		return o;
	}

	public static String write(JsonObject message) {
		return GSON.toJson(message);
	}

	public static JsonObject read(String text) {
		try {
			return JsonParser.parseString(text).getAsJsonObject();
		} catch (RuntimeException ex) {
			return new JsonObject();
		}
	}

	public static String type(JsonObject message) {
		return message.has("t") ? message.get("t").getAsString() : "";
	}

	public static String string(JsonObject o, String key, String fallback) {
		return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : fallback;
	}

	public static int integer(JsonObject o, String key, int fallback) {
		try {
			return o.has(key) ? o.get(key).getAsInt() : fallback;
		} catch (RuntimeException ex) {
			return fallback;
		}
	}
}
