package tn.nightbeam.better_with_gold.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

public class GoldenScytheLivingEntityIsHitWithToolProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null || !(sourceentity instanceof Player player)) {
			return;
		}
		final Vec3 center = new Vec3(x, y, z);
		List<Entity> entities = world.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(5 / 2d), e -> true).stream()
				.sorted(Comparator.comparingDouble(candidate -> candidate.distanceToSqr(center)))
				.toList();
		for (Entity target : entities) {
			if (target != sourceentity && target != entity) {
				target.hurt(player.damageSources().playerAttack(player), 5);
			}
		}
	}
}
