package tn.nightbeam.better_with_gold.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

public final class FabricModItems {

	private FabricModItems() {
	}

	public static void register() {
		ModItems.register((id, factory) -> Registry.register(BuiltInRegistries.ITEM, id, factory.get()));
	}
}
