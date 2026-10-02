package tn.nightbeam.better_with_gold.item;

import net.minecraft.world.item.Item;

public class ReinforcedGoldenPickAxeItem extends Item {

	public ReinforcedGoldenPickAxeItem(Item.Properties properties) {
		super(properties.pickaxe(ModToolMaterials.REINFORCED_GOLDEN, 1f, -2.8f));
	}
}
