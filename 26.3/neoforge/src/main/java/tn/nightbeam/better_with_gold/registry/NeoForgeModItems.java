package tn.nightbeam.better_with_gold.registry;

import tn.nightbeam.better_with_gold.Constants;

import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.minecraft.core.registries.Registries;

public final class NeoForgeModItems {


	private NeoForgeModItems() {
	}

	public static void register(IEventBus bus) {
		bus.addListener((RegisterEvent event) -> {
			if (event.getRegistryKey().equals(Registries.ITEM)) {
				ModItems.register((id, factory) -> {
					Item item = factory.apply(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)));
					event.register(Registries.ITEM, id, () -> item);
					return item;
				});
			}
		});
	}
}
