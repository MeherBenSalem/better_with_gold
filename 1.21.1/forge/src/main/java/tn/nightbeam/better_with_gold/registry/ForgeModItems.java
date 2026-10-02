package tn.nightbeam.better_with_gold.registry;

import tn.nightbeam.better_with_gold.Constants;
import tn.nightbeam.better_with_gold.item.ForgeGoldenScytheItem;
import tn.nightbeam.better_with_gold.item.ForgeReinforcedGoldenScytheItem;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraft.core.registries.Registries;

import java.util.function.Supplier;

public final class ForgeModItems {


	private ForgeModItems() {
	}

	public static void register(IEventBus bus) {
		bus.addListener((RegisterEvent event) -> {
			if (event.getRegistryKey().equals(Registries.ITEM)) {
				ModItems.register((id, factory) -> {
					Item item = resolveFactory(id.getPath(), factory).get();
					event.register(Registries.ITEM, id, () -> item);
					return item;
				});
			}
		});
	}

	private static Supplier<Item> resolveFactory(String path, Supplier<Item> defaultFactory) {
		return switch (path) {
			case "golden_scythe" -> ForgeGoldenScytheItem::new;
			case "reinforced_golden_scythe" -> ForgeReinforcedGoldenScytheItem::new;
			default -> defaultFactory;
		};
	}
}
