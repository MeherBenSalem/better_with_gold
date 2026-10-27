package tn.nightbeam.better_with_gold.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Rolls new recruits: a name, an epithet from their first trait, one or two traits and a skin. Plain Java. */
public final class Recruits {
	private static final String[] NAMES = {
			"Aldric", "Alden", "Ansel", "Ashby", "Brenna", "Bryn", "Brannoc", "Calder", "Cassia", "Corwin", "Dagny", "Darian",
			"Della", "Edric", "Elsa", "Emeric", "Faye", "Fenna", "Garrick", "Gideon", "Hale", "Hilde", "Hollis", "Ines",
			"Isolde", "Ivo", "Jessamy", "Jory", "Kael", "Kestrel", "Leif", "Liora", "Lisbet", "Maren", "Mira", "Morwen",
			"Nessa", "Niall", "Odile", "Orrin", "Osric", "Pell", "Perrin", "Petra", "Quill", "Rhosyn", "Rowan", "Runa",
			"Sera", "Sigrun", "Soren", "Talia", "Tamsin", "Thane", "Tobin", "Ulric", "Una", "Vale", "Varek", "Vesna",
			"Willa", "Wren", "Wynne", "Yara", "Ysolde", "Zora", "Bastian", "Celyn", "Dorran", "Esk", "Galen", "Hesper",
			"Ilsa", "Joss", "Katla", "Lorne", "Merrick", "Nyra", "Oswin", "Priya", "Rook", "Saoirse", "Teodor", "Viggo"
	};

	private Recruits() {
	}

	public static String name(Random random) {
		return NAMES[random.nextInt(NAMES.length)];
	}

	public static List<String> names() {
		return List.of(NAMES);
	}

	/** A new, unhired ally of {@code allyClass} at {@code level}. */
	public static AllyRecord roll(Random random, AllyClass allyClass, int level) {
		AllyRecord r = new AllyRecord();
		r.classId = allyClass.id;
		r.level = Math.max(1, level);
		r.name = name(random);
		List<Trait> pool = new ArrayList<>(allyClass.allowedTraits());
		if (pool.isEmpty()) {
			pool.addAll(List.of(Trait.STEADFAST, Trait.SWIFT, Trait.BRUTAL, Trait.HARDY));
		}
		int count = random.nextFloat() < 0.35f ? 2 : 1;
		for (int i = 0; i < count && !pool.isEmpty(); i++) {
			Trait t = pool.remove(random.nextInt(pool.size()));
			r.traits.add(t.id());
		}
		Trait first = Trait.parse(r.traits.get(0));
		r.epithet = first == null ? "" : first.epithet(random.nextInt(2));
		r.variant = random.nextInt(Math.max(1, allyClass.skins));
		return r;
	}
}
