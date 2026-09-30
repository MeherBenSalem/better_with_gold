package tn.nightbeam.better_with_gold.item;

import net.minecraft.world.item.Item;

public class ReinforcedGoldenSwordItem extends Item {

	public ReinforcedGoldenSwordItem() {
		super(ModToolMaterials.REINFORCED_GOLDEN.applySwordProperties(new Properties(), 3f, -2.4f));
	}
}
