package tn.nightbeam.better_with_gold.ally;

import tn.nightbeam.better_with_gold.compat.Compat;
import tn.nightbeam.better_with_gold.core.AllyClass;
import tn.nightbeam.better_with_gold.core.Chronicle;
import tn.nightbeam.better_with_gold.core.Trait;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Class abilities: the Bulwark's Challenge, the Skirmisher's Whirl, the Ranger's shots and Volley, the
 * Mender's healing and the Arcanist's bolts and Arc Storm. All effects are server-side particles and sounds,
 * so they look the same on every client without client code.
 */
public final class AllyCombat {
	private AllyCombat() {
	}

	/** Runs once per tick for each hired, standing ally. */
	public static void tick(AllyEntity ally) {
		if (ally.abilityCooldown > 0) {
			ally.abilityCooldown--;
		}
		if (ally.attackCooldown > 0) {
			ally.attackCooldown--;
		}
		if (ally.abilityCooldown > 0 || !(ally.level() instanceof ServerLevel level)) {
			return;
		}
		AllyClass c = ally.allyClass();
		boolean used = switch (c.role()) {
			case TANK -> challenge(ally, level, c);
			case MELEE -> whirl(ally, level, c);
			case HEALER -> mend(ally, level, c);
			case RANGED, CASTER -> false;
		};
		if (used) {
			ally.abilityCooldown = c.abilityCooldown;
		} else if (c.role() == tn.nightbeam.better_with_gold.core.Role.HEALER) {
			ally.abilityCooldown = 10;
		}
	}

	/** Enemies near the ally that are fighting its band. */
	public static List<Mob> hostilesNear(AllyEntity ally, double radius) {
		List<Mob> list = new ArrayList<>();
		for (Mob mob : ally.level().getEntitiesOfClass(Mob.class, ally.getBoundingBox().inflate(radius))) {
			if (mob.isAlive() && Fellowship.isEnemyOf(ally, mob) && mob.distanceToSqr(ally) <= radius * radius) {
				list.add(mob);
			}
		}
		return list;
	}

	private static boolean challenge(AllyEntity ally, ServerLevel level, AllyClass c) {
		List<Mob> pulled = new ArrayList<>();
		for (Mob mob : hostilesNear(ally, c.abilityRadius)) {
			LivingEntity target = Compat.currentTarget(mob);
			if (target != null && target != ally) {
				pulled.add(mob);
			}
		}
		if (pulled.isEmpty()) {
			return false;
		}
		int ticks = (int) Math.round(c.abilityPower * 20);
		for (Mob mob : pulled) {
			Threat.taunt(mob, ally, ticks);
			level.sendParticles(ParticleTypes.ANGRY_VILLAGER, mob.getX(), mob.getY() + mob.getBbHeight() + 0.3, mob.getZ(), 1, 0, 0, 0, 0);
		}
		ring(level, ally.position(), c.abilityRadius * 0.6, Compat.dust(0x66CCFF, 1.4F), 28);
		ring(level, ally.position(), c.abilityRadius * 0.3, ParticleTypes.CRIT, 16);
		Compat.playSound(level, ally.getX(), ally.getY(), ally.getZ(), "minecraft:entity.ravager.roar", 0.5F, 1.5F);
		Compat.playSound(level, ally.getX(), ally.getY(), ally.getZ(), "minecraft:item.shield.block", 0.9F, 0.8F);
		ally.swing(net.minecraft.world.InteractionHand.OFF_HAND);
		return true;
	}

	private static boolean whirl(AllyEntity ally, ServerLevel level, AllyClass c) {
		List<Mob> near = hostilesNear(ally, c.abilityRadius);
		if (near.size() < 2 && (ally.getTarget() == null || near.isEmpty())) {
			return false;
		}
		float damage = (float) (Compat.get(ally, "damage") * c.abilityPower);
		for (Mob mob : near) {
			Compat.hurt(mob, Compat.melee(ally), damage);
			level.sendParticles(ParticleTypes.SWEEP_ATTACK, mob.getX(), mob.getY() + mob.getBbHeight() * 0.5, mob.getZ(), 1, 0, 0, 0, 0);
		}
		ring(level, ally.position().add(0, 0.8, 0), c.abilityRadius * 0.8, ParticleTypes.SWEEP_ATTACK, 8);
		Compat.playSound(level, ally.getX(), ally.getY(), ally.getZ(), "minecraft:entity.player.attack.sweep", 1.0F, 0.9F);
		ally.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
		ally.markCombat();
		return true;
	}

	private static boolean mend(AllyEntity ally, ServerLevel level, AllyClass c) {
		LivingEntity best = null;
		double bestRatio = 1.0;
		ServerPlayer owner = ally.owner();
		List<LivingEntity> candidates = new ArrayList<>(Warbands.liveAllies(ally));
		if (owner != null && owner.level() == ally.level()) {
			candidates.add(owner);
		}
		for (LivingEntity e : candidates) {
			if (!e.isAlive() || e instanceof AllyEntity a && a.isDowned() || e.distanceToSqr(ally) > c.abilityRadius * c.abilityRadius) {
				continue;
			}
			if (e.getMaxHealth() - e.getHealth() < 2) {
				continue;
			}
			double ratio = e.getHealth() / e.getMaxHealth();
			if (ratio < bestRatio) {
				bestRatio = ratio;
				best = e;
			}
		}
		if (best == null) {
			return false;
		}
		double multiplier = 1;
		for (Trait t : ally.record().traitList()) {
			multiplier *= t.healingMultiplier();
		}
		float amount = (float) ((c.abilityPower + 0.25 * (ally.record().level - 1)) * multiplier);
		float before = best.getHealth();
		best.heal(amount);
		float healed = best.getHealth() - before;
		ally.record().stats.healing += healed;
		Threat.healed(ally, best, healed);
		beam(level, ally.position().add(0, 1.2, 0), best.position().add(0, best.getBbHeight() * 0.6, 0), Compat.dust(0xF2C94C, 1.0F));
		level.sendParticles(ParticleTypes.HEART, best.getX(), best.getY() + best.getBbHeight() + 0.2, best.getZ(), 3, 0.3, 0.2, 0.3, 0);
		Compat.playSound(level, best.getX(), best.getY(), best.getZ(), "minecraft:block.amethyst_block.chime", 1.0F, 1.3F);
		ally.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
		if (best == owner && before <= 6 && Warbands.mayRecordSave(ally)) {
			ally.chronicle(Chronicle.Type.SAVED_LEADER, Compat.playerName(owner), "", Math.max(1, (int) Math.ceil(before / 2)));
		}
		return true;
	}

	/** One arrow at the target. Returns true when it was loosed. */
	public static boolean shoot(AllyEntity ally, LivingEntity target, int arrows) {
		if (!(ally.level() instanceof ServerLevel level)) {
			return false;
		}
		ItemStack bow = ally.getMainHandItem();
		float damage = ally.rangedDamage();
		boolean keen = ally.record().has(Trait.KEEN);
		for (int i = 0; i < arrows; i++) {
			AbstractArrow arrow = Compat.arrow(ally, bow, damage);
			double dx = target.getX() - ally.getX();
			double dy = target.getY(0.3333) - arrow.getY();
			double dz = target.getZ() - ally.getZ();
			double dist = Math.sqrt(dx * dx + dz * dz);
			if (arrows > 1) {
				double angle = Math.toRadians((i - (arrows - 1) / 2.0) * 9.0);
				double rx = dx * Math.cos(angle) - dz * Math.sin(angle);
				double rz = dx * Math.sin(angle) + dz * Math.cos(angle);
				dx = rx;
				dz = rz;
			}
			arrow.shoot(dx, dy + dist * 0.2, dz, 1.6F, keen ? 2.0F : 5.0F);
			level.addFreshEntity(arrow);
		}
		Compat.playSound(level, ally.getX(), ally.getY(), ally.getZ(), "minecraft:entity.arrow.shoot", 1.0F, 1.0F / (ally.getRandom().nextFloat() * 0.4F + 0.8F));
		ally.markCombat();
		return true;
	}

	/** The Arcanist's arc bolt: an instant spark line and magic damage. */
	public static void bolt(AllyEntity ally, LivingEntity target, float damage) {
		if (!(ally.level() instanceof ServerLevel level)) {
			return;
		}
		Vec3 from = ally.getEyePosition().add(ally.getLookAngle().scale(0.4));
		Vec3 to = target.position().add(0, target.getBbHeight() * 0.6, 0);
		beam(level, from, to, ParticleTypes.ELECTRIC_SPARK);
		beam(level, from, to, Compat.dust(0xA06BE0, 0.9F));
		level.sendParticles(ParticleTypes.WITCH, to.x, to.y, to.z, 8, 0.2, 0.2, 0.2, 0.05);
		Compat.hurt(target, Compat.magic(ally), damage);
		Compat.playSound(level, ally.getX(), ally.getY(), ally.getZ(), "minecraft:entity.evoker.cast_spell", 0.7F, 1.7F);
		ally.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
		ally.markCombat();
	}

	/** The Arcanist's Arc Storm, when two or more enemies stand close together (or the target is a boss). */
	public static boolean arcStorm(AllyEntity ally, LivingEntity target) {
		if (!(ally.level() instanceof ServerLevel level) || ally.abilityCooldown > 0) {
			return false;
		}
		AllyClass c = ally.allyClass();
		List<Mob> hit = new ArrayList<>();
		for (Mob mob : level.getEntitiesOfClass(Mob.class, target.getBoundingBox().inflate(c.abilityRadius))) {
			if (mob.isAlive() && Fellowship.isEnemyOf(ally, mob)) {
				hit.add(mob);
			}
		}
		if (hit.size() < 2 && !Compat.isBoss(target)) {
			return false;
		}
		Compat.visualLightning(level, target.getX(), target.getY(), target.getZ());
		float damage = (float) (ally.rangedDamage() * c.abilityPower);
		for (Mob mob : hit) {
			Compat.hurt(mob, Compat.magic(ally), damage);
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, mob.getX(), mob.getY() + mob.getBbHeight() * 0.5, mob.getZ(), 10, 0.3, 0.4, 0.3, 0.2);
		}
		ring(level, target.position().add(0, 0.1, 0), c.abilityRadius, Compat.dust(0xA06BE0, 1.5F), 36);
		ally.abilityCooldown = c.abilityCooldown;
		ally.markCombat();
		return true;
	}

	static void ring(ServerLevel level, Vec3 centre, double radius, ParticleOptions particle, int points) {
		for (int i = 0; i < points; i++) {
			double a = 2 * Math.PI * i / points;
			level.sendParticles(particle, centre.x + Math.sin(a) * radius, centre.y + 0.1, centre.z + Math.cos(a) * radius, 1, 0, 0, 0, 0);
		}
	}

	static void beam(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions particle) {
		Vec3 d = to.subtract(from);
		int steps = Math.max(2, (int) (d.length() * 3));
		for (int i = 0; i <= steps; i++) {
			Vec3 p = from.add(d.scale(i / (double) steps));
			level.sendParticles(particle, p.x, p.y, p.z, 1, 0, 0, 0, 0);
		}
	}
}
