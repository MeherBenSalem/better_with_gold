package tn.nightbeam.better_with_gold.item;

import tn.nightbeam.better_with_gold.procedures.GreaterGoldenHammerLivingEntityIsHitWithToolProcedure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class ReinforcedGreaterGoldenHammerItem extends Item {

	public ReinforcedGreaterGoldenHammerItem() {
		super(new Properties().durability(1640).attributes(createAttributes()));
	}

	private static ItemAttributeModifiers createAttributes() {
		return ItemAttributeModifiers.builder()
				.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 11.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
				.add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -2.9, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
				.build();
	}

	@Override
	public float getDestroySpeed(ItemStack stack, BlockState state) {
		return 1;
	}

	@Override
	public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
		stack.hurtAndBreak(1, entity, EquipmentSlot.MAINHAND);
		return true;
	}

	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		stack.hurtAndBreak(2, attacker, EquipmentSlot.MAINHAND);
		GreaterGoldenHammerLivingEntityIsHitWithToolProcedure.execute(target.level(), target, attacker);
		return true;
	}

	@Override
	public int getEnchantmentValue() {
		return 22;
	}
}
