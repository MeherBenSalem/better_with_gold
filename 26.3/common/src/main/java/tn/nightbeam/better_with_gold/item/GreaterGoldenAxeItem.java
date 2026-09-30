package tn.nightbeam.better_with_gold.item;

import tn.nightbeam.better_with_gold.procedures.GreaterGoldenAxeLivingEntityIsHitWithToolProcedure;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class GreaterGoldenAxeItem extends Item {

	public GreaterGoldenAxeItem() {
		super(new Properties().axe(ModToolMaterials.GREATER_GOLDEN_AXE, 14f, -3f));
	}

	@Override
	public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		super.hurtEnemy(stack, target, attacker);
		GreaterGoldenAxeLivingEntityIsHitWithToolProcedure.execute(target.level(), target);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, tooltip, flag);
		tooltip.accept(Component.translatable("item.better_with_gold.greater_golden_axe.description_0"));
	}
}
