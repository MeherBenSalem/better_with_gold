package tn.nightbeam.better_with_gold.item;

import tn.nightbeam.better_with_gold.procedures.GoldenFishPlayerFinishesUsingItemProcedure;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class GoldenFishItem extends Item {

	public GoldenFishItem(Item.Properties properties) {
		super(properties.stacksTo(64).rarity(Rarity.COMMON)
				.food(new FoodProperties.Builder().nutrition(10).saturationModifier(6f).alwaysEdible().build()));
	}

	@Override
	public ItemStack finishUsingItem(ItemStack itemstack, Level world, LivingEntity entity) {
		ItemStack retval = super.finishUsingItem(itemstack, world, entity);
		GoldenFishPlayerFinishesUsingItemProcedure.execute(entity);
		return retval;
	}
}
