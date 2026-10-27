package tn.nightbeam.better_with_gold.ally.goal;

import tn.nightbeam.better_with_gold.ally.AllyEntity;
import tn.nightbeam.better_with_gold.ally.Fellowship;
import tn.nightbeam.better_with_gold.ally.Warbands;
import tn.nightbeam.better_with_gold.compat.Compat;
import tn.nightbeam.better_with_gold.core.FellowshipConfig;
import tn.nightbeam.better_with_gold.core.Order;
import tn.nightbeam.better_with_gold.core.Role;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Picks what the ally fights, twice a second: the focus target first, then whatever hurts the leader or the band,
 * then whatever the leader attacks, then nearby hostiles that are going for the band (any hostile on Charge).
 * Allies on Hold or Guard banner do not chase past their leash.
 */
public class AllyTargetGoal extends Goal {
	private final AllyEntity ally;

	public AllyTargetGoal(AllyEntity ally) {
		this.ally = ally;
		setFlags(EnumSet.of(Flag.TARGET));
	}

	@Override
	public boolean canUse() {
		return ally.isHired() && !ally.isDowned();
	}

	@Override
	public boolean canContinueToUse() {
		return canUse();
	}

	@Override
	public void stop() {
		ally.setTarget(null);
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		if (Warbands.fallingBack(ally)) {
			ally.setTarget(null);
			return;
		}
		LivingEntity current = ally.getTarget();
		if (current != null && !valid(current)) {
			ally.setTarget(null);
			current = null;
		}
		if ((ally.tickCount + ally.getId()) % 10 != 0) {
			return;
		}
		LivingEntity focus = Warbands.focusFor(ally);
		if (focus != null && valid(focus)) {
			ally.setTarget(focus);
			return;
		}
		if (current != null) {
			LivingEntity urgent = urgent();
			if (urgent != null && urgent != current && ally.role() == Role.TANK) {
				ally.setTarget(urgent);
			}
			return;
		}
		LivingEntity pick = urgent();
		if (pick == null) {
			pick = nearby();
		}
		if (pick != null) {
			ally.setTarget(pick);
		}
	}

	/** Whatever is hurting the leader or the band right now. */
	private LivingEntity urgent() {
		ServerPlayer owner = ally.owner();
		if (owner != null && owner.level() == ally.level()) {
			LivingEntity e = owner.getLastHurtByMob();
			if (e != null && Compat.recentlyHurtBy(owner) && valid(e)) {
				return e;
			}
		}
		LivingEntity mine = ally.getLastHurtByMob();
		if (mine != null && Compat.recentlyHurtBy(ally) && valid(mine)) {
			return mine;
		}
		for (AllyEntity mate : Warbands.liveAllies(ally)) {
			if (mate == ally || mate.level() != ally.level()) {
				continue;
			}
			LivingEntity e = mate.getLastHurtByMob();
			if (e != null && Compat.recentlyHurtBy(mate) && valid(e)) {
				return e;
			}
		}
		if (owner != null && owner.level() == ally.level()) {
			LivingEntity e = owner.getLastHurtMob();
			if (e != null && Compat.recentlyHurt(owner) && valid(e)) {
				return e;
			}
		}
		return null;
	}

	/** Hostiles near the ally going for the band; on Charge, any hostile near the leader. */
	private LivingEntity nearby() {
		Order order = Warbands.orderFor(ally);
		boolean charge = order == Order.CHARGE || order == Order.FOCUS;
		double radius = charge ? 20 : ally.role() == Role.TANK ? 12 : 9;
		ServerPlayer owner = ally.owner();
		Vec3 centre = charge && owner != null && owner.level() == ally.level() ? owner.position() : ally.position();
		LivingEntity best = null;
		double bestDist = Double.MAX_VALUE;
		for (Mob mob : ally.level().getEntitiesOfClass(Mob.class, new net.minecraft.world.phys.AABB(centre, centre).inflate(radius))) {
			if (!(mob instanceof Enemy) || !valid(mob)) {
				continue;
			}
			LivingEntity target = Compat.currentTarget(mob);
			boolean threatening = target != null && (ally.isOwnedBy(target) || target instanceof AllyEntity a && a.ownerId() != null && a.ownerId().equals(ally.ownerId()));
			if (!charge && !threatening) {
				continue;
			}
			double d = mob.distanceToSqr(ally);
			if (d < bestDist) {
				bestDist = d;
				best = mob;
			}
		}
		return best;
	}

	private boolean valid(LivingEntity e) {
		if (!e.isAlive() || e.level() != ally.level() || e == ally || Fellowship.isFriendly(ally, e)) {
			return false;
		}
		if (e instanceof Player p && (!FellowshipConfig.INSTANCE.allowPvp || p.isCreative() || p.isSpectator())) {
			return false;
		}
		if (!(e instanceof Mob) && !(e instanceof Player)) {
			return false;
		}
		Order order = Warbands.orderFor(ally);
		Vec3 anchor = null;
		double leash = 26;
		if (order == Order.HOLD && ally.holdPos() != null) {
			BlockPos hold = ally.holdPos();
			anchor = Vec3.atBottomCenterOf(hold);
			leash = 14;
		} else if (order == Order.GUARD_BANNER && Warbands.slot(ally) != null) {
			anchor = Warbands.slot(ally);
			leash = 14;
		} else {
			ServerPlayer owner = ally.owner();
			if (owner != null && owner.level() == ally.level()) {
				anchor = owner.position();
			}
		}
		return anchor == null || e.position().distanceToSqr(anchor) <= leash * leash;
	}
}
