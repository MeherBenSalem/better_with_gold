package tn.nightbeam.better_with_gold.ally;

import tn.nightbeam.better_with_gold.ally.goal.AllyDownedGoal;
import tn.nightbeam.better_with_gold.ally.goal.AllyFallBackGoal;
import tn.nightbeam.better_with_gold.ally.goal.AllyFormationGoal;
import tn.nightbeam.better_with_gold.ally.goal.AllyMeleeGoal;
import tn.nightbeam.better_with_gold.ally.goal.AllyRangedGoal;
import tn.nightbeam.better_with_gold.ally.goal.AllyReviveGoal;
import tn.nightbeam.better_with_gold.ally.goal.AllyTargetGoal;
import tn.nightbeam.better_with_gold.ally.goal.AllyWanderGoal;
import tn.nightbeam.better_with_gold.compat.Compat;
import tn.nightbeam.better_with_gold.core.AllyClass;
import tn.nightbeam.better_with_gold.core.AllyRecord;
import tn.nightbeam.better_with_gold.core.Chronicle;
import tn.nightbeam.better_with_gold.core.ClassCatalog;
import tn.nightbeam.better_with_gold.core.FellowshipConfig;
import tn.nightbeam.better_with_gold.core.Progression;
import tn.nightbeam.better_with_gold.core.Role;
import tn.nightbeam.better_with_gold.core.Trait;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * A Fellowship ally: a hired sellsword with a class, a level, traits, morale and a chronicle. It is downed
 * instead of killed, revives, follows orders and formations, and remembers what it did. Minecraft 1.21.1 edition
 * of the entity overrides; the behaviour lives in Warbands, AllyCombat and the goals.
 */
public class AllyEntity extends PathfinderMob implements OwnableEntity {
	private static final EntityDataAccessor<String> DATA_CLASS = SynchedEntityData.defineId(AllyEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<String> DATA_OWNER = SynchedEntityData.defineId(AllyEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(AllyEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_DOWNED = SynchedEntityData.defineId(AllyEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_REVIVE = SynchedEntityData.defineId(AllyEntity.class, EntityDataSerializers.INT);

	private AllyRecord record = new AllyRecord();
	private BlockPos holdPos;
	public int abilityCooldown = 40;
	public int attackCooldown;
	private float reviveProgress;
	private int reviveIdle;
	private String lastReviver = "";
	private String downedBy = "";
	private long lastCombat = -10000;
	private String plate = "";
	private boolean statsDirty = true;
	private long lastLastStand = -100000;

	public AllyEntity(EntityType<? extends AllyEntity> type, Level level) {
		super(type, level);
		setPersistenceRequired();
		setCanPickUpLoot(false);
		xpReward = 0;
		if (getNavigation() instanceof GroundPathNavigation ground) {
			ground.setCanOpenDoors(true);
		}
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 24)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.ATTACK_DAMAGE, 3)
				.add(Attributes.FOLLOW_RANGE, 32)
				.add(Attributes.ARMOR, 2)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_CLASS, "bulwark");
		builder.define(DATA_OWNER, "");
		builder.define(DATA_VARIANT, 0);
		builder.define(DATA_DOWNED, 0);
		builder.define(DATA_REVIVE, 0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(0, new AllyDownedGoal(this));
		goalSelector.addGoal(1, new AllyFallBackGoal(this));
		goalSelector.addGoal(2, new AllyReviveGoal(this));
		goalSelector.addGoal(3, new AllyRangedGoal(this));
		goalSelector.addGoal(3, new AllyMeleeGoal(this));
		goalSelector.addGoal(5, new AllyFormationGoal(this));
		goalSelector.addGoal(6, new OpenDoorGoal(this, true));
		goalSelector.addGoal(7, new AllyWanderGoal(this));
		goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
		goalSelector.addGoal(9, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new AllyTargetGoal(this));
	}

	// ---- state ----

	public AllyRecord record() {
		return record;
	}

	public void setRecord(AllyRecord record) {
		this.record = record.repair();
		entityData.set(DATA_CLASS, this.record.classId);
		entityData.set(DATA_OWNER, this.record.owner);
		entityData.set(DATA_VARIANT, this.record.variant);
		statsDirty = true;
		plate = "";
	}

	public AllyClass allyClass() {
		return ClassCatalog.get(level().isClientSide ? entityData.get(DATA_CLASS) : record.classId);
	}

	public Role role() {
		return allyClass().role();
	}

	public String classId() {
		return entityData.get(DATA_CLASS);
	}

	public int variant() {
		return entityData.get(DATA_VARIANT);
	}

	public boolean isHired() {
		return !entityData.get(DATA_OWNER).isEmpty();
	}

	public UUID ownerId() {
		String owner = entityData.get(DATA_OWNER);
		if (owner.isEmpty()) {
			return null;
		}
		try {
			return UUID.fromString(owner);
		} catch (IllegalArgumentException ex) {
			return null;
		}
	}

	/** Vanilla ownership, so other mods that respect tamed pets treat allies as their leader's too. */
	@Override
	public UUID getOwnerUUID() {
		return ownerId();
	}

	public boolean isOwnedBy(Entity entity) {
		UUID owner = ownerId();
		return owner != null && entity != null && owner.equals(entity.getUUID());
	}

	/** The leader if online and in this world, else null. Server side. */
	public ServerPlayer owner() {
		UUID id = ownerId();
		if (id == null || !(level() instanceof ServerLevel server)) {
			return null;
		}
		return server.getServer().getPlayerList().getPlayer(id);
	}

	public boolean isDowned() {
		return entityData.get(DATA_DOWNED) > 0;
	}

	public int downedTicks() {
		return entityData.get(DATA_DOWNED);
	}

	public float reviveFraction() {
		return entityData.get(DATA_REVIVE) / 100f;
	}

	public BlockPos holdPos() {
		return holdPos;
	}

	public void setHoldPos(BlockPos pos) {
		holdPos = pos;
	}

	public long lastCombat() {
		return lastCombat;
	}

	public void markCombat() {
		lastCombat = Compat.gameTime(level());
	}

	public boolean inCombat() {
		return Compat.gameTime(level()) - lastCombat < 100;
	}

	public void markStatsDirty() {
		statsDirty = true;
	}

	public void chronicle(Chronicle.Type type, String a, String b, int n) {
		Chronicle.add(record.chronicle, new Chronicle.Entry(Compat.day(level()), type, a, b, n));
	}

	// ---- setup ----

	/** Gives the class weapon and offhand. */
	public void equipKit() {
		AllyClass c = allyClass();
		ItemStack main = Compat.stack(c.mainHand);
		ItemStack off = Compat.stack(c.offHand);
		setItemSlot(EquipmentSlot.MAINHAND, main);
		setItemSlot(EquipmentSlot.OFFHAND, off);
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			setDropChance(slot, 0.0F);
		}
	}

	/** Sets health, damage, armour and speed from class, level, traits, morale and the leader's regalia. */
	public void applyStats(boolean heal) {
		AllyClass c = allyClass();
		var traits = record.traitList();
		double regalia = Warbands.regaliaPieces(this);
		double health = c.health(record.level) * Progression.healthMultiplier(traits);
		double damage = c.damage(record.level) * Progression.damageMultiplier(traits, c.role())
				* Progression.moraleDamageMultiplier(record.morale) * (1 + regalia * FellowshipConfig.INSTANCE.regaliaDamagePerPiece);
		double knockback = c.knockbackResistance;
		for (Trait t : traits) {
			knockback += t.knockbackBonus();
		}
		Compat.setBase(this, "max_health", health);
		Compat.setBase(this, "damage", damage);
		Compat.setBase(this, "armor", c.armor(record.level));
		Compat.setBase(this, "speed", c.speed * Progression.speedMultiplier(traits));
		Compat.setBase(this, "knockback", Math.min(1, knockback));
		if (heal) {
			setHealth(getMaxHealth());
		} else if (getHealth() > getMaxHealth()) {
			setHealth(getMaxHealth());
		}
		statsDirty = false;
	}

	/** Damage of one ranged shot or bolt. */
	public float rangedDamage() {
		return (float) Compat.get(this, "damage");
	}

	public double preferredRange() {
		double range = allyClass().preferredRange;
		for (Trait t : record.traitList()) {
			range += t.rangeBonus();
		}
		return range;
	}

	// ---- downed and revived ----

	private void enterDowned(DamageSource source) {
		Entity killer = source.getEntity();
		downedBy = killer != null ? Compat.typeName(killer) : source.getMsgId();
		setHealth(1.0F);
		int ticks = (int) Math.round(FellowshipConfig.INSTANCE.bleedOutSeconds * 20 * bleedOutMultiplier());
		entityData.set(DATA_DOWNED, Math.max(20, ticks));
		entityData.set(DATA_REVIVE, 0);
		reviveProgress = 0;
		setPose(Pose.SLEEPING);
		setTarget(null);
		getNavigation().stop();
		clearFire();
		record.stats.downs++;
		record.morale = Math.max(0, record.morale - 5);
		chronicle(Chronicle.Type.DOWNED, downedBy, "", 0);
		Threat.forget(this);
		Warbands.allyDowned(this);
		if (level() instanceof ServerLevel server) {
			server.sendParticles(ParticleTypes.DAMAGE_INDICATOR, getX(), getY() + 0.5, getZ(), 12, 0.4, 0.2, 0.4, 0.1);
			Compat.playSound(server, getX(), getY(), getZ(), "minecraft:entity.player.big_fall", 1.0F, 0.7F);
			Compat.playSound(server, getX(), getY(), getZ(), "minecraft:entity.player.hurt", 1.0F, 0.6F);
		}
		plate = "";
	}

	private double bleedOutMultiplier() {
		double m = 1;
		for (Trait t : record.traitList()) {
			m *= t.bleedOutMultiplier();
		}
		return m;
	}

	/** Adds revive progress; a full bar stands the ally up. {@code perTick} is a fraction of the whole revive. */
	public void addRevive(float perTick, String byName, AllyEntity byAlly) {
		if (!isDowned()) {
			return;
		}
		reviveProgress += perTick;
		reviveIdle = 0;
		lastReviver = byName;
		entityData.set(DATA_REVIVE, Math.min(100, (int) (reviveProgress * 100)));
		if (reviveProgress >= 1.0F) {
			revive(byName, byAlly);
		}
	}

	public void revive(String byName, AllyEntity byAlly) {
		entityData.set(DATA_DOWNED, 0);
		entityData.set(DATA_REVIVE, 0);
		reviveProgress = 0;
		setPose(Pose.STANDING);
		setHealth(Math.max(2.0F, getMaxHealth() * 0.4F));
		invulnerableTime = 60;
		record.stats.revivesReceived++;
		record.morale = Math.min(100, record.morale + 5);
		Chronicle.markRevived(record.chronicle, byName);
		if (byAlly != null && byAlly != this) {
			byAlly.record.stats.revivesGiven++;
			if (byAlly.record.stats.revivesGiven <= 3 || Chronicle.killMilestone(byAlly.record.stats.revivesGiven)) {
				byAlly.chronicle(Chronicle.Type.REVIVED_OTHER, record.name, "", 0);
			}
		}
		if (level() instanceof ServerLevel server) {
			server.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, getX(), getY() + 1.0, getZ(), 30, 0.4, 0.6, 0.4, 0.3);
			Compat.playSound(server, getX(), getY(), getZ(), "minecraft:item.totem.use", 0.35F, 1.4F);
		}
		Warbands.allyRevived(this, byName);
		plate = "";
	}

	private void bleedOut() {
		entityData.set(DATA_DOWNED, 0);
		if (FellowshipConfig.INSTANCE.permadeath) {
			Warbands.allyFell(this, downedBy);
			for (EquipmentSlot slot : EquipmentSlot.values()) {
				setDropChance(slot, 2.0F);
			}
			super.die(damageSources().generic());
		} else {
			chronicle(Chronicle.Type.RETREATED, downedBy, "", 0);
			Warbands.retreat(this);
		}
	}

	private void tickDowned(ServerLevel server) {
		int left = entityData.get(DATA_DOWNED) - 1;
		if (getPose() != Pose.SLEEPING) {
			setPose(Pose.SLEEPING);
		}
		boolean helped = false;
		for (Player player : server.players()) {
			if (player.isSpectator() || !Compat.isCrouching(player) || player.distanceToSqr(this) > 2.6 * 2.6) {
				continue;
			}
			if (Fellowship.isFriendly(this, player)) {
				addRevive(1.0F / (FellowshipConfig.INSTANCE.reviveSeconds * 20.0F), player.getGameProfile().getName(), null);
				helped = true;
				if (!isDowned()) {
					return;
				}
			}
		}
		if (!helped && ++reviveIdle > 40 && reviveProgress > 0) {
			reviveProgress = Math.max(0, reviveProgress - 0.01F);
			entityData.set(DATA_REVIVE, (int) (reviveProgress * 100));
		}
		if (tickCount % 4 == 0) {
			int points = 16;
			int lit = Math.round(points * Math.max(0, Math.min(1, reviveProgress)));
			for (int i = 0; i < points; i++) {
				double a = 2 * Math.PI * i / points;
				server.sendParticles(Compat.dust(i < lit ? 0xF2C94C : 0x6B1E1E, 0.8F), getX() + Math.sin(a) * 1.1, getY() + 0.15, getZ() + Math.cos(a) * 1.1, 1, 0, 0, 0, 0);
			}
		}
		if (left <= 0) {
			bleedOut();
			return;
		}
		entityData.set(DATA_DOWNED, left);
	}

	// ---- combat ----

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (level().isClientSide) {
			return false;
		}
		if (!Compat.bypassesInvulnerability(source)) {
			Entity attacker = source.getEntity();
			if (Fellowship.protects(this, attacker) || isDowned()) {
				return false;
			}
			if (isHired() && record.has(Trait.LUCKY) && random.nextDouble() < Trait.LUCKY.dodgeChance()) {
				if (level() instanceof ServerLevel server) {
					server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1, getZ(), 6, 0.3, 0.3, 0.3, 0.02);
				}
				return false;
			}
			if (attacker instanceof LivingEntity) {
				markCombat();
			}
		}
		return super.hurt(source, amount);
	}

	@Override
	public void die(DamageSource source) {
		if (!level().isClientSide && isHired() && !isDowned() && !Compat.bypassesInvulnerability(source)) {
			enterDowned(source);
			return;
		}
		if (!level().isClientSide && isHired()) {
			Warbands.allyFell(this, source.getEntity() != null ? Compat.typeName(source.getEntity()) : source.getMsgId());
		}
		super.die(source);
	}

	@Override
	public boolean doHurtTarget(Entity target) {
		boolean hit = super.doHurtTarget(target);
		if (hit) {
			markCombat();
			swing(InteractionHand.MAIN_HAND);
		}
		return hit;
	}

	@Override
	public boolean canAttack(LivingEntity target) {
		return !Fellowship.isFriendly(this, target) && super.canAttack(target);
	}

	@Override
	public boolean isAlliedTo(Entity other) {
		return Fellowship.isFriendly(this, other) || super.isAlliedTo(other);
	}

	@Override
	public boolean canBeSeenAsEnemy() {
		return !isDowned() && super.canBeSeenAsEnemy();
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	public boolean requiresCustomPersistence() {
		return true;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.PLAYER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return null;
	}

	// ---- ticking ----

	@Override
	public void aiStep() {
		super.aiStep();
		if (level() instanceof ServerLevel server && isAlive()) {
			serverTick(server);
		}
	}

	private void serverTick(ServerLevel server) {
		if (record.name.isEmpty()) {
			Warbands.fillBlank(this);
		}
		if (isDowned()) {
			tickDowned(server);
			if (tickCount % 10 == 0) {
				refreshNameplate();
			}
			return;
		}
		if (getPose() == Pose.SLEEPING) {
			setPose(Pose.STANDING);
		}
		if ((tickCount + getId()) % 20 == 0) {
			secondTick(server);
		}
		if (isHired()) {
			AllyCombat.tick(this);
		}
		if (tickCount % 10 == 0) {
			refreshNameplate();
		}
	}

	private void secondTick(ServerLevel server) {
		if (!isHired()) {
			if (record.leaveAt > 0 && Compat.gameTime(server) > record.leaveAt) {
				server.sendParticles(ParticleTypes.POOF, getX(), getY() + 1, getZ(), 12, 0.3, 0.5, 0.3, 0.02);
				discard();
			}
			if (statsDirty) {
				applyStats(true);
			}
			return;
		}
		Warbands.track(this);
		applyStats(false);
		long now = Compat.gameTime(server);
		if (record.has(Trait.HARDY) && !inCombat() && now % 80 < 20 && getHealth() < getMaxHealth()) {
			heal(1.0F);
		}
		if (now % 1200 < 20) {
			record.morale = Progression.clampMorale(Progression.drift(record.morale), record.traitList(), Warbands.moraleFloor(this));
		}
		setAggressive(getTarget() != null);
	}

	/** Records a kill by this ally. */
	public void onKill(LivingEntity victim, boolean boss) {
		record.stats.kills++;
		record.morale = Math.min(100, record.morale + (boss ? 10 : 1));
		if (record.stats.kills == 1) {
			chronicle(Chronicle.Type.FIRST_BLOOD, Compat.typeName(victim), "", 0);
		} else if (Chronicle.killMilestone(record.stats.kills)) {
			chronicle(Chronicle.Type.KILLS, "", "", record.stats.kills);
		}
		if (boss) {
			record.stats.bossKills++;
			chronicle(Chronicle.Type.BOSS_KILL, Compat.typeName(victim), "", 0);
		}
		long now = Compat.gameTime(level());
		if (Warbands.downedCount(this) >= 2 && now - lastLastStand > 24000) {
			lastLastStand = now;
			chronicle(Chronicle.Type.LAST_STAND, "", "", 0);
		}
	}

	public void gainXp(int amount) {
		int before = record.level;
		int gained = record.addXp(amount, Warbands.levelCap(this));
		if (gained > 0) {
			applyStats(true);
			if (level() instanceof ServerLevel server) {
				server.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 1.2, getZ(), 16, 0.4, 0.6, 0.4, 0.1);
				Compat.playSound(server, getX(), getY(), getZ(), "minecraft:entity.player.levelup", 0.6F, 1.2F);
			}
			for (int l = before + 1; l <= record.level; l++) {
				if (Progression.titleLevel(l)) {
					chronicle(Chronicle.Type.TITLE, Progression.title(l), "", 0);
				}
			}
			Warbands.allyLevelled(this, before);
		}
	}

	// ---- nameplate ----

	public void refreshNameplate() {
		AllyClass c = allyClass();
		String key;
		MutableComponent name;
		if (isDowned()) {
			int seconds = (downedTicks() + 19) / 20;
			key = "d" + seconds + record.name;
			name = Component.literal("✚ " + record.name + " · DOWNED " + seconds + "s").withStyle(s -> s.withColor(TextColor.fromRgb(0xFF5555)));
		} else if (!isHired()) {
			int price = Warbands.price(this);
			key = "h" + price + record.level + record.fullName();
			name = Component.literal(record.fullName()).withStyle(s -> s.withColor(TextColor.fromRgb(c.colorRgb())))
					.append(Component.literal(" · " + c.name + " " + record.level + " · " + price + " gold").withStyle(s -> s.withColor(TextColor.fromRgb(0xF2C94C))));
		} else {
			key = "a" + record.level + record.name + c.name;
			name = Component.literal(record.name).withStyle(s -> s.withColor(TextColor.fromRgb(c.colorRgb())))
					.append(Component.literal(" · " + c.name + " " + record.level).withStyle(s -> s.withColor(TextColor.fromRgb(0xDDDDDD))));
		}
		if (!key.equals(plate)) {
			plate = key;
			setCustomName(name);
			setCustomNameVisible(true);
		}
	}

	// ---- interaction ----

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (level().isClientSide) {
			return InteractionResult.SUCCESS;
		}
		if (!(player instanceof ServerPlayer serverPlayer) || hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}
		return Warbands.interact(this, serverPlayer, player.getItemInHand(hand));
	}

	/** Puts a piece of gear on the ally and hands back what it wore. */
	public ItemStack equip(ItemStack stack) {
		EquipmentSlot slot = stack.is(Items.SHIELD) ? EquipmentSlot.OFFHAND : getEquipmentSlotForItem(stack);
		ItemStack old = getItemBySlot(slot).copy();
		setItemSlot(slot, stack.copyWithCount(1));
		setDropChance(slot, 2.0F);
		return old;
	}

	// ---- saving ----

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putString("Fellowship", record.toJson());
		tag.putInt("FellowshipDowned", entityData.get(DATA_DOWNED));
		if (holdPos != null) {
			tag.putLong("FellowshipHold", holdPos.asLong());
		}
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		setRecord(AllyRecord.fromJson(tag.getString("Fellowship")));
		entityData.set(DATA_DOWNED, Math.max(0, tag.getInt("FellowshipDowned")));
		holdPos = tag.contains("FellowshipHold") ? BlockPos.of(tag.getLong("FellowshipHold")) : null;
	}

	@Override
	public void remove(RemovalReason reason) {
		if (!level().isClientSide) {
			Warbands.untrack(this, reason.shouldDestroy());
		}
		super.remove(reason);
	}
}
