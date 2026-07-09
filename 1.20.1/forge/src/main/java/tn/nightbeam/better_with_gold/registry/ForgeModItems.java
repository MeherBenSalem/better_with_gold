package tn.nightbeam.better_with_gold.registry;

import tn.nightbeam.better_with_gold.Constants;
import tn.nightbeam.better_with_gold.item.ForgeGoldenScytheItem;
import tn.nightbeam.better_with_gold.item.ForgeReinforcedGoldenArmorItem;
import tn.nightbeam.better_with_gold.item.ForgeReinforcedGoldenScytheItem;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public final class ForgeModItems {

	private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Constants.MOD_ID);

	private ForgeModItems() {
	}

	public static void register(IEventBus bus) {
		ModItems.register((id, factory) -> ITEMS.register(id.getPath(), resolveFactory(id.getPath(), factory)).get());
		ITEMS.register(bus);
	}

	private static Supplier<Item> resolveFactory(String path, Supplier<Item> defaultFactory) {
		return switch (path) {
			case "golden_scythe" -> ForgeGoldenScytheItem::new;
			case "reinforced_golden_scythe" -> ForgeReinforcedGoldenScytheItem::new;
			case "reinforced_golden_armor_helmet" -> ForgeReinforcedGoldenArmorItem.Helmet::new;
			case "reinforced_golden_armor_chestplate" -> ForgeReinforcedGoldenArmorItem.Chestplate::new;
			case "reinforced_golden_armor_leggings" -> ForgeReinforcedGoldenArmorItem.Leggings::new;
			case "reinforced_golden_armor_boots" -> ForgeReinforcedGoldenArmorItem.Boots::new;
			default -> defaultFactory;
		};
	}
}
