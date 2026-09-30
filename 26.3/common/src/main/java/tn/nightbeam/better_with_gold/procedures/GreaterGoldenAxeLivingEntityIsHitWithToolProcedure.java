package tn.nightbeam.better_with_gold.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class GreaterGoldenAxeLivingEntityIsHitWithToolProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null || !(world instanceof Level level)) {
			return;
		}
		float damage = 0;
		if (entity instanceof LivingEntity living) {
			damage = (float) ((living.getMaxHealth() - living.getHealth()) / 2d);
		}
		entity.hurt(level.damageSources().generic(), damage);
	}
}
