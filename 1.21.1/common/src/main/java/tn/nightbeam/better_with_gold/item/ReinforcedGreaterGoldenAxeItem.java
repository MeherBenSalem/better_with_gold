package tn.nightbeam.better_with_gold.item;

import tn.nightbeam.better_with_gold.procedures.GreaterGoldenAxeLivingEntityIsHitWithToolProcedure;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ReinforcedGreaterGoldenAxeItem extends AxeItem {

	public ReinforcedGreaterGoldenAxeItem() {
		super(ModTiers.REINFORCED_GREATER_GOLDEN_AXE, new Properties());
	}

	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		boolean retval = super.hurtEnemy(stack, target, attacker);
		GreaterGoldenAxeLivingEntityIsHitWithToolProcedure.execute(target.level(), target);
		return retval;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);
		tooltip.add(Component.translatable("item.better_with_gold.reinforced_greater_golden_axe.description_0"));
	}
}
