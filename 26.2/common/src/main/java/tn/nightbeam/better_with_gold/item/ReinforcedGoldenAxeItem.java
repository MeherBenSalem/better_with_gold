package tn.nightbeam.better_with_gold.item;

import net.minecraft.world.item.Item;

public class ReinforcedGoldenAxeItem extends Item {

	public ReinforcedGoldenAxeItem(Item.Properties properties) {
		super(properties.axe(ModToolMaterials.REINFORCED_GOLDEN, 6f, -3.1f));
	}
}
