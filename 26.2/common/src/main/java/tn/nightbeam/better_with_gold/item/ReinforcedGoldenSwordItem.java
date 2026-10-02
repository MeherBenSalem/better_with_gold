package tn.nightbeam.better_with_gold.item;

import net.minecraft.world.item.Item;

public class ReinforcedGoldenSwordItem extends Item {

	public ReinforcedGoldenSwordItem(Item.Properties properties) {
		super(ModToolMaterials.REINFORCED_GOLDEN.applySwordProperties(properties, 3f, -2.4f));
	}
}
