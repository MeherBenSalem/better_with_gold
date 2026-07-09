package tn.nightbeam.better_with_gold.item;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;

public class UpgradeTemplateItem extends Item {
	public UpgradeTemplateItem() {
		super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
	}
}