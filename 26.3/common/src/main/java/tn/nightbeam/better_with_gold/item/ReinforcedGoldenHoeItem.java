package tn.nightbeam.better_with_gold.item;

import net.minecraft.world.item.Item;

public class ReinforcedGoldenHoeItem extends Item {

	public ReinforcedGoldenHoeItem(Item.Properties properties) {
		super(properties.hoe(ModToolMaterials.REINFORCED_GOLDEN, 0f, -3f));
	}
}
