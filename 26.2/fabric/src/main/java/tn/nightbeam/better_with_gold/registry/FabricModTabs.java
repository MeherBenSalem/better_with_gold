package tn.nightbeam.better_with_gold.registry;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTabs;

public final class FabricModTabs {

	private FabricModTabs() {
	}

	public static void register() {
		ModCreativeTabs.register((id, factory) -> Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id, factory.get()));

		CreativeModeTabEvents.modifyOutputEvent(ModCreativeTabs.TAB_KEY).register(output -> ModCreativeTabs.addItems(output::accept));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> output.accept(ModItems.UPGRADE_TEMPLATE));
	}
}
