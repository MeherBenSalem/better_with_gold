package tn.nightbeam.better_with_gold.item;

import tn.nightbeam.better_with_gold.procedures.GoldenScytheLivingEntityIsHitWithToolProcedure;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class ReinforcedGoldenScytheItem extends TieredItem {

	public ReinforcedGoldenScytheItem() {
		super(ModTiers.REINFORCED_GOLDEN_SCYTHE, new Item.Properties().attributes(createAttributes()));
	}

	private static ItemAttributeModifiers createAttributes() {
		return ItemAttributeModifiers.builder()
				.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 8.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
				.add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -2.6, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
				.build();
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
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		stack.hurtAndBreak(2, attacker, EquipmentSlot.MAINHAND);
		GoldenScytheLivingEntityIsHitWithToolProcedure.execute(target.level(), target.getX(), target.getY(), target.getZ(), target, attacker);
		return true;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);
		tooltip.add(Component.translatable("item.better_with_gold.reinforced_golden_scythe.description_0"));
	}
}
