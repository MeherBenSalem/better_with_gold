package tn.nightbeam.better_with_gold.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;

public final class FabricModItems {

	private FabricModItems() {
	}

	public static void register() {
		ModItems.register((id, factory) -> Registry.register(BuiltInRegistries.ITEM, id, factory.apply(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)))));
	}
}
