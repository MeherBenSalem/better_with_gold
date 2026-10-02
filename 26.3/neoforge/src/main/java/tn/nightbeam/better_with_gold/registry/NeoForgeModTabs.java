package tn.nightbeam.better_with_gold.registry;

import tn.nightbeam.better_with_gold.Constants;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

public final class NeoForgeModTabs {


	private NeoForgeModTabs() {
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
		bus.addListener(NeoForgeModTabs::buildTabContents);
		bus.addListener(NeoForgeModTabs::buildTabContentsVanilla);
	}

	private static void buildTabContents(BuildCreativeModeTabContentsEvent event) {
		if (event.getTabKey() == ModCreativeTabs.TAB_KEY) {
			ModCreativeTabs.addItems(stack -> event.accept(stack));
		}
	}

	private static void buildTabContentsVanilla(BuildCreativeModeTabContentsEvent event) {
		if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
			event.accept(new ItemStack(ModItems.UPGRADE_TEMPLATE));
		}
	}
}
