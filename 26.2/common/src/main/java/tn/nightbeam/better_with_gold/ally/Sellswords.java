package tn.nightbeam.better_with_gold.ally;

import tn.nightbeam.better_with_gold.compat.Compat;
import tn.nightbeam.better_with_gold.core.AllyClass;
import tn.nightbeam.better_with_gold.core.ClassCatalog;
import tn.nightbeam.better_with_gold.core.FellowshipConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/**
 * Sellswords for hire turn up near village bells. Every few minutes per player, if a village is close and it has
 * fewer than the configured number waiting, one may arrive. Their level grows with the age of the world (and with
 * the leader's RPG level when RPG Attribute System is installed).
 */
public final class Sellswords {
	private static final Map<UUID, Long> NEXT = new HashMap<>();
	private static final Random RANDOM = new Random();

	private Sellswords() {
	}

	public static void forget(ServerPlayer player) {
		NEXT.remove(player.getUUID());
	}

	public static void tick(MinecraftServer server) {
		FellowshipConfig cfg = FellowshipConfig.INSTANCE;
		if (!cfg.sellswordsSpawn || server.getTickCount() % 40 != 7) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player.isSpectator()) {
				continue;
			}
			ServerLevel level = Compat.level(player);
			long now = Compat.gameTime(level);
			Long next = NEXT.get(player.getUUID());
			if (next == null) {
				NEXT.put(player.getUUID(), now + cfg.sellswordIntervalSeconds * 10L);
				continue;
			}
			if (now < next) {
				continue;
			}
			NEXT.put(player.getUUID(), now + cfg.sellswordIntervalSeconds * 20L);
			if (RANDOM.nextDouble() > cfg.sellswordChance) {
				continue;
			}
			trySpawn(level, player);
		}
	}

	/** Places one sellsword near the closest village bell. Returns the new sellsword or null. */
	public static AllyEntity trySpawn(ServerLevel level, ServerPlayer player) {
		FellowshipConfig cfg = FellowshipConfig.INSTANCE;
		Optional<BlockPos> centre = Compat.villageCentre(level, player.blockPosition(), 96);
		if (centre.isEmpty()) {
			return null;
		}
		BlockPos bell = centre.get();
		List<AllyEntity> waiting = level.getEntitiesOfClass(AllyEntity.class, new AABB(bell).inflate(64), a -> !a.isHired());
		if (waiting.size() >= cfg.maxSellswordsNearVillage || Warbands.liveCount() + waiting.size() >= cfg.worldAllyCap) {
			return null;
		}
		for (int attempt = 0; attempt < 12; attempt++) {
			double angle = RANDOM.nextDouble() * Math.PI * 2;
			int dist = 4 + RANDOM.nextInt(8);
			BlockPos top = Compat.surface(level, bell.getX() + (int) Math.round(Math.cos(angle) * dist), bell.getZ() + (int) Math.round(Math.sin(angle) * dist));
			if (!level.getBlockState(top).isAir() || !level.getBlockState(top.above()).isAir() || !level.getFluidState(top.below()).isEmpty()) {
				continue;
			}
			List<AllyClass> classes = ClassCatalog.enabled();
			AllyClass c = classes.get(RANDOM.nextInt(classes.size()));
			return Warbands.spawn(level, c, rollLevel(level, player), Vec3.atBottomCenterOf(top), null);
		}
		return null;
	}

	public static int rollLevel(ServerLevel level, ServerPlayer player) {
		int base = 1 + RANDOM.nextInt(3) + (int) Math.min(8, Compat.day(level) / 8);
		int rpg = RasBridge.level(player);
		if (rpg > 0) {
			base = Math.max(base, rpg / 2 + RANDOM.nextInt(3));
		}
		return Math.max(1, Math.min(FellowshipConfig.INSTANCE.levelCap, base));
	}
}
