package tn.nightbeam.better_with_gold;

import tn.nightbeam.better_with_gold.registry.ForgeModItems;
import tn.nightbeam.better_with_gold.registry.ForgeModTabs;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Constants.MOD_ID)
public class BetterWithGoldMod {

	public BetterWithGoldMod() {
		var bus = FMLJavaModLoadingContext.get().getModEventBus();
		ForgeModItems.register(bus);
		ForgeModTabs.register(bus);
		BetterWithGold.init();
	}
}
