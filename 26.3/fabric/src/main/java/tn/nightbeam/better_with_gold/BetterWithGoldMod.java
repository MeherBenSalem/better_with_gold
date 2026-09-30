package tn.nightbeam.better_with_gold;

import tn.nightbeam.better_with_gold.registry.FabricModItems;
import tn.nightbeam.better_with_gold.registry.FabricModTabs;

import net.fabricmc.api.ModInitializer;

public class BetterWithGoldMod implements ModInitializer {

	@Override
	public void onInitialize() {
		FabricModItems.register();
		FabricModTabs.register();
		BetterWithGold.init();
	}
}
