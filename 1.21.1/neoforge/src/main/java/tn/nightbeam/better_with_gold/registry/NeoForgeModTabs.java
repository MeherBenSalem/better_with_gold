package tn.nightbeam.better_with_gold.registry;

import tn.nightbeam.better_with_gold.Constants;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = Constants.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class NeoForgeModTabs {

	private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, Constants.MOD_ID);

	private NeoForgeModTabs() {
	}

	public static void register(IEventBus bus) {
		ModCreativeTabs.register((id, factory) -> TABS.register(id.getPath(), factory).get());
		TABS.register(bus);
	}

	@SubscribeEvent
	public static void buildTabContentsVanilla(BuildCreativeModeTabContentsEvent event) {
		if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
			event.accept(new ItemStack(ModItems.UPGRADE_TEMPLATE));
		}
	}
}
