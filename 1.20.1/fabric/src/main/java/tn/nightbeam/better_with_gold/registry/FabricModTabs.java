package tn.nightbeam.better_with_gold.registry;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTabs;

public final class FabricModTabs {

	private FabricModTabs() {
	}

	public static void register() {
		ModCreativeTabs.register((id, factory) -> Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id, factory.get()));

		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> entries.accept(ModItems.UPGRADE_TEMPLATE));
	}
}
