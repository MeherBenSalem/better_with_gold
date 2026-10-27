package tn.nightbeam.better_with_gold.ally.goal;

import tn.nightbeam.better_with_gold.ally.AllyEntity;
import tn.nightbeam.better_with_gold.ally.Warbands;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** Fall back: break off and run to the leader. */
public class AllyFallBackGoal extends Goal {
	private final AllyEntity ally;

	public AllyFallBackGoal(AllyEntity ally) {
		this.ally = ally;
		setFlags(EnumSet.of(Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		return ally.isHired() && !ally.isDowned() && Warbands.fallingBack(ally) && ally.owner() != null;
	}

	@Override
	public void tick() {
		ServerPlayer owner = ally.owner();
		if (owner == null) {
			return;
		}
		ally.setTarget(null);
		if (ally.distanceToSqr(owner) > 4 && ally.getNavigation().isDone() || ally.tickCount % 10 == 0) {
			ally.getNavigation().moveTo(owner, 1.4);
		}
	}
}
