package tn.nightbeam.better_with_gold.ally;

import tn.nightbeam.better_with_gold.compat.Compat;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Fellowship's visual effects: short animations of vanilla particles and layered vanilla sounds, played from the
 * server so every client sees them with no resource pack. An effect is a function of the tick it is on; {@link #play}
 * runs it once per server tick for its length.
 */
public final class Fx {
	private static final List<Job> JOBS = new ArrayList<>();
	private static final int MAX_JOBS = 400;

	private static final class Job {
		final ServerLevel level;
		final int length;
		final IntConsumer frame;
		int t;

		Job(ServerLevel level, int length, IntConsumer frame) {
			this.level = level;
			this.length = length;
			this.frame = frame;
		}
	}

	// Palette (RGB) shared by the effects.
	public static final int GOLD = 0xF2C94C;
	public static final int GOLD_PALE = 0xFFE89A;
	public static final int STEEL = 0x9ECBFF;
	public static final int BLOOD = 0xE5534B;
	public static final int ARCANE = 0xB07CFF;
	public static final int ARCANE_CORE = 0xE6D4FF;
	public static final int HEAL = 0x8EE07A;
	public static final int EMBER = 0x7A1E1E;

	private Fx() {
	}

	/** Plays an animation: {@code frame} gets 0, 1, ... length-1 on consecutive server ticks, starting now. */
	public static void play(ServerLevel level, int length, IntConsumer frame) {
		frame.accept(0);
		if (length > 1 && JOBS.size() < MAX_JOBS) {
			Job job = new Job(level, length, frame);
			job.t = 1;
			JOBS.add(job);
		}
	}

	/** Advances every running animation. Called once per server tick. */
	public static void tick(MinecraftServer server) {
		if (JOBS.isEmpty()) {
			return;
		}
		List<Job> now = new ArrayList<>(JOBS);
		for (Job job : now) {
			try {
				job.frame.accept(job.t);
			} catch (RuntimeException ignored) {
				job.t = job.length;
			}
			job.t++;
		}
		for (Iterator<Job> it = JOBS.iterator(); it.hasNext(); ) {
			Job job = it.next();
			if (job.t >= job.length) {
				it.remove();
			}
		}
	}

	public static void clear() {
		JOBS.clear();
	}

	// ---- primitives ---------------------------------------------------------------------------------------------

	static void at(ServerLevel level, ParticleOptions p, double x, double y, double z) {
		level.sendParticles(p, x, y, z, 1, 0, 0, 0, 0);
	}

	static void burst(ServerLevel level, ParticleOptions p, Vec3 c, int count, double spread, double speed) {
		level.sendParticles(p, c.x, c.y, c.z, count, spread, spread, spread, speed);
	}

	/** A horizontal ring of particles. */
	static void ring(ServerLevel level, Vec3 c, double radius, ParticleOptions p, int points, double phase) {
		for (int i = 0; i < points; i++) {
			double a = phase + 2 * Math.PI * i / points;
			at(level, p, c.x + Math.sin(a) * radius, c.y, c.z + Math.cos(a) * radius);
		}
	}

	/** A straight line of particles. */
	static void line(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions p, double perBlock) {
		Vec3 d = to.subtract(from);
		int steps = Math.max(2, (int) (d.length() * perBlock));
		for (int i = 0; i <= steps; i++) {
			Vec3 q = from.add(d.scale(i / (double) steps));
			at(level, p, q.x, q.y, q.z);
		}
	}

	/** A jagged electric line: straight ends, random kinks in between. */
	static void bolt(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions core, ParticleOptions glow, RandomSource r) {
		Vec3 d = to.subtract(from);
		int kinks = Math.max(3, (int) (d.length() * 1.5));
		Vec3 prev = from;
		for (int k = 1; k <= kinks; k++) {
			Vec3 next = from.add(d.scale(k / (double) kinks));
			if (k < kinks) {
				double j = 0.35;
				next = next.add((r.nextDouble() - 0.5) * j, (r.nextDouble() - 0.5) * j, (r.nextDouble() - 0.5) * j);
			}
			line(level, prev, next, core, 6);
			prev = next;
		}
		at(level, glow, to.x, to.y, to.z);
	}

	static Vec3 chest(Entity e) {
		return e.position().add(0, e.getBbHeight() * 0.6, 0);
	}

	static void sound(ServerLevel level, Vec3 at, String id, float volume, float pitch) {
		Compat.playSound(level, at.x, at.y, at.z, id, volume, pitch);
	}

	// ---- abilities -----------------------------------------------------------------------------------------------

	/** Bulwark's Challenge: a war cry shockwave, then a red chain to every enemy it pulls. */
	public static void challenge(ServerLevel level, AllyEntity ally, double radius, List<? extends LivingEntity> pulled) {
		Vec3 feet = ally.position().add(0, 0.15, 0);
		Vec3 chest = chest(ally);
		sound(level, feet, "minecraft:entity.ravager.roar", 0.55F, 1.45F);
		sound(level, feet, "minecraft:block.bell.use", 0.7F, 0.55F);
		sound(level, feet, "minecraft:item.shield.block", 0.9F, 0.75F);
		burst(level, ParticleTypes.POOF, feet, 14, 0.5, 0.06);
		play(level, 8, t -> {
			double r = 0.6 + (radius * 0.85 - 0.6) * (t + 1) / 8.0;
			ring(level, feet, r, Compat.dust(STEEL, 1.5F), 18 + t * 3, t * 0.2);
			ring(level, feet.add(0, 0.9, 0), r * 0.8, ParticleTypes.ENCHANTED_HIT, 10 + t, -t * 0.25);
			if (t == 2) {
				for (LivingEntity mob : pulled) {
					line(level, chest, chest(mob), Compat.dust(BLOOD, 0.9F), 5);
					burst(level, ParticleTypes.ANGRY_VILLAGER, mob.position().add(0, mob.getBbHeight() + 0.35, 0), 1, 0, 0);
				}
			}
			if (t >= 3) {
				for (LivingEntity mob : pulled) {
					if (mob.isAlive()) {
						double a = t * 0.9;
						Vec3 c = mob.position();
						at(level, Compat.dust(BLOOD, 1.0F), c.x + Math.sin(a) * 0.6, c.y + 0.2 + t * 0.12, c.z + Math.cos(a) * 0.6);
					}
				}
			}
		});
	}

	/** Skirmisher's Whirl: a spinning blade arc around the ally. */
	public static void whirl(ServerLevel level, AllyEntity ally, double radius, List<? extends LivingEntity> hit) {
		Vec3 c = ally.position().add(0, 0.9, 0);
		sound(level, c, "minecraft:entity.player.attack.sweep", 1.0F, 0.85F);
		sound(level, c, "minecraft:item.trident.riptide_1", 0.45F, 1.7F);
		play(level, 6, t -> {
			double r = radius * 0.85;
			for (int k = 0; k < 3; k++) {
				double a = (t * 6 + k) * (Math.PI / 9) + ally.getYRot() * Math.PI / 180;
				at(level, ParticleTypes.SWEEP_ATTACK, c.x + Math.sin(a) * r, c.y + 0.1 * Math.sin(t), c.z + Math.cos(a) * r);
				at(level, ParticleTypes.CRIT, c.x + Math.sin(a - 0.3) * r * 0.9, c.y, c.z + Math.cos(a - 0.3) * r * 0.9);
			}
			if (t == 1) {
				for (LivingEntity mob : hit) {
					burst(level, ParticleTypes.ENCHANTED_HIT, chest(mob), 8, 0.25, 0.25);
					burst(level, ParticleTypes.DAMAGE_INDICATOR, chest(mob), 2, 0.2, 0.1);
				}
			}
		});
	}

	/** Ranger's Volley: a bowstring flash and a glowing trail behind every arrow for a second. */
	public static void volley(ServerLevel level, AllyEntity ally, List<? extends Entity> arrows) {
		Vec3 bow = ally.getEyePosition().add(ally.getLookAngle().scale(0.8));
		sound(level, bow, "minecraft:item.crossbow.shoot", 0.8F, 1.25F);
		sound(level, bow, "minecraft:entity.arrow.shoot", 1.0F, 0.9F);
		burst(level, ParticleTypes.CRIT, bow, 10, 0.15, 0.3);
		play(level, 24, t -> {
			for (Entity arrow : arrows) {
				if (arrow.isAlive() && !arrow.onGround()) {
					Vec3 p = arrow.position();
					at(level, Compat.dust(HEAL, 0.8F), p.x, p.y, p.z);
					if (t % 2 == 0) {
						at(level, ParticleTypes.END_ROD, p.x, p.y, p.z);
					}
				}
			}
		});
	}

	/** Mender's Mend: a golden mote flies to the patient and a spiral of light rises around them. */
	public static void mend(ServerLevel level, AllyEntity mender, LivingEntity patient) {
		Vec3 from = chest(mender).add(mender.getLookAngle().scale(0.3));
		sound(level, from, "minecraft:block.amethyst_block.chime", 1.0F, 1.35F);
		play(level, 18, t -> {
			if (t < 6) {
				Vec3 to = chest(patient);
				double u = (t + 1) / 6.0;
				Vec3 p = from.add(to.subtract(from).scale(u)).add(0, Math.sin(u * Math.PI) * 0.7, 0);
				at(level, ParticleTypes.END_ROD, p.x, p.y, p.z);
				burst(level, Compat.dust(GOLD_PALE, 1.2F), p, 3, 0.06, 0);
				if (t == 5) {
					sound(level, to, "minecraft:entity.allay.item_given", 0.8F, 1.4F);
					burst(level, ParticleTypes.HEART, to.add(0, 0.6, 0), 2, 0.3, 0);
				}
				return;
			}
			if (!patient.isAlive()) {
				return;
			}
			int k = t - 6;
			Vec3 c = patient.position();
			for (int s = 0; s < 2; s++) {
				double a = k * 0.7 + s * Math.PI;
				at(level, Compat.dust(s == 0 ? GOLD : HEAL, 0.9F), c.x + Math.sin(a) * 0.55, c.y + 0.1 + k * 0.17, c.z + Math.cos(a) * 0.55);
			}
			if (k % 3 == 0) {
				at(level, ParticleTypes.HAPPY_VILLAGER, c.x, c.y + 0.3 + k * 0.15, c.z);
			}
		});
	}

	/** Arcanist's bolt: a jagged arcane arc and a crackle on impact. */
	public static void arcaneBolt(ServerLevel level, AllyEntity ally, Vec3 from, LivingEntity target) {
		Vec3 to = chest(target);
		RandomSource r = ally.getRandom();
		bolt(level, from, to, Compat.dust(ARCANE, 0.8F), ParticleTypes.END_ROD, r);
		bolt(level, from, to, ParticleTypes.ELECTRIC_SPARK, ParticleTypes.ELECTRIC_SPARK, r);
		burst(level, ParticleTypes.WITCH, to, 8, 0.2, 0.05);
		burst(level, Compat.dust(ARCANE_CORE, 1.4F), to, 6, 0.15, 0);
		sound(level, from, "minecraft:entity.evoker.cast_spell", 0.7F, 1.7F);
		sound(level, to, "minecraft:block.amethyst_block.hit", 0.8F, 0.6F);
	}

	/** Arcanist's Arc Storm: lightning on the target, chain arcs to every enemy around it, a rune circle. */
	public static void arcStorm(ServerLevel level, AllyEntity ally, LivingEntity target, double radius, List<? extends LivingEntity> hit) {
		Vec3 ground = target.position().add(0, 0.1, 0);
		RandomSource r = ally.getRandom();
		Compat.visualLightning(level, ground.x, ground.y, ground.z);
		sound(level, ground, "minecraft:item.trident.thunder", 0.6F, 1.25F);
		sound(level, ground, "minecraft:entity.illusioner.cast_spell", 0.9F, 0.8F);
		play(level, 10, t -> {
			double rr = radius * Math.min(1, (t + 2) / 6.0);
			ring(level, ground, rr, Compat.dust(ARCANE, 1.6F), 24, t * 0.15);
			if (t % 2 == 0) {
				ring(level, ground, rr * 0.55, ParticleTypes.ENCHANT, 12, -t * 0.2);
			}
			if (t == 1 || t == 4) {
				for (LivingEntity mob : hit) {
					if (mob != target && mob.isAlive()) {
						bolt(level, chest(target), chest(mob), Compat.dust(ARCANE_CORE, 0.7F), ParticleTypes.ELECTRIC_SPARK, r);
					}
					burst(level, ParticleTypes.ELECTRIC_SPARK, chest(mob), 8, 0.3, 0.2);
				}
			}
		});
	}

	// ---- moments ------------------------------------------------------------------------------------------------

	/** An ally collapses: a red burst and slow souls. */
	public static void downed(ServerLevel level, AllyEntity ally) {
		Vec3 c = ally.position().add(0, 0.4, 0);
		burst(level, ParticleTypes.DAMAGE_INDICATOR, c, 10, 0.4, 0.1);
		burst(level, Compat.dust(BLOOD, 1.4F), c, 16, 0.5, 0);
		sound(level, c, "minecraft:entity.player.big_fall", 1.0F, 0.7F);
		sound(level, c, "minecraft:entity.player.hurt", 1.0F, 0.6F);
		sound(level, c, "minecraft:block.bell.resonate", 0.25F, 0.5F);
		play(level, 30, t -> {
			if (t % 5 == 0 && ally.isAlive()) {
				at(level, ParticleTypes.SOUL, ally.getX() + (ally.getRandom().nextDouble() - 0.5) * 0.6, ally.getY() + 0.3, ally.getZ() + (ally.getRandom().nextDouble() - 0.5) * 0.6);
			}
		});
	}

	/** The revive ring under a downed ally: lit points turn gold as the revive fills, with a spark at its edge. */
	public static void reviveRing(ServerLevel level, AllyEntity ally, float progress, long tick) {
		int points = 24;
		int lit = Math.round(points * Math.max(0, Math.min(1, progress)));
		double r = 1.1;
		for (int i = 0; i < points; i++) {
			double a = 2 * Math.PI * i / points;
			boolean on = i < lit;
			at(level, Compat.dust(on ? GOLD : EMBER, on ? 1.0F : 0.6F), ally.getX() + Math.sin(a) * r, ally.getY() + 0.12, ally.getZ() + Math.cos(a) * r);
		}
		if (lit > 0 && lit < points) {
			double a = 2 * Math.PI * lit / points;
			at(level, ParticleTypes.END_ROD, ally.getX() + Math.sin(a) * r, ally.getY() + 0.2, ally.getZ() + Math.cos(a) * r);
		}
		if (lit == 0 && tick % 20 == 0) {
			ring(level, ally.position().add(0, 0.12, 0), 0.7, Compat.dust(BLOOD, 1.1F), 12, 0);
		}
	}

	/** Back on their feet: a totem burst, a pillar of light and a gold ring. */
	public static void revived(ServerLevel level, AllyEntity ally) {
		Vec3 c = ally.position();
		burst(level, ParticleTypes.TOTEM_OF_UNDYING, c.add(0, 1, 0), 30, 0.4, 0.35);
		sound(level, c, "minecraft:item.totem.use", 0.35F, 1.4F);
		sound(level, c, "minecraft:block.beacon.activate", 0.5F, 1.6F);
		play(level, 12, t -> {
			for (int k = 0; k < 3; k++) {
				at(level, ParticleTypes.END_ROD, c.x + (ally.getRandom().nextDouble() - 0.5) * 0.3, c.y + t * 0.3 + k * 0.1, c.z + (ally.getRandom().nextDouble() - 0.5) * 0.3);
			}
			ring(level, c.add(0, 0.1, 0), 0.4 + t * 0.12, Compat.dust(GOLD_PALE, 1.2F), 16, t * 0.3);
		});
	}

	/** Level up: a rising double spiral of gold and a firework sparkle. */
	public static void levelUp(ServerLevel level, AllyEntity ally) {
		Vec3 c = ally.position();
		sound(level, c, "minecraft:entity.player.levelup", 0.6F, 1.2F);
		sound(level, c, "minecraft:entity.firework_rocket.twinkle", 0.6F, 1.1F);
		play(level, 16, t -> {
			for (int s = 0; s < 2; s++) {
				double a = t * 0.8 + s * Math.PI;
				at(level, Compat.dust(GOLD, 1.1F), ally.getX() + Math.sin(a) * 0.7, ally.getY() + t * 0.13, ally.getZ() + Math.cos(a) * 0.7);
			}
			if (t == 15) {
				burst(level, ParticleTypes.FIREWORK, ally.position().add(0, ally.getBbHeight() + 0.3, 0), 20, 0.1, 0.12);
				burst(level, ParticleTypes.HAPPY_VILLAGER, ally.position().add(0, 1.2, 0), 8, 0.4, 0.1);
			}
		});
	}

	/** Swearing the oath: gold sparks around the new ally and a bell. */
	public static void oath(ServerLevel level, AllyEntity ally) {
		Vec3 c = ally.position();
		sound(level, c, "minecraft:item.armor.equip_gold", 1.0F, 1.0F);
		sound(level, c, "minecraft:block.bell.use", 0.4F, 1.4F);
		sound(level, c, "minecraft:entity.player.levelup", 0.35F, 1.7F);
		burst(level, ParticleTypes.WAX_ON, c.add(0, 1.2, 0), 18, 0.45, 0.6);
		play(level, 10, t -> ring(level, c.add(0, 0.1 + t * 0.2, 0), 0.75, Compat.dust(GOLD, 1.0F), 12, t * 0.5));
	}

	/** The Warbanner planted: a gold shockwave from its foot and light running up the pole. */
	public static void bannerPlanted(ServerLevel level, Vec3 foot) {
		sound(level, foot, "minecraft:item.goat_horn.sound.0", 1.0F, 1.0F);
		sound(level, foot, "minecraft:item.trident.thunder", 0.25F, 1.6F);
		sound(level, foot, "minecraft:block.bell.use", 0.6F, 0.8F);
		burst(level, ParticleTypes.TOTEM_OF_UNDYING, foot.add(0, 1.5, 0), 40, 0.6, 0.4);
		play(level, 14, t -> {
			ring(level, foot.add(0, 0.1, 0), 0.6 + t * 0.55, Compat.dust(GOLD, 1.6F), 20 + t * 2, t * 0.1);
			at(level, ParticleTypes.END_ROD, foot.x, foot.y + t * 0.18, foot.z);
		});
	}
}
