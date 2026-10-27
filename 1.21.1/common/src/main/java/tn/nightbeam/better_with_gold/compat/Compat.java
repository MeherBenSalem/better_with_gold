package tn.nightbeam.better_with_gold.compat;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;
import org.joml.Vector3f;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Every Minecraft call that differs between the supported versions lives here, so the rest of the shared code
 * is identical in each version folder. This file is the Minecraft 1.21.1 edition.
 */
public final class Compat {
	private static final TagKey<EntityType<?>> BOSSES = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("c", "bosses"));

	private Compat() {
	}

	public static ResourceLocation id(String namespace, String path) {
		return ResourceLocation.fromNamespaceAndPath(namespace, path);
	}

	public static ResourceLocation parse(String id) {
		ResourceLocation parsed = ResourceLocation.tryParse(id);
		return parsed == null ? ResourceLocation.withDefaultNamespace("air") : parsed;
	}

	public static Item item(String id) {
		return BuiltInRegistries.ITEM.getOptional(parse(id)).orElse(Items.AIR);
	}

	public static ItemStack stack(String id) {
		if (id == null || id.isBlank()) {
			return ItemStack.EMPTY;
		}
		Item item = item(id);
		return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
	}

	public static String itemId(Item item) {
		return BuiltInRegistries.ITEM.getKey(item).toString();
	}

	public static SoundEvent sound(String id) {
		return BuiltInRegistries.SOUND_EVENT.get(parse(id));
	}

	public static void playSound(Level level, double x, double y, double z, String id, float volume, float pitch) {
		SoundEvent sound = sound(id);
		if (sound != null) {
			level.playSound(null, x, y, z, sound, SoundSource.NEUTRAL, volume, pitch);
		}
	}

	public static ParticleOptions dust(int rgb, float size) {
		return new DustParticleOptions(new Vector3f(((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f), size);
	}

	public static ServerLevel serverLevel(Entity entity) {
		return entity.level() instanceof ServerLevel server ? server : null;
	}

	public static MinecraftServer server(ServerPlayer player) {
		return player.server;
	}

	public static ServerLevel level(ServerPlayer player) {
		return player.serverLevel();
	}

	public static Path worldDirectory(MinecraftServer server) {
		return server.getWorldPath(LevelResource.ROOT);
	}

	public static boolean hasPermission(CommandSourceStack source, int level) {
		return source.hasPermission(level);
	}

	public static boolean bypassesInvulnerability(DamageSource source) {
		return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
	}

	public static DamageSource magic(LivingEntity attacker) {
		return attacker.damageSources().indirectMagic(attacker, attacker);
	}

	public static DamageSource melee(LivingEntity attacker) {
		return attacker.damageSources().mobAttack(attacker);
	}

	public static boolean hurt(LivingEntity target, DamageSource source, float amount) {
		return target.hurt(source, amount);
	}

	public static void credit(LivingEntity victim, Player leader) {
		victim.setLastHurtByPlayer(leader);
	}

	public static void setBase(LivingEntity entity, String which, double value) {
		AttributeInstance a = attribute(entity, which);
		if (a != null && Math.abs(a.getBaseValue() - value) > 1e-6) {
			a.setBaseValue(value);
		}
	}

	public static double get(LivingEntity entity, String which) {
		AttributeInstance a = attribute(entity, which);
		return a == null ? 0 : a.getValue();
	}

	private static AttributeInstance attribute(LivingEntity entity, String which) {
		net.minecraft.core.Holder<Attribute> holder = switch (which) {
			case "max_health" -> Attributes.MAX_HEALTH;
			case "speed" -> Attributes.MOVEMENT_SPEED;
			case "damage" -> Attributes.ATTACK_DAMAGE;
			case "armor" -> Attributes.ARMOR;
			case "knockback" -> Attributes.KNOCKBACK_RESISTANCE;
			case "follow" -> Attributes.FOLLOW_RANGE;
			default -> null;
		};
		return holder == null ? null : entity.getAttribute(holder);
	}

	/** Points a mob at a new target, including mobs that think with a brain instead of goals. */
	public static void forceTarget(Mob mob, LivingEntity target) {
		mob.setTarget(target);
		if (mob.getBrain().checkMemory(MemoryModuleType.ATTACK_TARGET, MemoryStatus.REGISTERED)) {
			mob.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
		}
	}

	public static LivingEntity currentTarget(Mob mob) {
		LivingEntity target = mob.getTarget();
		if (target == null && mob.getBrain().checkMemory(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT)) {
			target = mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
		}
		return target;
	}

	public static boolean isBoss(LivingEntity entity) {
		return entity.getType().is(BOSSES) || entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon
				|| entity instanceof net.minecraft.world.entity.boss.wither.WitherBoss
				|| entity instanceof net.minecraft.world.entity.monster.warden.Warden
				|| entity instanceof net.minecraft.world.entity.monster.ElderGuardian || entity.getMaxHealth() >= 150;
	}

	public static String typeName(Entity entity) {
		return entity.getType().getDescription().getString();
	}

	public static String biomeName(Level level, BlockPos pos) {
		return level.getBiome(pos).unwrapKey().map(k -> humanize(k.location().getPath())).orElse("wilds");
	}

	public static String dimensionId(Level level) {
		return level.dimension().location().toString();
	}

	public static String dimensionName(Level level) {
		ResourceLocation id = level.dimension().location();
		return switch (id.toString()) {
			case "minecraft:overworld" -> "the Overworld";
			case "minecraft:the_nether" -> "the Nether";
			case "minecraft:the_end" -> "the End";
			default -> "the " + humanize(id.getPath());
		};
	}

	public static ServerLevel levelById(MinecraftServer server, String id) {
		ResourceLocation key = ResourceLocation.tryParse(id == null ? "" : id);
		if (key == null) {
			return null;
		}
		return server.getLevel(ResourceKey.create(Registries.DIMENSION, key));
	}

	public static String humanize(String path) {
		StringBuilder sb = new StringBuilder();
		for (String word : path.split("[_/]")) {
			if (word.isEmpty()) {
				continue;
			}
			if (sb.length() > 0) {
				sb.append(' ');
			}
			sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}
		return sb.toString();
	}

	/** The whole entity, with its type id, as text. */
	public static String saveEntity(Entity entity) {
		CompoundTag tag = new CompoundTag();
		return entity.save(tag) ? tag.toString() : "";
	}

	public static Entity loadEntity(String text, ServerLevel level) {
		try {
			CompoundTag tag = TagParser.parseTag(text);
			return EntityType.loadEntityRecursive(tag, level, e -> e);
		} catch (Exception ex) {
			return null;
		}
	}

	public static Optional<BlockPos> villageCentre(ServerLevel level, BlockPos near, int radius) {
		return level.getPoiManager().findClosest(h -> h.is(PoiTypes.MEETING), near, radius, PoiManager.Occupancy.ANY);
	}

	public static BlockPos surface(ServerLevel level, int x, int z) {
		return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
	}

	public static void visualLightning(ServerLevel level, double x, double y, double z) {
		LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
		if (bolt != null) {
			bolt.moveTo(x, y, z);
			bolt.setVisualOnly(true);
			level.addFreshEntity(bolt);
		}
	}

	public static AbstractArrow arrow(LivingEntity shooter, ItemStack bow, float damage) {
		AbstractArrow arrow = ProjectileUtil.getMobArrow(shooter, new ItemStack(Items.ARROW), 1.0F, bow.isEmpty() ? new ItemStack(Items.BOW) : bow);
		arrow.setBaseDamage(damage / 1.6);
		arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
		return arrow;
	}

	public static <T extends Entity> T create(EntityType<T> type, ServerLevel level) {
		return type.create(level);
	}

	public static void moveTo(Entity entity, double x, double y, double z, float yaw) {
		entity.moveTo(x, y, z, yaw, 0);
	}

	public static void message(ServerPlayer player, Component text, boolean actionBar) {
		player.displayClientMessage(text, actionBar);
	}

	/** Gold border and diamond on a black banner. */
	public static void decorateWarbanner(ServerLevel level, BlockPos pos) {
		BlockEntity be = level.getBlockEntity(pos);
		if (be == null) {
			return;
		}
		try {
			CompoundTag tag = be.saveWithFullMetadata(level.registryAccess());
			ListTag patterns = new ListTag();
			patterns.add(pattern("minecraft:rhombus", "yellow"));
			patterns.add(pattern("minecraft:circle", "black"));
			patterns.add(pattern("minecraft:border", "yellow"));
			patterns.add(pattern("minecraft:stripe_bottom", "yellow"));
			tag.put("patterns", patterns);
			be.loadWithComponents(tag, level.registryAccess());
			be.setChanged();
			level.sendBlockUpdated(pos, be.getBlockState(), be.getBlockState(), 3);
		} catch (RuntimeException ignored) {
			// A plain banner still works as a rally point.
		}
	}

	private static CompoundTag pattern(String id, String color) {
		CompoundTag t = new CompoundTag();
		t.putString("pattern", id);
		t.putString("color", color);
		return t;
	}

	/** The name a name tag carries, or null. */
	public static String customName(ItemStack stack) {
		Component name = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
		return name == null ? null : name.getString();
	}

	/** Weapons an ally may hold in its main hand. */
	public static boolean isWeapon(ItemStack stack) {
		Item item = stack.getItem();
		return item instanceof net.minecraft.world.item.TieredItem || item instanceof net.minecraft.world.item.ProjectileWeaponItem
				|| item instanceof net.minecraft.world.item.TridentItem || item instanceof net.minecraft.world.item.MaceItem
				|| stack.is(Items.BLAZE_ROD) || stack.is(Items.BOOK) || stack.is(Items.STICK);
	}

	public static boolean isArmor(ItemStack stack) {
		return stack.getItem() instanceof net.minecraft.world.item.ArmorItem;
	}

	/** Places the black-and-gold Warbanner facing {@code yaw}. Returns false when the spot is taken. */
	public static boolean placeWarbanner(ServerLevel level, BlockPos pos, float yaw) {
		if (!level.getBlockState(pos).canBeReplaced() || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)) {
			return false;
		}
		level.setBlock(pos, net.minecraft.world.level.block.Blocks.BLACK_BANNER.defaultBlockState()
				.setValue(net.minecraft.world.level.block.BannerBlock.ROTATION,
						net.minecraft.world.level.block.state.properties.RotationSegment.convertToSegment(yaw + 180.0F)), 3);
		decorateWarbanner(level, pos);
		return true;
	}

	public static boolean isWarbanner(ServerLevel level, BlockPos pos) {
		return level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.BLACK_BANNER);
	}

	public static void removeBlock(ServerLevel level, BlockPos pos) {
		level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
	}

	public static void forceChunk(ServerLevel level, int chunkX, int chunkZ, boolean on) {
		level.setChunkForced(chunkX, chunkZ, on);
	}

	public static void teleport(Entity entity, double x, double y, double z) {
		entity.teleportTo(x, y, z);
	}

	public static String playerName(Player player) {
		return player.getGameProfile().getName();
	}

	/** The owner of a tamed or owned entity, or null. */
	public static java.util.UUID ownerOf(Entity entity) {
		return entity instanceof net.minecraft.world.entity.OwnableEntity ownable ? ownable.getOwnerUUID() : null;
	}

	public static boolean isVillagerOrGolem(Entity entity) {
		return entity instanceof net.minecraft.world.entity.npc.Npc || entity instanceof net.minecraft.world.entity.animal.AbstractGolem;
	}

	public static void ride(Entity passenger, Entity vehicle) {
		passenger.startRiding(vehicle, true);
	}

	/** Hurt by a living attacker within the last five seconds. */
	public static boolean recentlyHurtBy(LivingEntity entity) {
		return entity.tickCount - entity.getLastHurtByMobTimestamp() < 100;
	}

	/** Hurt something within the last five seconds. */
	public static boolean recentlyHurt(LivingEntity entity) {
		return entity.tickCount - entity.getLastHurtMobTimestamp() < 100;
	}

	public static long gameTime(Level level) {
		return level.getGameTime();
	}

	/** The in-game day, counting the first day as day 1. */
	public static long day(Level level) {
		return level.getDayTime() / 24000L + 1;
	}

	public static boolean isCrouching(Player player) {
		return player.isShiftKeyDown();
	}
}
