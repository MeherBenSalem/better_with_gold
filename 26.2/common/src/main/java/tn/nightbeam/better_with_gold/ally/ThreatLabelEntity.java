package tn.nightbeam.better_with_gold.ally;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * An invisible marker that rides an engaged enemy and shows, as its name, who holds the enemy's attention.
 * Never saved; the threat manager recreates it while the fight lasts. Minecraft 26.2 edition.
 */
public class ThreatLabelEntity extends Entity {
	public ThreatLabelEntity(EntityType<?> type, Level level) {
		super(type, level);
		noPhysics = true;
		setInvisible(true);
		setNoGravity(true);
		setSilent(true);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}

	@Override
	public void tick() {
		super.tick();
		if (!level().isClientSide() && (!isPassenger() || !Threat.ownsLabel(this))) {
			discard();
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	/**
	 * The label is a speck (0.05 blocks), and vanilla draws an entity within 64 times its size, which would hide the
	 * label beyond about 3 blocks. Draw it as far as a name tag reads.
	 */
	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		return distance < 48 * 48;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean isAttackable() {
		return false;
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}
}
