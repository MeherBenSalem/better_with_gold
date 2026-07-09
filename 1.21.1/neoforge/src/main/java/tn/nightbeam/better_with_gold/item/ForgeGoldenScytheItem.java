package tn.nightbeam.better_with_gold.item;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

public class ForgeGoldenScytheItem extends GoldenScytheItem {

	@Override
	public boolean canPerformAction(ItemStack stack, ItemAbility toolAction) {
		return ItemAbilities.DEFAULT_AXE_ACTIONS.contains(toolAction)
				|| ItemAbilities.DEFAULT_HOE_ACTIONS.contains(toolAction)
				|| ItemAbilities.DEFAULT_SHOVEL_ACTIONS.contains(toolAction)
				|| ItemAbilities.DEFAULT_PICKAXE_ACTIONS.contains(toolAction)
				|| ItemAbilities.DEFAULT_SWORD_ACTIONS.contains(toolAction);
	}
}
