package tn.nightbeam.better_with_gold.registry;

import tn.nightbeam.better_with_gold.Constants;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NeoForgeModTabs {

	private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, Constants.MOD_ID);

	private NeoForgeModTabs() {
	}

	public static void register(IEventBus bus) {
		ModCreativeTabs.register((id, factory) -> TABS.register(id.getPath(), factory).get());
		TABS.register(bus);
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
