package tn.nightbeam.better_with_gold.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public final class ModTiers {

	public static final Tier GOLDEN_SCYTHE = tier(100, 12f, 5f, 22, Items.GOLD_INGOT);
	public static final Tier REINFORCED_GOLDEN = tier(1640, 12f, 1f, 22, Items.GOLD_INGOT);
	public static final Tier REINFORCED_GOLDEN_SCYTHE = tier(1640, 14f, 7f, 22, Items.GOLD_INGOT);
	public static final Tier GREATER_GOLDEN_AXE = tier(600, 4f, 14f, 2, Items.GOLD_BLOCK);
	public static final Tier REINFORCED_GREATER_GOLDEN_AXE = tier(1640, 14f, 8f, 22, Items.GOLD_BLOCK);

	private ModTiers() {
	}

	private static Tier tier(int uses, float speed, float attackDamage, int enchantment, net.minecraft.world.level.ItemLike repair) {
		return new Tier() {
			@Override
			public int getUses() {
				return uses;
			}

			@Override
			public float getSpeed() {
				return speed;
			}

			@Override
			public float getAttackDamageBonus() {
				return attackDamage;
			}

			@Override
			public TagKey<Block> getIncorrectBlocksForDrops() {
				return BlockTags.INCORRECT_FOR_GOLD_TOOL;
			}

			@Override
			public int getEnchantmentValue() {
				return enchantment;
			}

			@Override
			public Ingredient getRepairIngredient() {
				return Ingredient.of(new ItemStack(repair));
			}
		};
	}
}
