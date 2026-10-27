package tn.nightbeam.better_with_gold.ally;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * An invisible marker that rides an engaged enemy and shows, as its name, who holds the enemy's attention.
 * Never saved; the threat manager recreates it while the fight lasts. Minecraft 1.21.1 edition.
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
	protected void readAdditionalSaveData(CompoundTag tag) {
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
	}

	@Override
	public void tick() {
		super.tick();
		if (!level().isClientSide && (!isPassenger() || !Threat.ownsLabel(this))) {
			discard();
		}
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		return false;
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
