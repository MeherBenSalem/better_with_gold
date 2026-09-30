package tn.nightbeam.better_with_gold.registry;

import tn.nightbeam.better_with_gold.Constants;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NeoForgeModItems {

	private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

	private NeoForgeModItems() {
	}

	public static void register(IEventBus bus) {
		ModItems.register((id, factory) -> ITEMS.register(id.getPath(), factory).get());
		ITEMS.register(bus);
	}
}
