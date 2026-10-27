package tn.nightbeam.better_with_gold.ally;

import tn.nightbeam.better_with_gold.Constants;
import tn.nightbeam.better_with_gold.compat.Compat;
import tn.nightbeam.better_with_gold.core.AllyClass;
import tn.nightbeam.better_with_gold.core.AllyRecord;
import tn.nightbeam.better_with_gold.core.Chronicle;
import tn.nightbeam.better_with_gold.core.ClassCatalog;
import tn.nightbeam.better_with_gold.core.FellowshipConfig;
import tn.nightbeam.better_with_gold.core.Formation;
import tn.nightbeam.better_with_gold.core.Order;
import tn.nightbeam.better_with_gold.core.Progression;
import tn.nightbeam.better_with_gold.core.Recruits;
import tn.nightbeam.better_with_gold.core.Trait;
import tn.nightbeam.better_with_gold.net.Net;
import tn.nightbeam.better_with_gold.platform.Services;
import tn.nightbeam.better_with_gold.registry.ModEntities;
import tn.nightbeam.better_with_gold.registry.ModItems;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Every leader's warband: who is in it, where they are, the standing order and formation, the Warbanner, the
 * camp and the fallen. Saves one JSON file per leader in the world folder. Server side; plain static state that
 * is cleared when the server stops.
 */
public final class Warbands {
	private static final Map<UUID, WarbandState> STATES = new HashMap<>();
	private static final Map<UUID, Map<UUID, AllyEntity>> LIVE = new HashMap<>();
	private static final Map<UUID, Map<UUID, Vec3>> SLOTS = new HashMap<>();
	private static final Map<UUID, double[]> ANCHORS = new HashMap<>();
	private static final Map<UUID, Long> SAVE_THROTTLE = new HashMap<>();
	private static final Map<UUID, Long> ROSTER_WATCH = new HashMap<>();
	private static final List<RecallJob> RECALLS = new ArrayList<>();
	private static final List<Pending> PENDING = new ArrayList<>();
	private static final Random RANDOM = new Random();
	private static MinecraftServer server;

	private record RecallJob(UUID owner, UUID ally, ServerLevel level, int chunkX, int chunkZ, long until) {
	}

	private record Pending(UUID owner, long at) {
	}

	private Warbands() {
	}

	// ---- lifecycle ----

	public static void start(MinecraftServer s) {
		server = s;
		STATES.clear();
		LIVE.clear();
		SLOTS.clear();
	}

	public static void stop() {
		for (UUID owner : new ArrayList<>(STATES.keySet())) {
			save(owner);
		}
		for (RecallJob job : RECALLS) {
			Compat.forceChunk(job.level(), job.chunkX(), job.chunkZ(), false);
		}
		RECALLS.clear();
		PENDING.clear();
		STATES.clear();
		LIVE.clear();
		SLOTS.clear();
		server = null;
	}

	private static Path file(UUID owner) {
		return Compat.worldDirectory(server).resolve(Constants.MOD_ID).resolve("fellowship").resolve(owner + ".json");
	}

	public static WarbandState state(UUID owner) {
		return STATES.computeIfAbsent(owner, id -> {
			WarbandState s = null;
			if (server != null) {
				Path f = file(id);
				if (Files.isRegularFile(f)) {
					try (Reader reader = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
						s = Net.GSON.fromJson(reader, WarbandState.class);
					} catch (IOException | RuntimeException ex) {
						Constants.LOG.warn("Could not read warband file {}", f, ex);
					}
				}
			}
			if (s == null) {
				s = new WarbandState();
				s.owner = id.toString();
			}
			return s.repair();
		});
	}

	public static void save(UUID owner) {
		WarbandState s = STATES.get(owner);
		if (s == null || server == null) {
			return;
		}
		Path f = file(owner);
		try {
			Files.createDirectories(f.getParent());
			Path tmp = f.resolveSibling(f.getFileName() + ".tmp");
			try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
				Net.GSON.toJson(s, writer);
			}
			Files.move(tmp, f, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			s.dirty = false;
		} catch (IOException ex) {
			Constants.LOG.warn("Could not save warband file {}", f, ex);
		}
	}

	// ---- tracking ----

	public static void track(AllyEntity ally) {
		UUID owner = ally.ownerId();
		if (owner == null) {
			return;
		}
		LIVE.computeIfAbsent(owner, id -> new LinkedHashMap<>()).put(ally.getUUID(), ally);
		WarbandState s = state(owner);
		WarbandState.Seen seen = s.seen.computeIfAbsent(ally.getUUID().toString(), id -> new WarbandState.Seen());
		seen.dimension = Compat.dimensionId(ally.level());
		seen.x = ally.getX();
		seen.y = ally.getY();
		seen.z = ally.getZ();
		seen.name = ally.record().name;
		seen.classId = ally.record().classId;
		seen.level = ally.record().level;
		String dim = Compat.dimensionName(ally.level());
		if (!ally.record().visited.contains(seen.dimension)) {
			ally.record().visited.add(seen.dimension);
			if (ally.record().visited.size() > 1) {
				ally.chronicle(Chronicle.Type.DIMENSION, dim, "", 0);
			}
		}
		s.dirty = true;
	}

	public static void untrack(AllyEntity ally, boolean destroyed) {
		UUID owner = ally.ownerId();
		if (owner == null) {
			return;
		}
		Map<UUID, AllyEntity> live = LIVE.get(owner);
		if (live != null && live.get(ally.getUUID()) == ally) {
			live.remove(ally.getUUID());
		}
	}

	private static void forgetSeen(AllyEntity ally) {
		UUID owner = ally.ownerId();
		if (owner != null) {
			state(owner).seen.remove(ally.getUUID().toString());
			state(owner).dirty = true;
		}
	}

	public static List<AllyEntity> liveAllies(UUID owner) {
		Map<UUID, AllyEntity> live = LIVE.get(owner);
		if (live == null) {
			return List.of();
		}
		List<AllyEntity> list = new ArrayList<>();
		for (AllyEntity a : live.values()) {
			if (a.isAlive() && !a.isRemoved()) {
				list.add(a);
			}
		}
		return list;
	}

	/** The ally's band-mates that are loaded, itself included. */
	public static List<AllyEntity> liveAllies(AllyEntity ally) {
		UUID owner = ally.ownerId();
		return owner == null ? List.of() : liveAllies(owner);
	}

	public static boolean leadsLiveBand(UUID player) {
		return !liveAllies(player).isEmpty();
	}

	public static int downedCount(AllyEntity ally) {
		int n = 0;
		for (AllyEntity a : liveAllies(ally)) {
			if (a.isDowned()) {
				n++;
			}
		}
		return n;
	}

	// ---- numbers ----

	public static int price(AllyEntity ally) {
		return Progression.price(ally.allyClass(), ally.record().level, ally.record().traitList(), FellowshipConfig.INSTANCE.priceMultiplier);
	}

	public static int levelCap(AllyEntity ally) {
		int cap = FellowshipConfig.INSTANCE.levelCap;
		if (FellowshipConfig.INSTANCE.rpgLevelCap && RasBridge.isLoaded()) {
			ServerPlayer owner = ally.owner();
			if (owner != null) {
				cap = Math.min(cap, Math.max(FellowshipConfig.INSTANCE.rpgLevelCapFloor, RasBridge.level(owner)));
			}
		}
		return Math.max(ally.record().level, cap);
	}

	/** Pieces of Reinforced Golden Armour the leader wears, when within 24 blocks. */
	public static int regaliaPieces(AllyEntity ally) {
		if (ally.level().isClientSide) {
			return 0;
		}
		ServerPlayer owner = ally.owner();
		if (owner == null || owner.level() != ally.level() || owner.distanceToSqr(ally) > 24 * 24) {
			return 0;
		}
		return regaliaPieces(owner);
	}

	public static int regaliaPieces(Player player) {
		int n = 0;
		for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
			Item item = player.getItemBySlot(slot).getItem();
			if (item == ModItems.REINFORCED_GOLDEN_ARMOR_HELMET || item == ModItems.REINFORCED_GOLDEN_ARMOR_CHESTPLATE
					|| item == ModItems.REINFORCED_GOLDEN_ARMOR_LEGGINGS || item == ModItems.REINFORCED_GOLDEN_ARMOR_BOOTS) {
				n++;
			}
		}
		return n;
	}

	public static int moraleFloor(AllyEntity ally) {
		return regaliaPieces(ally) * FellowshipConfig.INSTANCE.regaliaMoraleFloorPerPiece;
	}

	public static boolean mayRecordSave(AllyEntity ally) {
		long now = Compat.gameTime(ally.level());
		Long last = SAVE_THROTTLE.get(ally.getUUID());
		if (last != null && now - last < 2400) {
			return false;
		}
		SAVE_THROTTLE.put(ally.getUUID(), now);
		return true;
	}

	// ---- recruiting ----

	/** Spawns a new ally. With an owner it is already sworn; without one it is a sellsword for hire. */
	public static AllyEntity spawn(ServerLevel level, AllyClass allyClass, int levelValue, Vec3 at, ServerPlayer owner) {
		AllyEntity ally = Compat.create(ModEntities.ALLY.get(), level);
		if (ally == null) {
			return null;
		}
		AllyRecord record = Recruits.roll(new Random(RANDOM.nextLong()), allyClass, levelValue);
		if (owner == null) {
			record.leaveAt = Compat.gameTime(level) + FellowshipConfig.INSTANCE.sellswordStayMinutes * 1200L;
		}
		Compat.moveTo(ally, at.x, at.y, at.z, RANDOM.nextFloat() * 360F);
		ally.setRecord(record);
		ally.equipKit();
		ally.applyStats(true);
		if (owner != null) {
			swear(ally, owner);
		}
		level.addFreshEntity(ally);
		ally.refreshNameplate();
		return ally;
	}

	/** An ally made by /summon or a spawn egg has no identity yet: it becomes a sellsword of a random class. */
	public static void fillBlank(AllyEntity ally) {
		List<AllyClass> classes = tn.nightbeam.better_with_gold.core.ClassCatalog.enabled();
		AllyClass c = classes.get(RANDOM.nextInt(classes.size()));
		int level = 1 + RANDOM.nextInt(3) + (int) Math.min(8, Compat.day(ally.level()) / 8);
		AllyRecord record = Recruits.roll(new Random(RANDOM.nextLong()), c, Math.min(FellowshipConfig.INSTANCE.levelCap, level));
		record.leaveAt = Compat.gameTime(ally.level()) + FellowshipConfig.INSTANCE.sellswordStayMinutes * 1200L;
		ally.setRecord(record);
		ally.equipKit();
		ally.applyStats(true);
		ally.refreshNameplate();
	}

	private static void swear(AllyEntity ally, ServerPlayer owner) {
		AllyRecord r = ally.record();
		WarbandState s = state(owner.getUUID());
		s.ownerName = owner.getGameProfile().getName();
		r.owner = owner.getUUID().toString();
		r.ownerName = owner.getGameProfile().getName();
		r.swornDay = Compat.day(owner.level());
		r.leaveAt = 0;
		r.order = s.orderValue() == Order.FOCUS ? Order.FOLLOW.id() : s.order;
		r.name = uniqueName(s, owner.getUUID(), r.name, ally.getUUID());
		if (!r.visited.contains(Compat.dimensionId(ally.level()))) {
			r.visited.add(Compat.dimensionId(ally.level()));
		}
		ally.setRecord(r);
		ally.chronicle(Chronicle.Type.SWORN, r.ownerName, Compat.biomeName(ally.level(), ally.blockPosition()), 0);
		ally.applyStats(true);
		track(ally);
	}

	/** Keeps names unique inside one warband, so the roster and the chronicle never show two of the same. */
	private static String uniqueName(WarbandState s, UUID owner, String wanted, UUID self) {
		java.util.Set<String> taken = new java.util.HashSet<>();
		s.seen.forEach((id, seen) -> {
			if (!id.equals(self.toString())) {
				taken.add(seen.name.toLowerCase(Locale.ROOT));
			}
		});
		s.stowed.forEach(st -> taken.add(AllyRecord.fromJson(st.record).name.toLowerCase(Locale.ROOT)));
		for (AllyEntity a : liveAllies(owner)) {
			if (!a.getUUID().equals(self)) {
				taken.add(a.record().name.toLowerCase(Locale.ROOT));
			}
		}
		if (!taken.contains(wanted.toLowerCase(Locale.ROOT))) {
			return wanted;
		}
		List<String> names = new ArrayList<>(Recruits.names());
		java.util.Collections.shuffle(names, RANDOM);
		for (String name : names) {
			if (!taken.contains(name.toLowerCase(Locale.ROOT))) {
				return name;
			}
		}
		return wanted;
	}

	public static boolean hire(AllyEntity ally, ServerPlayer player) {
		FellowshipConfig cfg = FellowshipConfig.INSTANCE;
		WarbandState s = state(player.getUUID());
		if (s.rosterSize() >= cfg.partySize) {
			Compat.message(player, Component.literal("Your warband is full (" + s.rosterSize() + "/" + cfg.partySize + "). Dismiss someone first.").withStyle(ChatFormatting.RED), true);
			return false;
		}
		int price = price(ally);
		Item currency = Compat.item(cfg.currency);
		if (!player.getAbilities().instabuild) {
			int have = count(player, currency);
			if (have < price) {
				Compat.message(player, Component.literal(ally.record().name + " wants " + price + " " + currencyName(currency, price) + ". You have " + have + ".")
						.withStyle(ChatFormatting.GOLD), true);
				return false;
			}
			take(player, currency, price);
		}
		swear(ally, player);
		ally.refreshNameplate();
		ServerLevel level = (ServerLevel) ally.level();
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, ally.getX(), ally.getY() + 1.2, ally.getZ(), 14, 0.4, 0.6, 0.4, 0.1);
		AllyCombat.ring(level, ally.position(), 1.2, Compat.dust(0xF2C94C, 1.2F), 20);
		Compat.playSound(level, ally.getX(), ally.getY(), ally.getZ(), "minecraft:item.armor.equip_gold", 1.0F, 1.0F);
		Compat.playSound(level, ally.getX(), ally.getY(), ally.getZ(), "minecraft:block.bell.use", 0.4F, 1.4F);
		Compat.message(player, Component.literal(ally.record().fullName() + " swore the oath (" + ally.allyClass().name + ", level " + ally.record().level + ").")
				.withStyle(ChatFormatting.GOLD), false);
		pushRoster(player);
		return true;
	}

	private static String currencyName(Item currency, int n) {
		if (currency == Items.GOLD_INGOT) {
			return n == 1 ? "gold ingot" : "gold ingots";
		}
		return new ItemStack(currency).getHoverName().getString();
	}

	private static int count(Player player, Item item) {
		int n = 0;
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (stack.is(item)) {
				n += stack.getCount();
			}
		}
		return n;
	}

	private static void take(Player player, Item item, int amount) {
		for (int i = 0; i < player.getInventory().getContainerSize() && amount > 0; i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (stack.is(item)) {
				int n = Math.min(amount, stack.getCount());
				stack.shrink(n);
				amount -= n;
			}
		}
	}

	// ---- interaction ----

	public static InteractionResult interact(AllyEntity ally, ServerPlayer player, ItemStack stack) {
		AllyRecord r = ally.record();
		if (!ally.isHired()) {
			if (stack.is(Compat.item(FellowshipConfig.INSTANCE.currency))) {
				hire(ally, player);
			} else {
				StringBuilder traits = new StringBuilder();
				for (Trait t : r.traitList()) {
					traits.append(traits.length() == 0 ? "" : ", ").append(t.label());
				}
				Compat.message(player, Component.literal(r.fullName() + ", " + ally.allyClass().name + " (level " + r.level + ", " + traits
						+ "). Hire for " + price(ally) + " " + currencyName(Compat.item(FellowshipConfig.INSTANCE.currency), price(ally))
						+ ": use them while holding the gold.").withStyle(ChatFormatting.GOLD), false);
			}
			return InteractionResult.CONSUME;
		}
		if (!ally.isOwnedBy(player)) {
			Compat.message(player, Component.literal(r.name + " is sworn to " + r.ownerName + ".").withStyle(ChatFormatting.GRAY), true);
			return InteractionResult.CONSUME;
		}
		if (ally.isDowned()) {
			Compat.message(player, Component.literal("Crouch beside " + r.name + " to revive them.").withStyle(ChatFormatting.YELLOW), true);
			return InteractionResult.CONSUME;
		}
		ServerLevel level = (ServerLevel) ally.level();
		if (stack.is(ModItems.GOLDEN_FISH)) {
			ally.setHealth(ally.getMaxHealth());
			r.morale = Math.min(100, r.morale + 30);
			consume(player, stack);
			level.sendParticles(ParticleTypes.HEART, ally.getX(), ally.getY() + 2.0, ally.getZ(), 6, 0.4, 0.3, 0.4, 0);
			Compat.playSound(level, ally.getX(), ally.getY(), ally.getZ(), "minecraft:entity.generic.eat", 1.0F, 1.0F);
			Compat.message(player, Component.literal(r.name + " feasts on the Golden Fish. Fully healed, morale " + r.morale + ".").withStyle(ChatFormatting.GOLD), true);
			ally.applyStats(false);
			return InteractionResult.CONSUME;
		}
		if (stack.is(Items.GOLD_NUGGET)) {
			r.morale = Math.min(100, r.morale + 5);
			ally.heal(2);
			consume(player, stack);
			Compat.playSound(level, ally.getX(), ally.getY(), ally.getZ(), "minecraft:entity.experience_orb.pickup", 0.6F, 1.4F);
			Compat.message(player, Component.literal(r.name + " pockets the nugget. Morale " + r.morale + " (" + Progression.moraleState(r.morale) + ").").withStyle(ChatFormatting.GOLD), true);
			return InteractionResult.CONSUME;
		}
		if (stack.is(Items.NAME_TAG) && Compat.customName(stack) != null) {
			String name = Compat.customName(stack).trim();
			if (!name.isEmpty() && name.length() <= 24) {
				r.name = name;
				r.epithet = "";
				ally.chronicle(Chronicle.Type.RENAMED, name, "", 0);
				consume(player, stack);
				ally.refreshNameplate();
				return InteractionResult.CONSUME;
			}
		}
		if (player.isShiftKeyDown() && !stack.isEmpty() && (Compat.isWeapon(stack) || Compat.isArmor(stack) || stack.is(Items.SHIELD))) {
			ItemStack old = ally.equip(stack);
			consume(player, stack);
			if (!old.isEmpty() && !player.getInventory().add(old)) {
				player.drop(old, false);
			}
			Compat.playSound(level, ally.getX(), ally.getY(), ally.getZ(), "minecraft:item.armor.equip_generic", 1.0F, 1.0F);
			Compat.message(player, Component.literal(r.name + " takes " + stack.getHoverName().getString() + ".").withStyle(ChatFormatting.GRAY), true);
			return InteractionResult.CONSUME;
		}
		openRoster(player, ally);
		return InteractionResult.CONSUME;
	}

	private static void consume(Player player, ItemStack stack) {
		if (!player.getAbilities().instabuild) {
			stack.shrink(1);
		}
	}

	// ---- band events ----

	public static void allyDowned(AllyEntity ally) {
		ServerPlayer owner = ally.owner();
		for (AllyEntity other : liveAllies(ally)) {
			if (other != ally && !other.record().has(Trait.FEARLESS)) {
				other.record().morale = Math.max(0, other.record().morale - 3);
			}
		}
		if (owner != null) {
			Compat.message(owner, Component.literal(ally.record().name + " is down! Crouch beside them to revive ("
					+ (ally.downedTicks() / 20) + "s).").withStyle(ChatFormatting.RED), true);
		}
	}

	public static void allyRevived(AllyEntity ally, String by) {
		ServerPlayer owner = ally.owner();
		if (owner != null) {
			Compat.message(owner, Component.literal(by + " pulled " + ally.record().name + " back up.").withStyle(ChatFormatting.GREEN), true);
		}
	}

	public static void allyLevelled(AllyEntity ally, int before) {
		ServerPlayer owner = ally.owner();
		if (owner != null) {
			int l = ally.record().level;
			String title = Progression.titleLevel(l) ? " and earned the title " + Progression.title(l) : "";
			Compat.message(owner, Component.literal(ally.record().name + " reached level " + l + title + ".").withStyle(ChatFormatting.AQUA), false);
		}
	}

	/** Permadeath, or a forced kill: the ally is gone and remembered. */
	public static void allyFell(AllyEntity ally, String cause) {
		UUID ownerId = ally.ownerId();
		if (ownerId == null) {
			return;
		}
		AllyRecord r = ally.record();
		ally.chronicle(Chronicle.Type.FELL, cause, "in " + Compat.dimensionName(ally.level()), 0);
		WarbandState s = state(ownerId);
		WarbandState.Fallen f = new WarbandState.Fallen();
		f.name = r.fullName();
		f.classId = r.classId;
		f.level = r.level;
		f.day = Compat.day(ally.level());
		f.cause = cause;
		f.headline = Chronicle.headline(r);
		f.chronicle = Chronicle.lines(r.chronicle);
		s.fallen.add(0, f);
		while (s.fallen.size() > 64) {
			s.fallen.remove(s.fallen.size() - 1);
		}
		forgetSeen(ally);
		Threat.forget(ally);
		for (AllyEntity other : liveAllies(ally)) {
			if (other != ally && !other.record().has(Trait.FEARLESS)) {
				other.record().morale = Math.max(0, other.record().morale - 20);
			}
		}
		ServerPlayer owner = ally.owner();
		if (owner != null) {
			Compat.message(owner, Component.literal(r.fullName() + " has fallen. Their name joins the Remembrance.").withStyle(ChatFormatting.DARK_RED), false);
		}
		s.dirty = true;
		save(ownerId);
	}

	/** Bled out without permadeath: carried back to camp to recover. */
	public static void retreat(AllyEntity ally) {
		UUID ownerId = ally.ownerId();
		if (ownerId == null) {
			ally.discard();
			return;
		}
		ally.setHealth(ally.getMaxHealth());
		long recoverAt = Compat.gameTime(ally.level()) + FellowshipConfig.INSTANCE.recoverySeconds * 20L;
		if (ally.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.POOF, ally.getX(), ally.getY() + 0.5, ally.getZ(), 16, 0.4, 0.3, 0.4, 0.02);
		}
		stow(ally, false, recoverAt);
		ServerPlayer owner = ally.owner();
		if (owner == null) {
			owner = server == null ? null : server.getPlayerList().getPlayer(ownerId);
		}
		if (owner != null) {
			Compat.message(owner, Component.literal(ally.record().name + " bled out and was carried back to camp. Ready again in "
					+ FellowshipConfig.INSTANCE.recoverySeconds / 60 + " min.").withStyle(ChatFormatting.GOLD), false);
		}
	}

	// ---- camp ----

	/** Moves an ally from the world to camp. Returns false when it could not be saved. */
	public static boolean stow(AllyEntity ally, boolean travelling, long recoverAt) {
		UUID ownerId = ally.ownerId();
		if (ownerId == null) {
			return false;
		}
		ally.ejectPassengers();
		ally.stopRiding();
		String saved = Compat.saveEntity(ally);
		if (saved.isEmpty()) {
			return false;
		}
		WarbandState s = state(ownerId);
		WarbandState.Stowed stowed = new WarbandState.Stowed();
		stowed.id = ally.getUUID().toString();
		stowed.entity = saved;
		stowed.record = ally.record().toJson();
		stowed.recoverAt = recoverAt;
		stowed.travelling = travelling;
		s.stowed.removeIf(x -> x.id.equals(stowed.id));
		s.stowed.add(stowed);
		forgetSeen(ally);
		Threat.forget(ally);
		ally.discard();
		s.dirty = true;
		return true;
	}

	/** Brings an ally back from camp next to the leader. */
	private static AllyEntity unstow(WarbandState.Stowed stowed, ServerPlayer owner) {
		ServerLevel level = Compat.level(owner);
		Entity e = Compat.loadEntity(stowed.entity, level);
		if (!(e instanceof AllyEntity ally)) {
			Constants.LOG.warn("Could not restore an ally from camp for {}", owner.getGameProfile().getName());
			return null;
		}
		if (level.getEntity(ally.getUUID()) != null) {
			return null;
		}
		Vec3 spot = safeSpotNear(level, owner.position(), 3);
		Compat.moveTo(ally, spot.x, spot.y, spot.z, owner.getYRot());
		ally.setRecord(AllyRecord.fromJson(stowed.record));
		ally.record().order = state(owner.getUUID()).order;
		level.addFreshEntity(ally);
		level.sendParticles(ParticleTypes.POOF, spot.x, spot.y + 0.5, spot.z, 10, 0.3, 0.4, 0.3, 0.02);
		track(ally);
		return ally;
	}

	public static int summon(ServerPlayer owner, String which) {
		WarbandState s = state(owner.getUUID());
		long now = Compat.gameTime(owner.level());
		int count = 0;
		Iterator<WarbandState.Stowed> it = s.stowed.iterator();
		List<WarbandState.Stowed> toSummon = new ArrayList<>();
		while (it.hasNext()) {
			WarbandState.Stowed st = it.next();
			AllyRecord r = AllyRecord.fromJson(st.record);
			boolean match = which == null || which.equalsIgnoreCase("all") || r.name.equalsIgnoreCase(which) || st.id.equals(which);
			if (!match) {
				continue;
			}
			if (st.recoverAt > now) {
				Compat.message(owner, Component.literal(r.name + " is still recovering (" + ((st.recoverAt - now) / 20 + 59) / 60 + " min).").withStyle(ChatFormatting.GOLD), true);
				continue;
			}
			toSummon.add(st);
			it.remove();
		}
		for (WarbandState.Stowed st : toSummon) {
			if (unstow(st, owner) != null) {
				count++;
			} else {
				s.stowed.add(st);
			}
		}
		s.dirty = true;
		if (count > 0) {
			Compat.playSound(owner.level(), owner.getX(), owner.getY(), owner.getZ(), "minecraft:item.goat_horn.sound.5", 0.6F, 1.2F);
			pushRoster(owner);
		}
		return count;
	}

	/** Sends allies to camp: "all", a name, or an ally UUID. */
	public static int stowByName(ServerPlayer owner, String which) {
		int n = 0;
		for (AllyEntity a : liveAllies(owner.getUUID())) {
			if (which.equalsIgnoreCase("all") || a.record().name.equalsIgnoreCase(which) || a.getUUID().toString().equals(which)) {
				if (a.isDowned()) {
					continue;
				}
				if (stow(a, false, 0)) {
					n++;
				}
			}
		}
		pushRoster(owner);
		return n;
	}

	/** Releases an ally from service for good. */
	public static boolean dismiss(ServerPlayer owner, String which) {
		WarbandState s = state(owner.getUUID());
		for (AllyEntity a : liveAllies(owner.getUUID())) {
			if (a.record().name.equalsIgnoreCase(which) || a.getUUID().toString().equals(which)) {
				forgetSeen(a);
				Threat.forget(a);
				if (a.level() instanceof ServerLevel level) {
					level.sendParticles(ParticleTypes.POOF, a.getX(), a.getY() + 0.5, a.getZ(), 12, 0.3, 0.4, 0.3, 0.02);
				}
				Compat.message(owner, Component.literal(a.record().fullName() + " leaves your service.").withStyle(ChatFormatting.GRAY), false);
				a.discard();
				pushRoster(owner);
				return true;
			}
		}
		boolean removed = s.stowed.removeIf(st -> st.id.equals(which) || AllyRecord.fromJson(st.record).name.equalsIgnoreCase(which));
		if (!removed) {
			removed = s.seen.entrySet().removeIf(e -> e.getKey().equals(which) || e.getValue().name.equalsIgnoreCase(which));
		}
		if (removed) {
			s.dirty = true;
			pushRoster(owner);
		}
		return removed;
	}

	// ---- orders ----

	public static Order orderFor(AllyEntity ally) {
		UUID owner = ally.ownerId();
		if (owner == null) {
			return Order.HOLD;
		}
		WarbandState s = STATES.get(owner);
		return s != null ? s.orderValue() : ally.record().orderValue();
	}

	public static boolean fallingBack(AllyEntity ally) {
		UUID owner = ally.ownerId();
		WarbandState s = owner == null ? null : STATES.get(owner);
		return s != null && s.orderValue() == Order.FALL_BACK && Compat.gameTime(ally.level()) < s.fallBackUntil;
	}

	public static LivingEntity focusFor(AllyEntity ally) {
		UUID owner = ally.ownerId();
		WarbandState s = owner == null ? null : STATES.get(owner);
		if (s == null || s.orderValue() != Order.FOCUS || s.focus == null || !(ally.level() instanceof ServerLevel level)) {
			return null;
		}
		Entity e = level.getEntity(s.focus);
		return e instanceof LivingEntity living && living.isAlive() ? living : null;
	}

	public static Vec3 slot(AllyEntity ally) {
		UUID owner = ally.ownerId();
		Map<UUID, Vec3> slots = owner == null ? null : SLOTS.get(owner);
		return slots == null ? null : slots.get(ally.getUUID());
	}

	public static void order(ServerPlayer owner, Order order, Entity focus) {
		WarbandState s = state(owner.getUUID());
		List<AllyEntity> band = liveAllies(owner.getUUID());
		if (order == Order.FOCUS) {
			if (!(focus instanceof LivingEntity living) || !living.isAlive() || focus instanceof AllyEntity a && a.isOwnedBy(owner) || focus == owner) {
				Compat.message(owner, Component.literal("Look at an enemy to focus it.").withStyle(ChatFormatting.YELLOW), true);
				return;
			}
			s.focus = focus.getUUID();
		}
		if (order == Order.GUARD_BANNER && s.banner == null) {
			Compat.message(owner, Component.literal("Plant a Warbanner first.").withStyle(ChatFormatting.YELLOW), true);
			return;
		}
		if (order == Order.FALL_BACK) {
			s.fallBackUntil = Compat.gameTime(owner.level()) + 160;
		}
		s.order = order.id();
		s.dirty = true;
		for (AllyEntity a : band) {
			a.record().order = order.id();
			if (order == Order.HOLD) {
				a.setHoldPos(a.blockPosition());
			}
			if (order == Order.FALL_BACK) {
				a.setTarget(null);
			}
			if (order == Order.FOCUS && focus instanceof LivingEntity living) {
				a.setTarget(living);
			}
		}
		Compat.message(owner, Component.literal("Warband: " + order.label() + (order == Order.FOCUS ? " " + focus.getName().getString() : "")).withStyle(ChatFormatting.GOLD), true);
		Compat.playSound(owner.level(), owner.getX(), owner.getY(), owner.getZ(), "minecraft:item.goat_horn.sound.0", 0.35F, 1.4F);
		pushParty(owner, true);
	}

	public static void formation(ServerPlayer owner, Formation formation) {
		WarbandState s = state(owner.getUUID());
		Formation f = formation == null ? s.formationValue().next() : formation;
		s.formation = f.id();
		s.dirty = true;
		Compat.message(owner, Component.literal("Formation: " + f.label()).withStyle(ChatFormatting.GOLD), true);
		pushParty(owner, true);
	}

	public static void plantBanner(ServerPlayer owner, BlockPos pos, float yaw) {
		ServerLevel level = Compat.level(owner);
		WarbandState s = state(owner.getUUID());
		removeBanner(s);
		if (!Compat.placeWarbanner(level, pos, yaw)) {
			Compat.message(owner, Component.literal("The Warbanner needs open ground.").withStyle(ChatFormatting.YELLOW), true);
			return;
		}
		WarbandState.Banner b = new WarbandState.Banner();
		b.dimension = Compat.dimensionId(level);
		b.x = pos.getX();
		b.y = pos.getY();
		b.z = pos.getZ();
		b.yaw = yaw;
		s.banner = b;
		s.order = Order.GUARD_BANNER.id();
		s.dirty = true;
		for (int i = 0; i < 48; i++) {
			level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + i * 0.35, pos.getZ() + 0.5, 1, 0.05, 0.05, 0.05, 0.01);
		}
		AllyCombat.ring(level, Vec3.atBottomCenterOf(pos), 2.5, Compat.dust(0xF2C94C, 1.6F), 32);
		AllyCombat.ring(level, Vec3.atBottomCenterOf(pos), 4.0, Compat.dust(0xF2C94C, 1.2F), 40);
		level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 40, 0.6, 1.0, 0.6, 0.4);
		Compat.playSound(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, "minecraft:item.goat_horn.sound.0", 1.0F, 1.0F);
		Compat.playSound(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, "minecraft:item.trident.thunder", 0.25F, 1.6F);
		computeSlots(owner);
		for (AllyEntity a : liveAllies(owner.getUUID())) {
			a.record().order = Order.GUARD_BANNER.id();
			a.setTarget(null);
			Vec3 slot = slot(a);
			if (slot != null && a.level() == level && a.position().distanceToSqr(slot) > 24 * 24) {
				Vec3 spot = safeSpotNear(level, slot, 1);
				Compat.teleport(a, spot.x, spot.y, spot.z);
			}
		}
		Compat.message(owner, Component.literal("Warbanner planted. The band holds this ground.").withStyle(ChatFormatting.GOLD), true);
		pushParty(owner, true);
	}

	public static boolean liftBanner(ServerPlayer owner) {
		WarbandState s = state(owner.getUUID());
		if (s.banner == null) {
			return false;
		}
		removeBanner(s);
		s.banner = null;
		if (s.orderValue() == Order.GUARD_BANNER) {
			s.order = Order.FOLLOW.id();
		}
		s.dirty = true;
		Compat.message(owner, Component.literal("Warbanner lifted. The band follows you.").withStyle(ChatFormatting.GOLD), true);
		pushParty(owner, true);
		return true;
	}

	private static void removeBanner(WarbandState s) {
		if (s.banner == null || server == null) {
			return;
		}
		ServerLevel level = Compat.levelById(server, s.banner.dimension);
		BlockPos pos = new BlockPos(s.banner.x, s.banner.y, s.banner.z);
		if (level != null && level.isLoaded(pos) && Compat.isWarbanner(level, pos)) {
			Compat.removeBlock(level, pos);
		}
	}

	private static void computeSlots(ServerPlayer owner) {
		WarbandState s = state(owner.getUUID());
		Order order = s.orderValue();
		double ax;
		double ay;
		double az;
		float yaw;
		String dim;
		if (order == Order.GUARD_BANNER && s.banner != null) {
			ax = s.banner.x + 0.5;
			ay = s.banner.y;
			az = s.banner.z + 0.5;
			yaw = s.banner.yaw;
			dim = s.banner.dimension;
		} else {
			double[] last = ANCHORS.get(owner.getUUID());
			yaw = last == null ? owner.getYRot() : (float) last[3];
			if (last == null || Math.hypot(owner.getX() - last[0], owner.getZ() - last[2]) > 2.0) {
				if (last != null) {
					yaw = (float) Math.toDegrees(Math.atan2(-(owner.getX() - last[0]), owner.getZ() - last[2]));
				}
				ANCHORS.put(owner.getUUID(), new double[]{owner.getX(), owner.getY(), owner.getZ(), yaw});
			}
			ax = owner.getX();
			ay = owner.getY();
			az = owner.getZ();
			dim = Compat.dimensionId(owner.level());
		}
		List<Formation.Member<UUID>> members = new ArrayList<>();
		Map<UUID, AllyEntity> byId = new HashMap<>();
		for (AllyEntity a : liveAllies(owner.getUUID())) {
			if (!a.isDowned() && Compat.dimensionId(a.level()).equals(dim)) {
				members.add(new Formation.Member<>(a.getUUID(), a.role()));
				byId.put(a.getUUID(), a);
			}
		}
		List<UUID> ordered = Formation.order(members);
		List<tn.nightbeam.better_with_gold.core.Role> roles = new ArrayList<>();
		for (UUID id : ordered) {
			roles.add(byId.get(id).role());
		}
		List<Formation.Offset> offsets = s.formationValue().offsets(roles);
		Map<UUID, Vec3> slots = new HashMap<>();
		for (int i = 0; i < ordered.size(); i++) {
			double[] w = offsets.get(i).toWorld(yaw);
			slots.put(ordered.get(i), new Vec3(ax + w[0], ay, az + w[1]));
		}
		SLOTS.put(owner.getUUID(), slots);
	}

	// ---- leader lifecycle ----

	public static void join(ServerPlayer player) {
		WarbandState s = state(player.getUUID());
		s.ownerName = player.getGameProfile().getName();
		PENDING.add(new Pending(player.getUUID(), Compat.gameTime(player.level()) + 30));
	}

	public static void leave(ServerPlayer player) {
		WarbandState s = state(player.getUUID());
		for (AllyEntity a : liveAllies(player.getUUID())) {
			if (a.level() == player.level() && a.distanceToSqr(player) < 64 * 64 && a.record().orderValue().travels() && !a.isDowned()) {
				stow(a, true, 0);
			}
		}
		save(player.getUUID());
		STATES.remove(player.getUUID());
		SLOTS.remove(player.getUUID());
		ANCHORS.remove(player.getUUID());
		if (s.banner == null) {
			return;
		}
		// The banner stays planted while the leader is away.
	}

	private static void restoreTravellers(ServerPlayer player) {
		WarbandState s = state(player.getUUID());
		List<WarbandState.Stowed> back = new ArrayList<>();
		for (WarbandState.Stowed st : s.stowed) {
			if (st.travelling) {
				back.add(st);
			}
		}
		for (WarbandState.Stowed st : back) {
			s.stowed.remove(st);
			if (unstow(st, player) == null) {
				s.stowed.add(st);
			}
		}
		s.dirty = true;
		pushRoster(player);
	}

	/** Moves an ally that ended up in another dimension (or too far) to its leader. */
	private static void regroup(AllyEntity ally, ServerPlayer owner) {
		if (ally.level() == owner.level()) {
			Vec3 spot = safeSpotNear(Compat.level(owner), owner.position(), 3);
			Compat.teleport(ally, spot.x, spot.y, spot.z);
			ally.getNavigation().stop();
			return;
		}
		if (stow(ally, true, 0)) {
			WarbandState s = state(owner.getUUID());
			WarbandState.Stowed st = s.stowed.get(s.stowed.size() - 1);
			s.stowed.remove(st);
			if (unstow(st, owner) == null) {
				s.stowed.add(st);
			}
		}
	}

	/** Loads the chunks where missing allies were last seen and brings them to the leader. */
	public static int recall(ServerPlayer owner) {
		WarbandState s = state(owner.getUUID());
		int n = 0;
		for (AllyEntity a : liveAllies(owner.getUUID())) {
			if (!a.isDowned() && (a.level() != owner.level() || a.distanceToSqr(owner) > 16 * 16)) {
				regroup(a, owner);
				n++;
			}
		}
		for (Map.Entry<String, WarbandState.Seen> e : new ArrayList<>(s.seen.entrySet())) {
			UUID id;
			try {
				id = UUID.fromString(e.getKey());
			} catch (IllegalArgumentException ex) {
				continue;
			}
			Map<UUID, AllyEntity> live = LIVE.get(owner.getUUID());
			if (live != null && live.containsKey(id)) {
				continue;
			}
			startRecall(owner, id, e.getValue());
			n++;
		}
		n += summon(owner, "all");
		return n;
	}

	private static void startRecall(ServerPlayer owner, UUID ally, WarbandState.Seen seen) {
		if (server == null) {
			return;
		}
		ServerLevel level = Compat.levelById(server, seen.dimension);
		if (level == null) {
			return;
		}
		for (RecallJob job : RECALLS) {
			if (job.ally().equals(ally)) {
				return;
			}
		}
		int cx = ((int) Math.floor(seen.x)) >> 4;
		int cz = ((int) Math.floor(seen.z)) >> 4;
		Compat.forceChunk(level, cx, cz, true);
		RECALLS.add(new RecallJob(owner.getUUID(), ally, level, cx, cz, Compat.gameTime(level) + 100));
	}

	private static void tickRecalls() {
		Iterator<RecallJob> it = RECALLS.iterator();
		while (it.hasNext()) {
			RecallJob job = it.next();
			Entity e = job.level().getEntity(job.ally());
			ServerPlayer owner = server.getPlayerList().getPlayer(job.owner());
			boolean expired = Compat.gameTime(job.level()) > job.until();
			if (e instanceof AllyEntity ally && owner != null) {
				regroup(ally, owner);
			} else if (!expired && owner != null) {
				continue;
			} else if (owner != null) {
				WarbandState s = state(job.owner());
				WarbandState.Seen seen = s.seen.remove(job.ally().toString());
				s.dirty = true;
				if (seen != null) {
					Compat.message(owner, Component.literal(seen.name + " could not be found where they were last seen and was struck from the roll.")
							.withStyle(ChatFormatting.GRAY), false);
				}
			}
			Compat.forceChunk(job.level(), job.chunkX(), job.chunkZ(), false);
			it.remove();
		}
	}

	private static Vec3 safeSpotNear(ServerLevel level, Vec3 centre, int spread) {
		for (int attempt = 0; attempt < 10; attempt++) {
			int x = (int) Math.floor(centre.x) + RANDOM.nextInt(spread * 2 + 1) - spread;
			int z = (int) Math.floor(centre.z) + RANDOM.nextInt(spread * 2 + 1) - spread;
			for (int dy = 2; dy >= -3; dy--) {
				BlockPos pos = new BlockPos(x, (int) Math.floor(centre.y) + dy, z);
				if (level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
						&& level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)) {
					return new Vec3(x + 0.5, pos.getY(), z + 0.5);
				}
			}
		}
		return centre;
	}

	// ---- tick ----

	public static void tick(MinecraftServer s) {
		if (server == null) {
			return;
		}
		int t = s.getTickCount();
		if (!PENDING.isEmpty()) {
			Iterator<Pending> it = PENDING.iterator();
			while (it.hasNext()) {
				Pending p = it.next();
				ServerPlayer player = s.getPlayerList().getPlayer(p.owner());
				if (player == null) {
					it.remove();
				} else if (Compat.gameTime(player.level()) >= p.at()) {
					restoreTravellers(player);
					it.remove();
				}
			}
		}
		if (!RECALLS.isEmpty()) {
			tickRecalls();
		}
		if (t % 10 == 0) {
			for (ServerPlayer player : s.getPlayerList().getPlayers()) {
				if (LIVE.containsKey(player.getUUID()) || STATES.containsKey(player.getUUID())) {
					computeSlots(player);
					pushParty(player, false);
				}
			}
		}
		if (t % 20 == 5) {
			for (ServerPlayer player : s.getPlayerList().getPlayers()) {
				maintain(player);
			}
		}
		if (t % 6000 == 0) {
			for (Map.Entry<UUID, WarbandState> e : new ArrayList<>(STATES.entrySet())) {
				if (e.getValue().dirty) {
					save(e.getKey());
				}
			}
		}
	}

	private static void maintain(ServerPlayer owner) {
		WarbandState s = STATES.get(owner.getUUID());
		if (s == null) {
			return;
		}
		if (s.orderValue() == Order.FOCUS && focusFor(firstAlly(owner)) == null) {
			s.order = Order.FOLLOW.id();
			s.focus = null;
			for (AllyEntity a : liveAllies(owner.getUUID())) {
				a.record().order = s.order;
			}
		}
		if (s.banner != null && server != null) {
			ServerLevel level = Compat.levelById(server, s.banner.dimension);
			BlockPos pos = new BlockPos(s.banner.x, s.banner.y, s.banner.z);
			if (level != null && level.isLoaded(pos) && !Compat.isWarbanner(level, pos)) {
				s.banner = null;
				if (s.orderValue() == Order.GUARD_BANNER) {
					s.order = Order.FOLLOW.id();
				}
				Compat.message(owner, Component.literal("Your Warbanner was taken down. The band follows you.").withStyle(ChatFormatting.YELLOW), true);
			} else if (level != null && level.isLoaded(pos) && owner.tickCount % 40 < 20) {
				level.sendParticles(Compat.dust(0xF2C94C, 1.0F), pos.getX() + 0.5, pos.getY() + 2.2, pos.getZ() + 0.5, 3, 0.15, 0.4, 0.15, 0);
			}
		}
		int teleport = FellowshipConfig.INSTANCE.teleportDistance;
		for (AllyEntity a : liveAllies(owner.getUUID())) {
			if (a.isDowned() || !s.orderValue().travels()) {
				continue;
			}
			if (a.level() != owner.level() || a.distanceToSqr(owner) > (double) teleport * teleport) {
				if (owner.onGround() || owner.isPassenger()) {
					regroup(a, owner);
				}
			}
		}
		if (s.orderValue().travels() && owner.tickCount % 200 < 20) {
			Map<UUID, AllyEntity> live = LIVE.getOrDefault(owner.getUUID(), Map.of());
			for (Map.Entry<String, WarbandState.Seen> e : new ArrayList<>(s.seen.entrySet())) {
				try {
					UUID id = UUID.fromString(e.getKey());
					if (!live.containsKey(id)) {
						startRecall(owner, id, e.getValue());
					}
				} catch (IllegalArgumentException ignored) {
					s.seen.remove(e.getKey());
				}
			}
		}
	}

	private static AllyEntity firstAlly(ServerPlayer owner) {
		List<AllyEntity> band = liveAllies(owner.getUUID());
		return band.isEmpty() ? null : band.get(0);
	}

	// ---- client sync ----

	public static void pushParty(ServerPlayer owner, boolean force) {
		WarbandState s = state(owner.getUUID());
		JsonObject msg = Net.message(Net.PARTY);
		msg.addProperty("order", s.orderValue().label());
		msg.addProperty("formation", s.formationValue().label());
		msg.addProperty("banner", s.banner != null);
		msg.addProperty("cap", FellowshipConfig.INSTANCE.partySize);
		msg.addProperty("roster", s.rosterSize());
		msg.addProperty("camp", s.stowed.size());
		JsonArray allies = new JsonArray();
		List<AllyEntity> band = new ArrayList<>(liveAllies(owner.getUUID()));
		band.sort((a, b) -> a.role().rank() != b.role().rank() ? Integer.compare(a.role().rank(), b.role().rank()) : a.getUUID().compareTo(b.getUUID()));
		for (AllyEntity a : band) {
			JsonObject o = new JsonObject();
			o.addProperty("id", a.getId());
			o.addProperty("n", a.record().name);
			o.addProperty("c", a.record().classId);
			o.addProperty("cn", a.allyClass().name);
			o.addProperty("col", a.allyClass().colorRgb());
			o.addProperty("lv", a.record().level);
			o.addProperty("hp", Math.round(a.getHealth() * 10) / 10.0);
			o.addProperty("mhp", Math.round(a.getMaxHealth() * 10) / 10.0);
			o.addProperty("d", a.isDowned() ? (a.downedTicks() + 19) / 20 : 0);
			o.addProperty("r", a.isDowned() ? Math.round(a.reviveFraction() * 100) : 0);
			o.addProperty("far", a.level() != owner.level() || a.distanceToSqr(owner) > 48 * 48);
			allies.add(o);
		}
		msg.add("a", allies);
		String text = Net.write(msg);
		if (force || !text.equals(s.lastParty)) {
			s.lastParty = text;
			Services.PLATFORM.sendToPlayer(owner, text);
		}
		Long watch = ROSTER_WATCH.get(owner.getUUID());
		if (watch != null && Compat.gameTime(owner.level()) - watch < 40 && owner.tickCount % 40 < 10) {
			sendRoster(owner);
		}
	}

	public static void watchRoster(ServerPlayer owner) {
		ROSTER_WATCH.put(owner.getUUID(), Compat.gameTime(owner.level()));
	}

	private static void pushRoster(ServerPlayer owner) {
		Long watch = ROSTER_WATCH.get(owner.getUUID());
		if (watch != null && Compat.gameTime(owner.level()) - watch < 200) {
			sendRoster(owner);
		}
		pushParty(owner, true);
	}

	public static void openRoster(ServerPlayer owner, AllyEntity focus) {
		watchRoster(owner);
		sendRoster(owner);
		JsonObject msg = Net.message(Net.OPEN);
		msg.addProperty("id", focus == null ? "" : focus.getUUID().toString());
		Services.PLATFORM.sendToPlayer(owner, Net.write(msg));
	}

	public static void sendRoster(ServerPlayer owner) {
		WarbandState s = state(owner.getUUID());
		long now = Compat.gameTime(owner.level());
		JsonObject msg = Net.message(Net.ROSTER);
		msg.addProperty("cap", FellowshipConfig.INSTANCE.partySize);
		msg.addProperty("order", s.orderValue().label());
		msg.addProperty("formation", s.formationValue().label());
		msg.addProperty("permadeath", FellowshipConfig.INSTANCE.permadeath);
		msg.addProperty("regalia", regaliaPieces(owner));
		JsonArray allies = new JsonArray();
		Map<UUID, AllyEntity> live = LIVE.getOrDefault(owner.getUUID(), Map.of());
		for (AllyEntity a : liveAllies(owner.getUUID())) {
			String where = a.level() != owner.level() ? Compat.dimensionName(a.level())
					: (int) Math.sqrt(a.distanceToSqr(owner)) + " blocks away";
			allies.add(rosterEntry(a.getUUID().toString(), a.record(), a.isDowned() ? "downed" : "active", where, a.getHealth(), a.getMaxHealth()));
		}
		for (Map.Entry<String, WarbandState.Seen> e : s.seen.entrySet()) {
			try {
				if (live.containsKey(UUID.fromString(e.getKey()))) {
					continue;
				}
			} catch (IllegalArgumentException ex) {
				continue;
			}
			WarbandState.Seen seen = e.getValue();
			AllyRecord r = new AllyRecord();
			r.name = seen.name;
			r.classId = seen.classId;
			r.level = seen.level;
			allies.add(rosterEntry(e.getKey(), r, "away", "last seen at " + (int) seen.x + " " + (int) seen.y + " " + (int) seen.z, 0, 0));
		}
		for (WarbandState.Stowed st : s.stowed) {
			AllyRecord r = AllyRecord.fromJson(st.record);
			String state = st.recoverAt > now ? "recovering" : "camp";
			String where = st.recoverAt > now ? "recovering, " + ((st.recoverAt - now) / 20 + 59) / 60 + " min" : "at camp";
			double max = r.allyClass().health(r.level) * Progression.healthMultiplier(r.traitList());
			allies.add(rosterEntry(st.id, r, state, where, max, max));
		}
		msg.add("allies", allies);
		JsonArray fallen = new JsonArray();
		for (WarbandState.Fallen f : s.fallen) {
			JsonObject o = new JsonObject();
			o.addProperty("name", f.name);
			o.addProperty("cls", ClassCatalog.get(f.classId).name);
			o.addProperty("col", ClassCatalog.get(f.classId).colorRgb());
			o.addProperty("lv", f.level);
			o.addProperty("day", f.day);
			o.addProperty("cause", f.cause);
			o.addProperty("headline", f.headline);
			JsonArray lines = new JsonArray();
			f.chronicle.forEach(lines::add);
			o.add("lines", lines);
			fallen.add(o);
		}
		msg.add("fallen", fallen);
		Services.PLATFORM.sendToPlayer(owner, Net.write(msg));
	}

	private static JsonObject rosterEntry(String id, AllyRecord r, String state, String where, double hp, double maxHp) {
		AllyClass c = r.allyClass();
		JsonObject o = new JsonObject();
		o.addProperty("id", id);
		o.addProperty("name", r.name);
		o.addProperty("epithet", r.epithet);
		o.addProperty("cls", c.name);
		o.addProperty("role", c.role().label());
		o.addProperty("col", c.colorRgb());
		o.addProperty("ability", c.abilityName);
		o.addProperty("abilityText", c.abilityDescription);
		o.addProperty("lv", r.level);
		o.addProperty("xp", r.xp);
		o.addProperty("xpNext", Progression.xpToNext(r.level));
		o.addProperty("title", Progression.title(r.level));
		o.addProperty("morale", r.morale);
		o.addProperty("moraleState", Progression.moraleState(r.morale));
		o.addProperty("hp", Math.round(hp * 10) / 10.0);
		o.addProperty("mhp", Math.round(maxHp * 10) / 10.0);
		o.addProperty("state", state);
		o.addProperty("where", where);
		o.addProperty("headline", Chronicle.headline(r));
		o.addProperty("sworn", r.swornDay);
		JsonArray traits = new JsonArray();
		for (Trait t : r.traitList()) {
			JsonObject to = new JsonObject();
			to.addProperty("n", t.label());
			to.addProperty("d", t.description());
			traits.add(to);
		}
		o.add("traits", traits);
		JsonObject stats = new JsonObject();
		stats.addProperty("kills", r.stats.kills);
		stats.addProperty("bosses", r.stats.bossKills);
		stats.addProperty("bossFights", r.stats.bossFights);
		stats.addProperty("downs", r.stats.downs);
		stats.addProperty("revives", r.stats.revivesGiven);
		stats.addProperty("healing", Math.round(r.stats.healing));
		o.add("stats", stats);
		JsonArray lines = new JsonArray();
		for (String line : Chronicle.lines(r.chronicle)) {
			lines.add(line);
		}
		o.add("lines", lines);
		return o;
	}

	public static String describe(UUID owner) {
		WarbandState s = state(owner);
		return String.format(Locale.ROOT, "%d in the world, %d at camp, %d fallen; order %s, formation %s%s",
				s.seen.size(), s.stowed.size(), s.fallen.size(), s.orderValue().label(), s.formationValue().label(), s.banner == null ? "" : ", banner planted");
	}
}
