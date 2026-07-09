package tn.nightbeam.better_with_gold.item;

import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;

import net.minecraft.world.item.ItemStack;

public class ForgeReinforcedGoldenScytheItem extends ReinforcedGoldenScytheItem {

	@Override
	public boolean canPerformAction(ItemStack stack, ToolAction toolAction) {
		return ToolActions.DEFAULT_AXE_ACTIONS.contains(toolAction)
				|| ToolActions.DEFAULT_HOE_ACTIONS.contains(toolAction)
				|| ToolActions.DEFAULT_SHOVEL_ACTIONS.contains(toolAction)
				|| ToolActions.DEFAULT_PICKAXE_ACTIONS.contains(toolAction)
				|| ToolActions.DEFAULT_SWORD_ACTIONS.contains(toolAction);
	}
}
