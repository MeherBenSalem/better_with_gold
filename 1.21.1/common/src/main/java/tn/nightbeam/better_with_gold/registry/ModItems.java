package tn.nightbeam.better_with_gold.registry;

import tn.nightbeam.better_with_gold.Constants;
import tn.nightbeam.better_with_gold.item.GoldenFishItem;
import tn.nightbeam.better_with_gold.item.GoldenScytheItem;
import tn.nightbeam.better_with_gold.item.GreaterGoldenAxeItem;
import tn.nightbeam.better_with_gold.item.GreaterGoldenHammerItem;
import tn.nightbeam.better_with_gold.item.ReinforcedGoldenArmorItem;
import tn.nightbeam.better_with_gold.item.ReinforcedGoldenAxeItem;
import tn.nightbeam.better_with_gold.item.ReinforcedGoldenHoeItem;
import tn.nightbeam.better_with_gold.item.ReinforcedGoldenPickAxeItem;
import tn.nightbeam.better_with_gold.item.ReinforcedGoldenScytheItem;
import tn.nightbeam.better_with_gold.item.ReinforcedGoldenShovelItem;
import tn.nightbeam.better_with_gold.item.ReinforcedGoldenSwordItem;
import tn.nightbeam.better_with_gold.item.ReinforcedGreaterGoldenAxeItem;
import tn.nightbeam.better_with_gold.item.ReinforcedGreaterGoldenHammerItem;
import tn.nightbeam.better_with_gold.item.UpgradeTemplateItem;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public final class ModItems {

	public static Item UPGRADE_TEMPLATE;
	public static Item REINFORCED_GOLDEN_PICK_AXE;
	public static Item REINFORCED_GOLDEN_AXE;
	public static Item REINFORCED_GOLDEN_SHOVEL;
	public static Item REINFORCED_GOLDEN_HOE;
	public static Item REINFORCED_GOLDEN_SWORD;
	public static Item REINFORCED_GOLDEN_ARMOR_HELMET;
	public static Item REINFORCED_GOLDEN_ARMOR_CHESTPLATE;
	public static Item REINFORCED_GOLDEN_ARMOR_LEGGINGS;
	public static Item REINFORCED_GOLDEN_ARMOR_BOOTS;
	public static Item GOLDEN_SCYTHE;
	public static Item GOLDEN_FISH;
	public static Item GREATER_GOLDEN_AXE;
	public static Item GREATER_GOLDEN_HAMMER;
	public static Item REINFORCED_GOLDEN_SCYTHE;
	public static Item REINFORCED_GREATER_GOLDEN_AXE;
	public static Item REINFORCED_GREATER_GOLDEN_HAMMER;

	private ModItems() {
	}

	public static void register(ItemRegistrar registrar) {
		UPGRADE_TEMPLATE = registrar.register(id("upgrade_template"), UpgradeTemplateItem::new);
		REINFORCED_GOLDEN_PICK_AXE = registrar.register(id("reinforced_golden_pick_axe"), ReinforcedGoldenPickAxeItem::new);
		REINFORCED_GOLDEN_AXE = registrar.register(id("reinforced_golden_axe"), ReinforcedGoldenAxeItem::new);
		REINFORCED_GOLDEN_SHOVEL = registrar.register(id("reinforced_golden_shovel"), ReinforcedGoldenShovelItem::new);
		REINFORCED_GOLDEN_HOE = registrar.register(id("reinforced_golden_hoe"), ReinforcedGoldenHoeItem::new);
		REINFORCED_GOLDEN_SWORD = registrar.register(id("reinforced_golden_sword"), ReinforcedGoldenSwordItem::new);
		REINFORCED_GOLDEN_ARMOR_HELMET = registrar.register(id("reinforced_golden_armor_helmet"), ReinforcedGoldenArmorItem.Helmet::new);
		REINFORCED_GOLDEN_ARMOR_CHESTPLATE = registrar.register(id("reinforced_golden_armor_chestplate"), ReinforcedGoldenArmorItem.Chestplate::new);
		REINFORCED_GOLDEN_ARMOR_LEGGINGS = registrar.register(id("reinforced_golden_armor_leggings"), ReinforcedGoldenArmorItem.Leggings::new);
		REINFORCED_GOLDEN_ARMOR_BOOTS = registrar.register(id("reinforced_golden_armor_boots"), ReinforcedGoldenArmorItem.Boots::new);
		GOLDEN_SCYTHE = registrar.register(id("golden_scythe"), GoldenScytheItem::new);
		GOLDEN_FISH = registrar.register(id("golden_fish"), GoldenFishItem::new);
		GREATER_GOLDEN_AXE = registrar.register(id("greater_golden_axe"), GreaterGoldenAxeItem::new);
		GREATER_GOLDEN_HAMMER = registrar.register(id("greater_golden_hammer"), GreaterGoldenHammerItem::new);
		REINFORCED_GOLDEN_SCYTHE = registrar.register(id("reinforced_golden_scythe"), ReinforcedGoldenScytheItem::new);
		REINFORCED_GREATER_GOLDEN_AXE = registrar.register(id("reinforced_greater_golden_axe"), ReinforcedGreaterGoldenAxeItem::new);
		REINFORCED_GREATER_GOLDEN_HAMMER = registrar.register(id("reinforced_greater_golden_hammer"), ReinforcedGreaterGoldenHammerItem::new);
	}

	private static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path);
	}
}
