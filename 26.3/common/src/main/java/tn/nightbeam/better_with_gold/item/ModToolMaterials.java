package tn.nightbeam.better_with_gold.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ToolMaterial;

public final class ModToolMaterials {

	public static final ToolMaterial GOLDEN_SCYTHE = material(100, 12f, 5f, 22);
	public static final ToolMaterial REINFORCED_GOLDEN = material(1640, 12f, 1f, 22);
	public static final ToolMaterial REINFORCED_GOLDEN_SCYTHE = material(1640, 14f, 7f, 22);
	public static final ToolMaterial GREATER_GOLDEN_AXE = material(600, 4f, 14f, 2);
	public static final ToolMaterial REINFORCED_GREATER_GOLDEN_AXE = material(1640, 14f, 8f, 22);

	private ModToolMaterials() {
	}

	private static ToolMaterial material(int durability, float speed, float attackDamage, int enchantment) {
		return new ToolMaterial(BlockTags.INCORRECT_FOR_GOLD_TOOL, durability, speed, attackDamage, enchantment, ToolMaterial.GOLD.repairItems());
	}
}
