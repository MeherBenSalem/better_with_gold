package tn.nightbeam.better_with_gold.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Plain-Java regression checks for the version-independent Fellowship core: threat, formations, progression,
 * chronicle text, saved data and config files. Runs with ./gradlew :common:coreRegression (part of check).
 */
public final class CoreRegression {
	private static int checks;

	public static void main(String[] args) throws Exception {
		threat();
		formations();
		progression();
		chronicle();
		records();
		classes();
		config();
		recruits();
		System.out.println("CoreRegression: " + checks + " checks passed");
	}

	private static void threat() {
		ThreatTable t = new ThreatTable();
		UUID tank = UUID.randomUUID();
		UUID mage = UUID.randomUUID();
		UUID ranger = UUID.randomUUID();
		check(t.choose(null, 1.1, 0) == null, "empty table has no target");
		t.add(mage, 10, 0);
		check(mage.equals(t.choose(null, 1.1, 0)), "first attacker becomes target");
		t.add(tank, 10.5, 1);
		check(mage.equals(t.choose(mage, 1.1, 1)), "5% more threat does not pull the enemy");
		t.add(tank, 1.0, 2);
		check(tank.equals(t.choose(mage, 1.1, 2)), "more than 10% more threat pulls the enemy");
		t.add(ranger, 100, 3);
		t.taunt(tank, 3, 100);
		check(tank.equals(t.choose(ranger, 1.1, 50)), "taunt holds the enemy");
		check(t.get(tank) > t.get(ranger), "taunt gives the lead");
		check(t.taunted(99) && !t.taunted(100), "taunt ends on time");
		t.decay(0.0);
		check(t.isEmpty(), "full decay clears the table");
		ThreatTable s = new ThreatTable();
		s.add(tank, 30, 0);
		s.add(mage, 10, 0);
		check(Math.abs(s.share(tank) - 0.75) < 1e-9, "share is a fraction of the total");
		check(s.ranked().get(0).getKey().equals(tank), "ranked puts the top first");
		s.remove(tank);
		check(mage.equals(s.choose(tank, 1.1, 1)), "removed attacker loses the enemy");
	}

	private static void formations() {
		List<Role> roles = List.of(Role.TANK, Role.MELEE, Role.RANGED, Role.HEALER, Role.CASTER, Role.TANK);
		for (Formation f : Formation.values()) {
			for (int n = 1; n <= 24; n++) {
				List<Role> r = new ArrayList<>();
				for (int i = 0; i < n; i++) {
					r.add(roles.get(i % roles.size()));
				}
				List<Formation.Offset> slots = f.offsets(r);
				check(slots.size() == n, f + " gives one slot per ally (" + n + ")");
				Set<String> distinct = new HashSet<>();
				for (Formation.Offset o : slots) {
					check(o != null, f + " has no empty slot");
					check(Math.hypot(o.x(), o.z()) >= 1.0, f + " keeps allies off the anchor");
					distinct.add(Math.round(o.x() * 10) + ":" + Math.round(o.z() * 10));
				}
				check(distinct.size() == n, f + " never stacks two allies (" + n + ")");
			}
		}
		List<Formation.Offset> wall = Formation.SHIELD_WALL.offsets(List.of(Role.TANK, Role.RANGED, Role.HEALER, Role.MELEE));
		check(wall.get(0).z() > 0 && wall.get(3).z() > 0, "shield wall puts tanks and melee in front");
		check(wall.get(1).z() < 0 && wall.get(2).z() < 0, "shield wall puts ranged and healers behind");
		double[] south = new Formation.Offset(0, 1).toWorld(0);
		check(Math.abs(south[0]) < 1e-9 && Math.abs(south[1] - 1) < 1e-9, "yaw 0 faces south (+z)");
		double[] west = new Formation.Offset(0, 1).toWorld(90);
		check(Math.abs(west[0] + 1) < 1e-9 && Math.abs(west[1]) < 1e-9, "yaw 90 faces west (-x)");
		double[] left = new Formation.Offset(1, 0).toWorld(0);
		check(Math.abs(left[0] - 1) < 1e-9, "left of south is east (+x)");
		List<Formation.Member<String>> members = List.of(new Formation.Member<>("b", Role.RANGED), new Formation.Member<>("a", Role.TANK),
				new Formation.Member<>("c", Role.TANK));
		check(Formation.order(members).equals(List.of("a", "c", "b")), "order puts tanks first, then by key");
		check(Formation.parse("shield wall", null) == Formation.SHIELD_WALL, "formation names parse with spaces");
		check(Formation.CIRCLE.next() == Formation.LINE, "formation cycles");
	}

	private static void progression() {
		check(Progression.xpToNext(1) == 20, "level 1 needs 20 experience");
		check(Progression.totalXpFor(20) == 1748, "level 20 needs 1748 experience in total");
		check(Progression.killXp(20) == 10, "a zombie gives 10 experience");
		check(Progression.killXp(500) == 100, "kill experience is capped at 100");
		check(Progression.killXp(0.5) == 1, "kills give at least 1");
		AllyRecord r = new AllyRecord();
		check(r.addXp(20, 20) == 1 && r.level == 2 && r.xp == 0, "20 experience reaches level 2");
		check(r.addXp(100000, 20) == 18 && r.level == 20 && r.xp == 0, "experience stops at the cap");
		check(r.addXp(50, 20) == 0, "no levels past the cap");
		check(Progression.title(1).equals("Recruit") && Progression.title(5).equals("Veteran") && Progression.title(20).equals("Legend"), "titles");
		AllyClass v = ClassCatalog.get("bulwark");
		check(Progression.price(v, 1, List.of(), 1.0) == 8, "a level 1 Bulwark costs 8 gold");
		check(Progression.price(v, 3, List.of(Trait.GREEDY), 1.0) == 16, "a level 3 greedy Bulwark costs 16 gold");
		check(Progression.moraleDamageMultiplier(90) > 1 && Progression.moraleDamageMultiplier(10) < 1, "morale changes damage");
		check(Progression.clampMorale(5, List.of(Trait.DEVOTED), 0) == 40, "devoted keeps morale at 40");
		check(Progression.clampMorale(5, List.of(), 20) == 20, "regalia raises the morale floor");
		check(Progression.clampMorale(150, List.of(), 0) == 100, "morale caps at 100");
		check(Progression.drift(70) == 69 && Progression.drift(50) == 51, "morale drifts towards 60");
		check(Math.abs(Progression.damageMultiplier(List.of(Trait.BRUTAL, Trait.KEEN), Role.RANGED) - 1.15 * 1.15) < 1e-9, "trait damage stacks");
		check(Progression.damageMultiplier(List.of(Trait.KEEN), Role.TANK) == 1.0, "keen only helps ranged roles");
	}

	private static void chronicle() {
		List<Chronicle.Entry> entries = new ArrayList<>();
		Chronicle.add(entries, new Chronicle.Entry(3, Chronicle.Type.SWORN, "Meher", "Plains", 0));
		Chronicle.add(entries, new Chronicle.Entry(4, Chronicle.Type.FIRST_BLOOD, "Zombie", "", 0));
		Chronicle.add(entries, new Chronicle.Entry(5, Chronicle.Type.DOWNED, "Skeleton", "", 0));
		Chronicle.markRevived(entries, "Mira");
		check(Chronicle.line(entries.get(0)).equals("Day 3: swore the oath to Meher in the Plains."), "oath line");
		check(Chronicle.line(entries.get(1)).equals("Day 4: drew first blood against a Zombie."), "first blood line");
		check(Chronicle.line(entries.get(2)).equals("Day 5: cut down by a Skeleton; Mira pulled them back up."), "revive fills the downed line");
		check(Chronicle.line(new Chronicle.Entry(9, Chronicle.Type.KILLS, "", "", 50)).equals("Day 9: Fifty foes felled."), "kill milestone line");
		check(Chronicle.line(new Chronicle.Entry(9, Chronicle.Type.SAVED_LEADER, "Meher", "", 1)).equals("Day 9: kept Meher alive with one heart left."), "saved line");
		check(Chronicle.line(new Chronicle.Entry(9, Chronicle.Type.FIRST_BLOOD, "Enderman", "", 0)).contains("an Enderman"), "an before vowels");
		for (int i = 0; i < 100; i++) {
			Chronicle.add(entries, new Chronicle.Entry(10 + i, Chronicle.Type.KILLS, "", "", i));
		}
		check(entries.size() == Chronicle.MAX_ENTRIES, "chronicle is capped");
		check(entries.get(0).kind() == Chronicle.Type.SWORN, "the oath is kept when the chronicle is full");
		AllyRecord r = new AllyRecord();
		check(Chronicle.headline(r).equals("Fresh to the banner"), "new ally headline");
		r.stats.bossFights = 3;
		check(Chronicle.headline(r).equals("Survived three boss fights"), "boss fight headline");
		check(NumberWords.of(42).equals("forty-two") && NumberWords.of(250).equals("250"), "number words");
		check(Chronicle.killMilestone(100) && !Chronicle.killMilestone(99), "kill milestones");
	}

	private static void records() {
		AllyRecord r = Recruits.roll(new Random(7), ClassCatalog.get("ranger"), 4);
		r.owner = UUID.randomUUID().toString();
		r.stats.kills = 12;
		Chronicle.add(r.chronicle, new Chronicle.Entry(1, Chronicle.Type.SWORN, "Meher", "Forest", 0));
		AllyRecord back = AllyRecord.fromJson(r.toJson());
		check(back.name.equals(r.name) && back.level == 4 && back.classId.equals("ranger"), "record round trip");
		check(back.stats.kills == 12 && back.chronicle.size() == 1 && back.hired(), "record keeps stats, chronicle and owner");
		check(AllyRecord.fromJson("{not json").name.isEmpty(), "broken data gives an empty record");
		AllyRecord partial = AllyRecord.fromJson("{\"name\":\"Wren\",\"traits\":null,\"stats\":null}");
		check(partial.traits != null && partial.stats != null && partial.level == 1, "missing fields are repaired");
		check(Order.parse("fallback", null) == Order.FALL_BACK && Order.parse("guard banner", null) == Order.GUARD_BANNER, "orders parse");
		check(Order.FOLLOW.travels() && !Order.HOLD.travels(), "only moving orders travel with the leader");
	}

	private static void classes() throws Exception {
		check(ClassCatalog.enabled().size() == 5, "five built-in classes");
		for (AllyClass c : ClassCatalog.defaults()) {
			check(c.sanitize(), c.id + " is valid");
			check(!c.abilityName.isEmpty() && !c.abilityDescription.isEmpty(), c.id + " describes its ability");
			check(!c.allowedTraits().isEmpty(), c.id + " has traits");
		}
		Path dir = Files.createTempDirectory("fellowship-classes");
		ClassCatalog.load(dir);
		check(Files.exists(dir.resolve("mender.json")), "built-in class files are written");
		Files.writeString(dir.resolve("warden.json"), "{\"id\":\"warden\",\"name\":\"Warden\",\"role\":\"TANK\",\"baseHealth\":99999}");
		Files.writeString(dir.resolve("broken.json"), "{\"id\":");
		Files.writeString(dir.resolve("bad_id.json"), "{\"id\":\"Bad Id\"}");
		ClassCatalog.load(dir);
		check(ClassCatalog.has("warden") && ClassCatalog.get("warden").baseHealth == 1024, "a pack class loads and is clamped");
		check(ClassCatalog.get("warden").role() == Role.TANK, "a pack class keeps its role");
		check(ClassCatalog.problems().size() == 2, "broken class files are reported, not fatal");
		check(ClassCatalog.get("nope").id.equals("bulwark"), "unknown class falls back to bulwark");
		ClassCatalog.reset();
		check(!ClassCatalog.has("warden"), "reset restores the built-in classes");
	}

	private static void config() throws Exception {
		Path dir = Files.createTempDirectory("fellowship-config");
		Path file = dir.resolve("fellowship.json");
		FellowshipConfig c = FellowshipConfig.load(file);
		check(Files.exists(file) && c.partySize == 6, "config is written with defaults");
		Files.writeString(file, "{\"partySize\":999,\"aggroSwitchRatio\":0.2,\"currency\":\"\"}");
		c = FellowshipConfig.load(file);
		check(c.partySize == 24 && c.aggroSwitchRatio == 1.0 && c.currency.equals("minecraft:gold_ingot"), "config values are clamped");
		Files.writeString(file, "not json");
		c = FellowshipConfig.load(file);
		check(c.partySize == 6, "a broken config falls back to defaults");
		FellowshipConfig.INSTANCE = new FellowshipConfig();
	}

	private static void recruits() {
		Random random = new Random(11);
		for (int i = 0; i < 200; i++) {
			AllyClass c = ClassCatalog.defaults().get(i % 5);
			AllyRecord r = Recruits.roll(random, c, 1 + i % 20);
			check(!r.name.isEmpty() && !r.epithet.isEmpty(), "recruits have a name and an epithet");
			check(r.traits.size() >= 1 && r.traits.size() <= 2, "recruits have one or two traits");
			check(new HashSet<>(r.traits).size() == r.traits.size(), "no repeated traits");
			for (Trait t : r.traitList()) {
				check(c.allowedTraits().contains(t), c.id + " only rolls its own traits");
			}
			check(r.variant >= 0 && r.variant < c.skins, "skin variant in range");
		}
	}

	private static void check(boolean ok, String what) {
		checks++;
		if (!ok) {
			throw new AssertionError("CoreRegression failed: " + what);
		}
	}
}
