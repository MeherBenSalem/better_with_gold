/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package tn.nightbeam.better_with_gold.init;

import tn.nightbeam.better_with_gold.item.UpgradeTemplateItem;
import tn.nightbeam.better_with_gold.item.ReinforcedGreaterGoldenHammerItem;
import tn.nightbeam.better_with_gold.item.ReinforcedGreaterGoldenAxeItem;
import tn.nightbeam.better_with_gold.item.ReinforcedGoldenSwordItem;
import tn.nightbeam.better_with_gold.item.ReinforcedGoldenShovelItem;
import tn.nightbeam.better_with_gold.item.ReinforcedGoldenScytheItem;
import tn.nightbeam.better_with_gold.item.ReinforcedGoldenPickAxeItem;
import tn.nightbeam.better_with_gold.item.ReinforcedGoldenHoeItem;
import tn.nightbeam.better_with_gold.item.ReinforcedGoldenAxeItem;
import tn.nightbeam.better_with_gold.item.ReinforcedGoldenArmorItem;
import tn.nightbeam.better_with_gold.item.GreaterGoldenHammerItem;
import tn.nightbeam.better_with_gold.item.GreaterGoldenAxeItem;
import tn.nightbeam.better_with_gold.item.GoldenScytheItem;
import tn.nightbeam.better_with_gold.item.GoldenFishItem;
import tn.nightbeam.better_with_gold.BetterWithGoldMod;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.world.item.Item;

public class BetterWithGoldModItems {
	public static final DeferredRegister<Item> REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, BetterWithGoldMod.MODID);
	public static final RegistryObject<Item> UPGRADE_TEMPLATE = REGISTRY.register("upgrade_template", () -> new UpgradeTemplateItem());
	public static final RegistryObject<Item> REINFORCED_GOLDEN_PICK_AXE = REGISTRY.register("reinforced_golden_pick_axe", () -> new ReinforcedGoldenPickAxeItem());
	public static final RegistryObject<Item> REINFORCED_GOLDEN_AXE = REGISTRY.register("reinforced_golden_axe", () -> new ReinforcedGoldenAxeItem());
	public static final RegistryObject<Item> REINFORCED_GOLDEN_SHOVEL = REGISTRY.register("reinforced_golden_shovel", () -> new ReinforcedGoldenShovelItem());
	public static final RegistryObject<Item> REINFORCED_GOLDEN_HOE = REGISTRY.register("reinforced_golden_hoe", () -> new ReinforcedGoldenHoeItem());
	public static final RegistryObject<Item> REINFORCED_GOLDEN_SWORD = REGISTRY.register("reinforced_golden_sword", () -> new ReinforcedGoldenSwordItem());
	public static final RegistryObject<Item> REINFORCED_GOLDEN_ARMOR_HELMET = REGISTRY.register("reinforced_golden_armor_helmet", () -> new ReinforcedGoldenArmorItem.Helmet());
	public static final RegistryObject<Item> REINFORCED_GOLDEN_ARMOR_CHESTPLATE = REGISTRY.register("reinforced_golden_armor_chestplate", () -> new ReinforcedGoldenArmorItem.Chestplate());
	public static final RegistryObject<Item> REINFORCED_GOLDEN_ARMOR_LEGGINGS = REGISTRY.register("reinforced_golden_armor_leggings", () -> new ReinforcedGoldenArmorItem.Leggings());
	public static final RegistryObject<Item> REINFORCED_GOLDEN_ARMOR_BOOTS = REGISTRY.register("reinforced_golden_armor_boots", () -> new ReinforcedGoldenArmorItem.Boots());
	public static final RegistryObject<Item> GOLDEN_SCYTHE = REGISTRY.register("golden_scythe", () -> new GoldenScytheItem());
	public static final RegistryObject<Item> GOLDEN_FISH = REGISTRY.register("golden_fish", () -> new GoldenFishItem());
	public static final RegistryObject<Item> GREATER_GOLDEN_AXE = REGISTRY.register("greater_golden_axe", () -> new GreaterGoldenAxeItem());
	public static final RegistryObject<Item> GREATER_GOLDEN_HAMMER = REGISTRY.register("greater_golden_hammer", () -> new GreaterGoldenHammerItem());
	public static final RegistryObject<Item> REINFORCED_GOLDEN_SCYTHE = REGISTRY.register("reinforced_golden_scythe", () -> new ReinforcedGoldenScytheItem());
	public static final RegistryObject<Item> REINFORCED_GREATER_GOLDEN_AXE = REGISTRY.register("reinforced_greater_golden_axe", () -> new ReinforcedGreaterGoldenAxeItem());
	public static final RegistryObject<Item> REINFORCED_GREATER_GOLDEN_HAMMER = REGISTRY.register("reinforced_greater_golden_hammer", () -> new ReinforcedGreaterGoldenHammerItem());
	// Start of user code block custom items
	// End of user code block custom items
}