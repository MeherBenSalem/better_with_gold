package tn.nightbeam.better_with_gold.ally.goal;

import tn.nightbeam.better_with_gold.ally.AllyEntity;

import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** A downed ally lies still: no moving, looking, jumping or targeting until revived. */
public class AllyDownedGoal extends Goal {
	private final AllyEntity ally;

	public AllyDownedGoal(AllyEntity ally) {
		this.ally = ally;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP, Flag.TARGET));
	}

	@Override
	public boolean canUse() {
		return ally.isDowned();
	}

	@Override
	public void start() {
		ally.getNavigation().stop();
		ally.setTarget(null);
	}

	@Override
	public void tick() {
		ally.getNavigation().stop();
	}
}
