package tn.nightbeam.better_with_gold.item;

import tn.nightbeam.better_with_gold.procedures.GoldenFishPlayerFinishesUsingItemProcedure;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

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

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, tooltip, flag);
		tooltip.accept(Component.translatable("item.better_with_gold.golden_fish.description_0"));
	}
}
