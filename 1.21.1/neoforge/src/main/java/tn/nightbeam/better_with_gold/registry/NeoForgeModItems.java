package tn.nightbeam.better_with_gold.registry;

import tn.nightbeam.better_with_gold.Constants;
import tn.nightbeam.better_with_gold.item.ForgeGoldenScytheItem;
import tn.nightbeam.better_with_gold.item.ForgeReinforcedGoldenScytheItem;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class NeoForgeModItems {

	private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

	private NeoForgeModItems() {
	}

	public static void register(IEventBus bus) {
		ModItems.register((id, factory) -> ITEMS.register(id.getPath(), resolveFactory(id.getPath(), factory)).get());
		ITEMS.register(bus);
	}

	private static Supplier<Item> resolveFactory(String path, Supplier<Item> defaultFactory) {
		return switch (path) {
			case "golden_scythe" -> ForgeGoldenScytheItem::new;
			case "reinforced_golden_scythe" -> ForgeReinforcedGoldenScytheItem::new;
			default -> defaultFactory;
		};
	}
}
