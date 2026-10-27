package tn.nightbeam.better_with_gold.ally.goal;

import tn.nightbeam.better_with_gold.ally.AllyCombat;
import tn.nightbeam.better_with_gold.ally.AllyEntity;
import tn.nightbeam.better_with_gold.ally.Warbands;
import tn.nightbeam.better_with_gold.core.AllyClass;
import tn.nightbeam.better_with_gold.core.Role;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Rangers and Arcanists: hold their distance, step back when enemies close in, shoot or cast on a timer. */
public class AllyRangedGoal extends Goal {
	private final AllyEntity ally;

	public AllyRangedGoal(AllyEntity ally) {
		this.ally = ally;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	private boolean ranged() {
		Role role = ally.role();
		return (role == Role.RANGED || role == Role.CASTER) && ally.isHired() && !ally.isDowned() && !Warbands.fallingBack(ally);
	}

	@Override
	public boolean canUse() {
		LivingEntity target = ally.getTarget();
		return ranged() && target != null && target.isAlive();
	}

	@Override
	public boolean canContinueToUse() {
		return canUse();
	}

	@Override
	public void stop() {
		ally.getNavigation().stop();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		LivingEntity target = ally.getTarget();
		if (target == null) {
			return;
		}
		AllyClass c = ally.allyClass();
		double range = Math.max(4, ally.preferredRange());
		double dist = ally.distanceTo(target);
		boolean seen = ally.getSensing().hasLineOfSight(target);
		ally.getLookControl().setLookAt(target, 30, 30);
		if (ally.tickCount % 5 == 0) {
			if (dist > range + 2 || !seen) {
				ally.getNavigation().moveTo(target, 1.0);
			} else if (dist < range * 0.45) {
				Vec3 away = ally.position().subtract(target.position()).normalize().scale(6);
				Vec3 to = ally.position().add(away);
				ally.getNavigation().moveTo(to.x, to.y, to.z, 1.25);
			} else {
				ally.getNavigation().stop();
			}
		}
		if (seen && dist <= range + 4 && ally.attackCooldown <= 0) {
			if (c.role() == Role.RANGED) {
				boolean volley = ally.abilityCooldown <= 0;
				AllyCombat.shoot(ally, target, volley ? Math.max(1, (int) Math.round(c.abilityPower)) : 1);
				if (volley) {
					ally.abilityCooldown = c.abilityCooldown;
				}
			} else if (!AllyCombat.arcStorm(ally, target)) {
				AllyCombat.bolt(ally, target, ally.rangedDamage());
			}
			ally.attackCooldown = c.attackInterval;
		}
	}
}
