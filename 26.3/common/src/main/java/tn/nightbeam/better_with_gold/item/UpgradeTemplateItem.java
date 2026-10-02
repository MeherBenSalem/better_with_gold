package tn.nightbeam.better_with_gold.item;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;

public class UpgradeTemplateItem extends Item {
	public UpgradeTemplateItem(Item.Properties properties) {
		super(properties.stacksTo(1).rarity(Rarity.EPIC));
	}
}