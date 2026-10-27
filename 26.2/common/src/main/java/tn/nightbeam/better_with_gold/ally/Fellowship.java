package tn.nightbeam.better_with_gold.ally;

import tn.nightbeam.better_with_gold.Constants;
import tn.nightbeam.better_with_gold.compat.Compat;
import tn.nightbeam.better_with_gold.core.ClassCatalog;
import tn.nightbeam.better_with_gold.core.FellowshipConfig;
import tn.nightbeam.better_with_gold.core.Formation;
import tn.nightbeam.better_with_gold.core.Order;
import tn.nightbeam.better_with_gold.core.Progression;
import tn.nightbeam.better_with_gold.net.Net;
import tn.nightbeam.better_with_gold.platform.Services;

import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The server-side entry points the loaders call: server start and stop, ticks, players joining and leaving,
 * damage and deaths, and messages from clients. Shared by every loader and version.
 */
public final class Fellowship {
	private Fellowship() {
	}

	public static Path configDirectory() {
		return Services.PLATFORM.configDirectory().resolve(Constants.MOD_ID);
	}

	public static void reload() {
		Path dir = configDirectory();
		FellowshipConfig.load(dir.resolve("fellowship.json"));
		ClassCatalog.load(dir.resolve("classes"));
		for (String problem : ClassCatalog.problems()) {
			Constants.LOG.warn("Fellowship class file problem: {}", problem);
		}
	}

	public static void serverStarted(MinecraftServer server) {
		reload();
		Threat.clear();
		Warbands.start(server);
	}

	public static void serverStopping(MinecraftServer server) {
		Warbands.stop();
		Threat.clear();
	}

	public static void tick(MinecraftServer server) {
		Threat.tick(server);
		Warbands.tick(server);
		Sellswords.tick(server);
	}

	public static void join(ServerPlayer player) {
		Warbands.join(player);
	}

	public static void leave(ServerPlayer player) {
		Warbands.leave(player);
		Sellswords.forget(player);
	}

	// ---- friend and foe ----

	/** True when the ally must never hurt {@code other} (and the other way round, unless friendly fire is on). */
	public static boolean isFriendly(AllyEntity ally, Entity other) {
		if (other == null) {
			return false;
		}
		if (other == ally || other instanceof ThreatLabelEntity) {
			return true;
		}
		UUID owner = ally.ownerId();
		if (other instanceof AllyEntity a) {
			if (owner == null || a.ownerId() == null || owner.equals(a.ownerId())) {
				return true;
			}
			return !FellowshipConfig.INSTANCE.allowPvp;
		}
		if (other instanceof Player p) {
			if (owner == null || owner.equals(p.getUUID())) {
				return true;
			}
			Player leader = ally.owner();
			if (leader != null && leader.isAlliedTo(p)) {
				return true;
			}
			return !FellowshipConfig.INSTANCE.allowPvp;
		}
		if (owner != null && owner.equals(Compat.ownerOf(other))) {
			return true;
		}
		return Compat.isVillagerOrGolem(other);
	}

	/** Whether the ally ignores this hit: friends cannot hurt allies unless friendly fire is on for the leader. */
	public static boolean protects(AllyEntity ally, Entity attacker) {
		if (attacker == null || !isFriendly(ally, attacker)) {
			return false;
		}
		return !(FellowshipConfig.INSTANCE.friendlyFire && attacker instanceof Player && ally.isOwnedBy(attacker));
	}

	/** Mobs the ally's abilities should hit: hostiles, and anything going for its band. */
	public static boolean isEnemyOf(AllyEntity ally, LivingEntity e) {
		if (!(e instanceof Mob mob) || !e.isAlive() || isFriendly(ally, e)) {
			return false;
		}
		if (e instanceof Enemy) {
			return true;
		}
		LivingEntity target = Compat.currentTarget(mob);
		if (target != null && (ally.isOwnedBy(target) || target instanceof AllyEntity a && a.ownerId() != null && a.ownerId().equals(ally.ownerId()))) {
			return true;
		}
		for (UUID id : Threat.attackers(mob)) {
			if (ally.ownerId() != null && (id.equals(ally.ownerId()) || id.equals(ally.getUUID()))) {
				return true;
			}
		}
		return false;
	}

	// ---- damage and death hooks ----

	/** Called before damage lands. Returns false to cancel it. */
	public static boolean allowDamage(LivingEntity victim, DamageSource source, float amount) {
		if (victim.level().isClientSide()) {
			return true;
		}
		Entity attacker = source.getEntity();
		if (victim instanceof AllyEntity ally && !Compat.bypassesInvulnerability(source) && protects(ally, attacker)) {
			return false;
		}
		if (attacker instanceof AllyEntity ally && victim instanceof Player p && isFriendly(ally, p)) {
			return false;
		}
		if (victim instanceof Mob mob && attacker instanceof LivingEntity living && !(victim instanceof AllyEntity)) {
			Threat.damaged(mob, living, amount);
			if (attacker instanceof AllyEntity ally && ally.isHired()) {
				ServerPlayer owner = ally.owner();
				if (owner != null) {
					Compat.credit(mob, owner);
				}
				ally.record().stats.damage += amount;
				ally.markCombat();
			}
		}
		return true;
	}

	public static void onDeath(LivingEntity victim, DamageSource source) {
		if (victim.level().isClientSide() || victim instanceof AllyEntity) {
			return;
		}
		boolean boss = Compat.isBoss(victim);
		int xp = Progression.killXp(victim.getMaxHealth());
		AllyEntity killer = source.getEntity() instanceof AllyEntity a && a.isHired() ? a : null;
		if (killer != null) {
			killer.onKill(victim, boss);
			killer.gainXp(xp);
		}
		if (victim.level() instanceof ServerLevel level) {
			for (UUID id : Threat.attackers(victim)) {
				if (level.getEntity(id) instanceof AllyEntity a && a != killer && a.isHired() && a.distanceToSqr(victim) < 32 * 32) {
					a.gainXp(Math.max(1, xp / 2));
				}
			}
			if (boss) {
				for (AllyEntity a : level.getEntitiesOfClass(AllyEntity.class, victim.getBoundingBox().inflate(48))) {
					if (a.isHired() && a != killer) {
						a.record().stats.bossFights++;
						a.chronicle(tn.nightbeam.better_with_gold.core.Chronicle.Type.BOSS_FIGHT, Compat.typeName(victim), "", 0);
						a.record().morale = Math.min(100, a.record().morale + 10);
					}
				}
			}
		}
		if (victim instanceof ServerPlayer player) {
			for (AllyEntity a : Warbands.liveAllies(player.getUUID())) {
				a.record().morale = Math.max(0, a.record().morale - 15);
			}
		}
		Threat.died(victim);
	}

	// ---- client messages ----

	public static void handle(ServerPlayer player, String text) {
		JsonObject msg = Net.read(text);
		switch (Net.type(msg)) {
			case Net.ORDER -> {
				Order order = Order.parse(Net.string(msg, "o", ""), null);
				if (order == null) {
					return;
				}
				Entity focus = null;
				if (order == Order.FOCUS) {
					int id = Net.integer(msg, "id", -1);
					Entity picked = id >= 0 ? Compat.level(player).getEntity(id) : null;
					focus = picked != null && picked.distanceToSqr(player) < 64 * 64 ? picked : lookedAt(player, 48);
				}
				Warbands.order(player, order, focus);
			}
			case Net.FORMATION -> {
				String f = Net.string(msg, "f", "next");
				Warbands.formation(player, f.equals("next") ? null : Formation.parse(f, null));
			}
			case Net.RECALL -> Warbands.recall(player);
			case Net.SUMMON -> Warbands.summon(player, Net.string(msg, "id", "all"));
			case Net.STOW -> Warbands.stowByName(player, Net.string(msg, "id", ""));
			case Net.DISMISS -> Warbands.dismiss(player, Net.string(msg, "id", ""));
			case Net.ROSTER_REQUEST -> {
				Warbands.watchRoster(player);
				Warbands.sendRoster(player);
			}
			default -> {
			}
		}
	}

	/** The living entity under the player's crosshair, up to {@code range} blocks. */
	public static LivingEntity lookedAt(ServerPlayer player, double range) {
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getLookAngle().scale(range));
		LivingEntity best = null;
		double bestDist = range * range;
		List<LivingEntity> candidates = player.level().getEntitiesOfClass(LivingEntity.class, new AABB(eye, end).inflate(1.5),
				e -> e != player && e.isAlive() && !(e instanceof AllyEntity a && a.isOwnedBy(player)));
		for (LivingEntity e : candidates) {
			Optional<Vec3> hit = e.getBoundingBox().inflate(0.3).clip(eye, end);
			if (hit.isPresent()) {
				double d = eye.distanceToSqr(hit.get());
				if (d < bestDist) {
					bestDist = d;
					best = e;
				}
			}
		}
		return best;
	}
}
