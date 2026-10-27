package tn.nightbeam.better_with_gold.ally;

import tn.nightbeam.better_with_gold.compat.Compat;
import tn.nightbeam.better_with_gold.core.FellowshipConfig;
import tn.nightbeam.better_with_gold.core.Role;
import tn.nightbeam.better_with_gold.core.ThreatTable;
import tn.nightbeam.better_with_gold.registry.ModEntities;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Who each engaged enemy is angry at. Allies and warband leaders add threat by hurting enemies and by healing;
 * Bulwarks taunt. Twice a second every engaged enemy is pointed at whoever holds the most threat (with a margin,
 * so it does not flip every hit), and a label above it shows who that is.
 */
public final class Threat {
	private static final Map<UUID, Engagement> ENGAGED = new HashMap<>();

	private static final class Engagement {
		final ThreatTable table = new ThreatTable();
		final ServerLevel level;
		final UUID mob;
		ThreatLabelEntity label;
		String text = "";

		Engagement(ServerLevel level, UUID mob) {
			this.level = level;
			this.mob = mob;
		}
	}

	private Threat() {
	}

	public static void clear() {
		for (Engagement e : ENGAGED.values()) {
			if (e.label != null) {
				e.label.discard();
			}
		}
		ENGAGED.clear();
	}

	public static boolean ownsLabel(ThreatLabelEntity label) {
		Entity vehicle = label.getVehicle();
		if (vehicle == null) {
			return false;
		}
		Engagement e = ENGAGED.get(vehicle.getUUID());
		return e != null && e.label == label;
	}

	/** Called when {@code attacker} hurts {@code victim}. */
	public static void damaged(Mob victim, LivingEntity attacker, float amount) {
		if (!(victim.level() instanceof ServerLevel level) || attacker == null || amount <= 0) {
			return;
		}
		double multiplier = 1.0;
		if (attacker instanceof AllyEntity ally) {
			if (!ally.isHired() || ally.isOwnedBy(victim)) {
				return;
			}
			multiplier = ally.allyClass().threatMultiplier;
		} else if (attacker instanceof Player player) {
			if (!ENGAGED.containsKey(victim.getUUID()) && !Warbands.leadsLiveBand(player.getUUID())) {
				return;
			}
		} else {
			return;
		}
		Engagement e = ENGAGED.computeIfAbsent(victim.getUUID(), id -> new Engagement(level, id));
		e.table.add(attacker.getUUID(), amount * multiplier, Compat.gameTime(level));
	}

	public static void taunt(Mob mob, AllyEntity tank, int ticks) {
		if (!(mob.level() instanceof ServerLevel level)) {
			return;
		}
		Engagement e = ENGAGED.computeIfAbsent(mob.getUUID(), id -> new Engagement(level, id));
		long now = Compat.gameTime(level);
		e.table.taunt(tank.getUUID(), now, now + ticks);
		Compat.forceTarget(mob, tank);
		updateLabel(e, mob);
	}

	/** Healing makes the healer a little hated by every enemy fighting the healed one. */
	public static void healed(LivingEntity healer, LivingEntity target, float amount) {
		List<Engagement> fighting = new ArrayList<>();
		for (Engagement e : ENGAGED.values()) {
			if (e.table.get(target.getUUID()) > 0) {
				fighting.add(e);
			}
		}
		if (fighting.isEmpty()) {
			return;
		}
		double share = amount * 0.5 / fighting.size();
		for (Engagement e : fighting) {
			e.table.add(healer.getUUID(), share, Compat.gameTime(e.level));
		}
	}

	/** Drops an attacker from every table (downed, gone, teleported away) and frees enemies chasing it. */
	public static void forget(LivingEntity who) {
		for (Engagement e : ENGAGED.values()) {
			e.table.remove(who.getUUID());
		}
		if (who.level() instanceof ServerLevel level) {
			for (Mob mob : level.getEntitiesOfClass(Mob.class, who.getBoundingBox().inflate(32))) {
				if (Compat.currentTarget(mob) == who) {
					mob.setTarget(null);
				}
			}
		}
	}

	public static ThreatTable table(Mob mob) {
		Engagement e = ENGAGED.get(mob.getUUID());
		return e == null ? null : e.table;
	}

	public static List<UUID> attackers(LivingEntity mob) {
		Engagement e = ENGAGED.get(mob.getUUID());
		if (e == null) {
			return List.of();
		}
		List<UUID> list = new ArrayList<>();
		for (Map.Entry<UUID, Double> entry : e.table.ranked()) {
			list.add(entry.getKey());
		}
		return list;
	}

	public static boolean engaged(Mob mob) {
		return ENGAGED.containsKey(mob.getUUID());
	}

	public static void died(LivingEntity entity) {
		Engagement e = ENGAGED.remove(entity.getUUID());
		if (e != null && e.label != null) {
			e.label.discard();
		}
		forget(entity);
	}

	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % 10 != 0) {
			return;
		}
		double ratio = FellowshipConfig.INSTANCE.aggroSwitchRatio;
		Iterator<Engagement> it = ENGAGED.values().iterator();
		while (it.hasNext()) {
			Engagement e = it.next();
			Entity entity = e.level.getEntity(e.mob);
			if (!(entity instanceof Mob mob) || !mob.isAlive()) {
				drop(it, e);
				continue;
			}
			long now = Compat.gameTime(e.level);
			e.table.decay(0.98);
			for (Map.Entry<UUID, Double> entry : e.table.ranked()) {
				Entity who = e.level.getEntity(entry.getKey());
				if (!(who instanceof LivingEntity living) || !living.isAlive() || living.distanceToSqr(mob) > 48 * 48
						|| living instanceof AllyEntity ally && ally.isDowned() || living instanceof Player p && (p.isCreative() || p.isSpectator())) {
					e.table.remove(entry.getKey());
				}
			}
			if (e.table.isEmpty() || now - e.table.lastUpdate() > 600 && Compat.currentTarget(mob) == null) {
				drop(it, e);
				continue;
			}
			LivingEntity current = Compat.currentTarget(mob);
			UUID pick = e.table.choose(current == null ? null : current.getUUID(), ratio, now);
			if (pick != null && (current == null || !pick.equals(current.getUUID()))) {
				Entity target = e.level.getEntity(pick);
				if (target instanceof LivingEntity living && !(mob instanceof AllyEntity)) {
					Compat.forceTarget(mob, living);
				}
			}
			updateLabel(e, mob);
		}
	}

	private static void drop(Iterator<Engagement> it, Engagement e) {
		if (e.label != null) {
			e.label.discard();
		}
		it.remove();
	}

	private static void updateLabel(Engagement e, Mob mob) {
		boolean show = FellowshipConfig.INSTANCE.aggroLabels && hasAlly(e);
		if (!show) {
			if (e.label != null) {
				e.label.discard();
				e.label = null;
			}
			return;
		}
		UUID holder = e.table.holder();
		if (holder == null) {
			return;
		}
		Entity who = e.level.getEntity(holder);
		if (who == null) {
			return;
		}
		int color;
		String name;
		if (who instanceof AllyEntity ally) {
			name = ally.record().name;
			color = ally.role() == Role.TANK ? 0x66CCFF : 0xFF7755;
		} else {
			name = who.getName().getString();
			color = 0xF2C94C;
		}
		int percent = (int) Math.round(e.table.share(holder) * 100);
		boolean taunted = e.table.taunted(Compat.gameTime(e.level));
		String text = (taunted ? "⚔ " : "▶ ") + name + " " + percent + "%";
		if (e.label == null || e.label.isRemoved() || e.label.getVehicle() != mob) {
			if (e.label != null) {
				e.label.discard();
			}
			ThreatLabelEntity label = Compat.create(ModEntities.THREAT_LABEL.get(), e.level);
			if (label == null) {
				return;
			}
			Compat.moveTo(label, mob.getX(), mob.getY() + mob.getBbHeight(), mob.getZ(), 0);
			e.level.addFreshEntity(label);
			Compat.ride(label, mob);
			e.label = label;
			e.text = "";
		}
		if (!text.equals(e.text)) {
			e.text = text;
			MutableComponent c = Component.literal(text).withStyle(s -> s.withColor(TextColor.fromRgb(color)));
			if (taunted) {
				c = Component.literal("TAUNTED ").withStyle(s -> s.withColor(TextColor.fromRgb(0x66CCFF)).withBold(true)).append(c);
			}
			e.label.setCustomName(c);
			e.label.setCustomNameVisible(true);
		}
	}

	private static boolean hasAlly(Engagement e) {
		for (Map.Entry<UUID, Double> entry : e.table.ranked()) {
			if (e.level.getEntity(entry.getKey()) instanceof AllyEntity) {
				return true;
			}
		}
		return false;
	}
}
