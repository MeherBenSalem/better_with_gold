package tn.nightbeam.better_with_gold.ally.goal;

import tn.nightbeam.better_with_gold.ally.AllyEntity;
import tn.nightbeam.better_with_gold.ally.Warbands;
import tn.nightbeam.better_with_gold.core.Order;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Out of combat, each ally walks to its formation slot (around the leader or the Warbanner) or its hold spot. */
public class AllyFormationGoal extends Goal {
	private final AllyEntity ally;
	private Vec3 dest;

	public AllyFormationGoal(AllyEntity ally) {
		this.ally = ally;
		setFlags(EnumSet.of(Flag.MOVE));
	}

	private Vec3 destination() {
		Order order = Warbands.orderFor(ally);
		if (order == Order.HOLD) {
			BlockPos hold = ally.holdPos();
			return hold == null ? null : Vec3.atBottomCenterOf(hold);
		}
		Vec3 slot = Warbands.slot(ally);
		if (slot != null) {
			return slot;
		}
		ServerPlayer owner = ally.owner();
		return owner != null && owner.level() == ally.level() ? owner.position() : null;
	}

	@Override
	public boolean canUse() {
		if (!ally.isHired() || ally.isDowned() || ally.getTarget() != null) {
			return false;
		}
		dest = destination();
		return dest != null && ally.position().distanceToSqr(dest) > 1.5 * 1.5;
	}

	@Override
	public boolean canContinueToUse() {
		return ally.isHired() && !ally.isDowned() && ally.getTarget() == null && dest != null && ally.position().distanceToSqr(dest) > 0.8 * 0.8;
	}

	@Override
	public void start() {
		move();
	}

	@Override
	public void stop() {
		ally.getNavigation().stop();
		ServerPlayer owner = ally.owner();
		if (owner != null && owner.level() == ally.level()) {
			ally.getLookControl().setLookAt(owner, 20, 20);
		}
	}

	// Ticked every tick: a goal that is not ticks every other tick (by entity id), and the re-path below checks
	// tickCount % 10, so for half the allies the two never lined up and they stopped following.
	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		if (ally.tickCount % 10 == 0) {
			Vec3 next = destination();
			if (next != null) {
				dest = next;
				move();
			}
		}
	}

	private void move() {
		double d = ally.position().distanceTo(dest);
		ally.getNavigation().moveTo(dest.x, dest.y, dest.z, d > 8 ? 1.35 : 1.0);
	}
}
