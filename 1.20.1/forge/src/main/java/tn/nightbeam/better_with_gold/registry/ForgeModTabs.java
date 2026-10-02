package tn.nightbeam.better_with_gold.registry;

import tn.nightbeam.better_with_gold.Constants;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraft.core.registries.Registries;

@Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ForgeModTabs {


	private ForgeModTabs() {
	}

	public static void register(IEventBus bus) {
		bus.addListener((RegisterEvent event) -> {
			if (event.getRegistryKey().equals(Registries.CREATIVE_MODE_TAB)) {
				ModCreativeTabs.register((id, factory) -> {
					CreativeModeTab tab = factory.get();
					event.register(Registries.CREATIVE_MODE_TAB, id, () -> tab);
					return tab;
				});
			}
		});
	}

	@SubscribeEvent
	public static void buildTabContentsVanilla(BuildCreativeModeTabContentsEvent event) {
		if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
			event.accept(new ItemStack(ModItems.UPGRADE_TEMPLATE));
		}
	}
}
