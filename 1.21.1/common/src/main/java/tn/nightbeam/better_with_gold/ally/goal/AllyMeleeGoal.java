package tn.nightbeam.better_with_gold.ally.goal;

import tn.nightbeam.better_with_gold.ally.AllyEntity;
import tn.nightbeam.better_with_gold.ally.Warbands;
import tn.nightbeam.better_with_gold.core.Role;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

/** Close combat for Bulwarks and Skirmishers; Menders only swing at enemies right next to them. */
public class AllyMeleeGoal extends MeleeAttackGoal {
	private final AllyEntity ally;

	public AllyMeleeGoal(AllyEntity ally) {
		super(ally, 1.15, true);
		this.ally = ally;
	}

	private boolean allowed() {
		if (!ally.isHired() || ally.isDowned() || Warbands.fallingBack(ally)) {
			return false;
		}
		Role role = ally.role();
		if (role == Role.TANK || role == Role.MELEE) {
			return true;
		}
		LivingEntity target = ally.getTarget();
		return role == Role.HEALER && target != null && ally.distanceToSqr(target) < 3.5 * 3.5;
	}

	@Override
	public boolean canUse() {
		return allowed() && super.canUse();
	}

	@Override
	public boolean canContinueToUse() {
		return allowed() && super.canContinueToUse();
	}
}
