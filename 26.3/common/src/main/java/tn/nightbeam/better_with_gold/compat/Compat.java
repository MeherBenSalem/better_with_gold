package tn.nightbeam.better_with_gold.compat;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BannerPatterns;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.TagValueOutput;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

/**
 * Every Minecraft call that differs between the supported versions lives here, so the rest of the shared code
 * is identical in each version folder. This file is the Minecraft 26.3 edition.
 */
public final class Compat {
	private static final TagKey<EntityType<?>> BOSSES = TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("c", "bosses"));

	private Compat() {
	}

	public static Identifier id(String namespace, String path) {
		return Identifier.fromNamespaceAndPath(namespace, path);
	}

	public static Identifier parse(String id) {
		Identifier parsed = Identifier.tryParse(id);
		return parsed == null ? Identifier.withDefaultNamespace("air") : parsed;
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
		return BuiltInRegistries.SOUND_EVENT.getValue(parse(id));
	}

	public static void playSound(Level level, double x, double y, double z, String id, float volume, float pitch) {
		SoundEvent sound = sound(id);
		if (sound != null) {
			level.playSound(null, x, y, z, sound, SoundSource.NEUTRAL, volume, pitch);
		}
	}

	public static ParticleOptions dust(int rgb, float size) {
		return new DustParticleOptions(rgb & 0xFFFFFF, size);
	}

	public static ServerLevel serverLevel(Entity entity) {
		return entity.level() instanceof ServerLevel server ? server : null;
	}

	public static MinecraftServer server(ServerPlayer player) {
		return player.level().getServer();
	}

	public static ServerLevel level(ServerPlayer player) {
		return player.level();
	}

	public static Path worldDirectory(MinecraftServer server) {
		return server.getWorldPath(LevelResource.ROOT);
	}

	public static boolean hasPermission(CommandSourceStack source, int level) {
		Permission permission = switch (level) {
			case 0, 1 -> Permissions.COMMANDS_MODERATOR;
			case 2 -> Permissions.COMMANDS_GAMEMASTER;
			case 3 -> Permissions.COMMANDS_ADMIN;
			default -> Permissions.COMMANDS_OWNER;
		};
		return source.permissions().hasPermission(permission);
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
		return target.level() instanceof ServerLevel level && target.hurtServer(level, source, amount);
	}

	public static void credit(LivingEntity victim, Player leader) {
		victim.setLastHurtByPlayer(leader, 100);
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
		return entity.is(BOSSES) || entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon
				|| entity instanceof net.minecraft.world.entity.boss.wither.WitherBoss
				|| entity instanceof net.minecraft.world.entity.monster.warden.Warden
				|| entity instanceof net.minecraft.world.entity.monster.ElderGuardian || entity.getMaxHealth() >= 150;
	}

	public static String typeName(Entity entity) {
		return entity.getType().getDescription().getString();
	}

	public static String biomeName(Level level, BlockPos pos) {
		return level.getBiome(pos).unwrapKey().map(k -> humanize(k.identifier().getPath())).orElse("wilds");
	}

	public static String dimensionId(Level level) {
		return level.dimension().identifier().toString();
	}

	public static String dimensionName(Level level) {
		Identifier id = level.dimension().identifier();
		return switch (id.toString()) {
			case "minecraft:overworld" -> "the Overworld";
			case "minecraft:the_nether" -> "the Nether";
			case "minecraft:the_end" -> "the End";
			default -> "the " + humanize(id.getPath());
		};
	}

	public static ServerLevel levelById(MinecraftServer server, String id) {
		Identifier key = Identifier.tryParse(id == null ? "" : id);
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
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.registryAccess());
		return entity.save(output) ? output.buildResult().toString() : "";
	}

	public static Entity loadEntity(String text, ServerLevel level) {
		try {
			CompoundTag tag = TagParser.parseCompoundFully(text);
			return EntityType.loadEntityRecursive(tag, level, new net.minecraft.world.entity.EntitySpawnRequest(EntitySpawnReason.LOAD, false), e -> e);
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
		LightningBolt bolt = net.minecraft.world.entity.EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
		if (bolt != null) {
			bolt.snapTo(x, y, z);
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
		return type.create(level, EntitySpawnReason.COMMAND);
	}

	/** Swings the given hand (attack, cast or revive animation). */
	public static void swing(Mob mob, InteractionHand hand) {
		mob.swingForAttack(hand);
	}

	/** Puts a stack back in the player's inventory, dropping it at their feet when it is full. */
	public static void giveBack(Player player, ItemStack stack) {
		player.getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY);
	}

	public static void moveTo(Entity entity, double x, double y, double z, float yaw) {
		entity.snapTo(x, y, z, yaw, 0);
	}

	public static void message(ServerPlayer player, Component text, boolean actionBar) {
		if (actionBar) {
			player.sendOverlayMessage(text);
		} else {
			player.sendSystemMessage(text);
		}
	}

	private static Block block(String id) {
		return BuiltInRegistries.BLOCK.getValue(parse(id));
	}

	/** Gold border and diamond on a black banner. */
	public static void decorateWarbanner(ServerLevel level, BlockPos pos) {
		BlockEntity be = level.getBlockEntity(pos);
		if (be == null) {
			return;
		}
		try {
			var patterns = level.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN);
			BannerPatternLayers layers = new BannerPatternLayers.Builder()
					.add(patterns.getOrThrow(BannerPatterns.RHOMBUS_MIDDLE), DyeColor.YELLOW)
					.add(patterns.getOrThrow(BannerPatterns.CIRCLE_MIDDLE), DyeColor.BLACK)
					.add(patterns.getOrThrow(BannerPatterns.BORDER), DyeColor.YELLOW)
					.add(patterns.getOrThrow(BannerPatterns.STRIPE_BOTTOM), DyeColor.YELLOW)
					.build();
			ItemStack banner = new ItemStack(item("minecraft:black_banner"));
			banner.set(DataComponents.BANNER_PATTERNS, layers);
			be.applyComponentsFromItemStack(banner);
			be.setChanged();
			level.sendBlockUpdated(pos, be.getBlockState(), be.getBlockState(), 3);
		} catch (RuntimeException ignored) {
			// A plain banner still works as a rally point.
		}
	}

	/** The name a name tag carries, or null. */
	public static String customName(ItemStack stack) {
		Component name = stack.get(DataComponents.CUSTOM_NAME);
		return name == null ? null : name.getString();
	}

	/** Weapons an ally may hold in its main hand. */
	public static boolean isWeapon(ItemStack stack) {
		Item item = stack.getItem();
		return stack.has(DataComponents.WEAPON) || item instanceof net.minecraft.world.item.ProjectileWeaponItem
				|| item instanceof net.minecraft.world.item.TridentItem || item instanceof net.minecraft.world.item.MaceItem
				|| stack.is(Items.BLAZE_ROD) || stack.is(Items.BOOK) || stack.is(Items.STICK);
	}

	public static boolean isArmor(ItemStack stack) {
		var equippable = stack.get(DataComponents.EQUIPPABLE);
		return equippable != null && equippable.slot().isArmor();
	}

	/** Places the black-and-gold Warbanner facing {@code yaw}. Returns false when the spot is taken. */
	public static boolean placeWarbanner(ServerLevel level, BlockPos pos, float yaw) {
		if (!level.getBlockState(pos).canBeReplaced() || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
			return false;
		}
		level.setBlock(pos, block("minecraft:black_banner").defaultBlockState().setValue(BannerBlock.ROTATION, RotationSegment.convertToSegment(yaw + 180.0F)), 3);
		decorateWarbanner(level, pos);
		return true;
	}

	public static boolean isWarbanner(ServerLevel level, BlockPos pos) {
		return level.getBlockState(pos).is(block("minecraft:black_banner"));
	}

	public static void removeBlock(ServerLevel level, BlockPos pos) {
		level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
	}

	public static void forceChunk(ServerLevel level, int chunkX, int chunkZ, boolean on) {
		level.setChunkForced(chunkX, chunkZ, on);
	}

	public static void teleport(Entity entity, double x, double y, double z) {
		entity.teleportTo(x, y, z);
	}

	public static String playerName(Player player) {
		return player.getGameProfile().name();
	}

	/** The owner of a tamed or owned entity, or null. */
	public static UUID ownerOf(Entity entity) {
		return entity instanceof OwnableEntity ownable && ownable.getOwnerReference() != null ? ownable.getOwnerReference().getUUID() : null;
	}

	public static boolean isVillagerOrGolem(Entity entity) {
		return entity instanceof net.minecraft.world.entity.npc.Npc || entity instanceof net.minecraft.world.entity.animal.golem.AbstractGolem;
	}

	public static void ride(Entity passenger, Entity vehicle) {
		passenger.startRiding(vehicle, true, false);
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
		return level.getOverworldClockTime() / 24000L + 1;
	}

	public static boolean isCrouching(Player player) {
		return player.isShiftKeyDown();
	}
}
