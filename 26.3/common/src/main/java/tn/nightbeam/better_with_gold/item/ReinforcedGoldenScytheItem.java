package tn.nightbeam.better_with_gold.item;

import tn.nightbeam.better_with_gold.procedures.GoldenScytheLivingEntityIsHitWithToolProcedure;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;

public class ReinforcedGoldenScytheItem extends Item {

	public ReinforcedGoldenScytheItem() {
		super(ModToolMaterials.REINFORCED_GOLDEN_SCYTHE.applyToolProperties(new Properties(), BlockTags.MINEABLE_WITH_AXE, 7f, -2.6f, 14f));
	}

	@Override
	public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
		return !state.is(BlockTags.NEEDS_STONE_TOOL) && !state.is(BlockTags.NEEDS_IRON_TOOL) && !state.is(BlockTags.NEEDS_DIAMOND_TOOL);
	}

	@Override
	public float getDestroySpeed(ItemStack stack, BlockState state) {
		return 14f;
	}

	@Override
	public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
		stack.hurtAndBreak(1, entity, EquipmentSlot.MAINHAND);
		return true;
	}

	@Override
	public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		stack.hurtAndBreak(2, attacker, EquipmentSlot.MAINHAND);
		GoldenScytheLivingEntityIsHitWithToolProcedure.execute(target.level(), target.getX(), target.getY(), target.getZ(), target, attacker);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, tooltip, flag);
		tooltip.accept(Component.translatable("item.better_with_gold.reinforced_golden_scythe.description_0"));
	}
}
