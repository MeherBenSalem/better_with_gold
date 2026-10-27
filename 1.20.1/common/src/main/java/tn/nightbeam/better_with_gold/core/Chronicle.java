package tn.nightbeam.better_with_gold.core;

import java.util.ArrayList;
import java.util.List;

/**
 * An ally's story, written from what actually happened to it: where it swore the oath, its first kill, the
 * bosses it survived, the times it went down and who pulled it back. Entries are small structured records; the
 * sentences are built when shown, so they read the same on every version. Plain Java.
 */
public final class Chronicle {
	public static final int MAX_ENTRIES = 48;

	public enum Type {
		SWORN, FIRST_BLOOD, KILLS, BOSS_KILL, BOSS_FIGHT, DOWNED, REVIVED_OTHER, SAVED_LEADER, TITLE, DIMENSION,
		LAST_STAND, FELL, RETREATED, RENAMED
	}

	public static final class Entry {
		public long day;
		public String type = "";
		public String a = "";
		public String b = "";
		public int n;

		public Entry() {
		}

		public Entry(long day, Type type, String a, String b, int n) {
			this.day = day;
			this.type = type.name();
			this.a = a == null ? "" : a;
			this.b = b == null ? "" : b;
			this.n = n;
		}

		public Type kind() {
			try {
				return Type.valueOf(type);
			} catch (IllegalArgumentException | NullPointerException ex) {
				return Type.RENAMED;
			}
		}
	}

	private Chronicle() {
	}

	/** Adds an entry, keeping the oath and the most recent entries when the list is full. */
	public static void add(List<Entry> entries, Entry entry) {
		entries.add(entry);
		while (entries.size() > MAX_ENTRIES) {
			int drop = entries.get(0).kind() == Type.SWORN && entries.size() > 1 ? 1 : 0;
			entries.remove(drop);
		}
	}

	/** Fills in who revived the ally on its most recent unanswered "downed" entry. */
	public static void markRevived(List<Entry> entries, String reviver) {
		for (int i = entries.size() - 1; i >= 0; i--) {
			Entry e = entries.get(i);
			if (e.kind() == Type.DOWNED) {
				if (e.b == null || e.b.isEmpty()) {
					e.b = reviver;
				}
				return;
			}
		}
	}

	public static String line(Entry e) {
		String day = "Day " + e.day + ": ";
		return day + switch (e.kind()) {
			case SWORN -> "swore the oath to " + e.a + (e.b.isEmpty() ? "." : " in the " + e.b + ".");
			case FIRST_BLOOD -> "drew first blood against " + article(e.a) + ".";
			case KILLS -> capitalize(NumberWords.of(e.n)) + " foes felled.";
			case BOSS_KILL -> "struck the final blow on the " + e.a + ".";
			case BOSS_FIGHT -> "stood against the " + e.a + " and lived.";
			case DOWNED -> "cut down by " + article(e.a) + (e.b.isEmpty() ? "." : "; " + e.b + " pulled them back up.");
			case REVIVED_OTHER -> "dragged " + e.a + " back to their feet.";
			case SAVED_LEADER -> "kept " + e.a + " alive with " + NumberWords.count(e.n, "heart", "hearts") + " left.";
			case TITLE -> "earned the title " + e.a + ".";
			case DIMENSION -> "first crossed into " + e.a + ".";
			case LAST_STAND -> "fought on alone while the rest of the band was down.";
			case FELL -> "fell to " + article(e.a) + (e.b.isEmpty() ? "." : ", " + e.b + ".");
			case RETREATED -> "bled out against " + article(e.a) + " and was carried back to camp.";
			case RENAMED -> "took the name " + e.a + ".";
		};
	}

	/** The one line that sums an ally up, for the roster card. */
	public static String headline(AllyRecord r) {
		AllyRecord.Stats s = r.stats;
		if (s.bossKills >= 1) {
			return "Slew " + NumberWords.count(s.bossKills, "boss", "bosses");
		}
		if (s.bossFights >= 1) {
			return "Survived " + NumberWords.count(s.bossFights, "boss fight", "boss fights");
		}
		if (s.revivesGiven >= 3) {
			return "Pulled " + NumberWords.of(s.revivesGiven) + " allies back from the brink";
		}
		if (s.revivesReceived >= 2) {
			return "Came back from the brink " + NumberWords.of(s.revivesReceived) + " times";
		}
		if (s.kills >= 10) {
			return capitalize(NumberWords.of(s.kills)) + " foes felled";
		}
		if (s.kills >= 1) {
			return "Blooded";
		}
		return "Fresh to the banner";
	}

	/** Kill counts that earn a chronicle line. */
	public static boolean killMilestone(int kills) {
		return kills == 10 || kills == 25 || kills == 50 || kills == 100 || kills == 250 || kills == 500 || kills == 1000;
	}

	public static List<String> lines(List<Entry> entries) {
		List<String> out = new ArrayList<>();
		for (Entry e : entries) {
			out.add(line(e));
		}
		return out;
	}

	static String article(String noun) {
		if (noun == null || noun.isEmpty()) {
			return "an enemy";
		}
		if (noun.startsWith("the ") || Character.isUpperCase(noun.charAt(0)) && noun.contains(" the ")) {
			return noun;
		}
		char c = Character.toLowerCase(noun.charAt(0));
		return ("aeiou".indexOf(c) >= 0 ? "an " : "a ") + noun;
	}

	static String capitalize(String s) {
		return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}
}
