package tn.nightbeam.better_with_gold.item;

import net.minecraft.world.item.Item;

public class ReinforcedGoldenShovelItem extends Item {

	public ReinforcedGoldenShovelItem(Item.Properties properties) {
		super(properties.shovel(ModToolMaterials.REINFORCED_GOLDEN, 1.5f, -3f));
	}
}
