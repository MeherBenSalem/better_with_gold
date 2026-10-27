package tn.nightbeam.better_with_gold.ally.goal;

import tn.nightbeam.better_with_gold.ally.AllyEntity;
import tn.nightbeam.better_with_gold.ally.Warbands;
import tn.nightbeam.better_with_gold.compat.Compat;
import tn.nightbeam.better_with_gold.core.FellowshipConfig;
import tn.nightbeam.better_with_gold.core.Role;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** Menders run to downed band-mates and pull them back up. */
public class AllyReviveGoal extends Goal {
	private final AllyEntity ally;
	private AllyEntity patient;

	public AllyReviveGoal(AllyEntity ally) {
		this.ally = ally;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (!ally.isHired() || ally.isDowned() || ally.role() != Role.HEALER || ally.tickCount % 5 != 0) {
			return false;
		}
		patient = null;
		double best = 24 * 24;
		for (AllyEntity other : Warbands.liveAllies(ally)) {
			if (other != ally && other.isDowned() && other.level() == ally.level()) {
				double d = other.distanceToSqr(ally);
				if (d < best) {
					best = d;
					patient = other;
				}
			}
		}
		return patient != null;
	}

	@Override
	public boolean canContinueToUse() {
		return patient != null && patient.isAlive() && patient.isDowned() && !ally.isDowned();
	}

	@Override
	public void start() {
		ally.setTarget(null);
	}

	@Override
	public void stop() {
		patient = null;
	}

	@Override
	public void tick() {
		ally.getLookControl().setLookAt(patient, 30, 30);
		if (ally.distanceToSqr(patient) > 2.2 * 2.2) {
			if (ally.getNavigation().isDone() || ally.tickCount % 10 == 0) {
				ally.getNavigation().moveTo(patient, 1.35);
			}
			return;
		}
		ally.getNavigation().stop();
		if (ally.tickCount % 6 == 0 && ally.level() instanceof ServerLevel level) {
			level.sendParticles(Compat.dust(0xF2C94C, 0.9F), patient.getX(), patient.getY() + 0.4, patient.getZ(), 3, 0.3, 0.2, 0.3, 0);
			level.sendParticles(ParticleTypes.HAPPY_VILLAGER, patient.getX(), patient.getY() + 0.3, patient.getZ(), 1, 0.3, 0.1, 0.3, 0);
			ally.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
		}
		patient.addRevive(1.0F / (FellowshipConfig.INSTANCE.menderReviveSeconds * 20.0F), ally.record().name, ally);
	}
}
