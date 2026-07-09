package tn.nightbeam.better_with_gold;

import tn.nightbeam.better_with_gold.registry.NeoForgeModItems;
import tn.nightbeam.better_with_gold.registry.NeoForgeModTabs;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class BetterWithGoldMod {

	public BetterWithGoldMod(IEventBus modBus) {
		NeoForgeModItems.register(modBus);
		NeoForgeModTabs.register(modBus);
		BetterWithGold.init();
	}
}
