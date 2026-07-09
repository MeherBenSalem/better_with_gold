package tn.nightbeam.better_with_gold.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;

public class GreaterGoldenHammerLivingEntityIsHitWithToolProcedure {
	public static void execute(LevelAccessor world, Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null || !(sourceentity instanceof Player player)) {
			return;
		}
		entity.hurt(player.damageSources().playerAttack(player), (float) sourceentity.fallDistance);
	}
}
