package tn.nightbeam.better_with_gold.ally.goal;

import tn.nightbeam.better_with_gold.ally.AllyEntity;

import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;

/** Sellswords waiting for hire stroll around the village. */
public class AllyWanderGoal extends WaterAvoidingRandomStrollGoal {
	private final AllyEntity ally;

	public AllyWanderGoal(AllyEntity ally) {
		super(ally, 0.6);
		this.ally = ally;
	}

	@Override
	public boolean canUse() {
		return !ally.isHired() && super.canUse();
	}

	@Override
	public boolean canContinueToUse() {
		return !ally.isHired() && super.canContinueToUse();
	}
}
