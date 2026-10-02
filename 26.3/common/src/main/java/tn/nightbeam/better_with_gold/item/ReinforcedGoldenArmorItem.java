package tn.nightbeam.better_with_gold.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.equipment.ArmorType;

public abstract class ReinforcedGoldenArmorItem extends Item {

	private static final ArmorMaterial GOLD = ArmorMaterials.GOLD;
	private static final ArmorMaterial REINFORCED = new ArmorMaterial(GOLD.durability(), GOLD.defense(),
			GOLD.enchantmentValue(), GOLD.equipSound(), GOLD.toughness(), GOLD.knockbackResistance(),
			GOLD.repairIngredient(), ResourceKey.create(EquipmentAssets.ROOT_ID,
					Identifier.fromNamespaceAndPath("better_with_gold", "reinforced_golden_armor")));

	protected ReinforcedGoldenArmorItem(Item.Properties properties, ArmorType type) {
		super(properties.humanoidArmor(REINFORCED, type));
	}

	public static class Helmet extends ReinforcedGoldenArmorItem {
		public Helmet(Item.Properties properties) {
			super(properties, ArmorType.HELMET);
		}
	}

	public static class Chestplate extends ReinforcedGoldenArmorItem {
		public Chestplate(Item.Properties properties) {
			super(properties, ArmorType.CHESTPLATE);
		}
	}

	public static class Leggings extends ReinforcedGoldenArmorItem {
		public Leggings(Item.Properties properties) {
			super(properties, ArmorType.LEGGINGS);
		}
	}

	public static class Boots extends ReinforcedGoldenArmorItem {
		public Boots(Item.Properties properties) {
			super(properties, ArmorType.BOOTS);
		}
	}
}
